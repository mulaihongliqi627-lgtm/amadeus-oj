package com.amadeus.common.core.domain;

import com.amadeus.common.core.enums.ResultCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class Result <T>{

    private int code;  //定义一些固定的code，前后端商量好的   0  1  请求成功  常量  2  3  枚举

    private String msg;  //?  通常是code的辅助说明  一个code  对应一个msg

    private T data;  //请求某个接口返回的数据list  SysUser  泛型


    public static <T> Result<T> ok() {
        return assembleResult(null, ResultCode.SUCCESS);
    }

    public static <T> Result<T> ok(T data) {
        return assembleResult(data, ResultCode.SUCCESS);
    }

    public static <T> Result<T> fail() {
        return assembleResult(null, ResultCode.FAILED);
    }

    public static <T> Result<T> fail(int code, String msg) {
        return assembleResult(code, msg, null);
    }

    /**
     * 指定错误码
     *
     * @param resultCode 指定错误码
     * @param <T>
     * @return
     */
    public static <T> Result<T> fail(ResultCode resultCode) {
        return assembleResult(null, resultCode);
    }

    private static <T> Result<T> assembleResult(T data, ResultCode resultCode) {
        Result<T> r = new Result<>();
        r.setCode(resultCode.getCode());
        r.setData(data);
        r.setMsg(resultCode.getMsg());
        return r;
    }

    private static <T> Result<T> assembleResult(int code, String msg, T data) {
        Result<T> r = new Result<>();
        r.setCode(code);
        r.setData(data);
        r.setMsg(msg);
        return r;
    }

}
