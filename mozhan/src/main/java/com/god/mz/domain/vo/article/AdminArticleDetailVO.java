package com.god.mz.domain.vo.article;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.god.mz.common.enums.ArticleStatusEnum;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 后台「查看文章」弹窗的数据。
 * <p>
 * 单独开一个 VO 而不是复用 {@link ArticleDetailVO}，是因为那个类的字段是前台阅读页的契约，
 * 后台想加个 createTime 就得改它，没必要让两条链路互相牵制。
 * </p>
 */
@Data
public class AdminArticleDetailVO {
    private Long id;

    private String title;

    private String summary;

    private String content;

    private Long authorId;

    private String authorName;

    private Long categoryId;

    private String categoryName;

    private Long likeCount;

    private Long commentCount;

    private ArticleStatusEnum status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
}
