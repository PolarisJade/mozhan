package com.god.mz.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 消息状态枚举
 */
@Getter
public enum MessageStatusEnum {
    NORMAL(0, "正常"),
    RECALLED(1, "已撤回");

    @EnumValue
    @JsonValue
    private final Integer code;
    private final String desc;

    MessageStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
