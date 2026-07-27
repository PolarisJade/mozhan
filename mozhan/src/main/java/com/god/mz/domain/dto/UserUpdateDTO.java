package com.god.mz.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserUpdateDTO {
    private String nickname;
    private String avatar;
    private String intro;
    private String email;
}
