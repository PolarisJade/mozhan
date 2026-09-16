package com.god.mz.controller.admin;

import com.god.mz.domain.query.PageQuery.PageQueryVO;
import com.god.mz.domain.vo.comment.AdminCommentVO;
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

    /**
     * 两级返回：records 里每条是一级评论，其 replies 是它下面的回复。
     * 分页只对一级评论生效，total 也是按一级评论数算的。
     */
    @GetMapping("/page")
    public Result<PageQueryVO<AdminCommentVO>> queryCommentPage(
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long articleId,
            @RequestParam(required = false) Long userId) {

        PageQueryVO<AdminCommentVO> pageVO = commentService.queryAdminCommentPage(pageNum, pageSize, articleId, userId);
        return Result.success(pageVO);
    }
}

