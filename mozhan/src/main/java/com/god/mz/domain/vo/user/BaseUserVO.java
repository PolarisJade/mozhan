package com.god.mz.domain.vo.user;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BaseUserVO {
    private Long id;
    private String nickname;
    private String avatar;
    private Boolean isFollowed;
}
