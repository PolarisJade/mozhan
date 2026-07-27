package com.god.mz.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.god.mz.common.enums.BizCodeEnum;
import com.god.mz.common.enums.MessageStatusEnum;
import com.god.mz.common.enums.ReadStatusEnum;
import com.god.mz.domain.po.PrivateChatMessage;
import com.god.mz.domain.po.PrivateChatSession;
import com.god.mz.domain.vo.chat.ChatMessageVO;
import com.god.mz.domain.vo.chat.WebSocketMessageVO;
import com.god.mz.exception.BizException;
import com.god.mz.handler.ChatWebSocketHandler;
import com.god.mz.mapper.PrivateChatMessageMapper;
import com.god.mz.mapper.PrivateChatSessionMapper;
import com.god.mz.service.IPrivateChatMessageService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * <p>
 * Private chat message service implementation
 * </p>
 *
 * @author God
 * @since 2026-07-11
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PrivateChatMessageServiceImpl extends ServiceImpl<PrivateChatMessageMapper, PrivateChatMessage> implements IPrivateChatMessageService {

    private final ObjectMapper objectMapper;
    private final PrivateChatSessionMapper chatSessionMapper;
    private final PrivateChatMessageMapper messageMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recallMessage(Long userId, Long messageId) {
        PrivateChatMessage message = getById(messageId);
        if (message == null) {
            throw new BizException(BizCodeEnum.MESSAGE_NOT_EXIST);
        }

        if (!message.getSenderId().equals(userId)) {
            throw new BizException(BizCodeEnum.OPERATION_FAILURE);
        }

        long minutes = ChronoUnit.MINUTES.between(message.getCreateTime(), LocalDateTime.now());
        if (minutes > 2) {
            throw new BizException(BizCodeEnum.MESSAGE_RECALL_FAILURE);
        }

        message.setStatus(MessageStatusEnum.RECALLED);
        updateById(message);

        Long receiverId = message.getReceiverId();
        WebSocketSession receiverSession = ChatWebSocketHandler.getOnlineUserSession(receiverId);
        if (receiverSession != null && receiverSession.isOpen()) {
            try {
                receiverSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(
                        WebSocketMessageVO.success("RECALL_NOTICE", messageId))));
            } catch (IOException e) {
                log.error("Push recall notification failed", e);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markSessionAsRead(Long userId, Long sessionId) {
        PrivateChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BizException(BizCodeEnum.SESSION_NOT_EXIST);
        }

        if (!session.getUser1Id().equals(userId) && !session.getUser2Id().equals(userId)) {
            throw new BizException(BizCodeEnum.USER_NOT_AUTH);
        }

        // Determine the other party before the update
        Long otherUserId = session.getUser1Id().equals(userId) ? session.getUser2Id() : session.getUser1Id();

        lambdaUpdate()
                .eq(PrivateChatMessage::getSessionId, sessionId)
                .eq(PrivateChatMessage::getReceiverId, userId)
                .eq(PrivateChatMessage::getReadStatus, ReadStatusEnum.UNREAD)
                .set(PrivateChatMessage::getReadStatus, ReadStatusEnum.READ)
                .update();

        if (userId.equals(session.getUser1Id())) {
            chatSessionMapper.clearUser1UnreadCount(sessionId);
        } else {
            chatSessionMapper.clearUser2UnreadCount(sessionId);
        }

        // Push updated unread count to the reader
        Integer unreadCount = getUnreadCount(userId);
        WebSocketSession userSession = ChatWebSocketHandler.getOnlineUserSession(userId);
        if (userSession != null && userSession.isOpen()) {
            try {
                userSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(
                        WebSocketMessageVO.success("UNREAD_COUNT", unreadCount))));
            } catch (IOException e) {
                log.error("Push unread count failed", e);
            }
        }

        // Push MESSAGE_READ to the other party so their read receipts update in real-time
        WebSocketSession otherSession = ChatWebSocketHandler.getOnlineUserSession(otherUserId);
        if (otherSession != null && otherSession.isOpen()) {
            try {
                otherSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(
                        WebSocketMessageVO.success("MESSAGE_READ", sessionId))));
            } catch (IOException e) {
                log.error("Push MESSAGE_READ to other party failed", e);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ChatMessageVO sendMessage(Long senderId, Long receiverId, String content) {
        if (senderId.equals(receiverId)) {
            throw new BizException(BizCodeEnum.SEND_MESSAGE_ERROR);
        }

        Long user1Id = Math.min(senderId, receiverId);
        Long user2Id = Math.max(senderId, receiverId);

        PrivateChatSession session = chatSessionMapper.selectByUserPair(user1Id, user2Id);
        if (session == null) {
            session = new PrivateChatSession();
            session.setUser1Id(user1Id);
            session.setUser2Id(user2Id);
            session.setUser1UnreadCount(0);
            session.setUser2UnreadCount(0);
            chatSessionMapper.insert(session);
        }

        PrivateChatMessage message = new PrivateChatMessage();
        message.setSessionId(session.getId());
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setContent(content);
        message.setStatus(MessageStatusEnum.NORMAL);
        message.setReadStatus(ReadStatusEnum.UNREAD);
        messageMapper.insert(message);

        // Atomic SQL update: avoids concurrent read-modify-write race on unread counts
        Long lastMsgId = message.getId();
        if (receiverId.equals(user1Id)) {
            chatSessionMapper.incrementUser1UnreadCount(session.getId(), lastMsgId);
        } else {
            chatSessionMapper.incrementUser2UnreadCount(session.getId(), lastMsgId);
        }

        ChatMessageVO vo = new ChatMessageVO();
        vo.setMessageId(message.getId());
        vo.setSessionId(session.getId());
        vo.setSenderId(senderId);
        vo.setReceiverId(receiverId);
        vo.setContent(content);
        vo.setStatus(MessageStatusEnum.NORMAL);
        vo.setReadStatus(ReadStatusEnum.UNREAD);
        vo.setCreateTime(message.getCreateTime());

        WebSocketSession receiverSession = ChatWebSocketHandler.getOnlineUserSession(receiverId);
        if (receiverSession != null && receiverSession.isOpen()) {
            try {
                receiverSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(
                        WebSocketMessageVO.success("NEW_MESSAGE", vo))));
            } catch (IOException e) {
                log.error("Push message failed", e);
            }
        }
        return vo;
    }

    @Override
    public Integer getUnreadCount(Long userId) {
        return messageMapper.selectUnreadCount(userId);
    }
}
