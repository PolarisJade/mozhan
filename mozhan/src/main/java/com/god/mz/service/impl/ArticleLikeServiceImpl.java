package com.god.mz.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.god.mz.common.enums.BizCodeEnum;
import com.god.mz.domain.po.Article;
import com.god.mz.domain.po.ArticleLike;
import com.god.mz.domain.po.User;
import com.god.mz.domain.po.UserFollow;
import com.god.mz.domain.query.cursorQuery.CursorPageVO;
import com.god.mz.domain.vo.user.BaseUserVO;
import com.god.mz.exception.BizException;
import com.god.mz.mapper.ArticleLikeMapper;
import com.god.mz.mapper.ArticleMapper;
import com.god.mz.mapper.UserFollowMapper;
import com.god.mz.mapper.UserMapper;
import com.god.mz.service.IArticleLikeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.god.mz.util.CursorCodeUtil;
import com.god.mz.util.UserContext;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
    @Resource
    private ArticleMapper articleMapper;
    @Resource
    private UserFollowMapper userFollowMapper;
    @Resource
    private UserMapper userMapper;

    @Override
    public Integer likeArticle(Long articleId) {
        Long userId = UserContext.getUserId();
        Article article = articleMapper.selectById(articleId);
        if (article == null){
            throw new BizException(BizCodeEnum.ARTICLE_NOT_EXIST);
        }

        //判断是否已经点赞
        ArticleLike like = lambdaQuery()
                .eq(ArticleLike::getArticleId, articleId)
                .eq(ArticleLike::getUserId, userId)
                .one();
        if (like != null) {
            // 已经点过赞则取消点赞
            boolean success = removeById(like.getId());
            if (!success) {
                throw new BizException(BizCodeEnum.OPERATION_FAILURE);
            }
        } else {
            // 未点过赞则新增点赞记录
            ArticleLike articleLike = new ArticleLike();
            articleLike.setArticleId(articleId);
            articleLike.setUserId(userId);
            boolean success = save(articleLike);
            if (!success) {
                throw new BizException(BizCodeEnum.OPERATION_FAILURE);
            }
        }

        Long count = lambdaQuery()
                .eq(ArticleLike::getArticleId, articleId)
                .count();
        return Integer.parseInt(String.valueOf(count));
    }

    @Override
    public CursorPageVO<BaseUserVO> getLikeList(Long cursor, Integer pageSize,Long articleId) {
        // 获取当前用户ID
        Long currentUserId = UserContext.getUserId();

        // 验证文章是否存在
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new BizException(BizCodeEnum.ARTICLE_NOT_EXIST);
        }

        // 构建查询条件 - 查询该文章的点赞记录
        QueryWrapper<ArticleLike> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("article_id", articleId);

        // 游标分页处理
        if (cursor != null) {
            queryWrapper.lt("create_time",
                    lambdaQuery()
                            .select(ArticleLike::getCreateTime)
                            .eq(ArticleLike::getId, cursor)
                            .one()
                            .getCreateTime()
            );
        }

        // 按创建时间倒序排列，多查一条判断是否有更多数据
        queryWrapper.orderByDesc("create_time");
        queryWrapper.last("LIMIT " + (pageSize + 1));

        List<ArticleLike> likeList = list(queryWrapper);

        // 判断是否有更多数据
        boolean hasMore = likeList.size() > pageSize;
        if (hasMore) {
            likeList = likeList.subList(0, pageSize);
        }

        // 转换为 VO 对象
        List<BaseUserVO> voList = new ArrayList<>();
        if (!likeList.isEmpty()) {
            // 批量获取用户ID
            List<Long> userIds = likeList.stream()
                    .map(ArticleLike::getUserId)
                    .toList();

            // 批量查询用户信息
            List<User> users = userMapper.selectByIds(userIds);

            // 获取当前用户关注的用户列表（用于设置 isFollowed 字段）
            Set<Long> followedUserIds = new HashSet<>();
            if (currentUserId != null) {
                List<UserFollow> follows = userFollowMapper.selectList(
                        new QueryWrapper<UserFollow>().eq("user_id", currentUserId)
                );
                followedUserIds = follows.stream()
                        .map(UserFollow::getFollowId)
                        .collect(Collectors.toSet());
            }

            // 构建 VO 对象
            for (ArticleLike like : likeList) {
                User user = users.stream()
                        .filter(u -> u.getId().equals(like.getUserId()))
                        .findFirst()
                        .orElse(null);

                if (user != null) {
                    BaseUserVO vo = new BaseUserVO();
                    vo.setId(user.getId());
                    vo.setNickname(user.getNickname());
                    vo.setAvatar(user.getAvatar());
                    vo.setIsFollowed(followedUserIds.contains(user.getId()));
                    voList.add(vo);
                }
            }
        }

        // 计算下一个游标
        Long nextCursor = voList.isEmpty() ? null : likeList.getLast().getId();

        return new CursorPageVO<>(voList, hasMore,
                nextCursor != null ? CursorCodeUtil.encode(List.of(nextCursor)) : null);

    }
}
