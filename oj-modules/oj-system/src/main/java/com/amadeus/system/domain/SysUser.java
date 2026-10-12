package com.amadeus.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;


@TableName("tb_sys_user")
@Getter
@Setter
public class SysUser {
    @TableId(value = "user_id", type = IdType.ASSIGN_ID)
    private Long userId;
    private String userAccount;
    private String password;
    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
}

