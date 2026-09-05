package com.god.mz.service.impl;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.god.mz.common.enums.ChatEventTypeEnum;
import com.god.mz.domain.dto.aiWriter.AIWriterArticleDTO;
import com.god.mz.domain.dto.aiWriter.AIWriterMetaDTO;
import com.god.mz.domain.dto.aiWriter.AIWriterOutlineDTO;
import com.god.mz.domain.dto.aiWriter.AIWriterReviseDTO;
import com.god.mz.domain.vo.ai.ChatEventVO;
import com.god.mz.domain.vo.ai.aiWriter.ArticleMetaVO;
import com.god.mz.service.AIWriterService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class AIWriterServiceImpl implements AIWriterService {
    private final ChatClient writerChatClient;

    private static final String OUTLINE_PROMPT = ResourceUtil.readUtf8Str("prompt/writer-outline.txt");
    private static final String ARTICLE_PROMPT = ResourceUtil.readUtf8Str("prompt/writer-article.txt");
    private static final String REVISE_PROMPT = ResourceUtil.readUtf8Str("prompt/writer-revise.txt");
    private static final String META_PROMPT = ResourceUtil.readUtf8Str("prompt/writer-meta.txt");

    private static final ChatEventVO STOP_EVENT = ChatEventVO.builder()
            .eventType(ChatEventTypeEnum.STOP.getValue())
            .build();

    public AIWriterServiceImpl(@Qualifier("writerChatClient") ChatClient writerChatClient) {
        this.writerChatClient = writerChatClient;
    }

    @Override
    public String generateOutline(AIWriterOutlineDTO dto) {
        String userContent = StrUtil.format("""
                【文章主题】
                {}
    
                【写作要求】
                {}
                """, dto.getTopic(), StrUtil.blankToDefault(dto.getRequirement(), "无"));

        return writerChatClient.prompt()
                .system(system -> system.text(OUTLINE_PROMPT))
                .user(userContent)
                .call()
                .content();
    }

    @Override
    public Flux<ChatEventVO> generateArticle(AIWriterArticleDTO dto) {
        String userContent = StrUtil.format("""
                【文章主题】
                {}
    
                【文章大纲】
                {}
    
                【写作要求】
                {}
                """,
                dto.getTopic(),
                StrUtil.blankToDefault(dto.getOutline(), "（未提供大纲，请自行组织合理结构）"),
                StrUtil.blankToDefault(dto.getRequirement(), "无"));

        return stream(ARTICLE_PROMPT, userContent);
    }

    @Override
    public Flux<ChatEventVO> reviseArticle(AIWriterReviseDTO dto) {
        String userContent = StrUtil.format("""
                【当前文章正文（HTML）】
                {}
    
                【修改要求】
                {}
                """, dto.getCurrentContent(), dto.getInstruction());

        return stream(REVISE_PROMPT, userContent);
    }

    @Override
    public ArticleMetaVO generateMeta(AIWriterMetaDTO dto) {
        String result = writerChatClient.prompt()
                .system(system -> system.text(META_PROMPT))
                .user(dto.getContent())
                .call()
                .content();

        return JSONUtil.toBean(cleanJson(result), ArticleMetaVO.class);
    }

    /**
     * 统一的流式输出：将模型增量文本包装为 DATA 事件，结束时补一个 STOP 事件
     */
    private Flux<ChatEventVO> stream(String systemPrompt, String userContent) {
        return writerChatClient.prompt()
                .system(system -> system.text(systemPrompt))
                .user(userContent)
                .stream()
                .chatResponse()
                .map(response -> ChatEventVO.builder()
                        .eventData(response.getResult().getOutput().getText())
                        .eventType(ChatEventTypeEnum.DATA.getValue())
                        .build())
                .concatWith(Flux.just(STOP_EVENT));
    }

    /**
     * 去除模型可能包裹的 ```json 代码围栏，得到纯 JSON 字符串
     */
    private String cleanJson(String text) {
        if (StrUtil.isBlank(text)) {
            return "{}";
        }
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            trimmed = StrUtil.removePrefix(trimmed, "```json");
            trimmed = StrUtil.removePrefix(trimmed, "```");
            int end = trimmed.lastIndexOf("```");
            if (end >= 0) {
                trimmed = trimmed.substring(0, end);
            }
            trimmed = trimmed.trim();
        }
        return trimmed;
    }
}