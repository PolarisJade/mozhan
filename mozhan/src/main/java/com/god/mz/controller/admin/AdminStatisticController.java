package com.god.mz.controller.admin;

import com.god.mz.domain.vo.Result;
import com.god.mz.domain.vo.statistic.AdminStatisticVO;
import com.god.mz.service.IStatisticService;
import jakarta.annotation.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/admin/statistic")
public class AdminStatisticController {
    @Resource
    private IStatisticService statisticService;

    /**
     * 工作台总览。
     * <p>
     * 「今天 / 近7天 / 近30天」这几个预设区间由前端算成具体日期传进来，
     * 后端不做预设解析——这样「自定义区间」和预设走的是同一条代码路径，
     * 不会出现预设能查、自定义查不出的差异。
     * </p>
     *
     * @param start 起始日（含），格式 yyyy-MM-dd，不传取当天
     * @param end   结束日（含），格式 yyyy-MM-dd，不传取当天
     */
    @GetMapping("/overview")
    public Result<AdminStatisticVO> getOverview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return Result.success(statisticService.getAdminOverview(start, end));
    }
}
