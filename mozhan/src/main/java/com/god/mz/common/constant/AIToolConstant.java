package com.god.mz.common.constant;

public interface AIToolConstant {

    String USER_ID = "userId";
    String REQUEST_ID = "requestId";

    interface Tools {
        String QUERY_ARTICLE = "根据文章关键词查询文章";
        String QUERY_ESSAY = "根据随笔关键词查询随笔";
    }

    interface ToolParams {
        String ARTICLE_KEYWORD = "文章关键词";
        String ESSAY_KEYWORD = "随笔关键词";
    }

    interface Memory {
        /**
         * 消息metadata中，工具结果附加参数的键名
         */
        String PARAMS_KEY = "params";
    }
}
