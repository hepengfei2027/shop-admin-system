package com.example.market.mapper;

import com.example.market.entity.UserConversation;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserConversationMapper {

    @Insert("INSERT INTO user_conversation (user_id, conversation_id, status, last_read_time, create_time) VALUES (#{userId}, #{conversationId}, #{status}, #{lastReadTime}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserConversation userConversation);

    @Select("SELECT * FROM user_conversation WHERE user_id = #{userId} AND status = 1")
    List<UserConversation> findByUserId(Long userId);

    @Select("SELECT * FROM user_conversation WHERE user_id = #{userId} AND conversation_id = #{conversationId}")
    UserConversation findByUserIdAndConversationId(@Param("userId") Long userId, @Param("conversationId") Long conversationId);

    @Update("UPDATE user_conversation SET status = 0 WHERE user_id = #{userId} AND conversation_id = #{conversationId}")
    int softDelete(@Param("userId") Long userId, @Param("conversationId") Long conversationId);

    @Update("UPDATE user_conversation SET last_read_time = #{lastReadTime} WHERE user_id = #{userId} AND conversation_id = #{conversationId}")
    int updateLastReadTime(@Param("userId") Long userId, @Param("conversationId") Long conversationId, @Param("lastReadTime") String lastReadTime);
}