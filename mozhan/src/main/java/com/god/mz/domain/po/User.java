package com.god.mz.domain.po;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serial;
import java.time.LocalDateTime;
import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.god.mz.common.enums.UserStatusEnum;
import com.god.mz.common.enums.UserTypeEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 用户信息表
 * </p>
 *
 * @author God
 * @since 2026-05-09
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("users")
public class User implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    @JsonIgnore
    private String password;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 是否为管理员
     */
    private UserTypeEnum admin;

    /**
     * 头像地址
     */
    private String avatar;

    /**
     * 简介
     */
    private String intro;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 状态：0禁用，1启用
     */
    private UserStatusEnum status;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    /**
     * 修改时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
}
