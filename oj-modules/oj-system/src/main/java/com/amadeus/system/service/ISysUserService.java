package com.amadeus.system.service;

import com.amadeus.common.core.domain.Result;
import com.amadeus.system.controller.LoginResult;

public interface ISysUserService {
    Result<Void> login(String userAccount, String password);
}
