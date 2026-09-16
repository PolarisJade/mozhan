package com.god.mz.mapper;

import com.god.mz.domain.po.Category;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.god.mz.domain.vo.category.AdminCategoryVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 文章分类表 Mapper 接口
 * </p>
 *
 * @author God
 * @since 2026-05-09
 */
public interface CategoryMapper extends BaseMapper<Category> {

    List<AdminCategoryVO> selectAdminCategoryPage(@Param("name") String name, @Param("sortBy") String sortBy,
            @Param("isAsc") Boolean isAsc, @Param("offset") Integer offset, @Param("size") Integer size);

    Long selectAdminCategoryCount(@Param("name") String name);
}
