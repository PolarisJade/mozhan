package com.god.mz.service;

import com.god.mz.domain.po.AiSession;
import com.baomidou.mybatisplus.extension.service.IService;
import com.god.mz.domain.query.cursorQuery.AISessionCursorQuery;
import com.god.mz.domain.query.cursorQuery.CursorPageVO;
import com.god.mz.domain.vo.ai.ChatSessionVO;
import com.god.mz.domain.vo.ai.MessageVO;
import com.god.mz.domain.vo.ai.SessionVO;

import java.util.List;

/**
 * <p>
 * AI会话表 服务类
 * </p>
 *
 * @author God
 * @since 2026-08-21
 */
public interface IAISessionService extends IService<AiSession> {

    SessionVO createSession(Integer num);

    List<SessionVO.Example> getHotProblem(Integer num);

    List<MessageVO> queryBySessionId(String sessionId);

    void update(String sessionId, String title);

    void updateTitle(String sessionId, String title);

    void deleteHistorySession(String sessionId);

    CursorPageVO<ChatSessionVO> queryHistorySession(AISessionCursorQuery query);
}
