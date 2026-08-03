package com.god.mz.service.impl;

import com.god.mz.common.enums.BizCodeEnum;
import com.god.mz.common.enums.LikeTypeEnum;
import com.god.mz.exception.BizException;
import com.god.mz.service.IArticleLikeService;
import com.god.mz.service.IEssayLikeService;
import com.god.mz.service.ILikeService;
import com.god.mz.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class LikeServiceImpl implements ILikeService {

    private final StringRedisTemplate stringRedisTemplate;
    private final IEssayLikeService essayLikeService;
    private final IArticleLikeService articleLikeService;

    @Override
    @Transactional
    public void like(LikeTypeEnum type, Long targetId) {
        Long userId = UserContext.getUserId();
        String key = type.getSetKey(targetId);

        Long success = stringRedisTemplate.opsForSet().add(key, userId.toString());
        if (success == null || success <= 0) {
            throw new BizException(BizCodeEnum.OPERATION_FAILURE);
        }

        saveLikeCount(type, targetId, key);

        //同步到数据库
        if (type == LikeTypeEnum.article) {
            articleLikeService.addLikeArticle(targetId);
        }
        else if (type == LikeTypeEnum.essay) {
            essayLikeService.addLikeEssay(targetId);
        }
    }

    @Override
    @Transactional
    public void cancelLike(LikeTypeEnum type, Long targetId) {
        Long userId = UserContext.getUserId();
        String key = type.getSetKey(targetId);

        Long success = stringRedisTemplate.opsForSet().remove(key, userId.toString());
        if (success == null || success <= 0) {
            throw new BizException(BizCodeEnum.OPERATION_FAILURE);
        }

        saveLikeCount(type, targetId, key);

        if (type == LikeTypeEnum.article) {
            articleLikeService.cancelLikeArticle(targetId);
        }
        else if (type == LikeTypeEnum.essay) {
            essayLikeService.cancelLikeEssay(targetId);
        }
    }

    @Override
    public Set<Long> isLiked(LikeTypeEnum type, List<Long> targetIds) {
        Long userId = UserContext.getUserId();
        if (userId == null) return null;

        List<Object> results = stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            StringRedisConnection src = (StringRedisConnection) connection;
            for (Long id : targetIds) {
                src.sIsMember(type.getSetKey(id), userId.toString());
            }
            return null;
        });

        Set<Long> set = new HashSet<>();
        for (int i = 0; i < results.size(); i++) {
            if ((Boolean) results.get(i)) {
                set.add(targetIds.get(i));
            }
        }
        return set;
    }

    @Override
    public boolean isLiked(LikeTypeEnum type, Long targetId) {
        Long userId = UserContext.getUserId();
        if (userId == null) return false;
        String key = type.getSetKey(targetId);
        Boolean success = stringRedisTemplate.opsForSet().isMember(key, userId.toString());
        return Boolean.TRUE.equals(success);
    }

    private void saveLikeCount(LikeTypeEnum type, Long targetId, String key) {
        Long size = stringRedisTemplate.opsForSet().size(key);
        if (size == null) return;
        stringRedisTemplate.opsForZSet().add(type.getCountKey(), targetId.toString(), size);
    }
}
