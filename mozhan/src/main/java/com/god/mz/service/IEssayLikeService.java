package com.god.mz.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.god.mz.domain.po.EssayLike;


public interface IEssayLikeService extends IService<EssayLike> {
    void addLikeEssay(Long essayId);

    void cancelLikeEssay(Long targetId);
}
