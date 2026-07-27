package com.god.mz.exception;

import com.god.mz.common.enums.BizCodeEnum;
import lombok.Getter;

@Getter
public class BizException extends RuntimeException {
    private final Integer code;
    public BizException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(BizCodeEnum bizCodeEnum) {
        super(bizCodeEnum.getMessage());
        this.code = bizCodeEnum.getCode();
    }
}
