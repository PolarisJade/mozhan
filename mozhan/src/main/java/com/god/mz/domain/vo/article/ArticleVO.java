package com.god.mz.domain.vo.article;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.god.mz.common.enums.ArticleStatusEnum;
import com.god.mz.domain.vo.tag.TagVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ArticleVO {
    private Long id;

    private String title;

    private String summary;

    private String coverImage;

    private Long authorId;

    private String authorName;

    private Long categoryId;

    private String categoryName;

    private List<TagVO> tags;

    private Long likeCount;

    private Integer commentCount;

    private Boolean isLike;

    private Boolean isTop;

    private ArticleStatusEnum status;

    @TableField(fill = FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
}
