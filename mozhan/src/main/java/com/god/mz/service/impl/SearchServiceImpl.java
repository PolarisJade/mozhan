package com.god.mz.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.god.mz.common.enums.ArticleStatusEnum;
import com.god.mz.common.enums.BizCodeEnum;
import com.god.mz.domain.po.Article;
import com.god.mz.domain.po.User;
import com.god.mz.domain.query.PageQuery.PageQueryVO;
import com.god.mz.domain.query.PageQuery.SearchPageQuery;
import com.god.mz.domain.vo.article.ArticleVO;
import com.god.mz.domain.vo.user.BaseUserVO;
import com.god.mz.exception.BizException;
import com.god.mz.mapper.ArticleMapper;
import com.god.mz.mapper.UserMapper;
import com.god.mz.service.ISearchService;
import com.god.mz.util.ArticleVOBuilder;
import com.god.mz.util.FollowHelper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchServiceImpl implements ISearchService {
    @Resource
    private UserMapper userMapper;
    @Resource
    private ArticleMapper articleMapper;
    @Resource
    private FollowHelper followHelper;
    @Resource
    private ArticleVOBuilder articleVOBuilder;

    @Override
    public PageQueryVO<ArticleVO> searchArticle(SearchPageQuery query) {
        String keyword = validateKeyword(query);
        Page<Article> page = new Page<>(query.getPageNum(), query.getPageSize());

        LambdaQueryWrapper<Article> wrapper = Wrappers.lambdaQuery(Article.class);
        wrapper.eq(Article::getDelFlag, false)
                .eq(Article::getStatus, ArticleStatusEnum.PUBLISHED)
                .and(w -> w.like(Article::getTitle, keyword)
                        .or()
                        .like(Article::getSummary, keyword));
        wrapper.orderBy(true, query.getIsAsc(), Article::getCreateTime);

        IPage<Article> result = articleMapper.selectPage(page, wrapper);
        List<ArticleVO> voList = articleVOBuilder.build(result.getRecords());
        return toPageVO(result, voList);
    }

    @Override
    public PageQueryVO<BaseUserVO> searchUser(SearchPageQuery query) {
        String keyword = validateKeyword(query);
        Page<User> page = new Page<>(query.getPageNum(), query.getPageSize());

        LambdaQueryWrapper<User> wrapper = Wrappers.lambdaQuery(User.class);
        wrapper.and(w -> w.like(User::getNickname, keyword)
                .or()
                .like(User::getUsername, keyword));
        wrapper.orderBy(true, query.getIsAsc(), User::getCreateTime);

        IPage<User> result = userMapper.selectPage(page, wrapper);

        if (result.getRecords().isEmpty()) {
            return emptyPageVO();
        }

        Set<Long> followedUserIds = followHelper.getFollowedUserIds(
                result.getRecords().stream().map(User::getId).collect(Collectors.toList()));

        List<BaseUserVO> voList = result.getRecords().stream().map(user -> {
            BaseUserVO vo = BeanUtil.copyProperties(user, BaseUserVO.class);
            vo.setIsFollowed(followedUserIds.contains(user.getId()));
            return vo;
        }).collect(Collectors.toList());

        return toPageVO(result, voList);
    }

    private static String validateKeyword(SearchPageQuery query) {
        if (query.getKeyword() == null || query.getKeyword().trim().isEmpty()) {
            throw new BizException(BizCodeEnum.DATA_ERROR);
        }
        return query.getKeyword().trim();
    }

    private static <T> PageQueryVO<T> emptyPageVO() {
        return new PageQueryVO<>(Collections.emptyList(), 0L, 0L, 1L, 0L);
    }

    private static <T> PageQueryVO<T> toPageVO(IPage<?> page, List<T> records) {
        return new PageQueryVO<>(records, page.getTotal(), page.getSize(), page.getCurrent(), page.getPages());
    }
}
