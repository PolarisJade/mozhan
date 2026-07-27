package com.god.mz.domain.po;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serial;
import java.time.LocalDateTime;
import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 文章评论表
 * </p>
 *
 * @author God
 * @since 2026-05-09
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("comment")
public class Comment implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 文章ID
     */
    private Long articleId;

    /**
     * 评论用户ID
     */
    private Long userId;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 父评论ID
     */
    private Long parentId;

    /**
     * 回复评论ID
     */
    private Long replyToId;

    /**
     * 删除标志：0未删除，1已删除
     */
    private Boolean delFlag;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
