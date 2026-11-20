package com.amadeus.system;

import com.amadeus.common.core.constants.CacheConstants;
import com.amadeus.common.core.constants.Constants;
import com.amadeus.common.core.constants.HttpConstants;
import com.amadeus.common.core.constants.JwtConstants;
import com.amadeus.common.core.domain.LoginUser;
import com.amadeus.common.core.enums.ResultCode;
import com.amadeus.common.core.utils.JwtUtils;
import com.amadeus.common.core.utils.ThreadLocalUtil;
import com.amadeus.common.redis.service.RedisService;
import com.amadeus.common.security.handler.GlobalExceptionHandler;
import com.amadeus.common.security.interceptor.TokenInterceptor;
import com.amadeus.common.security.service.TokenService;
import com.amadeus.system.controller.SysUserController;
import com.amadeus.system.service.Impl.ISysUserServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SysUserLogoutTest {
    private static final String SECRET = "bG9nb3V0LXVuaXQtdGVzdC1zaWduaW5nLWtleS0wMTIzNDU2Nzg5";
    private static final String USER_KEY = "current-session";
    private final Map<String, LoginUser> sessions = new HashMap<>();
    private RedisService redisService;
    private ISysUserServiceImpl userService;
    private MockMvc http;
    private String token;

    @BeforeEach
    void setUp() {
        redisService = mock(RedisService.class);
        when(redisService.getCacheObject(anyString(), eq(LoginUser.class)))
                .thenAnswer(invocation -> sessions.get(invocation.getArgument(0)));
        when(redisService.deleteObject(anyString()))
                .thenAnswer(invocation -> sessions.remove(invocation.getArgument(0)) != null);
        TokenService tokenService = new TokenService();
        ReflectionTestUtils.setField(tokenService, "redisService", redisService);
        TokenInterceptor interceptor = new TokenInterceptor();
        ReflectionTestUtils.setField(interceptor, "tokenService", tokenService);
        ReflectionTestUtils.setField(interceptor, "secret", SECRET);
        userService = new ISysUserServiceImpl();
        ReflectionTestUtils.setField(userService, "tokenService", tokenService);
        SysUserController controller = new SysUserController();
        ReflectionTestUtils.setField(controller, "sysUserService", userService);
        http = MockMvcBuilders.standaloneSetup(controller)
                .addInterceptors(interceptor)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        token = createToken(USER_KEY, SECRET);
        sessions.put(CacheConstants.LOGIN_TOKEN_KEY + USER_KEY, new LoginUser());
    }

    @AfterEach
    void clearContext() {
        ThreadLocalUtil.remove();
    }

    @Test
    void revokesCurrentSessionAndRejectsTokenReuse() throws Exception {
        http.perform(post("/sysUser/logout").header(HttpConstants.AUTHENTICATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.SUCCESS.getCode()));

        assertThat(sessions).doesNotContainKey(CacheConstants.LOGIN_TOKEN_KEY + USER_KEY);
        assertThat(ThreadLocalUtil.get(Constants.USER_KEY, String.class)).isNull();
        http.perform(get("/sysUser/info").header(HttpConstants.AUTHENTICATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
        http.perform(post("/sysUser/logout").header(HttpConstants.AUTHENTICATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
        verify(redisService, times(1)).deleteObject(CacheConstants.LOGIN_TOKEN_KEY + USER_KEY);
    }

    @Test
    void ignoresClientSuppliedSessionKeyAndPreservesOtherLoginOfSameUser() throws Exception {
        String otherUserKey = "other-session";
        String otherToken = createToken(otherUserKey, SECRET);
        sessions.put(CacheConstants.LOGIN_TOKEN_KEY + otherUserKey, new LoginUser());

        http.perform(post("/sysUser/logout").param("userKey", otherUserKey)
                        .header(HttpConstants.AUTHENTICATION, "Bearer " + token))
                .andExpect(jsonPath("$.code").value(ResultCode.SUCCESS.getCode()));

        assertThat(sessions).containsKey(CacheConstants.LOGIN_TOKEN_KEY + otherUserKey);
        verify(redisService, never()).deleteObject(CacheConstants.LOGIN_TOKEN_KEY + otherUserKey);
        http.perform(post("/sysUser/logout").header(HttpConstants.AUTHENTICATION, "Bearer " + otherToken))
                .andExpect(jsonPath("$.code").value(ResultCode.SUCCESS.getCode()));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"invalid", "Bearer invalid.token.signature"})
    void rejectsMissingOrInvalidTokenWithoutDeletingSessions(String authorization) throws Exception {
        var request = post("/sysUser/logout");
        if (authorization != null) {
            request.header(HttpConstants.AUTHENTICATION, authorization);
        }
        http.perform(request).andExpect(status().isUnauthorized());
        verify(redisService, never()).deleteObject(anyString());
    }

    @Test
    void rejectsTokenSignedWithDifferentKey() throws Exception {
        String forgedToken = createToken(USER_KEY,
                "ZGlmZmVyZW50LXRlc3Qtc2lnbmluZy1rZXktMDEyMzQ1Njc4OQ==");
        http.perform(post("/sysUser/logout").header(HttpConstants.AUTHENTICATION, "Bearer " + forgedToken))
                .andExpect(status().isUnauthorized());
        verify(redisService, never()).deleteObject(anyString());
    }

    @Test
    void rejectsValidTokenWithoutRedisSession() throws Exception {
        sessions.clear();
        http.perform(post("/sysUser/logout").header(HttpConstants.AUTHENTICATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
        verify(redisService, never()).deleteObject(anyString());
    }

    @Test
    void returnsUnauthorizedWhenSessionDisappearsAfterAuthentication() throws Exception {
        doReturn(false).when(redisService).deleteObject(CacheConstants.LOGIN_TOKEN_KEY + USER_KEY);
        http.perform(post("/sysUser/logout").header(HttpConstants.AUTHENTICATION, "Bearer " + token))
                .andExpect(jsonPath("$.code").value(ResultCode.FAILED_UNAUTHORIZED.getCode()));
        assertThat(ThreadLocalUtil.get(Constants.USER_KEY, String.class)).isNull();
    }

    @Test
    void doesNotReportSuccessWhenRedisDeletionFails() throws Exception {
        doThrow(new IllegalStateException("Redis unavailable"))
                .when(redisService).deleteObject(CacheConstants.LOGIN_TOKEN_KEY + USER_KEY);
        http.perform(post("/sysUser/logout").header(HttpConstants.AUTHENTICATION, "Bearer " + token))
                .andExpect(jsonPath("$.code").value(ResultCode.ERROR.getCode()));
        assertThat(sessions).containsKey(CacheConstants.LOGIN_TOKEN_KEY + USER_KEY);
        assertThat(ThreadLocalUtil.get(Constants.USER_KEY, String.class)).isNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void rejectsMissingSessionKeyInDirectServiceCalls(String userKey) {
        assertThat(userService.logout(userKey).getCode()).isEqualTo(ResultCode.FAILED_UNAUTHORIZED.getCode());
        verify(redisService, never()).deleteObject(anyString());
    }

    private String createToken(String userKey, String secret) {
        return JwtUtils.createToken(Map.of(
                JwtConstants.LOGIN_USER_ID, 1L, JwtConstants.LOGIN_USER_KEY, userKey), secret);
    }
}
