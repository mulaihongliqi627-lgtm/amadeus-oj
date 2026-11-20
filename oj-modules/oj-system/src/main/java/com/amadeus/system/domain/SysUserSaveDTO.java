package com.amadeus.system.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class SysUserSaveDTO {

    @Schema(description = "用户账号")
    @NotBlank
    private String userAccount;

    @Schema(description = "用户昵称")
    @NotBlank(message = "用户昵称不能为空")
    private String nickName;

    @Schema(description = "用户密码")
    @NotBlank
    private String password;
}
