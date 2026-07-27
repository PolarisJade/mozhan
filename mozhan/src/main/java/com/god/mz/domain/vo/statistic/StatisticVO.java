package com.god.mz.domain.vo.statistic;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StatisticVO {
    private Long articleCount;
    private Long userCount;
    private Long EssayCount;
    private Long TagCount;
}
