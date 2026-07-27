package com.god.mz.domain.vo.user;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.god.mz.common.enums.UserStatusEnum;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminUserVO {
    private Long id;
    private String nickname;
    private String avatar;
    private UserStatusEnum status;
    private Long articleCount;
    private Long EssayCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

}
