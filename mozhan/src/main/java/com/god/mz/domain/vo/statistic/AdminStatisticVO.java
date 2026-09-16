package com.god.mz.domain.vo.statistic;

import lombok.Data;

import java.util.List;

/**
 * 后台工作台总览。
 */
@Data
public class AdminStatisticVO {
    /**
     * 查询区间的起止（含首尾）。回显给前端，避免前端算的区间和后端实际统计的不一致。
     */
    private String startDate;

    private String endDate;

    /**
     * 区间内的新增量
     */
    private Long newUserCount;

    private Long newArticleCount;

    private Long newEssayCount;

    /**
     * 全站累计量（不受区间影响）
     */
    private Long totalUserCount;

    private Long totalArticleCount;

    private Long totalEssayCount;

    /**
     * 区间内按天拆分的趋势，缺失日期已补 0
     */
    private List<DailyCountVO> dailyTrend;
}
