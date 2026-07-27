package com.god.mz.domain.query.PageQuery;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class CommentPageQuery extends PageQuery {
    private Long articleId;
    private Long userId;
}
