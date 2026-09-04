package com.god.mz.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.stream.StreamUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.god.mz.common.constant.AIToolConstant;
import com.god.mz.common.enums.BizCodeEnum;
import com.god.mz.common.enums.MessageTypeEnum;
import com.god.mz.config.AISessionProperties;
import com.god.mz.domain.po.AiSession;
import com.god.mz.domain.query.cursorQuery.AISessionCursorQuery;
import com.god.mz.domain.query.cursorQuery.CursorPageVO;
import com.god.mz.domain.vo.ai.ChatSessionVO;
import com.god.mz.domain.vo.ai.MessageVO;
import com.god.mz.domain.vo.ai.SessionVO;
import com.god.mz.exception.BizException;
import com.god.mz.mapper.AiSessionMapper;
import com.god.mz.service.AIChatService;
import com.god.mz.service.IAISessionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.god.mz.util.CursorCodeUtil;
import com.god.mz.util.CursorQueryUtil;
import com.god.mz.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * AI会话表 服务实现类
 * </p>
 *
 * @author God
 * @since 2026-08-21
 */
@Service
@RequiredArgsConstructor
public class AISessionServiceImpl extends ServiceImpl<AiSessionMapper, AiSession> implements IAISessionService {

    private final AISessionProperties aiSessionProperties;
    private final ChatMemory chatMemory;
    private final ChatModel chatModel;

    private static final String TITLE_PROMPT = ResourceUtil.readUtf8Str("prompt/title-generation.txt");

    @Override
    public SessionVO createSession(Integer num) {
        SessionVO vo = BeanUtil.toBean(aiSessionProperties, SessionVO.class);
        // 随机获取examples
        vo.setExamples(RandomUtil.randomEleList(aiSessionProperties.getExamples(), num));

        // 查看是否已有刚新建的对话
        Long userId = UserContext.getUserId();
        AiSession existing = lambdaQuery()
                .eq(AiSession::getUserId, userId)
                .and(w -> w.isNull(AiSession::getTitle))
                .orderByAsc(AiSession::getId)
                .last("LIMIT 1")
                .one();
        if (existing != null) {
            // 已有未使用的会话，直接复用其sessionId
            vo.setSessionId(existing.getSessionId());
            return vo;
        }

        // 随机生成sessionId
        vo.setSessionId(IdUtil.simpleUUID());
        // 构建持久化对象，并持久化
        AiSession chatSession = AiSession.builder()
                .sessionId(vo.getSessionId())
                .userId(userId)
                .build();

        save(chatSession);

        return vo;
    }

    @Override
    public List<SessionVO.Example> getHotProblem(Integer num) {
        return RandomUtil.randomEleList(aiSessionProperties.getExamples(), num);
    }

    @Override
    public List<MessageVO> queryBySessionId(String sessionId) {
        //将sessionId转换为对话Id
        String conversationId = AIChatService.getConversationId(sessionId);

        //查询对话列表
        List<Message> messageList = chatMemory.get(conversationId);

        //转换为VO列表
        return StreamUtil.of(messageList)
                .filter(message -> message.getMessageType() == MessageType.USER ||
                        message.getMessageType() == MessageType.ASSISTANT)
                .map(message -> MessageVO.builder()
                        .type(MessageTypeEnum.valueOf(message.getMessageType().name()))
                        .content(message.getText())
                        .params(extractParams(message))
                        .build())
                .toList();

    }

    @Async
    @Override
    public void update(String sessionId, String title) {
        AiSession aiSession = lambdaQuery()
                .eq(AiSession::getSessionId, sessionId)
                .eq(AiSession::getUserId, UserContext.getUserId())
                .one();

        if (aiSession == null) {
            throw new BizException(BizCodeEnum.SESSION_NOT_EXIST);
        }

        if (StrUtil.isEmpty(aiSession.getTitle()) && StrUtil.isNotEmpty(title)) {
            String generatedTitle = generateTitleByAI(title);
            aiSession.setTitle(StrUtil.sub(generatedTitle, 0, 100));
        }

        updateById(aiSession);
    }

    @Override
    public void updateTitle(String sessionId, String title) {
        AiSession chatSession = lambdaQuery()
                .eq(AiSession::getSessionId, sessionId)
                .eq(AiSession::getUserId, UserContext.getUserId())
                .one();

        if (ObjectUtil.isEmpty(chatSession)) {
            throw new BizException(BizCodeEnum.SESSION_NOT_EXIST);
        }

        chatSession.setTitle(StrUtil.sub(title, 0, 100));
        updateById(chatSession);
    }

    @Override
    public void deleteHistorySession(String sessionId) {
        LambdaQueryWrapper<AiSession> queryWrapper = Wrappers.<AiSession>lambdaQuery()
                .eq(AiSession::getSessionId, sessionId)
                .eq(AiSession::getUserId, UserContext.getUserId());

        remove(queryWrapper);

        String conversationId = AIChatService.getConversationId(sessionId);
        chatMemory.clear(conversationId);
    }

    @Override
    public CursorPageVO<ChatSessionVO> queryHistorySession(AISessionCursorQuery query) {
        Long userId = UserContext.getUserId();
        Integer pageSize = query.getPageSize();
        String sortBy = query.getSortBy();

        QueryWrapper<AiSession> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId)
                .isNotNull("title")
                .ne("title", "");

        CursorQueryUtil.applyCursor(queryWrapper, query, "id");

        List<AiSession> sessionList = list(queryWrapper);

        boolean hasMore = sessionList.size() > pageSize;
        if (hasMore) {
            sessionList = sessionList.subList(0, pageSize);
        }
        if (sessionList.isEmpty()) {
            return new CursorPageVO<>(new ArrayList<>(), false, null);
        }

        List<ChatSessionVO> voList = sessionList.stream()
                .map(session -> ChatSessionVO.builder()
                        .sessionId(session.getSessionId())
                        .title(session.getTitle())
                        .updateTime(session.getUpdateTime())
                        .build())
                .toList();

        List<Long> cursors = CursorQueryUtil.getNextCursor(sessionList, sortBy, "id");
        return new CursorPageVO<>(voList, hasMore, CursorCodeUtil.encode(cursors));
    }

    /**
     * 从助手消息的metadata中提取工具结果参数
     *
     * @param message 对话消息
     * @return 工具结果参数，无则为null
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> extractParams(Message message) {
        if (message instanceof AssistantMessage assistantMessage) {
            Object params = assistantMessage.getMetadata().get(AIToolConstant.Memory.PARAMS_KEY);
            if (params instanceof Map) {
                return (Map<String, Object>) params;
            }
        }
        return null;
    }

    private String generateTitleByAI(String question) {
        try {
            Prompt prompt = new Prompt(List.of(
                    new SystemMessage(TITLE_PROMPT),
                    new UserMessage(question)
            ));
            String result = chatModel.call(prompt).getResult().getOutput().getText();
            return StrUtil.blankToDefault(result, question);
        } catch (Exception e) {
            return question;
        }
    }
}
