package com.amadeus.system.service;

import com.amadeus.system.controller.LoginResult;

public interface ISysUserService {
    LoginResult login(String userAccount, String password);
}
