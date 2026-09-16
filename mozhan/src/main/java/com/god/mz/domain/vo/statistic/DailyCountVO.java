package com.god.mz.domain.vo.statistic;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工作台趋势图上的一天。
 * <p>
 * 区间内没有数据的日期也会出现在序列里（补 0），这样前端画柱状图时不需要自己去补空洞，
 * x 轴也不会因为缺日期而错位。
 * </p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DailyCountVO {
    /**
     * 格式 yyyy-MM-dd
     */
    private String date;

    private Long userCount;

    private Long articleCount;

    private Long essayCount;
}
