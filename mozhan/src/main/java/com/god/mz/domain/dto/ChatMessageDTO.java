package com.god.mz.domain.dto;

import lombok.Data;

/**
 * WebSocket聊天消息DTO
 */
@Data
public class ChatMessageDTO {

    /**
     * 消息类型：CHAT/RECALL/READ
     */
    private String type;

    /**
     * 接收者ID（发送消息时使用）
     */
    private Long receiverId;

    /**
     * 消息内容（发送消息时使用）
     */
    private String content;

    /**
     * 消息ID（撤回消息时使用）
     */
    private Long messageId;

    /**
     * 会话ID（已读确认时使用）
     */
    private Long sessionId;
}
