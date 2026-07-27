package com.god.mz.service;

import com.god.mz.domain.po.PrivateChatMessage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.god.mz.domain.vo.chat.ChatMessageVO;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.WebSocketSession;

/**
 * <p>
 * 私信消息表 服务类
 * </p>
 *
 * @author God
 * @since 2026-07-11
 */
public interface IPrivateChatMessageService extends IService<PrivateChatMessage> {

    void recallMessage(Long userId, Long messageId);

    void markSessionAsRead(Long userId, Long sessionId);

    @Transactional(rollbackFor = Exception.class)
    ChatMessageVO sendMessage(Long senderId, Long receiverId, String content);

    Integer getUnreadCount(Long userId);
}
