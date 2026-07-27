package com.god.mz.controller.user;


import com.god.mz.domain.vo.Result;
import com.god.mz.service.IPrivateChatMessageService;
import com.god.mz.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 私信消息表 前端控制器
 * </p>
 *
 * @author God
 * @since 2026-07-11
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/chat/message")
public class PrivateChatMessageController {

    private final IPrivateChatMessageService chatMessageService;

    @PutMapping("/sessions/{sessionId}/read")
    public Result<Void> markSessionAsRead(@PathVariable Long sessionId) {
        Long userId = UserContext.getUserId();
        chatMessageService.markSessionAsRead(userId, sessionId);
        return Result.success();
    }

    @PutMapping("/{messageId}/recall")
    public Result<Void> recallMessage(@PathVariable Long messageId) {
        Long userId = UserContext.getUserId();
        chatMessageService.recallMessage(userId, messageId);
        return Result.success();
    }

    @GetMapping("/unread/count")
    public Result<Integer> getUnreadCount() {
        Long userId = UserContext.getUserId();
        Integer count = chatMessageService.getUnreadCount(userId);
        return Result.success(count);
    }

}
