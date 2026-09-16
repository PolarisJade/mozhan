package com.god.mz.service;

import com.god.mz.domain.dto.CategoryDTO;
import com.god.mz.domain.po.Category;
import com.baomidou.mybatisplus.extension.service.IService;
import com.god.mz.domain.query.PageQuery.PageQueryVO;
import com.god.mz.domain.vo.category.AdminCategoryVO;
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

    /**
     * 后台：分类分页（含关联文章数）。仅供 /admin/category/page 使用。
     */
    PageQueryVO<AdminCategoryVO> queryCategoryPage(Integer pageNum, Integer pageSize, String name, String sortBy,
            Boolean isAsc);

    List<CategoryItemVO> queryAllCategory();
}
