package com.god.mz.domain.po;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serial;
import java.time.LocalDateTime;
import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.Accessors;

/**
 * <p>
 * AI会话表
 * </p>
 *
 * @author God
 * @since 2026-08-21
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("ai_session")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AiSession implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 数据id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 会话id
     */
    private String sessionId;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 会话标题
     */
    private String title;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

    /**
     * 创建人
     */
    private Long creator;

    /**
     * 更新人
     */
    private Long updater;


}
