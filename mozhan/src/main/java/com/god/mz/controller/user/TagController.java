package com.god.mz.controller.user;


import com.god.mz.domain.dto.TagDTO;
import com.god.mz.domain.vo.tag.HotTagVO;
import com.god.mz.domain.vo.Result;
import com.god.mz.domain.vo.tag.TagVO;
import com.god.mz.service.ITagService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 文章标签表 前端控制器
 * </p>
 *
 * @author God
 * @since 2026-05-09
 */
@RestController
@RequestMapping("/tag")
public class TagController {
    @Resource
    private ITagService tagService;
    @GetMapping("/list")
    public Result<List<TagVO>> queryTagList() {
        List<TagVO> list = tagService.queryTagList();
        return Result.success(list);
    }

    @PostMapping
    public Result<TagVO> addTag(@RequestBody TagDTO tagDTO) {
        TagVO vo = tagService.addTag(tagDTO);
        return Result.success(vo);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteTag(@PathVariable Long id) {
        tagService.deleteTag(id);
        return Result.success();
    }

    @GetMapping("/hot")
    public Result<List<HotTagVO>> queryHotTagList(@RequestParam(required = false, defaultValue = "10") Integer limit) {
        List<HotTagVO> list = tagService.queryHotTagList(limit);
        return Result.success(list);
    }
}
