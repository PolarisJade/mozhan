package com.god.mz.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.god.mz.common.enums.BizCodeEnum;
import com.god.mz.common.enums.UserStatusEnum;
import com.god.mz.domain.dto.TagDTO;
import com.god.mz.domain.po.ArticleTag;
import com.god.mz.domain.po.EssayTag;
import com.god.mz.domain.po.Tag;
import com.god.mz.domain.po.User;
import com.god.mz.domain.query.PageQuery.PageQueryVO;
import com.god.mz.domain.vo.tag.AdminTagVO;
import com.god.mz.domain.vo.tag.HotTagVO;
import com.god.mz.domain.vo.tag.TagVO;
import com.god.mz.exception.BizException;
import com.god.mz.mapper.ArticleTagMapper;
import com.god.mz.mapper.EssayTagMapper;
import com.god.mz.mapper.TagMapper;
import com.god.mz.mapper.UserMapper;
import com.god.mz.service.ITagService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.god.mz.util.UserContext;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 文章标签表 服务实现类
 * </p>
 *
 * @author God
 * @since 2026-05-09
 */
@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag> implements ITagService {
    @Resource
    private UserMapper userMapper;
    @Resource
    private TagMapper tagMapper;
    @Resource
    private EssayTagMapper essayTagMapper;
    @Resource
    private ArticleTagMapper articleTagMapper;

    @Override
    public List<TagVO> queryTagList() {
        List<Tag> list = list();
        return BeanUtil.copyToList(list, TagVO.class);
    }

    @Override
    public TagVO addTag(TagDTO tagDTO) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BizException(BizCodeEnum.USER_NOT_AUTH);
        }

        User user = userMapper.selectById(userId);
        if (user == null || user.getStatus() != UserStatusEnum.ENABLE) {
            throw new BizException(BizCodeEnum.USER_NOT_FOUND);
        }
        Tag tag = BeanUtil.copyProperties(tagDTO, Tag.class);
        tag.setCreateBy(user.getNickname());
        boolean success = save(tag);
        if (!success) {
            throw new BizException(BizCodeEnum.OPERATION_FAILURE);
        }
        return BeanUtil.copyProperties(tag, TagVO.class);
    }

    @Override
    public void deleteTag(Long id) {
        boolean articleExists = articleTagMapper.exists(
                new QueryWrapper<ArticleTag>().eq("tag_id", id));
        boolean essayExists = essayTagMapper.exists(
                new QueryWrapper<EssayTag>().eq("tag_id", id));

        if (articleExists || essayExists) {
            throw new BizException(BizCodeEnum.TAG_DELETE_ERROR);
        }

        boolean success = removeById(id);
        if (!success) {
            throw new BizException(BizCodeEnum.OPERATION_FAILURE);
        }
    }

    @Override
    public List<HotTagVO> queryHotTagList(Integer limit) {
        return tagMapper.queryHotTagList(limit);
    }

    @Override
    public PageQueryVO<AdminTagVO> getTagPage(Integer pageNum, Integer pageSize, String name) {
        Integer offset = (pageNum - 1) * pageSize;
        List<AdminTagVO> voList = tagMapper.selectAdminTagPage(name, offset, pageSize);
        Long total = tagMapper.selectAdminTagCount(name);

        PageQueryVO<AdminTagVO> result = new PageQueryVO<>();
        result.setRecords(voList);
        result.setTotal(total);
        result.setPageSize(pageSize.longValue());
        result.setPageNo(pageNum.longValue());
        result.setPages((long) Math.ceil((double) total / pageSize));

        return result;
    }
}
