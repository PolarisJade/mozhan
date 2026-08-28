package com.god.mz.controller.user;


import com.god.mz.domain.vo.Result;
import com.god.mz.domain.vo.ai.MessageVO;
import com.god.mz.domain.vo.ai.SessionVO;
import com.god.mz.service.IAiSessionService;
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

    private final IAiSessionService aiSessionService;

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
    public List<MessageVO> queryBySessionId(@PathVariable("sessionId") String sessionId) {
        return aiSessionService.queryBySessionId(sessionId);
    }
}
