package com.god.mz.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.god.mz.common.enums.ChatMessageTypeEnum;
import com.god.mz.domain.dto.ChatMessageDTO;
import com.god.mz.domain.vo.chat.ChatMessageVO;
import com.god.mz.domain.vo.chat.WebSocketMessageVO;
import com.god.mz.service.IPrivateChatMessageService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final ConcurrentHashMap<Long, WebSocketSession> ONLINE_USERS = new ConcurrentHashMap<>();

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private IPrivateChatMessageService chatMessageService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            ONLINE_USERS.put(userId, session);
            log.info("User {} connected to WebSocket, online count: {}", userId, ONLINE_USERS.size());

            // Push current online user set to the newly connected user
            Set<Long> onlineUserIds = ONLINE_USERS.keySet();
            sendJson(session, WebSocketMessageVO.success("ONLINE_USERS", onlineUserIds));

            // Broadcast USER_ONLINE to all other online users
            broadcastExcept(userId, WebSocketMessageVO.success("USER_ONLINE", userId));
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        Long userId = (Long) session.getAttributes().get("userId");
        String payload = message.getPayload();
        log.debug("Received WebSocket message: {}", payload);

        try {
            ChatMessageDTO dto = objectMapper.readValue(payload, ChatMessageDTO.class);
            ChatMessageTypeEnum msgType;
            try {
                msgType = ChatMessageTypeEnum.valueOf(dto.getType());
            } catch (IllegalArgumentException e) {
                sendJson(session, WebSocketMessageVO.error("Unknown message type: " + dto.getType()));
                return;
            }

            switch (msgType) {
                case CHAT:
                    ChatMessageVO messageVO = chatMessageService.sendMessage(userId, dto.getReceiverId(), dto.getContent());
                    sendJson(session, WebSocketMessageVO.success("NEW_MESSAGE", messageVO));
                    // Push authoritative unread count to both sender and receiver
                    sendJsonToUser(dto.getReceiverId(), WebSocketMessageVO.success("UNREAD_COUNT", chatMessageService.getUnreadCount(dto.getReceiverId())));
                    sendJsonToUser(userId, WebSocketMessageVO.success("UNREAD_COUNT", chatMessageService.getUnreadCount(userId)));
                    break;
                case RECALL:
                    chatMessageService.recallMessage(userId, dto.getMessageId());
                    sendJson(session, WebSocketMessageVO.success("RECALL_NOTICE", dto.getMessageId()));
                    break;
                case READ:
                    chatMessageService.markSessionAsRead(userId, dto.getSessionId());
                    sendJson(session, WebSocketMessageVO.success("READ", dto.getSessionId()));
                    break;
                case PING:
                    sendJson(session, WebSocketMessageVO.success("PONG", System.currentTimeMillis()));
                    break;
                default:
                    sendJson(session, WebSocketMessageVO.error("Unknown message type"));
            }
        } catch (Exception e) {
            log.error("Failed to handle message", e);
            sendJson(session, WebSocketMessageVO.error(e.getMessage()));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            // Only remove if this session is still the current one (avoid removing reconnected session)
            WebSocketSession current = ONLINE_USERS.get(userId);
            if (current == session) {
                ONLINE_USERS.remove(userId);
                log.info("User {} disconnected from WebSocket, online count: {}", userId, ONLINE_USERS.size());
                // Broadcast USER_OFFLINE to all remaining online users
                broadcast(WebSocketMessageVO.success("USER_OFFLINE", userId));
            }
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        Long userId = (Long) session.getAttributes().get("userId");
        log.error("WebSocket transport error, user: {}", userId, exception);
        if (userId != null) {
            WebSocketSession current = ONLINE_USERS.get(userId);
            if (current == session) {
                ONLINE_USERS.remove(userId);
                broadcast(WebSocketMessageVO.success("USER_OFFLINE", userId));
            }
        }
    }

    public static WebSocketSession getOnlineUserSession(Long userId) {
        return ONLINE_USERS.get(userId);
    }

    public static boolean isUserOnline(Long userId) {
        WebSocketSession session = ONLINE_USERS.get(userId);
        return session != null && session.isOpen();
    }

    public static Set<Long> getOnlineUserIds() {
        return Set.copyOf(ONLINE_USERS.keySet());
    }

    public void sendJsonToUser(Long userId, Object message) {
        WebSocketSession session = ONLINE_USERS.get(userId);
        if (session != null && session.isOpen()) {
            sendJson(session, message);
        }
    }

    public void broadcast(Object message) {
        String json;
        try {
            json = objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            log.error("Failed to serialize broadcast message", e);
            return;
        }
        for (Map.Entry<Long, WebSocketSession> entry : ONLINE_USERS.entrySet()) {
            try {
                WebSocketSession s = entry.getValue();
                if (s.isOpen()) {
                    synchronized (s) {
                        s.sendMessage(new TextMessage(json));
                    }
                }
            } catch (IOException e) {
                log.error("Failed to send broadcast to user {}", entry.getKey(), e);
            }
        }
    }

    public void broadcastExcept(Long excludeUserId, Object message) {
        String json;
        try {
            json = objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            log.error("Failed to serialize broadcast message", e);
            return;
        }
        for (Map.Entry<Long, WebSocketSession> entry : ONLINE_USERS.entrySet()) {
            if (entry.getKey().equals(excludeUserId)) continue;
            try {
                WebSocketSession s = entry.getValue();
                if (s.isOpen()) {
                    synchronized (s) {
                        s.sendMessage(new TextMessage(json));
                    }
                }
            } catch (IOException e) {
                log.error("Failed to send broadcast to user {}", entry.getKey(), e);
            }
        }
    }

    private void sendJson(WebSocketSession session, Object message) {
        try {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
            }
        } catch (Exception e) {
            log.error("Failed to send message", e);
        }
    }
}
