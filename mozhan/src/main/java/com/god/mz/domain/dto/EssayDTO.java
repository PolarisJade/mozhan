package com.god.mz.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EssayDTO {
    private Long id;
    private String content;
    private List<Long> tagIdList;
}
