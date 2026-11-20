package com.amadeus.common.core.controller;

import com.amadeus.common.core.domain.Result;
import org.springframework.stereotype.Component;

@Component
public class BaseController {

    public Result<Void> toResult(int insert){
        return insert > 0 ? Result.ok() : Result.fail();
    }


    public Result<Void> toResult(boolean insert){
        return insert ? Result.ok() : Result.fail();
    }
}
