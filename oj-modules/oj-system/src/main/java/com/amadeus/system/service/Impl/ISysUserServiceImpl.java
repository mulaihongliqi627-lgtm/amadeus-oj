package com.amadeus.system.service.Impl;

import com.amadeus.common.core.domain.Result;
import com.amadeus.common.core.enums.ResultCode;
import com.amadeus.common.core.enums.UserIdentity;
import com.amadeus.common.security.exception.ServiceException;
import com.amadeus.common.security.service.TokenService;
import com.amadeus.system.domain.SysUser;
import com.amadeus.system.domain.SysUserSaveDTO;
import com.amadeus.system.domain.SysUserVO;
import com.amadeus.system.mapper.SysUserMapper;
import com.amadeus.system.service.ISysUserService;
import com.amadeus.system.utils.BCryptUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import static com.amadeus.common.core.enums.ResultCode.FAILED_USER_NOT_EXISTS;

@Slf4j
@Service
public class ISysUserServiceImpl implements ISysUserService {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private TokenService tokenService;

    @Value("${jwt.secret}")
    private String secret;

    @Override
    public Result<String> login(String userAccount, String password) {
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(SysUser::getUserId, SysUser::getPassword, SysUser::getNickName)
                .eq(SysUser::getUserAccount, userAccount);
        SysUser sysUser = sysUserMapper.selectOne(queryWrapper);
        if (sysUser == null) {
            return Result.fail(FAILED_USER_NOT_EXISTS);
        } else if (password != null && sysUser.getPassword() != null
                && BCryptUtils.matchesPassword(password, sysUser.getPassword())) {
            return Result.ok(tokenService.createToken(
                    sysUser.getUserId(), secret, UserIdentity.ADMIN.getValue(), sysUser.getNickName(), null));
        } else {
            return Result.fail(ResultCode.FAILED_LOGIN);
        }
    }

    @Override
    public int add(SysUserSaveDTO sysUserSaveDTO) {
        if (sysUserSaveDTO == null || sysUserSaveDTO.getUserAccount() == null
                || sysUserSaveDTO.getUserAccount().isBlank()
                || sysUserSaveDTO.getNickName() == null || sysUserSaveDTO.getNickName().isBlank()
                || sysUserSaveDTO.getPassword() == null || sysUserSaveDTO.getPassword().isBlank()) {
            throw new ServiceException(ResultCode.FAILED_PARAMS_VALIDATE);
        }
        Long existingUserCount = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUserAccount, sysUserSaveDTO.getUserAccount()));
        if (existingUserCount > 0) {
            throw new ServiceException(ResultCode.FAILED_USER_EXISTS);
        }
        SysUser sysUser = new SysUser();
        sysUser.setUserAccount(sysUserSaveDTO.getUserAccount());
        sysUser.setNickName(sysUserSaveDTO.getNickName());
        sysUser.setPassword(BCryptUtils.encryptPassword(sysUserSaveDTO.getPassword()));
        return sysUserMapper.insert(sysUser);
    }

    @Override
    public Result<SysUserVO> getUserInfo(Long userId) {
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(SysUser::getUserAccount, SysUser::getNickName)
                .eq(SysUser::getUserId, userId);
        SysUser sysUser = sysUserMapper.selectOne(queryWrapper);
        if (sysUser == null) {
            return Result.fail(FAILED_USER_NOT_EXISTS);
        }
        SysUserVO vo = new SysUserVO();
        vo.setUserAccount(sysUser.getUserAccount());
        vo.setNickName(sysUser.getNickName());
        return Result.ok(vo);
    }

    @Override
    public Result<Void> logout(String userKey) {
        return tokenService.deleteLoginUserByUserKey(userKey)
                ? Result.ok() : Result.fail(ResultCode.FAILED_UNAUTHORIZED);
    }
}
