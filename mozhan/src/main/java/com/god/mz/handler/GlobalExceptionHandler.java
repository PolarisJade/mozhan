package com.god.mz.handler;

import com.god.mz.domain.vo.Result;
import com.god.mz.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public Result<String> handleBizException(BizException e) {
        return Result.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<String> handleException(Exception e) {
        log.error(e.getMessage());
        return Result.fail("服务器繁忙，请稍后再试！");
    }
}
