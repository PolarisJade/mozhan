package com.god.mz.util;

import com.god.mz.domain.po.Article;
import com.god.mz.domain.vo.article.ArticleVO;
import com.god.mz.domain.vo.tag.TagVO;
import com.god.mz.mapper.ArticleMapper;
import com.god.mz.mapper.TagMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ArticleVOBuilder {

    private final ArticleMapper articleMapper;
    private final TagMapper tagMapper;

    public List<ArticleVO> build(List<Article> articles) {
        if (articles == null || articles.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> articleIds = articles.stream()
                .map(Article::getId)
                .collect(Collectors.toList());

        Map<Long, ArticleVO> articleVOMap = articleMapper.selectArticleVOList(articleIds).stream()
                .collect(Collectors.toMap(ArticleVO::getId, vo -> vo));

        List<TagVO> tagVOList = tagMapper.selectTagVOByArticleIds(articleIds);
        Map<Long, List<TagVO>> tagMap = tagVOList.stream()
                .collect(Collectors.groupingBy(TagVO::getArticleId));

        List<ArticleVO> result = new ArrayList<>();
        for (Article article : articles) {
            ArticleVO articleVO = articleVOMap.get(article.getId());
            if (articleVO != null) {
                articleVO.setTags(tagMap.getOrDefault(article.getId(), new ArrayList<>()));
                result.add(articleVO);
            }
        }
        return result;
    }
}

