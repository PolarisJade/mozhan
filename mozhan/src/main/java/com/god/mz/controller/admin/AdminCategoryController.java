package com.god.mz.controller.admin;

import com.god.mz.domain.dto.CategoryDTO;
import com.god.mz.domain.query.PageQuery.PageQueryVO;
import com.god.mz.domain.vo.category.CategoryItemVO;
import com.god.mz.domain.vo.Result;
import com.god.mz.service.ICategoryService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/category")
public class AdminCategoryController {
    @Resource
    private ICategoryService categoryService;

    @GetMapping("/page")
    public Result<PageQueryVO<CategoryItemVO>> queryCategoryPage(
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize,
            @RequestParam(required = false, defaultValue = "sort") String sortBy,
            @RequestParam(required = false, defaultValue = "true") Boolean isAsc) {

        PageQueryVO<CategoryItemVO> vo = categoryService.queryCategoryPage(pageNum, pageSize, sortBy, isAsc);
        return Result.success(vo);
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
