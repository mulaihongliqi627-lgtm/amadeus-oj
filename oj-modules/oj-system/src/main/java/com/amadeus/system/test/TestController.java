package com.amadeus.system.test;

import com.amadeus.common.redis.service.RedisService;
import com.amadeus.system.domain.SysUser;
import com.amadeus.system.test.service.ITestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/test")
@Slf4j
public class TestController {

    @Autowired
    private ITestService testService;

    @Autowired
    private RedisService redisService;

    @RequestMapping("/list")
    public List<?> getList(){
        return testService.list();
    }

    @GetMapping("/testLog")
    public String testLog(){
        log.info("我是Info级别的日志");
        log.error("我是error级别的日志");
        log.warn("我是warn级别的日志");
        return "日志测试";
    }

    @GetMapping("/testRedis")
    public String testRedis(){
        SysUser sysUser = new SysUser();
        sysUser.setUserAccount("userAccount");
        redisService.setCacheObject("u",sysUser);
        SysUser getUser = redisService.getCacheObject("u",SysUser.class);
        return getUser.toString();
    }
}
