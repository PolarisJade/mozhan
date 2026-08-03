package com.god.mz.common.enums;

import com.god.mz.common.constant.RedisConstant;
import lombok.Getter;

@Getter
public enum LikeTypeEnum {
    article(RedisConstant.ARTICLE_LIKE_KEY_PREFIX, RedisConstant.ARTICLE_LIKE_COUNT_KEY),
    essay(RedisConstant.ESSAY_LIKE_KEY_PREFIX, RedisConstant.ESSAY_LIKE_COUNT_KEY);

    private final String setKeyPrefix;
    private final String countKey;

    LikeTypeEnum(String setKeyPrefix, String countKey) {
        this.setKeyPrefix = setKeyPrefix;
        this.countKey = countKey;
    }

    public String getSetKey(Long targetId) {
        return setKeyPrefix + targetId;
    }
}
