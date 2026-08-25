package com.god.mz.service.impl;

import cn.hutool.core.io.resource.ResourceUtil;
import com.god.mz.common.enums.ChatEventTypeEnum;
import com.god.mz.domain.vo.ai.ChatEventVO;
import com.god.mz.service.AIChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;


import static com.god.mz.common.constant.RedisConstant.CHAT_SESSION_GENERATE_STATUS_KEY;

@Service
@RequiredArgsConstructor
public class AIChatServiceImpl implements AIChatService {

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 小栈助手的系统提示词，从 resources/prompt/xiaozhan-system.txt 读取
     */
    private static final String SYSTEM_PROMPT = ResourceUtil.readUtf8Str("prompt/xiaozhan-system.txt");



    @Override
    public Flux<ChatEventVO> chat(String question, String sessionId) {

        //获取对话Id
        String conversationId = AIChatService.getConversationId(sessionId);

        BoundHashOperations<String, Object, Object> hashOps = stringRedisTemplate.boundHashOps(CHAT_SESSION_GENERATE_STATUS_KEY);

        // 大模型输出内容的缓存器，用于在输出中断后的数据存储
        StringBuilder outputBuilder = new StringBuilder();

        return chatClient.prompt()
                .system(systemPrompt ->
                        systemPrompt.text(SYSTEM_PROMPT)
                )
                .user(question)
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))   //设置对话记忆中的对话id
                .stream()
                .chatResponse()
                .doFirst(() -> hashOps.put(conversationId, "true"))    // 开始生成时，设置标识
                .doOnError(throwable -> hashOps.delete(conversationId))   //出现异常时，删除标识
                .doOnComplete(() -> hashOps.delete(conversationId))     //正常结束时，删除标识
                .doOnCancel(() -> saveStopHistoryRecord(conversationId, outputBuilder.toString()))    // 当输出被取消时，保存输出的内容到历史记录中
                .takeWhile(response -> Boolean.TRUE.equals(hashOps.hasKey(conversationId)))
                .map(chatResponse -> {
                    String text = chatResponse.getResult().getOutput().getText();

                    // 累加输出内容
                    outputBuilder.append(text);

                    return ChatEventVO.builder()
                            .eventData(text)
                            .eventType(ChatEventTypeEnum.DATA.getValue())
                            .build();
                })
                .concatWith(Flux.just(ChatEventVO.builder()
                        .eventData(null)
                        .eventType(ChatEventTypeEnum.STOP.getValue())
                        .build()));

    }

    private void saveStopHistoryRecord(String conversationId, String content) {
        chatMemory.add(conversationId, new AssistantMessage(content));
    }
}
