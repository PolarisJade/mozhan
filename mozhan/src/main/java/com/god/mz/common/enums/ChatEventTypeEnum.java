package com.god.mz.common.enums;

import lombok.Getter;

/**
 * 聊天消息事件类型
 */
@Getter
public enum ChatEventTypeEnum {
    DATA(1001, "数据事件"),
    STOP(1002, "停止事件"),
    PARAM(1003, "参数事件");

    private final int value;
    private final String desc;

    ChatEventTypeEnum(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    boolean equalsValue(Integer value){
        if (value == null) {
            return false;
        }
        return getValue() == value;
    }

    @Override
    public String toString() {
        return this.name();
    }
}
