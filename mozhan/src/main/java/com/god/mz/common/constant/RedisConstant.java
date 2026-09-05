package com.god.mz.common.constant;

public interface RedisConstant {
    String TOKEN_BLACKLIST_PREFIX = "mozhan:token:blacklist:";
    String USER_PROFILE_KEY_PREFIX = "mozhan:user:profile:";
    long USER_INFO_EXPIRE_HOURS = 24L;
    String CATEGORY_LIST = "mozhan:category:list";
    long DEFAULT_EXPIRE_HOURS = 168L;
    String STATISTIC_KEY = "mozhan:statistic";
    String ESSAY_LIKE_KEY_PREFIX = "mozhan:like:essay:set:";
    String ESSAY_LIKE_COUNT_KEY = "mozhan:like:essay:count";
    String ARTICLE_LIKE_KEY_PREFIX = "mozhan:like:article:set:";
    String ARTICLE_LIKE_COUNT_KEY = "mozhan:like:article:count";
    String HOT_ARTICLE_KEY = "mozhan:hot:article";

    String CHAT_SESSION_GENERATE_STATUS_KEY = "mozhan:ai:generate:status";
    String CHAT_MEMORY_PREFIX = "mozhan:ai:chat:memory:";
}
