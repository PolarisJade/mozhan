package com.god.mz.controller.user;


import com.god.mz.domain.query.cursorQuery.CursorPageVO;
import com.god.mz.domain.vo.user.BaseUserVO;
import com.god.mz.domain.vo.Result;
import com.god.mz.service.IArticleLikeService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 文章点赞记录表 前端控制器
 * </p>
 *
 * @author God
 * @since 2026-05-09
 */
@RestController
@RequestMapping("/article/like")
public class ArticleLikeController {
    @Resource
    private IArticleLikeService articleLikeService;

    @PostMapping("/id")
    public Result<Integer> likeArticle(Long articleId) {
        Integer likeCount = articleLikeService.likeArticle(articleId);
        return Result.success(likeCount);
    }

    @GetMapping("users/{articleId}")
    public Result<CursorPageVO<BaseUserVO>> getLikeList(@RequestParam Long cursor,
                                                        @RequestParam Integer pageSize,
                                                        @PathVariable Long articleId) {
        CursorPageVO<BaseUserVO> vo = articleLikeService.getLikeList(cursor, pageSize, articleId);
        return Result.success(vo);
    }
}
