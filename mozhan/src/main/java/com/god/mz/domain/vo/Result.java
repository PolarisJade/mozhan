package com.god.mz.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 统一返回结果类
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Result<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Integer code;

    private String message;

    private T data;


    public static <T> Result<T> success() {
        return new Result<>(200, "success", null);
    }

    /**
     * 成功返回（有数据）
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data);
    }

    /**
     * 成功返回（自定义消息）
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data);
    }

    /**
     * 失败返回
     */
    public static <T> Result<T> fail() {
        return new Result<>(500, "fail", null);
    }

    /**
     * 失败返回（自定义消息）
     */
    public static <T> Result<T> fail(String message) {
        return new Result<>(500, message, null);
    }

    /**
     * 失败返回（自定义状态码和消息）
     */
    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<>(code, message, null);
    }
}
