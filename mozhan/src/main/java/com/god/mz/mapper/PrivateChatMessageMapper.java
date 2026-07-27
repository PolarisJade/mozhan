package com.god.mz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.god.mz.domain.po.PrivateChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 私信消息表 Mapper 接口
 */
@Mapper
public interface PrivateChatMessageMapper extends BaseMapper<PrivateChatMessage> {

    /**
     * 查询会话的历史消息（分页）
     */
    @Select("SELECT * FROM private_chat_message WHERE session_id = #{sessionId} ORDER BY create_time DESC LIMIT #{offset}, #{size}")
    List<PrivateChatMessage> selectBySessionId(@Param("sessionId") Long sessionId, @Param("offset") Long offset, @Param("size") Integer size);

    /**
     * 标记会话的所有消息为已读
     */
    @Update("UPDATE private_chat_message SET read_status = 1 WHERE session_id = #{sessionId} AND receiver_id = #{userId} AND read_status = 0")
    int markAsRead(@Param("sessionId") Long sessionId, @Param("userId") Long userId);

    /**
     * 查询未读消息数
     */
    @Select("SELECT COUNT(*) FROM private_chat_message WHERE receiver_id = #{userId} AND read_status = 0")
    int selectUnreadCount(@Param("userId") Long userId);
}
