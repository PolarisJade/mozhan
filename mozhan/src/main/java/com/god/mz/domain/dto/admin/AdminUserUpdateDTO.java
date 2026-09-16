package com.god.mz.domain.dto.admin;

import lombok.Data;

/**
 * 后台编辑用户资料。
 * 刻意不含 password：管理员不提供改他人密码的能力。
 */
@Data
public class AdminUserUpdateDTO {
    private Long id;
    private String nickname;
    private String email;
    private String avatar;
    private String intro;
}
