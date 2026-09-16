package com.god.mz.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.god.mz.common.enums.BizCodeEnum;
import com.god.mz.common.constant.RedisConstant;
import com.god.mz.domain.dto.CategoryDTO;
import com.god.mz.domain.po.Article;
import com.god.mz.domain.po.Category;
import com.god.mz.domain.query.PageQuery.PageQueryVO;
import com.god.mz.domain.vo.category.AdminCategoryVO;
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
    public PageQueryVO<AdminCategoryVO> queryCategoryPage(Integer pageNum, Integer pageSize, String name,
            String sortBy, Boolean isAsc) {
        // 关联文章数必须靠 SQL 聚合，MyBatis-Plus 的 Wrapper 表达不了，所以走自定义 XML。
        // 代价是分页也要自己算 offset（同 TagServiceImpl 的写法）。
        int current = pageNum != null ? pageNum : 1;
        int size = pageSize != null ? pageSize : 10;
        String orderBy = StrUtil.isBlank(sortBy) ? "sort" : sortBy;
        Boolean asc = isAsc != null ? isAsc : Boolean.TRUE;
        String keyword = StrUtil.isBlank(name) ? null : name;

        List<AdminCategoryVO> voList = baseMapper.selectAdminCategoryPage(
                keyword, orderBy, asc, (current - 1) * size, size);
        Long total = baseMapper.selectAdminCategoryCount(keyword);

        PageQueryVO<AdminCategoryVO> result = new PageQueryVO<>();
        result.setRecords(voList);
        result.setTotal(total);
        result.setPageSize((long) size);
        result.setPageNo((long) current);
        result.setPages((long) Math.ceil((double) total / size));

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
