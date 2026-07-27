package com.god.mz.domain.po;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serial;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 用户日记表
 * </p>
 *
 * @author God
 * @since 2026-07-23
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("diary")
public class Diary implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 日记主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 所属用户ID，关联用户表
     */
    private Long userId;

    /**
     * 日记绑定的日期（如 2024-05-20）
     */
    private LocalDate diaryDate;

    /**
     * 日记正文内容（支持富文本、Emoji及图片URL）
     */
    private String content;

    /**
     * 当天天气（如：晴、多云、小雨）
     */
    private String weather;

    /**
     * 创建时间
     */

    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    /**
     * 最后更新时间
     */

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;


}
