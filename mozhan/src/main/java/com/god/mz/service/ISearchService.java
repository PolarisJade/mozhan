package com.god.mz.service;

import com.god.mz.domain.query.PageQuery.PageQueryVO;
import com.god.mz.domain.query.PageQuery.SearchPageQuery;
import com.god.mz.domain.vo.article.ArticleVO;
import com.god.mz.domain.vo.user.BaseUserVO;

public interface ISearchService {

    PageQueryVO<ArticleVO> searchArticle(SearchPageQuery query);

    PageQueryVO<BaseUserVO> searchUser(SearchPageQuery query);
}
