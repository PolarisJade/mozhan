package com.god.mz.controller.admin;

import com.god.mz.domain.dto.TagDTO;
import com.god.mz.domain.query.PageQuery.PageQueryVO;
import com.god.mz.domain.vo.tag.AdminTagVO;
import com.god.mz.domain.vo.Result;
import com.god.mz.domain.vo.tag.TagVO;
import com.god.mz.service.ITagService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/tag")
public class AdminTagController {
    @Resource
    private ITagService tagService;

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

    @GetMapping("page")
    public Result<PageQueryVO<AdminTagVO>> getTagPage(@RequestParam Integer pageNum,
                                                      @RequestParam Integer pageSize,
                                                      @RequestParam(required = false) String name) {
        PageQueryVO<AdminTagVO> pageVO = tagService.getTagPage(pageNum, pageSize, name);
        return Result.success(pageVO);
    }
}
