package com.amadeus.system.controller;

import com.amadeus.common.core.domain.Result;
import com.amadeus.system.domain.LoginDTO;
import com.amadeus.system.service.ISysUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequestMapping("/sysUser")
public class SysUserController {


    @Autowired
    private ISysUserService sysUserService;

    @RequestMapping("/login")
    public Result<Void> login(LoginDTO loginDTO){

        return sysUserService.login(loginDTO.getUserAccount(),loginDTO.getPassword());

    }
}
