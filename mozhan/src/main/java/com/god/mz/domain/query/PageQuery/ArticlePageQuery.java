package com.god.mz.domain.query.PageQuery;

import com.god.mz.common.enums.ArticleStatusEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ArticlePageQuery extends PageQuery {
    private Long authorId;
    private Long categoryId;
    private ArticleStatusEnum status;
}
