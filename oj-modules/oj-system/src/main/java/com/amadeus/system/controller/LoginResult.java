package com.amadeus.system.controller;

import lombok.Data;

@Data
public class LoginResult {

    /**
     * 登录信息
     */
    private String msg;

    /**
     * 0 : 登录失败
     * 1 : 登录成功
     */
    private int code;
}
