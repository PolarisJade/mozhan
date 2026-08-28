package com.god.mz.service;

import com.god.mz.domain.vo.ai.ChatEventVO;
import com.god.mz.util.UserContext;
import reactor.core.publisher.Flux;

public interface AIChatService {

    /**
     * 获取对话id，规则：用户id_会话id
     *
     * @param sessionId 会话id
     * @return 对话id
     */
    static String getConversationId(String sessionId) {
        return UserContext.getUserId() + "_" + sessionId;
    }

    Flux<ChatEventVO> chat(String question, String sessionId);

    void stop(String sessionId);
}
