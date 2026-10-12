package com.amadeus.system.controller;

import com.amadeus.system.service.ISysUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class SysUserController {


    @Autowired
    private ISysUserService sysUserService;

    @RequestMapping("/login")
    public LoginResult login(String userAccount,String password){

        return sysUserService.login(userAccount,password);

    }
}
