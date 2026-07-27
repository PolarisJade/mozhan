package com.god.mz.service;

import com.god.mz.domain.dto.CategoryDTO;
import com.god.mz.domain.po.Category;
import com.baomidou.mybatisplus.extension.service.IService;
import com.god.mz.domain.query.PageQuery.PageQueryVO;
import com.god.mz.domain.vo.category.CategoryItemVO;

import java.util.List;


/**
 * <p>
 * 文章分类表 服务类
 * </p>
 *
 * @author God
 * @since 2026-05-09
 */
public interface ICategoryService extends IService<Category> {


    CategoryItemVO addCategory(CategoryDTO categoryDTO);

    void updateCategory(Long categoryId, String name, Integer sort);

    void deleteCategory(Long categoryId);

    PageQueryVO<CategoryItemVO> queryCategoryPage(Integer pageNum, Integer pageSize, String sortBy, Boolean isAsc);

    List<CategoryItemVO> queryAllCategory();
}
