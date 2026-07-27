package com.god.mz.controller.user;

import com.god.mz.domain.dto.EssayDTO;
import com.god.mz.domain.query.cursorQuery.CursorPageVO;
import com.god.mz.domain.query.cursorQuery.EssayCursorQuery;
import com.god.mz.domain.vo.essay.EssayVO;
import com.god.mz.domain.vo.Result;
import com.god.mz.service.IEssayService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/essay")
public class EssayController {
    @Resource
    private IEssayService essayService;

    @PostMapping("/add")
    public Result<Void> addEssay(@RequestBody EssayDTO essayDTO) {
        essayService.addEssay(essayDTO);
        return Result.success();
    }

    @GetMapping("/detail/{id}")
    public Result<EssayVO> getEssayDetail(@PathVariable Long id) {
        EssayVO vo = essayService.getEssayDetail(id);
        return Result.success(vo);
    }

    @GetMapping("/list")
    public Result<CursorPageVO<EssayVO>> getEssayList(EssayCursorQuery query){
        CursorPageVO<EssayVO> vo = essayService.getEssayList(query);
        return Result.success(vo);
    }

    @DeleteMapping("delete/{id}")
    public Result<Void> deleteEssay(@PathVariable Long id){
        essayService.deleteEssay(id);
        return Result.success();
    }

    @GetMapping("/my/{userId}")
    public Result<CursorPageVO<EssayVO>> getMyEssayList(@PathVariable Long userId, EssayCursorQuery query) {
        CursorPageVO<EssayVO> vo = essayService.getMyEssayList(userId, query);
        return Result.success(vo);
    }

    @PutMapping("/update")
    public Result<Void> updateEssay(@RequestBody EssayDTO essayDTO) {
        essayService.updateEssay(essayDTO);
        return Result.success();
    }
}
