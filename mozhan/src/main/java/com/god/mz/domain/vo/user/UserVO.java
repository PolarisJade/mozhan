package com.god.mz.domain.vo.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserVO {
    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String intro;
    private String email;
    private Integer articleCount;
    private Integer followingCount;
    private Integer followerCount;
    private Boolean isFollowed;
}
