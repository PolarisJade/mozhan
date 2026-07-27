package com.god.mz.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.god.mz.common.enums.BizCodeEnum;
import com.god.mz.common.constant.RedisConstant;
import com.god.mz.common.enums.UserStatusEnum;
import com.god.mz.domain.po.User;
import com.god.mz.domain.po.UserFollow;
import com.god.mz.domain.query.cursorQuery.CursorPageVO;
import com.god.mz.domain.query.cursorQuery.CursorQuery;
import com.god.mz.domain.vo.user.UserFollowItemVO;
import com.god.mz.exception.BizException;
import com.god.mz.mapper.UserFollowMapper;
import com.god.mz.mapper.UserMapper;
import com.god.mz.service.IUserFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.god.mz.util.CursorCodeUtil;
import com.god.mz.util.UserContext;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 用户关注记录表 服务实现类
 * </p>
 *
 * @author God
 * @since 2026-05-09
 */
@Service
public class UserFollowServiceImpl extends ServiceImpl<UserFollowMapper, UserFollow> implements IUserFollowService {

    @Resource
    private UserMapper userMapper;
    @Resource
    private UserFollowMapper userFollowMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void followUser(Long followId) {
        //获取当前用户id
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BizException(BizCodeEnum.USER_NOT_AUTH);
        }
        //校验被关注的用户状态
        User fUser = userMapper.selectById(followId);
        if (fUser == null) {
            throw new BizException(BizCodeEnum.USER_NOT_FOUND);
        }
        if (fUser.getStatus() == UserStatusEnum.DISABLE) {
            throw new BizException(BizCodeEnum.USER_DISABLED);
        }
        UserFollow userFollow = new UserFollow()
                .setFollowId(followId)
                .setUserId(userId);
        boolean success = save(userFollow);
        if (!success){
            throw new BizException(BizCodeEnum.OPERATION_FAILURE);
        }

        stringRedisTemplate.delete(RedisConstant.USER_PROFILE_KEY_PREFIX + userId);
        stringRedisTemplate.delete(RedisConstant.USER_PROFILE_KEY_PREFIX + followId);

    }

    @Override
    public void cancelFollowUser(Long userId) {
        //获取当前用户id
        Long currentUserId = UserContext.getUserId();
        if (currentUserId == null) {
            throw new BizException(BizCodeEnum.USER_NOT_AUTH);
        }
        boolean success = remove(new QueryWrapper<UserFollow>().eq("user_id", currentUserId).eq("follow_id", userId));
        if (!success){
            throw new BizException(BizCodeEnum.OPERATION_FAILURE);
        }
        stringRedisTemplate.delete(RedisConstant.USER_PROFILE_KEY_PREFIX + currentUserId);
        stringRedisTemplate.delete(RedisConstant.USER_PROFILE_KEY_PREFIX + userId);

    }

    @Override
    public CursorPageVO<UserFollowItemVO> queryFollowingList(CursorQuery query) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BizException(BizCodeEnum.USER_NOT_AUTH);
        }
        Integer pageSize = query.getPageSize();
        Long cursor = resolveCursor(query);

        List<UserFollow> followList = userFollowMapper.selectFollowingList(userId, cursor, pageSize + 1);

        boolean hasMore = followList.size() > pageSize;
        if (hasMore) {
            followList = followList.subList(0, pageSize);
        }

        List<UserFollowItemVO> voList = new ArrayList<>();
        if (!followList.isEmpty()) {
            List<Long> followIds = followList.stream()
                    .map(UserFollow::getFollowId)
                    .toList();
            List<User> users = userMapper.selectByIds(followIds);

            for (UserFollow follow : followList) {
                User user = users.stream()
                        .filter(u -> u.getId().equals(follow.getFollowId()))
                        .findFirst()
                        .orElse(null);

                if (user != null) {
                    UserFollowItemVO vo = new UserFollowItemVO();
                    vo.setId(user.getId());
                    vo.setNickname(user.getNickname());
                    vo.setAvatar(user.getAvatar());
                    voList.add(vo);
                }
            }
        }
        Long nextCursor = voList.isEmpty() ? null : followList.getLast().getId();
        return new CursorPageVO<>(voList, hasMore,
                nextCursor != null ? CursorCodeUtil.encode(List.of(nextCursor)) : null);
    }

    @Override
    public CursorPageVO<UserFollowItemVO> queryFollowerList(CursorQuery query) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BizException(BizCodeEnum.USER_NOT_AUTH);
        }
        Integer pageSize = query.getPageSize();
        Long cursor = resolveCursor(query);

        List<UserFollow> followList = userFollowMapper.selectFollowerList(userId, cursor, pageSize + 1);

        boolean hasMore = followList.size() > pageSize;
        if (hasMore) {
            followList = followList.subList(0, pageSize);
        }
        List<UserFollowItemVO> voList = new ArrayList<>();

        if (!followList.isEmpty()) {
            List<Long> followerIds = followList.stream()
                    .map(UserFollow::getUserId)
                    .toList();

            List<User> users = userMapper.selectByIds(followerIds);
            for (UserFollow follow : followList) {
                User user = users.stream()
                        .filter(u -> u.getId().equals(follow.getUserId()))
                        .findFirst()
                        .orElse(null);

                if (user != null) {
                    UserFollowItemVO vo = BeanUtil.copyProperties(user, UserFollowItemVO.class);
                    vo.setFollowTime(follow.getCreateTime());
                    voList.add(vo);
                }
            }
        }
        Long nextCursor = voList.isEmpty() ? null : followList.getLast().getId();
        return new CursorPageVO<>(voList, hasMore,
                nextCursor != null ? CursorCodeUtil.encode(List.of(nextCursor)) : null);
    }

    private static Long resolveCursor(CursorQuery query) {
        List<Long> cursors = CursorCodeUtil.decode(query.getNextCursor());
        return cursors != null && !cursors.isEmpty() ? cursors.getFirst() : null;
    }

}
