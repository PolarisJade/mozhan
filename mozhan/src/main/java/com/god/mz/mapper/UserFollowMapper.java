package com.god.mz.mapper;

import com.god.mz.domain.po.UserFollow;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
 * <p>
 * 用户关注记录表 Mapper 接口
 * </p>
 *
 * @author God
 * @since 2026-05-09
 */
public interface UserFollowMapper extends BaseMapper<UserFollow> {

    List<UserFollow> selectFollowingList(Long userId, Long cursor, int pageSize);

    List<UserFollow> selectFollowerList(Long followId, Long cursor, int pageSize);
}
