package com.amadeus.system.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
public class LoginDTO {

    @Schema(description = "登录用户名")
    private  String userAccount;

    @Schema(description = "登录密码")
    private String password;

}
