package com.god.mz.tool.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleInfo {
    private Long id;
    private String title;
    private String summary;
    private String authorName;
}
