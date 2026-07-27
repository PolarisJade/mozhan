package com.god.mz.domain.dto;

import lombok.Data;
import java.time.LocalDate;


@Data
public class DiaryDTO {
    private Long id;
    private LocalDate diaryDate;
    private String content;
    private String weather;
}
