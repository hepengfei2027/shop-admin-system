package com.example.market.mapper;

import com.example.market.entity.Message;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface MessageMapper {

    @Insert("INSERT INTO message(sender_id, receiver_id, conversation_id, content, status, create_time) " +
            "VALUES(#{senderId}, #{receiverId}, #{conversationId}, #{content}, #{status}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Message message);

    @Select("SELECT * FROM message WHERE receiver_id = #{userId} ORDER BY create_time DESC")
    List<Message> findByReceiverId(Long userId);

    @Select("SELECT m.* FROM message m JOIN user_conversation uc ON m.conversation_id = uc.conversation_id WHERE uc.user_id = #{userId} AND uc.status = 1 ORDER BY m.create_time DESC")
    List<Message> findAllByUserId(Long userId);

    @Select("SELECT * FROM message WHERE conversation_id = #{conversationId} ORDER BY create_time ASC")
    List<Message> findByConversationId(Long conversationId);

    @Update("UPDATE message SET status = 1, read_time = NOW() WHERE id = #{id}")
    int markAsRead(Long id);

    @Select("SELECT COUNT(*) FROM message m JOIN user_conversation uc ON m.conversation_id = uc.conversation_id WHERE m.receiver_id = #{userId} AND m.status = 0 AND uc.status = 1")
    int countUnread(Long userId);

    @Delete("DELETE FROM message WHERE id = #{id} AND sender_id = #{senderId}")
    int delete(@Param("id") Long id, @Param("senderId") Long senderId);
}