package com.example.market.dto;

import lombok.Data;

@Data
public class Result<T> {

    private Integer code; // 0 成功, 其他失败
    private String msg;
    private T data;

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.setCode(0);
        r.setMsg("success");
        r.setData(data);
        return r;
    }

    public static <T> Result<T> fail(String msg) {
        Result<T> r = new Result<>();
        r.setCode(-1);
        r.setMsg(msg);
        return r;
    }
}

