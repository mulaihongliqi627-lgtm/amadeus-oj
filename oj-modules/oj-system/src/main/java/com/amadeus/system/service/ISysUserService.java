package com.amadeus.system.service;

import com.amadeus.common.core.domain.Result;
import com.amadeus.system.domain.sysuser.dto.SysUserSaveDTO;
import com.amadeus.system.domain.sysuser.vo.SysUserVO;

public interface ISysUserService {
    Result<String> login(String userAccount, String password);

    int add(SysUserSaveDTO sysUserSaveDTO);

    Result<SysUserVO> getUserInfo(Long userId);

    Result<Void> logout(String userKey);
}
