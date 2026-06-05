package com.example.market.mapper;

import com.example.market.entity.CommentReply;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CommentReplyMapper {

    @Select("SELECT * FROM comment_reply WHERE comment_id = #{commentId} ORDER BY create_time ASC")
    List<CommentReply> listByCommentId(java.lang.Long commentId);

    @Insert("INSERT INTO comment_reply(comment_id, user_id, content, create_time) VALUES(#{commentId}, #{userId}, #{content}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CommentReply reply);

    @Delete("DELETE FROM comment_reply WHERE id = #{id}")
    int delete(java.lang.Long id);
}
