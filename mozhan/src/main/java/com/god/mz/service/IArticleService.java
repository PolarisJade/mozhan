package com.god.mz.service;

import com.god.mz.domain.dto.ArticleDTO;
import com.god.mz.domain.po.Article;
import com.baomidou.mybatisplus.extension.service.IService;
import com.god.mz.domain.query.PageQuery.ArticlePageQuery;
import com.god.mz.domain.query.PageQuery.PageQueryVO;
import com.god.mz.domain.vo.article.ArticleDetailVO;
import com.god.mz.domain.vo.article.ArticleInfoVO;
import com.god.mz.domain.vo.article.ArticleVO;
import com.god.mz.domain.vo.article.HotArticleVO;

/**
 * <p>
 * 文章信息表 服务类
 * </p>
 *
 * @author God
 * @since 2026-05-09
 */
public interface IArticleService extends IService<Article> {

    ArticleVO addArticle(ArticleDTO articleDTO);

    void updateArticle(Long id, ArticleDTO articleDTO);

    void deleteArticle(Long id);

    ArticleDetailVO getArticleDetail(Long id);

    PageQueryVO<ArticleVO> listArticle(ArticlePageQuery query);

    PageQueryVO<ArticleVO> listMyArticle(ArticlePageQuery query);

    void publishArticle(Long id);

    void topArticle(Long id, Boolean top);

    ArticleInfoVO getArticleInfoById(Long id);

    PageQueryVO<HotArticleVO> getHotArticle(ArticlePageQuery query);
}
