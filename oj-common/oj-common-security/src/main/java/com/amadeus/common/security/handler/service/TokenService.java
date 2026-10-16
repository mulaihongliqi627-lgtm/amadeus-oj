package com.amadeus.common.security.handler.service;

import cn.hutool.core.lang.UUID;
import com.amadeus.common.core.constants.CacheConstants;
import com.amadeus.common.core.domain.LoginUser;
import com.amadeus.common.core.utils.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class TokenService {
    @Autowired
    private RedisService redisService;
    /**
     * 创建令牌
     */
    public String createToken(LoginUser loginUser) {
        String token = UUID.fastUUID().toString();
        loginUser.setToken(token);
        refreshToken(loginUser);// Jwt存储信息
        Map<String, Object> claimsMap = new HashMap<String, Object>();
        claimsMap.put(SecurityConstants.USER_KEY, token);
        claimsMap.put(SecurityConstants.DETAILS_USER_ID, loginUser.getUserId());
        claimsMap.put(SecurityConstants.DETAILS_NICKNAME, loginUser.getNickName());
        return JwtUtils.createToken(claimsMap);;
    }
    /**
     * 刷新令牌有效期
     *
     * @param loginUser 登录信息
     */
    public void refreshToken(LoginUser loginUser) {
// 根据uuid将loginUser缓存
        String userKey = getTokenKey(loginUser.getToken());
        redisService.setCacheObject(userKey, loginUser,
                CacheConstants.
                        EXPIRATION, TimeUnit.
                        MINUTES);
    }
    private String getTokenKey(String token) {
        return CacheConstants.
                LOGIN_TOKEN_KEY + token;
    }
}
