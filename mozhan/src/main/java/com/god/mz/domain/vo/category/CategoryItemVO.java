package com.god.mz.domain.vo.category;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryItemVO {
    private Long id;

    private String name;

    private Integer sort;
}
