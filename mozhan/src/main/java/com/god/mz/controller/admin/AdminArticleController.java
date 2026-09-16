package com.god.mz.controller.admin;

import com.god.mz.domain.query.PageQuery.AdminArticlePageQuery;
import com.god.mz.domain.query.PageQuery.PageQueryVO;
import com.god.mz.domain.vo.Result;
import com.god.mz.domain.vo.article.AdminArticleDetailVO;
import com.god.mz.domain.vo.article.AdminArticleVO;
import com.god.mz.service.IArticleService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/article")
public class AdminArticleController {
    @Resource
    private IArticleService articleService;

    @GetMapping("/page")
    public Result<PageQueryVO<AdminArticleVO>> listArticle(AdminArticlePageQuery query) {
        return Result.success(articleService.listAdminArticle(query));
    }

    /**
     * 查看文章内容。路径上带 /content 而不是直接 GET /{id}，
     * 是为了和「列表项」区分开——这个接口会白白带出整篇正文，不该被列表顺手调用。
     */
    @GetMapping("/{id}/content")
    public Result<AdminArticleDetailVO> getArticleContent(@PathVariable Long id) {
        return Result.success(articleService.getAdminArticleDetail(id));
    }
}
