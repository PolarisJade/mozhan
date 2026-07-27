package com.god.mz.controller.user;


import com.god.mz.domain.dto.CategoryDTO;
import com.god.mz.domain.vo.category.CategoryItemVO;
import com.god.mz.domain.vo.Result;
import com.god.mz.service.ICategoryService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 文章分类表 前端控制器
 * </p>
 *
 * @author God
 * @since 2026-05-09
 */
@RestController
@RequestMapping("/category")
public class CategoryController {

    @Resource
    private ICategoryService categoryService;

    @GetMapping("/list")
    public Result<List<CategoryItemVO>> queryAllCategory() {
        List<CategoryItemVO> list = categoryService.queryAllCategory();
        return Result.success(list);
    }

    @PostMapping
    public Result<CategoryItemVO> addCategory(@RequestBody CategoryDTO categoryDTO) {
        CategoryItemVO vo = categoryService.addCategory(categoryDTO);
        return Result.success(vo);
    }

    @PutMapping("{categoryId}")
    public Result<CategoryItemVO> updateCategory(@PathVariable Long categoryId,
                                                 @RequestParam(required = false) String name,
                                                 @RequestParam(required = false) Integer sort) {
        categoryService.updateCategory(categoryId, name, sort);
        return Result.success();
    }

    @DeleteMapping("/{categoryId}")
    public Result<Void> deleteCategory(@PathVariable Long categoryId) {
        categoryService.deleteCategory(categoryId);
        return Result.success();
    }

}
