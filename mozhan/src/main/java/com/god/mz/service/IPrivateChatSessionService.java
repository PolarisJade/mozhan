package com.god.mz.service;

import com.god.mz.domain.po.PrivateChatSession;
import com.baomidou.mybatisplus.extension.service.IService;
import com.god.mz.domain.query.cursorQuery.CursorPageVO;
import com.god.mz.domain.query.cursorQuery.MsgCursorQuery;
import com.god.mz.domain.query.cursorQuery.SessionCursorQuery;
import com.god.mz.domain.vo.chat.ChatMessageVO;
import com.god.mz.domain.vo.chat.ChatSessionVO;

/**
 * <p>
 * 私信会话表 服务类
 * </p>
 *
 * @author God
 * @since 2026-07-11
 */
public interface IPrivateChatSessionService extends IService<PrivateChatSession> {

    CursorPageVO<ChatSessionVO> getSessionList(SessionCursorQuery query);

    CursorPageVO<ChatMessageVO> getMessageHistory(MsgCursorQuery query);
}
