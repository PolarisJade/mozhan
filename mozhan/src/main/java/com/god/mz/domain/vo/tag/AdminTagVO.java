package com.god.mz.domain.vo.tag;

import com.god.mz.domain.po.Tag;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class AdminTagVO extends Tag {
    private Integer articleCount;
    private Integer essayCount;
}
