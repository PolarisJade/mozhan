package com.god.mz.mapper;

import com.god.mz.domain.po.Tag;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.god.mz.domain.vo.tag.AdminTagVO;
import com.god.mz.domain.vo.tag.HotTagVO;
import com.god.mz.domain.vo.tag.TagVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 文章标签表 Mapper 接口
 * </p>
 *
 * @author God
 * @since 2026-05-09
 */
public interface TagMapper extends BaseMapper<Tag> {

    List<HotTagVO> queryHotTagList(Integer limit);

    List<TagVO> selectTagVOByArticleIds(@Param("articleIds") List<Long> articleIds);

    List<AdminTagVO> selectAdminTagPage(@Param("name") String name, @Param("offset") Integer offset,
            @Param("size") Integer size);

    Long selectAdminTagCount(@Param("name") String name);
}
