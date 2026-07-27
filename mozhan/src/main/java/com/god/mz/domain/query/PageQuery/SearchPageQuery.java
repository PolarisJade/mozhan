package com.god.mz.domain.query.PageQuery;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class SearchPageQuery extends PageQuery{
    private String keyword;
}
