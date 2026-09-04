package com.god.mz.controller.user;


import com.god.mz.domain.query.cursorQuery.AISessionCursorQuery;
import com.god.mz.domain.query.cursorQuery.CursorPageVO;
import com.god.mz.domain.vo.ai.ChatSessionVO;
import com.god.mz.domain.vo.Result;
import com.god.mz.domain.vo.ai.MessageVO;
import com.god.mz.domain.vo.ai.SessionVO;
import com.god.mz.service.IAISessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * AI会话表 前端控制器
 * </p>
 *
 * @author God
 * @since 2026-08-21
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/ai/session")
public class AISessionController {

    private final IAISessionService aiSessionService;

    /**
     * 新建会话
     */
    @PostMapping
    public Result<SessionVO> createSession(@RequestParam(value = "num", defaultValue = "3") Integer num) {
        SessionVO vo = aiSessionService.createSession(num);
        return Result.success(vo);
    }

    /**
     * 热门问题
     */
    @GetMapping("/hot")
    public Result<List<SessionVO.Example>> getHotProblem(@RequestParam(value = "num", defaultValue = "3") Integer num) {
        List<SessionVO.Example> hotProblem = aiSessionService.getHotProblem(num);
        return Result.success(hotProblem);
    }

    /**
     * 查询单个历史对话详情
     *
     * @return 对话记录列表
     */
    @GetMapping("/{sessionId}")
    public Result<List<MessageVO>> queryBySessionId(@PathVariable("sessionId") String sessionId) {
        List<MessageVO> messages = aiSessionService.queryBySessionId(sessionId);
        return Result.success(messages);
    }

    /**
     * 更新历史会话标题
     */
    @PutMapping("/history")
    public Result<Void> updateTitle(@RequestParam("sessionId") String sessionId,
                            @RequestParam("title") String title) {
        aiSessionService.updateTitle(sessionId, title);
        return Result.success();
    }

    /**
     * 删除历史会话列表
     */
    @DeleteMapping("/history")
    public Result<Void> deleteHistorySession(@RequestParam("sessionId") String sessionId) {
        aiSessionService.deleteHistorySession(sessionId);
        return Result.success();
    }

    /**
     * 查询历史会话列表
     */
    @GetMapping("/history")
    public Result<CursorPageVO<ChatSessionVO>> queryHistorySession(AISessionCursorQuery query) {
        CursorPageVO<ChatSessionVO> vo = aiSessionService.queryHistorySession(query);
        return Result.success(vo);
    }
}
