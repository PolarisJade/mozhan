package com.god.mz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.god.mz.domain.po.Essay;
import io.lettuce.core.dynamic.annotation.Param;

import java.util.List;
import java.util.Map;

public interface EssayMapper extends BaseMapper<Essay> {
    List<Map<String, Object>> selectUserEssayCounts(@Param("userIds") List<Long> userIds);
}
