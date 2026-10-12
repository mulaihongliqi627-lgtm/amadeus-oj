package com.amadeus.system.service.Impl;

import com.amadeus.system.controller.LoginResult;
import com.amadeus.system.controller.SysUserController;
import com.amadeus.system.domain.SysUser;
import com.amadeus.system.mapper.SysUserMapper;
import com.amadeus.system.service.ISysUserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ISysUserServiceImpl implements ISysUserService {


    @Autowired
    private SysUserMapper sysUserMapper;

    @Override
    public LoginResult login(String userAccount, String password) {
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        //从数据库中查询对应用户信息 == select password from tb_sys_user where user_account = ${userAccount};
        queryWrapper.select(SysUser::getPassword)
                .eq(SysUser::getUserAccount, userAccount);
        SysUser sysUser = sysUserMapper.selectOne(queryWrapper);
        LoginResult loginResult = new LoginResult();
        if(sysUser == null){
            loginResult.setCode(0);
            loginResult.setMsg("用户不存在");
            return loginResult;
        }else if(sysUser.getPassword().equals(password)){
            loginResult.setCode(1);
            loginResult.setMsg("用户登录成功");
            return loginResult;
        }else{
            loginResult.setCode(0);
            loginResult.setMsg("用户名或密码错误");
            return loginResult;
        }
    }
}
