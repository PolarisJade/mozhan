package com.god.mz.domain.vo.essay;

import com.god.mz.domain.vo.tag.TagVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EssayVO {
    private Long id;
    private Long authorId;
    private String authorName;
    private String avatar;
    private String content;
    private List<TagVO> tagVOList;
    private Boolean isLike;
    private Long likeCount;
    private LocalDateTime createTime;
}
