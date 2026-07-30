package com.god.mz.domain.vo.article;

import com.god.mz.domain.vo.tag.TagVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ArticleInfoVO {
    private Long id;
    private String title;
    private String summary;
    private String coverImage;
    private String content;
    private Long categoryId;
    private String categoryName;
    private List<TagVO> tags;
}
