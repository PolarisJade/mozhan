package com.god.mz.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserPwdDTO {
    private String oldPassword;

    private String newPassword;
}
