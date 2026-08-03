package com.god.mz.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.god.mz.domain.po.ArticleLike;
import com.god.mz.mapper.ArticleLikeMapper;
import com.god.mz.service.IArticleLikeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.god.mz.util.UserContext;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 文章点赞记录表 服务实现类
 * </p>
 *
 * @author God
 * @since 2026-05-09
 */
@Service
public class ArticleLikeServiceImpl extends ServiceImpl<ArticleLikeMapper, ArticleLike> implements IArticleLikeService {

    @Override
    public void addLikeArticle(Long articleId) {
        //获取用户id
        Long userId = UserContext.getUserId();
        ArticleLike articleLike = new ArticleLike();
        articleLike.setArticleId(articleId);
        articleLike.setUserId(userId);
        save(articleLike);
    }

    @Override
    public void cancelLikeArticle(Long articleId) {
        Long userId = UserContext.getUserId();
        remove(new LambdaQueryWrapper<ArticleLike>()
                .eq(ArticleLike::getUserId, userId)
                .eq(ArticleLike::getArticleId, articleId));
    }
}
