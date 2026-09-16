package com.god.mz.service;

import com.god.mz.domain.dto.CommentDTO;
import com.god.mz.domain.po.Comment;
import com.baomidou.mybatisplus.extension.service.IService;
import com.god.mz.domain.query.PageQuery.CommentPageQuery;
import com.god.mz.domain.query.PageQuery.PageQueryVO;
import com.god.mz.domain.vo.comment.AdminCommentVO;
import com.god.mz.domain.vo.comment.CommentVO;
import com.god.mz.domain.vo.comment.MyCommentVO;

/**
 * <p>
 * 文章评论表 服务类
 * </p>
 *
 * @author God
 * @since 2026-05-09
 */
public interface ICommentService extends IService<Comment> {

    CommentVO addComment(CommentDTO commentDTO);

    void deleteComment(Long id);

    PageQueryVO<CommentVO> getArticleComments(Long articleId, CommentPageQuery query);

    PageQueryVO<MyCommentVO> getMyComments(CommentPageQuery query);

    PageQueryVO<CommentVO> queryCommentPage(Integer pageNum, Integer pageSize, Long articleId, Long userId);

    /**
     * 后台：评论分页，按「一级评论 + 其下回复」两级返回。
     *
     * @param articleId 只看某篇文章的评论，可为空
     * @param userId    只看某人发的一级评论，可为空
     */
    PageQueryVO<AdminCommentVO> queryAdminCommentPage(Integer pageNum, Integer pageSize, Long articleId, Long userId);

}
