package com.god.mz.domain.vo.article;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ArticleDetailVO extends ArticleVO{
    private String authorAvatar;
    private String content;
    private Boolean isAuthor;
    private Boolean isFollowed;
    private Boolean isLike;
}
