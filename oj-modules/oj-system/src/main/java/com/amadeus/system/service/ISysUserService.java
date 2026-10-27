package com.amadeus.system.service;

import com.amadeus.common.core.domain.Result;

public interface ISysUserService {
    Result<String> login(String userAccount, String password);
}
