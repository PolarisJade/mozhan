package com.god.mz.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.god.mz.common.enums.BizCodeEnum;
import com.god.mz.domain.po.PrivateChatMessage;
import com.god.mz.domain.po.PrivateChatSession;
import com.god.mz.domain.po.User;
import com.god.mz.domain.query.cursorQuery.CursorPageVO;
import com.god.mz.domain.query.cursorQuery.MsgCursorQuery;
import com.god.mz.domain.query.cursorQuery.SessionCursorQuery;
import com.god.mz.domain.vo.chat.ChatMessageVO;
import com.god.mz.domain.vo.chat.ChatSessionVO;
import com.god.mz.domain.vo.user.BaseUserVO;
import com.god.mz.exception.BizException;
import com.god.mz.mapper.PrivateChatMessageMapper;
import com.god.mz.mapper.PrivateChatSessionMapper;
import com.god.mz.mapper.UserMapper;
import com.god.mz.service.IPrivateChatSessionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.god.mz.util.CursorCodeUtil;
import com.god.mz.util.CursorQueryUtil;
import com.god.mz.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 私信会话表 服务实现类
 * </p>
 *
 * @author God
 * @since 2026-07-11
 */
@Service
@RequiredArgsConstructor
public class PrivateChatSessionServiceImpl extends ServiceImpl<PrivateChatSessionMapper, PrivateChatSession> implements IPrivateChatSessionService {

    private final UserMapper userMapper;
    private final PrivateChatMessageMapper messageMapper;

    @Override
    public CursorPageVO<ChatSessionVO> getSessionList(SessionCursorQuery query) {
        Long userId = UserContext.getUserId();
        Integer pageSize = query.getPageSize();
        String sortBy = query.getSortBy();

        QueryWrapper<PrivateChatSession> queryWrapper = new QueryWrapper<>();
        queryWrapper.and(w -> w.eq("user1_id", userId).or().eq("user2_id", userId));

        CursorQueryUtil.applyCursor(queryWrapper, query, "id");

        List<PrivateChatSession> sessionList = list(queryWrapper);

        boolean hasMore = sessionList.size() > pageSize;
        if (hasMore) {
            sessionList = sessionList.subList(0, pageSize);
        }

        if (sessionList.isEmpty()) {
            return new CursorPageVO<>(new ArrayList<>(),false, null);

        }

        // 提取目标用户ID列表
        List<Long> targetUserIds = sessionList.stream()
                .map(session -> session.getUser1Id().equals(userId)
                        ? session.getUser2Id()
                        : session.getUser1Id())
                .toList();

        // 提取最后一条消息ID列表
        List<Long> lastMsgIds = sessionList.stream()
                .map(PrivateChatSession::getLastMessageId)
                .filter(Objects::nonNull)
                .toList();

        // 批量查询目标用户信息
        Map<Long, User> targetUserMap = userMapper.selectByIds(targetUserIds)
                .stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        // 批量查询最后一条消息
        Map<Long, PrivateChatMessage> lastMsgMap = lastMsgIds.isEmpty()
                ? Collections.emptyMap()
                : messageMapper.selectByIds(lastMsgIds)
                .stream()
                .collect(Collectors.toMap(PrivateChatMessage::getId, message -> message));

        // 组装会话VO列表
        List<ChatSessionVO> voList = new ArrayList<>(sessionList.size());
        for (PrivateChatSession session : sessionList) {
            ChatSessionVO vo = new ChatSessionVO();
            Long targetUserId = session.getUser1Id().equals(userId) ? session.getUser2Id() : session.getUser1Id();
            User targetUser = targetUserMap.get(targetUserId);
            PrivateChatMessage message = lastMsgMap.get(session.getLastMessageId());

            if (targetUser != null) {
                BaseUserVO targetUserVO = BeanUtil.copyProperties(targetUser, BaseUserVO.class);
                vo.setTargetUser(targetUserVO);
            }

            if (message != null) {
                vo.setLastMessage(message.getContent());
            }

            vo.setSessionId(session.getId());
            vo.setLastMessageTime(session.getLastMessageTime());
            vo.setUnreadCount(session.getUser1Id().equals(userId) ? session.getUser1UnreadCount() : session.getUser2UnreadCount());
            voList.add(vo);
        }

        // 复合游标：主排序 + id
        List<Long> cursors = CursorQueryUtil.getNextCursor(sessionList, sortBy, "id");
        return new CursorPageVO<>(voList, hasMore, CursorCodeUtil.encode(cursors));
    }

    @Override
    public CursorPageVO<ChatMessageVO> getMessageHistory(MsgCursorQuery query) {
        Long userId = UserContext.getUserId();

        PrivateChatSession session = getById(query.getSessionId());
        if (session == null) {
            throw new BizException(BizCodeEnum.SESSION_NOT_EXIST);
        }
        if (!session.getUser1Id().equals(userId) && !session.getUser2Id().equals(userId)) {
            throw new BizException(BizCodeEnum.USER_NOT_AUTH);
        }

        Integer pageSize = query.getPageSize();
        String sortBy = query.getSortBy();

        QueryWrapper<PrivateChatMessage> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("session_id", query.getSessionId());

        CursorQueryUtil.applyCursor(queryWrapper, query, "id");

        List<PrivateChatMessage> messageList = messageMapper.selectList(queryWrapper);

        boolean hasMore = messageList.size() > pageSize;
        if (hasMore) {
            messageList = messageList.subList(0, pageSize);
        }

        if (messageList.isEmpty()) {
            return new CursorPageVO<>(new ArrayList<>(), false, null);
        }

        List<ChatMessageVO> voList = messageList.stream().map(msg -> {
            ChatMessageVO vo = BeanUtil.copyProperties(msg, ChatMessageVO.class);
            vo.setMessageId(msg.getId());
            return vo;
        }).toList();

        List<Long> cursors = CursorQueryUtil.getNextCursor(messageList, sortBy, "id");
        return new CursorPageVO<>(voList, hasMore, CursorCodeUtil.encode(cursors));
    }
}
