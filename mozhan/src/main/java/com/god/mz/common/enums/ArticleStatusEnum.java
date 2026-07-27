package com.god.mz.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum ArticleStatusEnum {
    PUBLISHED(1, "发布"),
    DRAFT(0, "草稿");
    @EnumValue
    private final Integer code;

    @JsonValue
    private final String desc;

    ArticleStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
