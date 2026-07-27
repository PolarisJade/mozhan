package com.god.mz.service;

import com.god.mz.domain.po.ArticleLike;
import com.baomidou.mybatisplus.extension.service.IService;
import com.god.mz.domain.query.cursorQuery.CursorPageVO;
import com.god.mz.domain.vo.user.BaseUserVO;

/**
 * <p>
 * 文章点赞记录表 服务类
 * </p>
 *
 * @author God
 * @since 2026-05-09
 */
public interface IArticleLikeService extends IService<ArticleLike> {

    Integer likeArticle(Long articleId);

    CursorPageVO<BaseUserVO> getLikeList(Long cursor, Integer pageSize,Long articleId);
}
