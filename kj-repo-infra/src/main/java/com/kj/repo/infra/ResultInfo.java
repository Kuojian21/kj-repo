package com.kj.repo.infra;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * @author kj
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResultInfo<T> {

    public static final Integer CODE_SUCC = 200;
    public static final Integer CODE_FAIL = 400;

    private Integer code;
    private String msg;
    private T data;

    public static <T> ResultInfo<T> succ() {
        return succ(null);
    }

    public static <T> ResultInfo<T> succ(T data) {
        ResultInfo<T> result = new ResultInfo<T>();
        result.setCode(CODE_SUCC);
        result.setMsg("success");
        result.setData(data);
        return result;
    }

    public static <T> ResultInfo<T> fail(String msg) {
        return fail(msg, null);
    }

    public static <T> ResultInfo<T> fail(String msg, T data) {
        ResultInfo<T> result = new ResultInfo<T>();
        result.setCode(CODE_FAIL);
        result.setMsg(msg);
        result.setData(data);
        return result;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

}
