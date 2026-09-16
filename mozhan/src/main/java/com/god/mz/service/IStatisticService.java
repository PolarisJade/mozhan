package com.god.mz.service;

import com.god.mz.domain.vo.statistic.AdminStatisticVO;
import com.god.mz.domain.vo.statistic.StatisticVO;

import java.time.LocalDate;

public interface IStatisticService {
    StatisticVO getStatistics();

    /**
     * 后台工作台总览：区间内的新增用户/文章/随笔 + 全站累计 + 逐日趋势。
     *
     * @param start 起始日，含当天；为空取当天
     * @param end   结束日，含当天；为空取当天
     */
    AdminStatisticVO getAdminOverview(LocalDate start, LocalDate end);
}
