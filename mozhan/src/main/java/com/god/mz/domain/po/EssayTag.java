package com.god.mz.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EssayTag {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long essayId;
    private Long tagId;
}
