package com.god.mz.task;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.god.mz.common.constant.RedisConstant;
import com.god.mz.domain.po.Article;
import com.god.mz.domain.po.ArticleLike;
import com.god.mz.domain.po.ArticleTag;
import com.god.mz.domain.po.Comment;
import com.god.mz.domain.vo.article.HotArticleVO;
import com.god.mz.mapper.ArticleLikeMapper;
import com.god.mz.mapper.ArticleMapper;
import com.god.mz.mapper.ArticleTagMapper;
import com.god.mz.mapper.CommentMapper;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
@EnableScheduling
public class ArticleTask {
    @Resource
    private ArticleMapper articleMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private ObjectMapper objectMapper;
    @Resource
    private CommentMapper commentMapper;
    @Resource
    private ArticleTagMapper articleTagMapper;
    @Resource
    private ArticleLikeMapper articleLikeMapper;

    /**
     * 每周一凌晨 4:00 刷新热门文章排行榜
     */
    @Scheduled(cron = "0 0 1 ? * MON")
    public void refreshHotArticles() {
        List<HotArticleVO> list = articleMapper.selectHotArticles(100);
        try {
            String json = objectMapper.writeValueAsString(list);
            stringRedisTemplate.opsForValue().set(
                    RedisConstant.HOT_ARTICLE_KEY,
                    json,
                    RedisConstant.DEFAULT_EXPIRE_HOURS,
                    TimeUnit.HOURS
            );
        } catch (JsonProcessingException ignored) {
        }
    }

    /**
     * 每周一凌晨 2:00 清除已被删除的文章
     */
    @Transactional
    @Scheduled(cron = "0 0 2 ? * MON")
    public void deleteArticle() {
        List<Article> deletedArticles = articleMapper.selectList(
                new QueryWrapper<Article>().eq("del_flag", true));

        if (deletedArticles.isEmpty()) {
            return;
        }

        List<Long> articleIds = deletedArticles.stream()
                .map(Article::getId)
                .collect(Collectors.toList());

        commentMapper.delete(new QueryWrapper<Comment>().in("article_id", articleIds));

        articleTagMapper.delete(new QueryWrapper<ArticleTag>().in("article_id", articleIds));

        articleLikeMapper.delete(new QueryWrapper<ArticleLike>().in("article_id", articleIds));

        articleMapper.deleteByIds(articleIds);

    }
}
