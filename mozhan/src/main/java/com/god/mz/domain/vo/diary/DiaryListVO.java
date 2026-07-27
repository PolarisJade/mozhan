package com.god.mz.domain.vo.diary;

import lombok.Data;

import java.time.YearMonth;
import java.util.List;

@Data
public class DiaryListVO {
    private List<DiaryVO> diaryList;
    private Long total;
    private YearMonth recordTimes;
}
