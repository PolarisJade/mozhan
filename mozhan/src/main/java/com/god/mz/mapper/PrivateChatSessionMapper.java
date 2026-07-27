package com.god.mz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.god.mz.domain.po.PrivateChatSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;


/**
 * 私信会话表 Mapper 接口
 */
@Mapper
public interface PrivateChatSessionMapper extends BaseMapper<PrivateChatSession> {

    /**
     * 根据用户ID对查询会话（确保user1_id < user2_id）
     */
    @Select("SELECT * FROM private_chat_session WHERE user1_id = #{user1Id} AND user2_id = #{user2Id}")
    PrivateChatSession selectByUserPair(@Param("user1Id") Long user1Id, @Param("user2Id") Long user2Id);

    /**
     *  user1
     */
    @Update("UPDATE private_chat_session SET user1_unread_count = user1_unread_count + 1, last_message_id = #{lastMessageId}, last_message_time = NOW(), update_time = NOW() WHERE id = #{sessionId}")
    int incrementUser1UnreadCount(@Param("sessionId") Long sessionId, @Param("lastMessageId") Long lastMessageId);

    /**
     *  user2
     */
    @Update("UPDATE private_chat_session SET user2_unread_count = user2_unread_count + 1, last_message_id = #{lastMessageId}, last_message_time = NOW(), update_time = NOW() WHERE id = #{sessionId}")
    int incrementUser2UnreadCount(@Param("sessionId") Long sessionId, @Param("lastMessageId") Long lastMessageId);

    /**
     * 
     */
    @Update("UPDATE private_chat_session SET user1_unread_count = 0 WHERE id = #{sessionId}")
    int clearUser1UnreadCount(@Param("sessionId") Long sessionId);

    /**
     * 
     */
    @Update("UPDATE private_chat_session SET user2_unread_count = 0 WHERE id = #{sessionId}")
    int clearUser2UnreadCount(@Param("sessionId") Long sessionId);
}
