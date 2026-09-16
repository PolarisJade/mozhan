package com.god.mz.domain.vo.category;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 后台分类列表项。
 * <p>
 * 刻意不复用 {@link CategoryItemVO}：那个 VO 同时被公开接口 {@code /category/list} 使用，
 * 且它的结果会写进 Redis 缓存。往里面塞 articleCount 会让前台缓存多背一份无用的聚合结果，
 * 而且 articleCount 是随文章增删变化的，混进长缓存里迟早读到脏数据。
 * </p>
 */
@Data
public class AdminCategoryVO {
    private Long id;

    private String name;

    private Integer sort;

    /**
     * 关联文章数。只统计未软删除的文章（article.del_flag = 0）。
     */
    private Long articleCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
