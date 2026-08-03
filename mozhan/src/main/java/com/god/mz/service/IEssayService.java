package com.god.mz.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.god.mz.domain.dto.EssayDTO;
import com.god.mz.domain.po.Essay;
import com.god.mz.domain.query.cursorQuery.CursorPageVO;
import com.god.mz.domain.query.cursorQuery.EssayCursorQuery;
import com.god.mz.domain.query.PageQuery.PageQueryVO;
import com.god.mz.domain.vo.essay.EssayVO;

public interface IEssayService extends IService<Essay> {
    void addEssay(EssayDTO essayDTO);

    EssayVO getEssayDetail(Long id);

    CursorPageVO<EssayVO> getEssayList(EssayCursorQuery query);

    void deleteEssay(Long id);

    CursorPageVO<EssayVO> getMyEssayList(Long userId, EssayCursorQuery query);

    void updateEssay(EssayDTO essayDTO);

    PageQueryVO<EssayVO> queryEssayPage(Integer pageNum, Integer pageSize, Long authorId);

    void updateLikeCount(int maxSize);
}
