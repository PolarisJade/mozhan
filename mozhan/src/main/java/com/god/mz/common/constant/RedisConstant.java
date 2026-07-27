package com.god.mz.common.constant;

public interface RedisConstant {
    String TOKEN_BLACKLIST_PREFIX = "mozhan:token:blacklist:";
    String USER_PROFILE_KEY_PREFIX = "mozhan:user:profile:";
    long USER_INFO_EXPIRE_HOURS = 24L;
    String CATEGORY_LIST = "mozhan:category:list";
    long DEFAULT_EXPIRE_HOURS = 168L;
    String STATISTIC_KEY = "mozhan:statistic";
    String HOT_ARTICLE_KEY = "mozhan:hot:article";
}
