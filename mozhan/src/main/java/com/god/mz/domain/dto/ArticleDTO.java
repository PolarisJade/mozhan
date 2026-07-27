package com.god.mz.domain.dto;

import com.god.mz.common.enums.ArticleStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ArticleDTO {
    private String title;
    private String summary;
    private String content;
    private Long categoryId;
    private List<Long> tagIds;
    private Boolean isTop = false;
    private ArticleStatusEnum status;
}
