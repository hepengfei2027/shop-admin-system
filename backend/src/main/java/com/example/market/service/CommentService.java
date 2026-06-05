package com.example.market.service;

import com.example.market.entity.CommentMedia;
import com.example.market.entity.CommentReply;
import com.example.market.entity.ProductComment;

import java.util.List;

public interface CommentService {

    ProductComment createComment(ProductComment comment);

    List<ProductComment> getCommentsByGoodsId(Long goodsId);

    List<CommentMedia> getMediaByCommentId(Long commentId);

    List<CommentReply> getRepliesByCommentId(Long commentId);

    CommentReply replyToComment(Long commentId, Long userId, String content);
}
