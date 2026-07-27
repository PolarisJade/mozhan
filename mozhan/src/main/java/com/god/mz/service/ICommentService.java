package com.god.mz.service;

import com.god.mz.domain.dto.CommentDTO;
import com.god.mz.domain.po.Comment;
import com.baomidou.mybatisplus.extension.service.IService;
import com.god.mz.domain.query.PageQuery.CommentPageQuery;
import com.god.mz.domain.query.PageQuery.PageQueryVO;
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

}
