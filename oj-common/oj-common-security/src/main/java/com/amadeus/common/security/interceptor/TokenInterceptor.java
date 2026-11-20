package com.amadeus.common.security.interceptor;

import cn.hutool.core.util.StrUtil;
import com.amadeus.common.core.constants.Constants;
import com.amadeus.common.core.constants.HttpConstants;
import com.amadeus.common.core.domain.LoginUser;
import com.amadeus.common.core.utils.ThreadLocalUtil;
import com.amadeus.common.security.service.TokenService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class TokenInterceptor implements HandlerInterceptor {

    @Autowired
    private TokenService tokenService;

    @Value("${jwt.secret}")
    private String secret;  //从哪个服务的配置文件中读取，取决于这个bean对象交给了哪个服务的spring容器进行管理。

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = getToken(request);  //请求头中获取token
        if (StrUtil.isEmpty(token)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "令牌不能为空");
            return false;
        }
        Claims claims = tokenService.getClaims(token, secret);
        if (claims == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "令牌已过期或验证不正确");
            return false;
        }

        Long userId = tokenService.getUserId(claims);
        String userKey = tokenService.getUserKey(claims);
        if (userId == null || StrUtil.isBlank(userKey)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "令牌验证失败");
            return false;
        }

        LoginUser loginUser = tokenService.getLoginUser(token, secret);
        if (loginUser == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "登录状态已过期");
            return false;
        }

        tokenService.extendToken(claims);
        ThreadLocalUtil.set(Constants.USER_ID, userId);
        ThreadLocalUtil.set(Constants.USER_KEY, userKey);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
            throws Exception {
        ThreadLocalUtil.remove();
    }


    private String getToken(HttpServletRequest request) {
        String token = request.getHeader(HttpConstants.AUTHENTICATION);
        if (StrUtil.isNotEmpty(token) && token.startsWith(HttpConstants.PREFIX)) {
            token = token.replaceFirst(HttpConstants.PREFIX, "");
        }
        return token;
    }
}
