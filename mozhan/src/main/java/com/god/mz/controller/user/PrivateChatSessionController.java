package com.god.mz.controller.user;


import com.god.mz.domain.query.cursorQuery.CursorPageVO;
import com.god.mz.domain.query.cursorQuery.MsgCursorQuery;
import com.god.mz.domain.query.cursorQuery.SessionCursorQuery;
import com.god.mz.domain.vo.Result;
import com.god.mz.domain.vo.chat.ChatMessageVO;
import com.god.mz.domain.vo.chat.ChatSessionVO;
import com.god.mz.service.IPrivateChatMessageService;
import com.god.mz.service.IPrivateChatSessionService;
import com.god.mz.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 私信会话表 前端控制器
 * </p>
 *
 * @author God
 * @since 2026-07-11
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/chat/session")
public class PrivateChatSessionController {

    private final IPrivateChatSessionService chatSessionService;
    private final IPrivateChatMessageService chatMessageService;

    @GetMapping("/sessions")
    public Result<CursorPageVO<ChatSessionVO>> getSessionList(SessionCursorQuery query) {
        CursorPageVO<ChatSessionVO> list = chatSessionService.getSessionList(query);
        return Result.success(list);
    }

    @GetMapping("/{sessionId}")
    public Result<CursorPageVO<ChatMessageVO>> getMessageHistory(@PathVariable Long sessionId, MsgCursorQuery query) {
        query.setSessionId(sessionId);
        CursorPageVO<ChatMessageVO> vo = chatSessionService.getMessageHistory(query);
        return Result.success(vo);
    }

    @PutMapping("/{sessionId}/read")
    public Result<Void> markSessionAsRead(@PathVariable Long sessionId) {
        Long userId = UserContext.getUserId();
        chatMessageService.markSessionAsRead(userId, sessionId);
        return Result.success();
    }
}
