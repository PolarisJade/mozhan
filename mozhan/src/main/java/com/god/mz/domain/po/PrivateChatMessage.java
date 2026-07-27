package com.god.mz.domain.po;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.god.mz.common.enums.MessageStatusEnum;
import com.god.mz.common.enums.ReadStatusEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 私信消息表
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("private_chat_message")
public class PrivateChatMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

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
     * 状态：0正常 1已撤回
     */
    private MessageStatusEnum status;

    /**
     * 读取状态：0未读 1已读
     */
    private ReadStatusEnum readStatus;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
}
