package com.god.mz.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentDTO {
    private Long articleId;
    private String content;
    private Long parentId = 0L;
    private Long replyToId = 0L;
}
