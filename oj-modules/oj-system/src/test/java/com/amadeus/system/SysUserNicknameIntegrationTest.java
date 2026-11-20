package com.amadeus.system;

import com.amadeus.common.core.constants.CacheConstants;
import com.amadeus.common.core.constants.HttpConstants;
import com.amadeus.common.core.constants.JwtConstants;
import com.amadeus.common.core.domain.LoginUser;
import com.amadeus.common.core.enums.ResultCode;
import com.amadeus.common.core.enums.UserIdentity;
import com.amadeus.common.core.utils.JwtUtils;
import com.amadeus.common.redis.service.RedisService;
import com.amadeus.common.security.exception.ServiceException;
import com.amadeus.common.security.service.TokenService;
import com.amadeus.system.domain.SysUserSaveDTO;
import com.amadeus.system.service.ISysUserService;
import com.amadeus.system.utils.BCryptUtils;
import com.fasterxml.jackson.databind.JsonNode;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.cloud.bootstrap.enabled=false",
        "spring.cloud.nacos.config.enabled=false",
        "spring.cloud.nacos.discovery.enabled=false",
        "spring.cloud.nacos.config.import-check.enabled=false",
        "spring.profiles.active=test",
        "springdoc.api-docs.enabled=false",
        "logging.config=classpath:logback-test.xml",
        "jwt.secret=" + SysUserNicknameIntegrationTest.SECRET
})
class SysUserNicknameIntegrationTest {
    // Test-only signing key; no application secrets or development data are used.
    static final String SECRET = "bmlja25hbWUtaW50ZWdyYXRpb24tdGVzdC1zaWduaW5nLWtleS0wMTIzNDU2Nzg5";
    private static final String ACCOUNT = "nickname_integration_admin";
    private static final String PASSWORD = "TestPassword123!";
    private static final String NICK_NAME = "昵称验证_管理员";

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("nickname_verification");

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7.0.15"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void dataSources(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @Autowired
    private TestRestTemplate http;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private TokenService tokenService;
    @Autowired
    private RedisService redisService;
    @Autowired
    private ISysUserService userService;

    private String adminToken;

