package com.god.mz.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum UserTypeEnum {
    ENABLE(1, "管理员"),
    DISABLE(0, "普通用户");
    @EnumValue
    private final Integer code;

    @JsonValue
    private final String desc;

    UserTypeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
