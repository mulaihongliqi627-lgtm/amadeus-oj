package com.amadeus.system.test.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@TableName("tb_test")
@Getter
@Setter
public class TestDomain {

    @TableId(value = "USER_ID", type = IdType.ASSIGN_ID)
    private String testId;

    private String title;

    private String content;
}
