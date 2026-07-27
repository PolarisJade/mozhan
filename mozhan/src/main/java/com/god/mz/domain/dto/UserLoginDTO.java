package com.god.mz.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserLoginDTO {
    private String username;
    private String password;
    private String nickname;
    private String email;
    private String verificationCode;   //验证码
    private Boolean admin;   //是否为管理端登录
}
