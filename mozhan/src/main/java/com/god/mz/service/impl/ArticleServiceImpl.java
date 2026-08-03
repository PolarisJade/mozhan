package com.god.mz.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.god.mz.common.enums.ArticleStatusEnum;
import com.god.mz.common.enums.BizCodeEnum;
import com.god.mz.common.constant.RedisConstant;
import com.god.mz.common.enums.LikeTypeEnum;
import com.god.mz.domain.dto.ArticleDTO;
import com.god.mz.domain.po.*;
import com.god.mz.domain.query.PageQuery.ArticlePageQuery;
import com.god.mz.domain.query.PageQuery.PageQueryVO;
import com.god.mz.domain.vo.article.ArticleDetailVO;
import com.god.mz.domain.vo.article.ArticleInfoVO;
import com.god.mz.domain.vo.article.ArticleVO;
import com.god.mz.domain.vo.article.HotArticleVO;
import com.god.mz.domain.vo.tag.TagVO;
import com.god.mz.exception.BizException;
import com.god.mz.mapper.*;
import com.god.mz.service.IArticleService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.god.mz.service.ILikeService;
import com.god.mz.util.ArticleVOBuilder;
import com.god.mz.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p>
 * 文章信息表 服务实现类
 * </p>
 *
 * @author God
 * @since 2026-05-09
 */
