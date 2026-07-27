package com.god.mz.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.god.mz.common.enums.BizCodeEnum;
import com.god.mz.common.constant.RedisConstant;
import com.god.mz.domain.dto.CategoryDTO;
import com.god.mz.domain.po.Article;
import com.god.mz.domain.po.Category;
import com.god.mz.domain.query.PageQuery.PageQueryVO;
import com.god.mz.domain.vo.category.CategoryItemVO;
import com.god.mz.exception.BizException;
import com.god.mz.mapper.ArticleMapper;
import com.god.mz.mapper.CategoryMapper;
import com.god.mz.service.ICategoryService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 文章分类表 服务实现类
 * </p>
 *
 * @author God
 * @since 2026-05-09
 */
@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements ICategoryService {
    @Resource
    private ArticleMapper articleMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private ObjectMapper objectMapper;

    @Override
    public CategoryItemVO addCategory(CategoryDTO categoryDTO) {
        if (categoryDTO == null || categoryDTO.getName() == null || categoryDTO.getName().trim().isEmpty()){
            throw new BizException(BizCodeEnum.DATA_NOT_EXIST);
        }
        Long count = lambdaQuery().eq(Category::getName, categoryDTO.getName()).count();
        if (count > 0){
            throw new BizException(BizCodeEnum.DATA_EXIST);
        }
        Category category = BeanUtil.copyProperties(categoryDTO, Category.class);
        boolean success = save(category);
        if (!success){
            throw new BizException(BizCodeEnum.OPERATION_FAILURE);
        }
        stringRedisTemplate.delete(RedisConstant.CATEGORY_LIST);
        return BeanUtil.copyProperties(category, CategoryItemVO.class);
    }

    @Override
    public void updateCategory(Long categoryId, String name, Integer sort) {
        Category category = getById(categoryId);
        if (category == null) {
            throw new BizException(BizCodeEnum.DATA_NOT_EXIST);
        }

        boolean needUpdate = false;

        if (name != null && !name.trim().isEmpty()) {
            Long count = lambdaQuery()
                    .eq(Category::getName, name)
                    .ne(Category::getId, categoryId)
                    .count();
            if (count > 0) {
                throw new BizException(BizCodeEnum.DATA_EXIST);
            }
            category.setName(name);
            needUpdate = true;
        }

        if (sort != null) {
            category.setSort(sort);
            needUpdate = true;
        }

        if (needUpdate) {
            boolean success = updateById(category);
            if (!success) {
                throw new BizException(BizCodeEnum.OPERATION_FAILURE);
            }
        }
        stringRedisTemplate.delete(RedisConstant.CATEGORY_LIST);
    }

    @Override
    public void deleteCategory(Long categoryId) {
        Category category = getById(categoryId);
        if (category == null) {
            throw new BizException(BizCodeEnum.DATA_NOT_EXIST);
        }

        Long count = articleMapper.selectCount(
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getCategoryId, categoryId)
                        .eq(Article::getDelFlag, false)
        );

        if (count > 0) {
            throw new BizException(BizCodeEnum.CATEGORY_DELETE_ERROR);
        }

        boolean success = removeById(categoryId);
        if (!success) {
            throw new BizException(BizCodeEnum.OPERATION_FAILURE);
        }
        stringRedisTemplate.delete(RedisConstant.CATEGORY_LIST);
    }

    @Override
    public PageQueryVO<CategoryItemVO> queryCategoryPage(Integer pageNum, Integer pageSize, String sortBy, Boolean isAsc) {
        Page<Category> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();

        if ("sort".equals(sortBy)) {
            queryWrapper.orderBy(true, isAsc, Category::getSort);
        } else {
            queryWrapper.orderBy(true, isAsc, Category::getId);
        }

        IPage<Category> categoryPage = page(page, queryWrapper);

        List<CategoryItemVO> voList = BeanUtil.copyToList(categoryPage.getRecords(), CategoryItemVO.class);

        PageQueryVO<CategoryItemVO> result = new PageQueryVO<>();
        result.setRecords(voList);
        result.setTotal(categoryPage.getTotal());
        result.setPageSize(categoryPage.getSize());
        result.setPageNo(categoryPage.getCurrent());
        result.setPages(categoryPage.getPages());

        return result;
    }

    @Override
    public List<CategoryItemVO> queryAllCategory() {
        // 1. 先从 Redis 中查询
        String json = stringRedisTemplate.opsForValue().get(RedisConstant.CATEGORY_LIST);
        if (json != null && !json.isEmpty()) {
            try {
                return objectMapper.readValue(json, new TypeReference<>() {
                });
            } catch (JsonProcessingException e) {
                // JSON 解析失败，从数据库查询
            }
        }

        // 2. Redis 中没有，从数据库查询
        List<Category> categoryList = lambdaQuery()
                .orderByAsc(Category::getSort)
                .list();

        List<CategoryItemVO> voList = BeanUtil.copyToList(categoryList, CategoryItemVO.class);

        // 3. 存入 Redis
        try {
            String categoryJson = objectMapper.writeValueAsString(voList);
            stringRedisTemplate.opsForValue().set(
                    RedisConstant.CATEGORY_LIST,
                    categoryJson,
                    RedisConstant.DEFAULT_EXPIRE_HOURS,
                    TimeUnit.HOURS
            );
        } catch (JsonProcessingException ignored) {
        }

        return voList;
    }
}
