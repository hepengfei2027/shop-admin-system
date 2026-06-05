package com.example.market.mapper;

import com.example.market.entity.CommentMedia;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CommentMediaMapper {

    @Select("SELECT * FROM comment_media WHERE comment_id = #{commentId}")
    List<CommentMedia> listByCommentId(java.lang.Long commentId);

    @Insert("INSERT INTO comment_media(comment_id, media_url, media_type, create_time) VALUES(#{commentId}, #{mediaUrl}, #{mediaType}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CommentMedia media);

    @Delete("DELETE FROM comment_media WHERE comment_id = #{commentId}")
    int deleteByCommentId(java.lang.Long commentId);
}
