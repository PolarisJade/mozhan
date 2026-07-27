package com.god.mz.service;

import com.god.mz.domain.dto.TagDTO;
import com.god.mz.domain.po.Tag;
import com.baomidou.mybatisplus.extension.service.IService;
import com.god.mz.domain.query.PageQuery.PageQueryVO;
import com.god.mz.domain.vo.tag.AdminTagVO;
import com.god.mz.domain.vo.tag.HotTagVO;
import com.god.mz.domain.vo.tag.TagVO;

import java.util.List;

/**
 * <p>
 * 文章标签表 服务类
 * </p>
 *
 * @author God
 * @since 2026-05-09
 */
public interface ITagService extends IService<Tag> {

    List<TagVO> queryTagList();

    TagVO addTag(TagDTO tagDTO);

    void deleteTag(Long id);

    List<HotTagVO> queryHotTagList(Integer limit);

    PageQueryVO<AdminTagVO> getTagPage(Integer pageNum, Integer pageSize, String name);
}
