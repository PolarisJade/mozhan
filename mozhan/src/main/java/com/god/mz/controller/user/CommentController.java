package com.god.mz.controller.user;


import com.god.mz.domain.dto.CommentDTO;
import com.god.mz.domain.query.PageQuery.CommentPageQuery;
import com.god.mz.domain.query.PageQuery.PageQueryVO;
import com.god.mz.domain.vo.comment.CommentVO;
import com.god.mz.domain.vo.comment.MyCommentVO;
import com.god.mz.domain.vo.Result;
import com.god.mz.service.ICommentService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 文章评论表 前端控制器
 * </p>
 *
 * @author God
 * @since 2026-05-09
 */
@RestController
@RequestMapping("/comment")
public class CommentController {
    @Resource
    private ICommentService commentService;

    @PostMapping
    public Result<CommentVO> addComment(@RequestBody CommentDTO commentDTO) {
        CommentVO vo = commentService.addComment(commentDTO);
        return Result.success(vo);
    }

    @PostMapping("/reply")
    public Result<CommentVO> addReply(@RequestBody CommentDTO commentDTO) {
        CommentVO vo = commentService.addComment(commentDTO);
        return Result.success(vo);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteComment(@PathVariable Long id) {
        commentService.deleteComment(id);
        return Result.success();
    }

    @GetMapping("/{articleId}")
    public Result<PageQueryVO<CommentVO>> getArticleComments(
            @PathVariable Long articleId,
            CommentPageQuery query) {
        PageQueryVO<CommentVO> page = commentService.getArticleComments(articleId, query);
        return Result.success(page);
    }

    @GetMapping("/my")
    public Result<PageQueryVO<MyCommentVO>> getMyComments(CommentPageQuery query) {
        PageQueryVO<MyCommentVO> page = commentService.getMyComments(query);
        return Result.success(page);
    }
}