@Service
@RequiredArgsConstructor
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article> implements IArticleService {

    private final ArticleTagMapper articleTagMapper;
    private final CategoryMapper categoryMapper;
    private final ArticleMapper articleMapper;
    private final CommentMapper commentMapper;
    private final UserFollowMapper userFollowMapper;
    private final UserMapper userMapper;
    private final TagMapper tagMapper;
    private final ArticleVOBuilder articleVOBuilder;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final ILikeService likeService;

    @Override
    @Transactional
    public ArticleVO addArticle(ArticleDTO articleDTO) {
        Long userId = UserContext.getUserId();
        if (userId == null){
            throw new BizException(BizCodeEnum.USER_NOT_AUTH);
        }

        Article article = new Article();
        BeanUtil.copyProperties(articleDTO, article);
        article.setAuthorId(userId);
        article.setDelFlag(false);

        boolean success = save(article);
        if (!success){
            throw new BizException(BizCodeEnum.OPERATION_FAILURE);
        }

        if (articleDTO.getTagIds() != null && !articleDTO.getTagIds().isEmpty()) {
            List<ArticleTag> articleTags = articleDTO.getTagIds().stream()
                    .map(tagId -> {
                        ArticleTag articleTag = new ArticleTag();
                        articleTag.setArticleId(article.getId());
                        articleTag.setTagId(tagId);
                        return articleTag;
                    })
                    .collect(Collectors.toList());

            articleTagMapper.insert(articleTags);
        }
        stringRedisTemplate.delete(RedisConstant.USER_PROFILE_KEY_PREFIX + userId);

        return buildArticleVO(article);
    }

    @Override
    @Transactional
    public void updateArticle(Long id, ArticleDTO articleDTO) {
        Long userId = UserContext.getUserId();
        if (userId == null){
            throw new BizException(BizCodeEnum.USER_NOT_AUTH);
        }

        //查询原文章，判断是否存在
        Article oldArticle = getById(id);
        if (oldArticle == null){
            throw new BizException(BizCodeEnum.DATA_NOT_EXIST);
        }

        //判断是否为文章的作者
        if (!oldArticle.getAuthorId().equals(userId)){
            throw new BizException(BizCodeEnum.USER_NOT_AUTH);
        }

        BeanUtil.copyProperties(articleDTO, oldArticle);

        boolean success = updateById(oldArticle);
        if (!success){
            throw new BizException(BizCodeEnum.OPERATION_FAILURE);
        }

        //更新关联的标签：先删除原有标签，再添加新标签
        if (articleDTO.getTagIds() != null){
            articleTagMapper.delete(
                    Wrappers.<ArticleTag>lambdaQuery()
                            .eq(ArticleTag::getArticleId, id)
            );
            if (!articleDTO.getTagIds().isEmpty()) {
                List<ArticleTag> articleTags = articleDTO.getTagIds().stream()
                        .map(tagId -> {
                            ArticleTag articleTag = new ArticleTag();
                            articleTag.setArticleId(id);
                            articleTag.setTagId(tagId);
                            return articleTag;
                        })
                        .collect(Collectors.toList());

                articleTagMapper.insert(articleTags);
            }
        }
    }

    @Override
    public void deleteArticle(Long id) {
        //获取用户id
        Long userId = UserContext.getUserId();
        Article article = getById(id);
        if (article == null){
            throw new BizException(BizCodeEnum.DATA_NOT_EXIST);
        }
        if (!article.getAuthorId().equals(userId)){
            throw new BizException(BizCodeEnum.USER_NOT_AUTH);
        }
        article.setDelFlag(true);
        boolean success = updateById(article);
        if (!success){
            throw new BizException(BizCodeEnum.OPERATION_FAILURE);
        }
        stringRedisTemplate.delete(RedisConstant.USER_PROFILE_KEY_PREFIX + userId);
        stringRedisTemplate.delete(RedisConstant.STATISTIC_KEY);
        stringRedisTemplate.delete(RedisConstant.HOT_ARTICLE_KEY);
    }

    @Override
    public ArticleDetailVO getArticleDetail(Long id) {
        ArticleDetailVO vo = articleMapper.selectArticleDetail(id);
        if (vo == null) {
            throw new BizException(BizCodeEnum.DATA_NOT_EXIST);
        }

        //从redis中获取点赞数
        Double count = stringRedisTemplate.opsForZSet().score(RedisConstant.ARTICLE_LIKE_COUNT_KEY, id.toString());
        if (count == null) {
            count = 0D;
        }
        vo.setLikeCount(count.longValue());

        List<TagVO> tagVOList = tagMapper.selectTagVOByArticleIds(Collections.singletonList(id));
        vo.setTags(tagVOList != null ? tagVOList : new ArrayList<>());

        Long userId = UserContext.getUserId();
        // 判断是否点过赞
        vo.setIsLike(likeService.isLiked(LikeTypeEnum.article, id));
        if (userId != null) {
            vo.setIsAuthor(userId.equals(vo.getAuthorId()));
            vo.setIsFollowed(userFollowMapper.exists(new QueryWrapper<UserFollow>()
                    .eq("user_id", userId)
                    .eq("follow_id", vo.getAuthorId())));
        } else {
            vo.setIsAuthor(false);
            vo.setIsFollowed(false);
        }
        return vo;
    }

    @Override
    public PageQueryVO<ArticleVO> listArticle(ArticlePageQuery query) {
        Page<Article> page = new Page<>(query.getPageNum(), query.getPageSize());

        QueryWrapper<Article> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("del_flag", false)
                .eq("status", ArticleStatusEnum.PUBLISHED);

        if (query.getCategoryId() != null) {
            queryWrapper.eq("category_id", query.getCategoryId());
        }

        addOrderBy(queryWrapper, query.getSortBy(), query.getIsAsc());
        IPage<Article> result = page(page, queryWrapper);
        List<ArticleVO> voList = articleVOBuilder.build(result.getRecords());
        return toPageVO(result, voList);
    }

    @Override
    public PageQueryVO<ArticleVO> listMyArticle(ArticlePageQuery query) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BizException(BizCodeEnum.USER_NOT_AUTH);
        }

        Page<Article> page = new Page<>(query.getPageNum(), query.getPageSize());

        QueryWrapper<Article> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("del_flag", false)
                .eq("author_id", userId);

        if (query.getStatus() != null) {
            queryWrapper.eq("status", query.getStatus());
        }

        addOrderBy(queryWrapper, query.getSortBy(), query.getIsAsc());
        IPage<Article> result = page(page, queryWrapper);
        List<ArticleVO> voList = articleVOBuilder.build(result.getRecords());
        return toPageVO(result, voList);
    }

    @Override
    public void publishArticle(Long id) {
        //根据id查询文章
        Article article = getById(id);
        if (article == null) {
            throw new BizException(BizCodeEnum.DATA_NOT_EXIST);
        }
        article.setStatus(ArticleStatusEnum.PUBLISHED);
        boolean success = updateById(article);
        if (!success) {
            throw new BizException(BizCodeEnum.OPERATION_FAILURE);
        }
    }

    @Override
    public void topArticle(Long id, Boolean top) {
        Article article = getById(id);
        if (article == null) {
            throw new BizException(BizCodeEnum.DATA_NOT_EXIST);
        }
        article.setIsTop(top);
        boolean success = updateById(article);
        if (!success) {
            throw new BizException(BizCodeEnum.OPERATION_FAILURE);
        }
    }

    @Override
    public ArticleInfoVO getArticleInfoById(Long id) {
        Article article = getById(id);
        if (article == null) {
            throw new BizException(BizCodeEnum.DATA_NOT_EXIST);
        }
        ArticleInfoVO vo = BeanUtil.copyProperties(article, ArticleInfoVO.class);

        //查询文章分类
        Category category = categoryMapper.selectById(article.getCategoryId());
        vo.setCategoryName(category.getName());

        //查询文章标签
        List<TagVO> tagVOList = tagMapper.selectTagVOByArticleIds(Collections.singletonList(id));
        vo.setTags(tagVOList);
        return vo;
    }

    @Override
    public PageQueryVO<HotArticleVO> getHotArticle(ArticlePageQuery query) {
        List<HotArticleVO> allHotArticles;

        String json = stringRedisTemplate.opsForValue().get(RedisConstant.HOT_ARTICLE_KEY);
        if (json != null && !json.isEmpty()) {
            try {
                allHotArticles = objectMapper.readValue(json, new TypeReference<>() {});
            } catch (JsonProcessingException e) {
                allHotArticles = loadHotArticlesFromDb(query.getPageSize());
            }
        } else {
            allHotArticles = loadHotArticlesFromDb(query.getPageSize());
        }

        int pageNum = query.getPageNum();
        int pageSize = query.getPageSize();
        int total = allHotArticles.size();
        int totalPages = (int) Math.ceil((double) total / pageSize);
        int start = (pageNum - 1) * pageSize;
        int end = Math.min(start + pageSize, total);

        List<HotArticleVO> pageList = start < total
                ? allHotArticles.subList(start, end)
                : List.of();

        return new PageQueryVO<>(pageList, (long) total, (long) pageSize, (long) pageNum, (long) totalPages);
    }

    @Override
    @Transactional
    public void updateLikeCount(int maxSize) {
        String key = LikeTypeEnum.article.getCountKey();
        Set<ZSetOperations.TypedTuple<String>> typedTuples = stringRedisTemplate.opsForZSet().popMin(key, maxSize);
        if (typedTuples == null) return;

        List<Article> list = new ArrayList<>(typedTuples.size());
        for (ZSetOperations.TypedTuple<String> tuple : typedTuples) {
            String targetId = tuple.getValue();
            Double count = tuple.getScore();
            if (targetId == null || count == null) continue;
            Article article = new Article();
            article.setId(Long.parseLong(targetId));
            article.setLikeCount(count.longValue());
            list.add(article);
        }
        updateBatchById(list);
    }

    private List<HotArticleVO> loadHotArticlesFromDb(Integer limit) {
        List<HotArticleVO> list = articleMapper.selectHotArticles(limit);
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
        return list;
    }

    private ArticleVO buildArticleVO(Article article) {
        ArticleVO articleVO = new ArticleVO();
        BeanUtil.copyProperties(article, articleVO);

        if (article.getCategoryId() != null) {
            Category category = categoryMapper.selectById(article.getCategoryId());
            if (category != null) {
                articleVO.setCategoryName(category.getName());
            }
        }

        if (article.getAuthorId() != null) {
            User user = userMapper.selectById(article.getAuthorId());
            if (user != null) {
                articleVO.setAuthorName(user.getNickname());
            }
        }

        List<ArticleTag> articleTags = articleTagMapper.selectList(
                Wrappers.<ArticleTag>lambdaQuery()
                        .eq(ArticleTag::getArticleId, article.getId())
        );

        if (articleTags != null && !articleTags.isEmpty()) {
            List<Long> tagIds = articleTags.stream()
                    .map(ArticleTag::getTagId)
                    .collect(Collectors.toList());

            List<Tag> tags = tagMapper.selectByIds(tagIds);
            List<TagVO> tagVOList = tags.stream()
                    .map(tag -> new TagVO(tag.getId(), tag.getName()))
                    .collect(Collectors.toList());

            articleVO.setTags(tagVOList);
        } else {
            articleVO.setTags(new ArrayList<>());
        }

        //获得评论数
        Long commentCount = commentMapper.selectCount(new QueryWrapper<Comment>()
                .eq("article_id", article.getId()));
        articleVO.setCommentCount(Math.toIntExact(commentCount));

        return articleVO;
    }

    private void addOrderBy(QueryWrapper<Article> wrapper, String sortBy, Boolean isAsc) {
        wrapper.orderBy(true, isAsc, sortBy);
    }

    private <T> PageQueryVO<T> toPageVO(IPage<?> page, List<T> records) {
        return new PageQueryVO<>(records, page.getTotal(), page.getSize(), page.getCurrent(), page.getPages());
    }
}
