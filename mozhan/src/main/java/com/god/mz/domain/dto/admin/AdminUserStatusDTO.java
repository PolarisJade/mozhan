package com.god.mz.domain.dto.admin;

import com.god.mz.common.enums.UserStatusEnum;
import lombok.Data;

/**
 * 后台启用/禁用用户。
 * <p>
 * 走 JSON body 而不是查询参数，是因为 {@link UserStatusEnum} 把 {@code @JsonValue} 打在中文 desc 上：
 * body 走 Jackson，能正确接收 "启用"/"禁用"；而查询参数走 Spring 的 Enum.valueOf，只认 ENABLE/DISABLE。
 * 统一用 body 可以少一个容易踩的坑。
 * </p>
 */
@Data
public class AdminUserStatusDTO {
    private UserStatusEnum status;
}
