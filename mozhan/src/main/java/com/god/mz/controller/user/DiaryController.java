package com.god.mz.controller.user;


import com.god.mz.domain.dto.DiaryDTO;
import com.god.mz.domain.query.PageQuery.DiaryPageQuery;
import com.god.mz.domain.query.PageQuery.PageQueryVO;
import com.god.mz.domain.vo.Result;
import com.god.mz.domain.vo.diary.DiaryListVO;
import com.god.mz.domain.vo.diary.DiaryVO;
import com.god.mz.service.IDiaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 用户日记表 前端控制器
 * </p>
 *
 * @author God
 * @since 2026-07-23
 */
@RestController
@RequestMapping("/diary")
@RequiredArgsConstructor
public class DiaryController {

    private final IDiaryService diaryService;

    @PostMapping("/add")
    public Result<Void> addDiary(@RequestBody DiaryDTO diaryDTO) {
        diaryService.addDiary(diaryDTO);
        return Result.success();
    }

    @PutMapping("/delete/{id}")
    public Result<Void> deleteDiary(@PathVariable Long id) {
        diaryService.deleteDiary(id);
        return Result.success();
    }

    @PutMapping("/update")
    public Result<Void> updateDiary(@RequestBody DiaryDTO diaryDTO) {
        diaryService.updateDiary(diaryDTO);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<DiaryVO> queryDiaryDetail(@PathVariable Long id) {
        DiaryVO vo = diaryService.queryDiaryDetail(id);
        return Result.success(vo);
    }

    @GetMapping("/list")
    public Result<PageQueryVO<DiaryListVO>> queryDiaryList(DiaryPageQuery query) {
        PageQueryVO<DiaryListVO> vo = diaryService.queryDiaryList(query);
        return Result.success(vo);
    }
}
