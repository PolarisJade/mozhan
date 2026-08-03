package com.god.mz.service;

import com.god.mz.common.enums.LikeTypeEnum;

import java.util.List;
import java.util.Set;

public interface ILikeService {

    void like(LikeTypeEnum type, Long targetId);

    void cancelLike(LikeTypeEnum type, Long targetId);

    Set<Long> isLiked(LikeTypeEnum type, List<Long> targetIds);

    boolean isLiked(LikeTypeEnum type, Long targetId);
}
