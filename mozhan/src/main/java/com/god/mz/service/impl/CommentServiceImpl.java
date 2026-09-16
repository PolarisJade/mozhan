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
import com.god.mz.domain.vo.comment.AdminCommentVO;
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


    @Override
    public PageQueryVO<AdminCommentVO> queryAdminCommentPage(Integer pageNum, Integer pageSize, Long articleId,
            Long userId) {
        // 1. 先分页拿一级评论。parent_id = 0 是一级，其余指向所属一级评论的 id（只有两层）。
        Page<Comment> page = new Page<>(pageNum != null ? pageNum : 1, pageSize != null ? pageSize : 10);

        LambdaQueryWrapper<Comment> wrapper = Wrappers.lambdaQuery(Comment.class)
                .eq(Comment::getParentId, 0)
                .eq(Comment::getDelFlag, false);
        if (articleId != null) {
            wrapper.eq(Comment::getArticleId, articleId);
        }
        if (userId != null) {
            wrapper.eq(Comment::getUserId, userId);
        }
        wrapper.orderByDesc(Comment::getCreateTime);

        IPage<Comment> result = page(page, wrapper);
        List<Comment> parents = result.getRecords();
        if (parents.isEmpty()) {
            return emptyPageVO();
        }

        // 2. 一次查出本页所有回复再分组，避免逐条一级评论去查子节点
        List<Long> parentIds = parents.stream().map(Comment::getId).collect(Collectors.toList());
        Map<Long, List<Comment>> repliesMap = list(Wrappers.lambdaQuery(Comment.class)
                .in(Comment::getParentId, parentIds)
                .eq(Comment::getDelFlag, false)
                .orderByAsc(Comment::getCreateTime))
                .stream()
                .collect(Collectors.groupingBy(Comment::getParentId));

        List<Comment> allComments = new ArrayList<>(parents);
        repliesMap.values().forEach(allComments::addAll);

        // 3. 用户昵称一次批量取
        Set<Long> userIds = allComments.stream().map(Comment::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = userIds.isEmpty()
                ? Collections.emptyMap()
                : userMapper.selectByIds(userIds).stream().collect(Collectors.toMap(User::getId, u -> u));

        // 4. 文章标题一次批量取，不要每行查一次
        Set<Long> articleIds = allComments.stream()
                .map(Comment::getArticleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> articleTitleMap = articleIds.isEmpty()
                ? Collections.emptyMap()
                : articleMapper.selectByIds(articleIds).stream()
                        .collect(Collectors.toMap(Article::getId, Article::getTitle));

        // 5. 回复的 reply_to_id 指向某条评论，被回复人的昵称靠这张 id -> userId 表定位。
        //    绝大多数目标都在上面的 allComments 里，只有被回复的评论已软删时才会落空。
        Map<Long, Long> commentAuthorMap = allComments.stream()
                .collect(Collectors.toMap(Comment::getId, Comment::getUserId));

        Set<Long> missingReplyToIds = allComments.stream()
                .filter(c -> c.getParentId() != null && c.getParentId() > 0)
                .map(c -> c.getReplyToId() != null && c.getReplyToId() > 0 ? c.getReplyToId() : c.getParentId())
                .filter(id -> !commentAuthorMap.containsKey(id))
                .collect(Collectors.toSet());

        // 已经软删的评论要显示「评论已删除」而不是空白，这需要知道它到底是不存在还是被删了，
        // 所以这里再补一次批量查询（仍然是一次，不是逐行）
        Set<Long> deletedCommentIds = missingReplyToIds.isEmpty()
                ? Collections.emptySet()
                : baseMapper.selectByIds(missingReplyToIds).stream()
                        .map(Comment::getId)
                        .collect(Collectors.toSet());

        List<AdminCommentVO> voList = parents.stream().map(parent -> {
            AdminCommentVO parentVO = buildAdminCommentVO(parent, userMap, articleTitleMap, commentAuthorMap,
                    deletedCommentIds);

            List<AdminCommentVO> replyVOs = repliesMap.getOrDefault(parent.getId(), new ArrayList<>())
                    .stream()
                    .map(reply -> buildAdminCommentVO(reply, userMap, articleTitleMap, commentAuthorMap,
                            deletedCommentIds))
                    .collect(Collectors.toList());

            parentVO.setReplies(replyVOs);
            parentVO.setTotalReplies(replyVOs.size());
            return parentVO;
        }).collect(Collectors.toList());

        return toPageVO(result, voList);
    }

    private AdminCommentVO buildAdminCommentVO(Comment comment, Map<Long, User> userMap,
            Map<Long, String> articleTitleMap, Map<Long, Long> commentAuthorMap, Set<Long> deletedCommentIds) {
        AdminCommentVO vo = new AdminCommentVO();
        vo.setId(comment.getId());
        vo.setParentId(comment.getParentId());
        vo.setReplyToId(comment.getReplyToId());
        vo.setUserId(comment.getUserId());
        vo.setContent(comment.getContent());
        vo.setArticleId(comment.getArticleId());
        vo.setArticleTitle(articleTitleMap.get(comment.getArticleId()));
        vo.setDelFlag(comment.getDelFlag());
        vo.setCreateTime(comment.getCreateTime());
        vo.setReplies(new ArrayList<>());

        User user = userMap.get(comment.getUserId());
        vo.setNickname(user != null ? user.getNickname() : "未知用户");
        vo.setAvatar(user != null ? user.getAvatar() : "");

        // 只有回复需要解析「回复了谁」；一级评论没有这个语义
        if (comment.getParentId() != null && comment.getParentId() > 0) {
            Long replyToId = comment.getReplyToId() != null && comment.getReplyToId() > 0
                    ? comment.getReplyToId()
                    : comment.getParentId();
            Long targetUserId = commentAuthorMap.get(replyToId);
            User target = targetUserId != null ? userMap.get(targetUserId) : null;
            if (target != null) {
                vo.setReplyToNickname(target.getNickname());
            } else if (deletedCommentIds.contains(replyToId)) {
                // 和前台评论区保持一致，别让后台看到一片空白还得去猜
                vo.setReplyToNickname("评论已删除");
            }
        }

        return vo;
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
