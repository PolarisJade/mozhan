package com.god.mz.service;

import com.god.mz.domain.dto.aiWriter.AIWriterArticleDTO;
import com.god.mz.domain.dto.aiWriter.AIWriterMetaDTO;
import com.god.mz.domain.dto.aiWriter.AIWriterOutlineDTO;
import com.god.mz.domain.dto.aiWriter.AIWriterReviseDTO;
import com.god.mz.domain.vo.ai.ChatEventVO;
import com.god.mz.domain.vo.ai.aiWriter.ArticleMetaVO;
import reactor.core.publisher.Flux;

/**
AI 辅助写作服务（方案A：前端携带当前正文，后端无状态）
 */
public interface AIWriterService {
    /**
    生成文章大纲
     */
    String generateOutline(AIWriterOutlineDTO dto);

    /**
    根据主题/大纲流式生成 HTML 正文
     */
    Flux<ChatEventVO> generateArticle(AIWriterArticleDTO dto);

    /**
    在当前正文基础上，按修改要求流式生成修改后的 HTML 正文 */
    Flux<ChatEventVO> reviseArticle(AIWriterReviseDTO dto);

    /**
    根据正文生成标题与摘要 */
    ArticleMetaVO generateMeta(AIWriterMetaDTO dto);
}