package com.god.mz.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.god.mz.domain.po.EssayLike;
import com.god.mz.mapper.EssayLikeMapper;
import com.god.mz.service.IEssayLikeService;
import com.god.mz.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class EssayLikeServiceImpl extends ServiceImpl<EssayLikeMapper, EssayLike> implements IEssayLikeService {

    @Override
    public void addLikeEssay(Long essayId) {
        //获取用户id
        Long userId = UserContext.getUserId();
        EssayLike essayLike = new EssayLike();
        essayLike.setUserId(userId);
        essayLike.setEssayId(essayId);
        save(essayLike);
    }

    @Override
    public void cancelLikeEssay(Long essayId) {
        Long userId = UserContext.getUserId();
        remove(new LambdaQueryWrapper<EssayLike>()
                .eq(EssayLike::getUserId, userId)
                .eq(EssayLike::getEssayId, essayId));
    }
}
