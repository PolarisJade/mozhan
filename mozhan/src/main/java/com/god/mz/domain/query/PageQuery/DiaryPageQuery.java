package com.god.mz.domain.query.PageQuery;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class DiaryPageQuery extends PageQuery{
    private String sortBy = "diary_date";
}
