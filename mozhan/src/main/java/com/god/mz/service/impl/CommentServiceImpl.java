package com.god.mz.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.god.mz.common.enums.BizCodeEnum;
import com.god.mz.domain.dto.CommentDTO;
import com.god.mz.domain.po.Article;
import com.god.mz.domain.po.Comment;
import com.god.mz.domain.po.User;
import com.god.mz.domain.query.PageQuery.CommentPageQuery;
import com.god.mz.domain.query.PageQuery.PageQueryVO;
import com.god.mz.domain.vo.comment.CommentVO;
import com.god.mz.domain.vo.comment.MyCommentVO;
import com.god.mz.exception.BizException;
import com.god.mz.mapper.ArticleMapper;
import com.god.mz.mapper.CommentMapper;
import com.god.mz.mapper.UserMapper;
import com.god.mz.service.ICommentService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.god.mz.util.UserContext;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 文章评论表 服务实现类
 * </p>
 *
 * @author God
 * @since 2026-05-09
 */
@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements ICommentService {
    @Resource
    private UserMapper userMapper;
    @Resource
    private ArticleMapper articleMapper;

    @Override
    public CommentVO addComment(CommentDTO commentDTO) {
        Long userId = UserContext.getUserId();

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(BizCodeEnum.USER_NOT_FOUND);
        }

        Comment comment = BeanUtil.copyProperties(commentDTO, Comment.class);
        comment.setUserId(userId);
        comment.setDelFlag(false);
        boolean success = save(comment);
        if (!success) {
            throw new BizException(BizCodeEnum.OPERATION_FAILURE);
        }

        CommentVO vo = buildCommentVO(comment, user, commentDTO.getArticleId());

        if (commentDTO.getParentId() != null && commentDTO.getParentId() > 0) {
            Long replyToId = commentDTO.getReplyToId() != null ? commentDTO.getReplyToId() : commentDTO.getParentId();
            Comment repliedComment = getById(replyToId);

            if (repliedComment != null) {
                vo.setReplyToId(repliedComment.getId());

                if (Boolean.TRUE.equals(repliedComment.getDelFlag())) {
                    vo.setReplyToNickname("评论已删除");
                } else {
                    User repliedUser = userMapper.selectById(repliedComment.getUserId());
                    if (repliedUser != null) {
                        vo.setReplyToNickname(repliedUser.getNickname());
                    }
                }
            }
        }
        return vo;
    }


    @Transactional
    @Override
    public void deleteComment(Long id) {
        Comment comment = getById(id);
        if (comment == null) {
            throw new BizException(BizCodeEnum.COMMENT_NOT_EXIST);
        }

        if (!comment.getUserId().equals(UserContext.getUserId())) {
            throw new BizException(BizCodeEnum.USER_NOT_AUTH);
        }

        if (comment.getParentId() == null || comment.getParentId() == 0) {
            List<Comment> replies = list(Wrappers.lambdaQuery(Comment.class)
                    .eq(Comment::getParentId, id));

            for (Comment reply : replies) {
                reply.setDelFlag(true);
            }
            boolean success = updateBatchById(replies);
            if (!success) {
                throw new BizException(BizCodeEnum.OPERATION_FAILURE);
            }
        }

        comment.setDelFlag(true);
        boolean success = updateById(comment);
        if (!success) {
            throw new BizException(BizCodeEnum.OPERATION_FAILURE);
        }
    }

    @Override
    public PageQueryVO<CommentVO> getArticleComments(Long articleId, CommentPageQuery query) {
        Page<Comment> page = new Page<>(query.getPageNum(), query.getPageSize());

        LambdaQueryWrapper<Comment> wrapper = Wrappers.lambdaQuery(Comment.class);
        wrapper.eq(Comment::getArticleId, articleId)
                .eq(Comment::getParentId, 0)
                .eq(Comment::getDelFlag, false)
                .orderBy(true, query.getIsAsc(), Comment::getCreateTime);

        IPage<Comment> result = page(page, wrapper);
        List<Comment> parentComments = result.getRecords();

        if (parentComments.isEmpty()) {
            return emptyPageVO();
        }

        List<Long> parentIds = parentComments.stream().map(Comment::getId).collect(Collectors.toList());

        Map<Long, List<Comment>> repliesMap = list(Wrappers.lambdaQuery(Comment.class)
                .in(Comment::getParentId, parentIds)
                .eq(Comment::getDelFlag, false)
                .orderByAsc(Comment::getCreateTime))
                .stream()
                .collect(Collectors.groupingBy(Comment::getParentId));

        Set<Long> allUserIds = new HashSet<>();
        parentComments.forEach(c -> allUserIds.add(c.getUserId()));
        repliesMap.values().forEach(replies -> replies.forEach(r -> allUserIds.add(r.getUserId())));

        Map<Long, User> userMap = userMapper.selectByIds(allUserIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        Article article = articleMapper.selectById(articleId);
        Long authorId = article != null ? article.getAuthorId() : null;

        List<CommentVO> voList = parentComments.stream().map(parent -> {
            User user = userMap.get(parent.getUserId());
            CommentVO parentVO = buildCommentVO(parent, user, authorId);

            List<Comment> replies = repliesMap.getOrDefault(parent.getId(), new ArrayList<>());
            List<CommentVO> replyVOs = replies.stream().map(reply -> {
                User replyUser = userMap.get(reply.getUserId());
                CommentVO replyVO = buildCommentVO(reply, replyUser, authorId);
                return getCommentVO(userMap, reply, replyVO);
            }).collect(Collectors.toList());

            parentVO.setReplies(replyVOs);
            parentVO.setTotalReplies(replyVOs.size());
            return parentVO;
        }).collect(Collectors.toList());

        return toPageVO(result, voList);
    }

    private CommentVO getCommentVO(Map<Long, User> userMap, Comment reply, CommentVO replyVO) {
        if (reply.getParentId() != null && reply.getParentId() > 0) {
            Long replyToId = reply.getReplyToId() != null ? reply.getReplyToId() : reply.getParentId();
            Comment repliedComment = getById(replyToId);

            if (repliedComment != null) {
                replyVO.setReplyToId(repliedComment.getId());

                if (Boolean.TRUE.equals(repliedComment.getDelFlag())) {
                    replyVO.setReplyToNickname("评论已删除");
                } else {
                    User repliedUser = userMap.get(repliedComment.getUserId());
                    if (repliedUser != null) {
                        replyVO.setReplyToNickname(repliedUser.getNickname());
                    }
                }
            }
        }
        return replyVO;
    }

    @Override
    public PageQueryVO<MyCommentVO> getMyComments(CommentPageQuery query) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BizException(BizCodeEnum.USER_NOT_AUTH);
        }

        Page<Comment> page = new Page<>(query.getPageNum(), query.getPageSize());

        LambdaQueryWrapper<Comment> wrapper = Wrappers.lambdaQuery(Comment.class);
        wrapper.eq(Comment::getUserId, userId)
                .eq(Comment::getDelFlag, false)
                .orderBy(true, query.getIsAsc(), Comment::getCreateTime);

        IPage<Comment> result = page(page, wrapper);

        if (result.getRecords().isEmpty()) {
            return emptyPageVO();
        }

        List<Comment> comments = result.getRecords();

        List<Long> articleIds = comments.stream()
                .map(Comment::getArticleId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, String> articleTitleMap = articleMapper.selectByIds(articleIds).stream()
                .collect(Collectors.toMap(Article::getId, Article::getTitle));

        Set<Long> repliedCommentIds = new HashSet<>();
        comments.forEach(comment -> {
            if (comment.getReplyToId() != null) {
                repliedCommentIds.add(comment.getReplyToId());
            } else if (comment.getParentId() != null && comment.getParentId() > 0) {
                repliedCommentIds.add(comment.getParentId());
            }
        });

        final Map<Long, Comment> repliedCommentsMap = !repliedCommentIds.isEmpty()
                ? listByIds(repliedCommentIds).stream().collect(Collectors.toMap(Comment::getId, c -> c))
                : Collections.emptyMap();

        Set<Long> repliedUserIds = repliedCommentsMap.values().stream()
                .map(Comment::getUserId)
                .collect(Collectors.toSet());

        final Map<Long, User> repliedUserMap = !repliedUserIds.isEmpty()
                ? userMapper.selectByIds(repliedUserIds).stream().collect(Collectors.toMap(User::getId, u -> u))
                : Collections.emptyMap();

        List<MyCommentVO> voList = comments.stream().map(comment -> {
            MyCommentVO vo = new MyCommentVO();
            vo.setId(comment.getId());
            vo.setArticleId(comment.getArticleId());
            vo.setArticleTitle(articleTitleMap.getOrDefault(comment.getArticleId(), "未知文章"));
            vo.setParentId(comment.getParentId());
            vo.setContent(comment.getContent());
            vo.setCreateTime(comment.getCreateTime());

            Long replyToId = comment.getReplyToId() != null ? comment.getReplyToId() : comment.getParentId();
            if (replyToId != null && replyToId > 0) {
                vo.setReplyToId(replyToId);

                Comment repliedComment = repliedCommentsMap.get(replyToId);
                if (repliedComment != null) {
                    if (Boolean.TRUE.equals(repliedComment.getDelFlag())) {
                        vo.setReplyToNickname("评论已删除");
                    } else {
                        User repliedUser = repliedUserMap.get(repliedComment.getUserId());
                        if (repliedUser != null) {
                            vo.setReplyToNickname(repliedUser.getNickname());
                        }
                    }
                }
            }

            return vo;
        }).collect(Collectors.toList());

        return toPageVO(result, voList);
    }

    @Override
    public PageQueryVO<CommentVO> queryCommentPage(Integer pageNum, Integer pageSize, Long articleId, Long userId) {
        Page<Comment> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<Comment> queryWrapper = new LambdaQueryWrapper<>();

        if (articleId != null) {
            queryWrapper.eq(Comment::getArticleId, articleId);
        }

        if (userId != null) {
            queryWrapper.eq(Comment::getUserId, userId);
        }

        queryWrapper.orderByDesc(Comment::getCreateTime);

        IPage<Comment> commentPage = page(page, queryWrapper);

        List<Long> userIds = commentPage.getRecords().stream()
                .map(Comment::getUserId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            userMap = userMapper.selectByIds(userIds).stream()
                    .collect(Collectors.toMap(User::getId, u -> u));
        }

        final Map<Long, User> finalUserMap = userMap;

        List<CommentVO> voList = commentPage.getRecords().stream().map(comment -> {
            CommentVO vo = new CommentVO();
            BeanUtil.copyProperties(comment, vo);

            User user = finalUserMap.get(comment.getUserId());
            if (user != null) {
                vo.setNickname(user.getNickname());
                vo.setAvatar(user.getAvatar());
            } else {
                vo.setNickname("未知用户");
                vo.setAvatar("");
            }

            vo.setIsAuthor(false);

            return getCommentVO(finalUserMap, comment, vo);
        }).collect(Collectors.toList());

        PageQueryVO<CommentVO> result = new PageQueryVO<>();
        result.setRecords(voList);
        result.setTotal(commentPage.getTotal());
        result.setPageSize(commentPage.getSize());
        result.setPageNo(commentPage.getCurrent());
        result.setPages(commentPage.getPages());

        return result;
    }


    private CommentVO buildCommentVO(Comment comment, User user, Long authorId) {
        CommentVO vo = new CommentVO();
        BeanUtil.copyProperties(comment, vo);

        if (user != null) {
            vo.setNickname(user.getNickname());
            vo.setAvatar(user.getAvatar());
        }

        if (authorId != null) {
            vo.setIsAuthor(authorId.equals(comment.getUserId()));
        } else {
            vo.setIsAuthor(false);
        }

        return vo;
    }

    private <T> PageQueryVO<T> emptyPageVO() {
        return new PageQueryVO<>(Collections.emptyList(), 0L, 0L, 1L, 0L);
    }

    private <T> PageQueryVO<T> toPageVO(IPage<?> page, List<T> records) {
        return new PageQueryVO<>(records, page.getTotal(), page.getSize(), page.getCurrent(), page.getPages());
    }
}
