package com.god.mz.controller.user;


import com.god.mz.domain.dto.ArticleDTO;
import com.god.mz.domain.query.PageQuery.ArticlePageQuery;
import com.god.mz.domain.query.PageQuery.PageQueryVO;
import com.god.mz.domain.vo.*;
import com.god.mz.domain.vo.article.ArticleDetailVO;
import com.god.mz.domain.vo.article.ArticleInfoVO;
import com.god.mz.domain.vo.article.ArticleVO;
import com.god.mz.domain.vo.article.HotArticleVO;
import com.god.mz.service.IArticleService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;


/**
 * <p>
 * 文章信息表 前端控制器
 * </p>
 *
 * @author God
 * @since 2026-05-09
 */
@RestController
@RequestMapping("/article")
public class ArticleController {
    @Resource
    private IArticleService articleService;

    @PostMapping
    public Result<ArticleVO> addArticle(@RequestBody ArticleDTO articleDTO) {
        ArticleVO vo = articleService.addArticle(articleDTO);
        return Result.success(vo);
    }

    @PutMapping("/update/{id}")
    public Result<Void> updateArticle(@PathVariable Long id, @RequestBody ArticleDTO articleDTO) {
        articleService.updateArticle(id, articleDTO);
        return Result.success();
    }

    @GetMapping("/info/{id}")
    public Result<ArticleInfoVO> getArticleInfoById(@PathVariable Long id) {
        ArticleInfoVO vo = articleService.getArticleInfoById(id);
        return Result.success(vo);
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteArticle(@PathVariable Long id) {
        articleService.deleteArticle(id);
        return Result.success();
    }

    @GetMapping("detail/{id}")
    public Result<ArticleDetailVO> getArticleDetail(@PathVariable Long id) {
        ArticleDetailVO vo = articleService.getArticleDetail(id);
        return Result.success(vo);
    }

    @GetMapping("/list")
    public Result<PageQueryVO<ArticleVO>> listArticle(ArticlePageQuery query) {
        PageQueryVO<ArticleVO> vo = articleService.listArticle(query);
        return Result.success(vo);
    }

    @GetMapping("/my")
    public Result<PageQueryVO<ArticleVO>> listMyArticle(ArticlePageQuery  query) {
        PageQueryVO<ArticleVO> vo = articleService.listMyArticle(query);
        return Result.success(vo);
    }

    @PutMapping("/publish/{id}")
    public Result<Void> publishArticle(@PathVariable Long id) {
        articleService.publishArticle(id);
        return Result.success();
    }

    @PutMapping("/top/{id}")
    public Result<Void> topArticle(@PathVariable Long id,
                                   @RequestParam Boolean top) {
        articleService.topArticle(id, top);
        return Result.success();
    }

    @GetMapping("hot")
    public Result<PageQueryVO<HotArticleVO>> getHotArticle(ArticlePageQuery query) {
        PageQueryVO<HotArticleVO> vo = articleService.getHotArticle(query);
        return Result.success(vo);
    }
}