    @BeforeEach
    void prepareIsolatedDatabase() {
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS tb_sys_user (
                    user_id BIGINT PRIMARY KEY,
                    user_account VARCHAR(255) NOT NULL UNIQUE,
                    nick_name VARCHAR(255),
                    password VARCHAR(255) NOT NULL,
                    create_by BIGINT,
                    create_time DATETIME,
                    update_by BIGINT,
                    update_time DATETIME
                )
                """);
        jdbc.update("DELETE FROM tb_sys_user");
        adminToken = tokenService.createToken(1L, SECRET, UserIdentity.ADMIN.getValue(), "测试操作员", null);
    }

    @Test
    void savesNicknameAndReturnsItFromSignedJwtAndRedisAfterLogin() {
        assertCode(post("/sysUser/add", request(NICK_NAME)), ResultCode.SUCCESS);
        Map<String, Object> stored = jdbc.queryForMap(
                "SELECT user_id, nick_name, password FROM tb_sys_user WHERE user_account = ?", ACCOUNT);
        assertThat(stored.get("nick_name")).isEqualTo(NICK_NAME);
        String encodedPassword = (String) stored.get("password");
        assertThat(encodedPassword).isNotEqualTo(PASSWORD);
        assertThat(BCryptUtils.matchesPassword(PASSWORD, encodedPassword)).isTrue();

        JsonNode response = post("/sysUser/login", Map.of("userAccount", ACCOUNT, "password", PASSWORD));
        assertCode(response, ResultCode.SUCCESS);
        String token = response.path("data").asText();
        Claims claims = JwtUtils.parseToken(token, SECRET);
        assertThat(JwtUtils.getNickName(claims)).isEqualTo(NICK_NAME);
        assertThat(tokenService.getNickName(tokenService.getClaims(token, SECRET))).isEqualTo(NICK_NAME);
        assertThat(tokenService.getUserId(claims)).isEqualTo(((Number) stored.get("user_id")).longValue());

        LoginUser loginUser = tokenService.getLoginUser(token, SECRET);
        assertThat(loginUser).isNotNull();
        assertThat(loginUser.getNickName()).isEqualTo(NICK_NAME);
        assertThat(loginUser.getIdentity()).isEqualTo(UserIdentity.ADMIN.getValue());
        String cacheKey = CacheConstants.LOGIN_TOKEN_KEY + tokenService.getUserKey(claims);
        assertThat(redisService.getExpire(cacheKey, TimeUnit.SECONDS))
                .isPositive().isLessThanOrEqualTo(TimeUnit.MINUTES.toSeconds(CacheConstants.EXP));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void rejectsMissingOrBlankNicknameBeforeInserting(String nickName) {
        assertCode(post("/sysUser/add", request(nickName)), ResultCode.FAILED_PARAMS_VALIDATE);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tb_sys_user", Long.class)).isZero();
    }

    @Test
    void rejectsDuplicateAccountWithoutOverwritingNickname() {
        assertCode(post("/sysUser/add", request(NICK_NAME)), ResultCode.SUCCESS);
        assertCode(post("/sysUser/add", request("另一个昵称")), ResultCode.FAILED_USER_EXISTS);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tb_sys_user", Long.class)).isEqualTo(1L);
        assertThat(jdbc.queryForObject("SELECT nick_name FROM tb_sys_user", String.class)).isEqualTo(NICK_NAME);
    }

    @Test
    void rejectsWrongPasswordWithoutIssuingToken() {
        assertCode(post("/sysUser/add", request(NICK_NAME)), ResultCode.SUCCESS);
        JsonNode response = post("/sysUser/login", Map.of("userAccount", ACCOUNT, "password", "incorrect"));
        assertCode(response, ResultCode.FAILED_LOGIN);
        assertThat(response.path("data").isNull()).isTrue();
    }

    @Test
    void returnsNullNicknameForLegacyToken() {
        String legacyToken = JwtUtils.createToken(Map.of(
                JwtConstants.LOGIN_USER_ID, 42L, JwtConstants.LOGIN_USER_KEY, "legacy-user-key"), SECRET);
        Claims claims = tokenService.getClaims(legacyToken, SECRET);
        assertThat(claims).isNotNull();
        assertThat(tokenService.getUserId(claims)).isEqualTo(42L);
        assertThat(tokenService.getNickName(claims)).isNull();
    }

    @Test
    void rejectsTokenSignedWithDifferentKey() {
        String token = JwtUtils.createToken(Map.of(JwtConstants.LOGIN_USER_NICK_NAME, NICK_NAME),
                "ZGlmZmVyZW50LXRlc3Qtc2lnbmluZy1rZXktMDEyMzQ1Njc4OQ==");
        assertThat(tokenService.getClaims(token, SECRET)).isNull();
        assertThat(tokenService.getLoginUser(token, SECRET)).isNull();
    }

    @Test
    void returnsNullNicknameForInvalidClaims() {
        assertThat(tokenService.getNickName(null)).isNull();
    }

    @Test
    void rejectsBlankNicknameInDirectServiceCalls() {
        SysUserSaveDTO dto = new SysUserSaveDTO();
        dto.setUserAccount(ACCOUNT);
        dto.setPassword(PASSWORD);
        dto.setNickName(" ");
        assertThatThrownBy(() -> userService.add(dto)).isInstanceOfSatisfying(ServiceException.class,
                exception -> assertThat(exception.getResultCode()).isEqualTo(ResultCode.FAILED_PARAMS_VALIDATE));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tb_sys_user", Long.class)).isZero();
    }

    @Test
    void rejectsMalformedJsonAsInvalidParameters() {
        assertCode(post("/sysUser/add", "{invalid"), ResultCode.FAILED_PARAMS_VALIDATE);
    }

    @Test
    void returnsCurrentDatabaseProfileWithoutExposingPasswordOrTrustingRequestedUserId() {
        assertCode(post("/sysUser/add", request(NICK_NAME)), ResultCode.SUCCESS);
        String token = post("/sysUser/login", Map.of("userAccount", ACCOUNT, "password", PASSWORD))
                .path("data").asText();
        String updatedNickname = "更新后的昵称";
        jdbc.update("UPDATE tb_sys_user SET nick_name = ? WHERE user_account = ?", updatedNickname, ACCOUNT);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        var response = http.exchange("/sysUser/info?userId=1", HttpMethod.GET,
                new HttpEntity<>(headers), JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertCode(response.getBody(), ResultCode.SUCCESS);
        JsonNode profile = response.getBody().path("data");
        assertThat(profile.size()).isEqualTo(2);
        assertThat(profile.path("userAccount").asText()).isEqualTo(ACCOUNT);
        assertThat(profile.path("nickName").asText()).isEqualTo(updatedNickname);
        assertThat(profile.has("password")).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"not-a-jwt", "Bearer invalid.token.signature"})
    void rejectsMissingOrInvalidTokenForCurrentUserInfo(String authorization) {
        HttpHeaders headers = new HttpHeaders();
        if (authorization != null) {
            headers.set(HttpConstants.AUTHENTICATION, authorization);
        }

        var response = http.exchange("/sysUser/info", HttpMethod.GET,
                new HttpEntity<>(headers), JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void rejectsRevokedSessionForCurrentUserInfo() {
        assertThat(tokenService.deleteLoginUser(adminToken, SECRET)).isTrue();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);

        var response = http.exchange("/sysUser/info", HttpMethod.GET,
                new HttpEntity<>(headers), JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void returnsUserNotFoundForValidSessionWhoseUserWasDeleted() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);

        var response = http.exchange("/sysUser/info", HttpMethod.GET,
                new HttpEntity<>(headers), JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertCode(response.getBody(), ResultCode.FAILED_USER_NOT_EXISTS);
    }

    @Test
    void logoutDeletesOnlyCurrentRedisSessionAndRejectsSubsequentRequests() {
        String otherToken = tokenService.createToken(1L, SECRET, UserIdentity.ADMIN.getValue(), NICK_NAME, null);
        String cacheKey = CacheConstants.LOGIN_TOKEN_KEY + tokenService.getUserKey(
                tokenService.getClaims(adminToken, SECRET));

        assertCode(post("/sysUser/logout", Map.of()), ResultCode.SUCCESS);

        assertThat(redisService.getCacheObject(cacheKey, LoginUser.class)).isNull();
        assertThat(tokenService.getLoginUser(adminToken, SECRET)).isNull();
        assertThat(tokenService.getLoginUser(otherToken, SECRET)).isNotNull();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        assertThat(http.exchange("/sysUser/info", HttpMethod.GET,
                new HttpEntity<>(headers), JsonNode.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(http.exchange("/sysUser/logout", HttpMethod.POST,
                new HttpEntity<>(headers), JsonNode.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private Map<String, String> request(String nickName) {
        Map<String, String> body = new HashMap<>();
        body.put("userAccount", ACCOUNT);
        body.put("password", PASSWORD);
        if (nickName != null) {
            body.put("nickName", nickName);
        }
        return body;
    }

    private JsonNode post(String path, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpConstants.AUTHENTICATION, HttpConstants.PREFIX + adminToken);
        return http.postForObject(path, new HttpEntity<>(body, headers), JsonNode.class);
    }

    private void assertCode(JsonNode response, ResultCode expected) {
        assertThat(response).isNotNull();
        assertThat(response.path("code").asInt()).as("response: %s", response).isEqualTo(expected.getCode());
    }
}
