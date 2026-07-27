package com.god.mz.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum UserStatusEnum {
    ENABLE(1, "启用"),
    DISABLE(0, "禁用");
    @EnumValue
    private final Integer code;

    @JsonValue
    private final String desc;

    UserStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
