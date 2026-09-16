package com.god.mz.domain.vo.user;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.god.mz.common.enums.UserStatusEnum;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminUserVO {
    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String avatar;
    private String intro;
    private UserStatusEnum status;
    /**
     * 是否管理员账号。刻意用 Boolean 而不是 {@link com.god.mz.common.enums.UserTypeEnum}：
     * 前端只需要拿它决定「状态开关要不要禁用」，用枚举会多绕一层中文序列化。
     */
    private Boolean admin;
    private Long articleCount;
    private Long EssayCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

}
