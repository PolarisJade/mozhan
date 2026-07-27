package com.god.mz.controller.user;

import com.god.mz.domain.query.PageQuery.PageQueryVO;
import com.god.mz.domain.query.PageQuery.SearchPageQuery;
import com.god.mz.domain.vo.article.ArticleVO;
import com.god.mz.domain.vo.user.BaseUserVO;
import com.god.mz.domain.vo.Result;
import com.god.mz.service.ISearchService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/search")
public class SearchController {
    @Resource
    private ISearchService searchService;

    @GetMapping("/article")
    public Result<PageQueryVO<ArticleVO>> searchArticle(SearchPageQuery query){
        PageQueryVO<ArticleVO> vo = searchService.searchArticle(query);
        return Result.success(vo);
    }

    @GetMapping("/user")
    public Result<PageQueryVO<BaseUserVO>> searchUser(SearchPageQuery query){
        PageQueryVO<BaseUserVO> vo = searchService.searchUser(query);
        return Result.success(vo);
    }
}
