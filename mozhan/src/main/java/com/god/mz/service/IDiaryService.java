package com.god.mz.service;

import com.god.mz.domain.dto.DiaryDTO;
import com.god.mz.domain.po.Diary;
import com.baomidou.mybatisplus.extension.service.IService;
import com.god.mz.domain.query.PageQuery.DiaryPageQuery;
import com.god.mz.domain.query.PageQuery.PageQueryVO;
import com.god.mz.domain.vo.diary.DiaryListVO;
import com.god.mz.domain.vo.diary.DiaryVO;

/**
 * <p>
 * 用户日记表 服务类
 * </p>
 *
 * @author God
 * @since 2026-07-23
 */
public interface IDiaryService extends IService<Diary> {

    void addDiary(DiaryDTO diaryDTO);

    void deleteDiary(Long id);

    void updateDiary(DiaryDTO diaryDTO);

    DiaryVO queryDiaryDetail(Long id);

    PageQueryVO<DiaryListVO> queryDiaryList(DiaryPageQuery query);
}
