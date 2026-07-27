package com.god.mz.util;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.god.mz.domain.po.UserFollow;
import com.god.mz.mapper.UserFollowMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class FollowHelper {

    private final UserFollowMapper userFollowMapper;

    public Set<Long> getFollowedUserIds(List<Long> userIds) {
        Long currentUserId = UserContext.getUserId();
        if (currentUserId == null || userIds == null || userIds.isEmpty()) {
            return new HashSet<>();
        }

        List<UserFollow> follows = userFollowMapper.selectList(
                Wrappers.lambdaQuery(UserFollow.class)
                        .eq(UserFollow::getUserId, currentUserId)
                        .in(UserFollow::getFollowId, userIds)
        );

        return follows.stream()
                .map(UserFollow::getFollowId)
                .collect(Collectors.toSet());
    }

    public boolean isFollowing(Long targetUserId) {
        Long currentUserId = UserContext.getUserId();
        if (currentUserId == null || targetUserId == null) {
            return false;
        }

        return userFollowMapper.exists(
                Wrappers.lambdaQuery(UserFollow.class)
                        .eq(UserFollow::getUserId, currentUserId)
                        .eq(UserFollow::getFollowId, targetUserId)
        );
    }
}
