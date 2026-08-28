package com.god.mz.tool;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.god.mz.common.constant.AIToolConstant;
import com.god.mz.service.IArticleService;
import com.god.mz.tool.result.ArticleInfo;
import com.god.mz.util.ToolResultHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ArticleTools {

    private final IArticleService articleService;

    @Tool(description = AIToolConstant.Tools.QUERY_ARTICLE)
    public List<ArticleInfo> queryArticleByName(@ToolParam(description = AIToolConstant.ToolParams.ARTICLE_KEYWORD) String keyword, ToolContext toolContext) {
        return Optional.ofNullable(keyword)
                .map(articleService::queryArticleByName)
                .map(articleInfos -> {
                    String requestId = MapUtil.get(toolContext.getContext(), AIToolConstant.REQUEST_ID, String.class);
                    String field = StrUtil.lowerFirst(ArticleInfo.class.getSimpleName()) + "_" + keyword;
                    ToolResultHolder.put(requestId, field, articleInfos);
                    return articleInfos;
                })
                .orElse(null);
    }
}
