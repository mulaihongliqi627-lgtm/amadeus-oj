package com.amadeus.common.security.service;

import cn.hutool.core.lang.UUID;
import com.amadeus.common.core.constants.CacheConstants;
import com.amadeus.common.core.constants.JwtConstants;
import com.amadeus.common.core.domain.LoginUser;
import com.amadeus.common.core.utils.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import com.amadeus.common.redis.service.RedisService;

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
    public String createToken(Long userId, String secret, Integer identity) {
        Map<String, Object> claims = new HashMap<>();
        String userKey = UUID.fastUUID().toString();
        claims.put(JwtConstants.LOGIN_USER_ID, userId);
        claims.put(JwtConstants.LOGIN_USER_KEY, userKey);
        String token = JwtUtils.createToken(claims, secret);
        //第三方机制中存储敏感的信息

        //身份认证具体还要存储那些信息   redis 表明用户身份字段  identity  1  表示普通用户  2 ： 表示管理员用户  对象

        //使用什么样的数据结构  String  key value    String   hash  list  zset  set
        //key 必须保证唯一     便于维护  统一前缀：logintoken:userId   userId是通过雪花算法生成的
        //自增  管理员  C端用户   1
        //过期时间我们怎么记录  过期时间应该定多长。     720分钟   2~3小时

        String tokenKey = getTokenKey(userKey);
        //String tokenKey = "logintoken:" + sysUser.getUserId();
        LoginUser loginUser = new LoginUser();
        loginUser.setIdentity(identity);
//        loginUser.setNickName(nickName);
//        loginUser.setHeadImage(headImage);
        redisService.setCacheObject(tokenKey, loginUser, CacheConstants.EXPIRATION, TimeUnit.MINUTES);

        return token;
    }


    /**
     * 刷新令牌有效期
     */
    public void refreshLoginUser(String nickName, String headImage, String userKey) {
        String tokenKey = getTokenKey(userKey);
        LoginUser loginUser = redisService.getCacheObject(tokenKey, LoginUser.class);
        loginUser.setNickName(nickName);
        loginUser.setHeadImage(headImage);
        redisService.setCacheObject(tokenKey, loginUser);
    }

    private String getTokenKey(String userKey) {
        return CacheConstants.LOGIN_TOKEN_KEY + userKey;
    }
}
