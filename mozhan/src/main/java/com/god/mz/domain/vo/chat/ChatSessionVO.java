package com.god.mz.domain.vo.chat;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.god.mz.domain.vo.user.BaseUserVO;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天会话VO
 */
@Data
public class ChatSessionVO {

    /**
     * 会话ID
     */
    private Long sessionId;

    /**
     * 对方用户信息
     */
    private BaseUserVO targetUser;

    /**
     * 最后一条消息内容
     */
    private String lastMessage;

    /**
     * 最后消息时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastMessageTime;

    /**
     * 未读消息数
     */
    private Integer unreadCount;
}
