package com.god.mz.domain.vo.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserLoginVO {
    private String token;
    private Long userId;
    private String nickname;
    private String avatar;
}
