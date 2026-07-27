package com.god.mz.domain.query.PageQuery;

import lombok.Data;

@Data
public class PageQuery {
    private Integer pageNum = 1;
    private Integer pageSize = 20;
    private String sortBy = "create_time";
    private Boolean isAsc = false;
}
