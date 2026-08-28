package com.god.mz.service.impl;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import com.god.mz.common.constant.AIToolConstant;
import com.god.mz.common.enums.ChatEventTypeEnum;
import com.god.mz.domain.vo.ai.ChatEventVO;
import com.god.mz.service.AIChatService;
import com.god.mz.util.ToolResultHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;


import java.util.Map;

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

    public static final ChatEventVO STOP_EVENT = ChatEventVO.builder()
            .eventType(ChatEventTypeEnum.STOP.getValue())
            .build();



    @Override
    public Flux<ChatEventVO> chat(String question, String sessionId) {

        //获取对话Id
        String conversationId = AIChatService.getConversationId(sessionId);
        //生成请求Id
        String requestId = IdUtil.simpleUUID();

        BoundHashOperations<String, Object, Object> hashOps = stringRedisTemplate.boundHashOps(CHAT_SESSION_GENERATE_STATUS_KEY);

        // 大模型输出内容的缓存器，用于在输出中断后的数据存储
        StringBuilder outputBuilder = new StringBuilder();

        return chatClient.prompt()
                .system(systemPrompt ->
                        systemPrompt.text(SYSTEM_PROMPT)
                )
                .user(question)
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))   //设置对话记忆中的对话id
                .toolContext(Map.of(AIToolConstant.REQUEST_ID, requestId))     //向工具中添加传递参数
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
                .concatWith(Flux.defer(() -> {
                    Map<String, Object> result = ToolResultHolder.get(requestId);
                    if (ObjectUtil.isNotEmpty(result)) {
                        ToolResultHolder.remove(requestId);

                        //工具被调用了，需要向前端传递参数
                        return Flux.just(ChatEventVO.builder()
                                .eventData(result)
                                .eventType(ChatEventTypeEnum.PARAM.getValue())
                                .build(), STOP_EVENT
                        );
                    }

                    return Flux.just(STOP_EVENT);
                }));

    }

    @Override
    public void stop(String sessionId) {
        BoundHashOperations<String, Object, Object> hashOps = stringRedisTemplate.boundHashOps(CHAT_SESSION_GENERATE_STATUS_KEY);
        String conversationId = AIChatService.getConversationId(sessionId);
        hashOps.delete(conversationId);
    }

    private void saveStopHistoryRecord(String conversationId, String content) {
        chatMemory.add(conversationId, new AssistantMessage(content));
    }
}
