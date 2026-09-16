package com.god.mz.domain.query.PageQuery;

import lombok.Data;

/**
 * 后台文章列表查询条件。
 */
@Data
public class AdminArticlePageQuery {
    private Integer pageNum = 1;

    private Integer pageSize = 10;

    /**
     * 关键词，模糊匹配标题。
     */
    private String keyword;

    private Long categoryId;

    private Long tagId;

    /**
     * 文章状态。
     * <p>
     * 这里刻意用 {@code String} 而不是 {@code ArticleStatusEnum}：
     * 查询参数走的是 Spring 的 {@code Enum.valueOf}，只认枚举名 {@code PUBLISHED}/{@code DRAFT}，
     * 而列表出参走 Jackson 的 {@code @JsonValue}，给的是中文「发布」/「草稿」。
     * 前端如果把列表里的值原样回传当筛选条件，声明成枚举就会直接 400。
     * 收 String 再自行兼容两种写法，可以把这个坑彻底填掉。
     * </p>
     */
    private String status;
}
