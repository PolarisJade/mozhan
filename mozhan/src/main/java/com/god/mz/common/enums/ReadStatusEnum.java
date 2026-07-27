package com.god.mz.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 消息读取状态枚举
 */
@Getter
public enum ReadStatusEnum {
    UNREAD(0, "未读"),
    READ(1, "已读");

    @EnumValue
    @JsonValue
    private final Integer code;
    private final String desc;

    ReadStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
