package com.god.mz.mapper;

import com.god.mz.domain.po.Article;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.god.mz.domain.vo.article.ArticleDetailVO;
import com.god.mz.domain.vo.article.ArticleVO;
import com.god.mz.domain.vo.article.HotArticleVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 文章信息表 Mapper 接口
 * </p>
 *
 * @author God
 * @since 2026-05-09
 */
public interface ArticleMapper extends BaseMapper<Article> {

    List<ArticleVO> selectArticleVOList(@Param("articleIds") List<Long> articleIds);

    ArticleDetailVO selectArticleDetail(@Param("id") Long id);

    List<HotArticleVO> selectHotArticles(@Param("limit") Integer limit);

    List<Map<String, Object>> selectUserArticleCounts(@Param("userIds") List<Long> userIds);
}
