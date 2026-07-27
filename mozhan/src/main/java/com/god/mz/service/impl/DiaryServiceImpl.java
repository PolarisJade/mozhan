package com.god.mz.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.god.mz.common.enums.BizCodeEnum;
import com.god.mz.domain.dto.DiaryDTO;
import com.god.mz.domain.po.Diary;
import com.god.mz.domain.query.PageQuery.DiaryPageQuery;
import com.god.mz.domain.query.PageQuery.PageQueryVO;
import com.god.mz.domain.vo.diary.DiaryListVO;
import com.god.mz.domain.vo.diary.DiaryVO;
import com.god.mz.exception.BizException;
import com.god.mz.mapper.DiaryMapper;
import com.god.mz.service.IDiaryService;
import com.god.mz.util.AESUtil;
import com.god.mz.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Diary service implementation with AES-256-GCM encryption.
 */
@Service
@RequiredArgsConstructor
public class DiaryServiceImpl extends ServiceImpl<DiaryMapper, Diary> implements IDiaryService {

    private final AESUtil aesUtil;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addDiary(DiaryDTO diaryDTO) {
        Long userId = UserContext.getUserId();

        LocalDate diaryDate = diaryDTO.getDiaryDate() != null
                ? diaryDTO.getDiaryDate()
                : LocalDate.now();

        //判断当天是否已写过日记
        boolean exists = lambdaQuery()
                .eq(Diary::getUserId, userId)
                .eq(Diary::getDiaryDate, diaryDate)
                .exists();
        if (exists) {
            throw new BizException(BizCodeEnum.DIARY_EXISTS);
        }

        Diary diary = BeanUtil.copyProperties(diaryDTO, Diary.class);
        diary.setUserId(userId);
        diary.setDiaryDate(diaryDate);
        diary.setContent(aesUtil.encrypt(diaryDTO.getContent()));
        save(diary);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDiary(Long id) {
        Long userId = UserContext.getUserId();
        Diary diary = getById(id);
        if (diary == null) {
            throw new BizException(BizCodeEnum.DATA_NOT_EXIST);
        }
        if (!diary.getUserId().equals(userId)) {
            throw new BizException(BizCodeEnum.USER_NOT_AUTH);
        }
        removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDiary(DiaryDTO diaryDTO) {
        Long userId = UserContext.getUserId();
        Diary diary = getById(diaryDTO.getId());
        if (diary == null) {
            throw new BizException(BizCodeEnum.DATA_NOT_EXIST);
        }
        if (!diary.getUserId().equals(userId)) {
            throw new BizException(BizCodeEnum.USER_NOT_AUTH);
        }

        diary.setContent(aesUtil.encrypt(diaryDTO.getContent()));
        diary.setWeather(diaryDTO.getWeather());
        if (diaryDTO.getDiaryDate() != null) {
            diary.setDiaryDate(diaryDTO.getDiaryDate());
        }
        updateById(diary);
    }

    @Override
    public DiaryVO queryDiaryDetail(Long id) {
        Long userId = UserContext.getUserId();
        Diary diary = getById(id);
        if (diary == null) {
            throw new BizException(BizCodeEnum.DATA_NOT_EXIST);
        }
        if (!diary.getUserId().equals(userId)) {
            throw new BizException(BizCodeEnum.USER_NOT_AUTH);
        }
        return makeDiaryVO(diary);
    }

    @Override
    public PageQueryVO<DiaryListVO> queryDiaryList(DiaryPageQuery query) {
        Long userId = UserContext.getUserId();

        LambdaQueryWrapper<Diary> wrapper = new LambdaQueryWrapper<Diary>()
                .eq(Diary::getUserId, userId);

        boolean asc = Boolean.TRUE.equals(query.getIsAsc());
        if (asc) {
            wrapper.orderByAsc(Diary::getDiaryDate);
        } else {
            wrapper.orderByDesc(Diary::getDiaryDate);
        }

        Page<Diary> page = new Page<>(query.getPageNum(), query.getPageSize());
        Page<Diary> result = page(page, wrapper);

        List<DiaryVO> diaryVOList = result.getRecords().stream()
                .map(this::makeDiaryVO)
                .toList();

        DiaryListVO diaryListVO = new DiaryListVO();
        diaryListVO.setDiaryList(diaryVOList);
        diaryListVO.setTotal(result.getTotal());
        if (!diaryVOList.isEmpty()) {
            diaryListVO.setRecordTimes(
                    YearMonth.from(result.getRecords().getFirst().getDiaryDate()));
        }

        return new PageQueryVO<>(
                List.of(diaryListVO),
                result.getTotal(),
                result.getSize(),
                result.getCurrent(),
                result.getPages());
    }

    private DiaryVO makeDiaryVO(Diary diary) {
        return new DiaryVO(
                diary.getId(),
                diary.getDiaryDate(),
                aesUtil.decrypt(diary.getContent()),
                diary.getWeather());
    }
}
