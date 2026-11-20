package com.amadeus.system;

import com.amadeus.common.core.constants.CacheConstants;
import com.amadeus.common.core.constants.JwtConstants;
import com.amadeus.common.core.domain.LoginUser;
import com.amadeus.common.core.domain.Result;
import com.amadeus.common.core.enums.ResultCode;
import com.amadeus.common.core.utils.JwtUtils;
import com.amadeus.common.redis.service.RedisService;
import com.amadeus.common.security.config.WebMvcConfig;
import com.amadeus.common.security.interceptor.TokenInterceptor;
import com.amadeus.system.controller.sysuser.SysUserController;
import com.amadeus.system.domain.sysuser.vo.SysUserVO;
import com.amadeus.system.service.ISysUserService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.ClassUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SysUserSecurityRegistrationTest {
    private static final String SECRET = "c2VjdXJpdHktcmVnaXN0cmF0aW9uLXRlc3Qtc2lnbmluZy1rZXktMDEyMzQ1Njc4OQ==";

    @Test
    void autoRegistersInterceptorAndProtectsUserEndpointsWithoutBlockingLogin() {
        RedisService redisService = mock(RedisService.class);
        ISysUserService userService = mock(ISysUserService.class);
        when(userService.login("admin", "password")).thenReturn(Result.ok("test-token"));

        contextRunner(redisService, userService).run(context -> {
            assertThat(context).hasSingleBean(WebMvcConfig.class).hasSingleBean(TokenInterceptor.class);
            MockMvc http = MockMvcBuilders.webAppContextSetup(context).build();

            http.perform(get("/sysUser/info")).andExpect(status().isUnauthorized());
            http.perform(post("/sysUser/logout")).andExpect(status().isUnauthorized());
            verifyNoInteractions(userService);
            http.perform(post("/sysUser/login").contentType("application/json")
                            .content("{\"userAccount\":\"admin\",\"password\":\"password\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ResultCode.SUCCESS.getCode()));
        });
    }

    @Test
    void registeredInterceptorPassesVerifiedUserIdToInfoController() {
        RedisService redisService = mock(RedisService.class);
        ISysUserService userService = mock(ISysUserService.class);
        String userKey = "registration-session";
        when(redisService.getCacheObject(CacheConstants.LOGIN_TOKEN_KEY + userKey, LoginUser.class))
                .thenReturn(new LoginUser());
        SysUserVO profile = new SysUserVO();
        profile.setUserAccount("admin");
        profile.setNickName("administrator");
        when(userService.getUserInfo(1L)).thenReturn(Result.ok(profile));
        String token = JwtUtils.createToken(Map.of(
                JwtConstants.LOGIN_USER_ID, 1L, JwtConstants.LOGIN_USER_KEY, userKey), SECRET);

        contextRunner(redisService, userService).run(context -> {
            MockMvc http = MockMvcBuilders.webAppContextSetup(context).build();
            http.perform(get("/sysUser/info").header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ResultCode.SUCCESS.getCode()))
                    .andExpect(jsonPath("$.data.userAccount").value("admin"));
            verify(userService).getUserInfo(1L);
        });
    }

    private WebApplicationContextRunner contextRunner(RedisService redisService, ISysUserService userService) {
        ClassLoader loader = getClass().getClassLoader();
        List<Class<?>> configurations = new ArrayList<>();
        configurations.add(WebMvcAutoConfiguration.class);
        for (String candidate : ImportCandidates.load(AutoConfiguration.class, loader)) {
            if (candidate.startsWith("com.amadeus.common.security.")) {
                configurations.add(ClassUtils.resolveClassName(candidate, loader));
            }
        }
        return new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(configurations.toArray(Class<?>[]::new)))
                .withPropertyValues("jwt.secret=" + SECRET)
                .withBean(RedisTemplate.class, () -> mock(RedisTemplate.class))
                .withBean(RedisService.class, () -> redisService)
                .withBean(ISysUserService.class, () -> userService)
                .withUserConfiguration(SysUserController.class);
    }
}
