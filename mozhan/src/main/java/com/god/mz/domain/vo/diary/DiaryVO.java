package com.god.mz.domain.vo.diary;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DiaryVO {
    private Long id;
    private LocalDate diaryDate;
    private String content;
    private String weather;
}
