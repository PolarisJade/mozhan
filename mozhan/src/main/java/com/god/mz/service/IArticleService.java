package com.god.mz.service;

import com.god.mz.domain.dto.ArticleDTO;
import com.god.mz.domain.po.Article;
import com.baomidou.mybatisplus.extension.service.IService;
import com.god.mz.domain.query.PageQuery.AdminArticlePageQuery;
import com.god.mz.domain.query.PageQuery.ArticlePageQuery;
import com.god.mz.domain.query.PageQuery.PageQueryVO;
import com.god.mz.domain.vo.article.AdminArticleDetailVO;
import com.god.mz.domain.vo.article.AdminArticleVO;
import com.god.mz.domain.vo.article.ArticleDetailVO;
import com.god.mz.domain.vo.article.ArticleInfoVO;
import com.god.mz.domain.vo.article.ArticleVO;
import com.god.mz.domain.vo.article.HotArticleVO;
import com.god.mz.tool.result.ArticleInfo;

import java.util.List;

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

    void updateLikeCount(int maxSize);

    List<ArticleInfo> queryArticleByName(String keyword);

    /**
     * 后台：文章分页（含作者/分类/标签/点赞数/评论数），支持关键词、分类、标签、状态筛选。
     */
    PageQueryVO<AdminArticleVO> listAdminArticle(AdminArticlePageQuery query);

    /**
     * 后台：查看单篇文章内容
     */
    AdminArticleDetailVO getAdminArticleDetail(Long id);
}
