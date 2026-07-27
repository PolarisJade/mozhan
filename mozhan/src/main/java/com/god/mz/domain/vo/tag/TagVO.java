package com.god.mz.domain.vo.tag;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TagVO {
    private Long id;
    private String name;
    private Long articleId;

    public TagVO(Long id, String name) {
        this.id = id;
        this.name = name;
    }
}
