package com.god.mz.service;

import com.god.mz.domain.po.UserFollow;
import com.baomidou.mybatisplus.extension.service.IService;
import com.god.mz.domain.query.cursorQuery.CursorPageVO;
import com.god.mz.domain.query.cursorQuery.CursorQuery;
import com.god.mz.domain.vo.user.UserFollowItemVO;

/**
 * <p>
 * 用户关注记录表 服务类
 * </p>
 *
 * @author God
 * @since 2026-05-09
 */
public interface IUserFollowService extends IService<UserFollow> {

    void followUser(Long followId);

    void cancelFollowUser(Long userId);

    CursorPageVO<UserFollowItemVO> queryFollowingList(CursorQuery query);

    CursorPageVO<UserFollowItemVO> queryFollowerList(CursorQuery query);

}
