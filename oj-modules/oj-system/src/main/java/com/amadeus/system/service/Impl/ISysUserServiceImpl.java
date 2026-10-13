package com.amadeus.system.service.Impl;

import com.amadeus.common.core.domain.Result;
import com.amadeus.common.core.enums.ResultCode;
import com.amadeus.system.controller.LoginResult;
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
    public Result<Void> login(String userAccount, String password) {
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        //从数据库中查询对应用户信息 == select password from tb_sys_user where user_account = ${userAccount};
        queryWrapper.select(SysUser::getPassword)
                .eq(SysUser::getUserAccount, userAccount);
        SysUser sysUser = sysUserMapper.selectOne(queryWrapper);
        Result<Void> loginResult = new Result<>();
        if(sysUser == null){
            loginResult.setCode(ResultCode.FAILED_USER_NOT_EXISTS.getCode());
            loginResult.setMsg(ResultCode.FAILED_USER_NOT_EXISTS.getMsg());
            return loginResult;
        }else if(password.equals(sysUser.getPassword())){
            loginResult.setCode(ResultCode.SUCCESS.getCode());
            loginResult.setMsg(ResultCode.SUCCESS.getMsg());
            return loginResult;
        }else{
            loginResult.setCode(ResultCode.FAILED_LOGIN.getCode());
            loginResult.setMsg(ResultCode.FAILED_LOGIN.getMsg());
            return loginResult;
        }
    }
}
