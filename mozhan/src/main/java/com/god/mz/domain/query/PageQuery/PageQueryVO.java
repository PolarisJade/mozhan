package com.god.mz.domain.query.PageQuery;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageQueryVO<T> {
    /**
     * 数据列表
     */
    private List<T> records;
    /**
     * 总记录数
     */
    private Long total;
    /**
     * 每页大小
     */
    private Long pageSize;
    /**
     * 当前页码
     */
    private Long pageNo;
    /**
     * 总页数
     */
    private Long pages;
}
