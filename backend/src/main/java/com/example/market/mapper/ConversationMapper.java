package com.example.market.mapper;

import com.example.market.entity.Conversation;
import org.apache.ibatis.annotations.*;

@Mapper
public interface ConversationMapper {

    @Insert("INSERT INTO conversation (create_time) VALUES (#{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Conversation conversation);

    @Select("SELECT * FROM conversation WHERE id = #{id}")
    Conversation findById(Long id);
}