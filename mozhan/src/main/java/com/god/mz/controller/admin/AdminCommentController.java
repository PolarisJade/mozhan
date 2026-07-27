package com.god.mz.controller.admin;

import com.god.mz.domain.query.PageQuery.PageQueryVO;
import com.god.mz.domain.vo.comment.CommentVO;
import com.god.mz.domain.vo.Result;
import com.god.mz.service.ICommentService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/comment")
public class AdminCommentController {
    @Resource
    private ICommentService commentService;

    @DeleteMapping("/{id}")
    public Result<Void> deleteComment(@PathVariable Long id) {
        commentService.deleteComment(id);
        return Result.success();
    }

    @GetMapping("/page")
    public Result<PageQueryVO<CommentVO>> queryCommentPage(
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long articleId,
            @RequestParam(required = false) Long userId) {

        PageQueryVO<CommentVO> pageVO = commentService.queryCommentPage(pageNum, pageSize, articleId, userId);
        return Result.success(pageVO);
    }
}

