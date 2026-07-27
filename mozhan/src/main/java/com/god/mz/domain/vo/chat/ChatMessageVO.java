package com.god.mz.domain.vo.chat;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.god.mz.common.enums.MessageStatusEnum;
import com.god.mz.common.enums.ReadStatusEnum;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天消息VO
 */
@Data
public class ChatMessageVO {

    /**
     * 消息ID
     */
    private Long messageId;

    /**
     * 会话ID
     */
    private Long sessionId;

    /**
     * 发送者ID
     */
    private Long senderId;

    /**
     * 接收者ID
     */
    private Long receiverId;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息状态
     */
    private MessageStatusEnum status;

    /**
     * 读取状态
     */
    private ReadStatusEnum readStatus;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
