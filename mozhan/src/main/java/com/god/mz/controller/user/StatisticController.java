package com.god.mz.controller.user;

import com.god.mz.domain.vo.Result;
import com.god.mz.domain.vo.statistic.StatisticVO;
import com.god.mz.service.IStatisticService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/statistics")
public class StatisticController {
    @Resource
    private IStatisticService statisticService;

    @GetMapping
    public Result<StatisticVO> getStatistics() {
        StatisticVO vo = statisticService.getStatistics();
        return Result.success(vo);
    }
}
