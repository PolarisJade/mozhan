package com.god.mz.controller.admin;

import com.god.mz.domain.query.PageQuery.PageQueryVO;
import com.god.mz.domain.vo.essay.EssayVO;
import com.god.mz.domain.vo.Result;
import com.god.mz.service.IEssayService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/essay")
public class AdminEssayController {
    @Resource
    private IEssayService essayService;

    @DeleteMapping("delete/{id}")
    public Result<Void> deleteEssay(@PathVariable Long id){
        essayService.deleteEssay(id);
        return Result.success();
    }

    @GetMapping("/page")
    public Result<PageQueryVO<EssayVO>> queryEssayPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long authorId) {

        PageQueryVO<EssayVO> pageVO = essayService.queryEssayPage(pageNum, pageSize, authorId);
        return Result.success(pageVO);
    }

}
