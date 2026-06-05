package com.example.market.service.impl;

import com.example.market.entity.CommentMedia;
import com.example.market.entity.CommentReply;
import com.example.market.entity.ProductComment;
import com.example.market.mapper.CommentMediaMapper;
import com.example.market.mapper.CommentReplyMapper;
import com.example.market.mapper.ProductCommentMapper;
import com.example.market.service.CommentService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class CommentServiceImpl implements CommentService {

    @Resource
    private ProductCommentMapper commentMapper;

    @Resource
    private CommentMediaMapper mediaMapper;

    @Resource
    private CommentReplyMapper replyMapper;

    @Override
    public ProductComment createComment(ProductComment comment) {
        comment.setStatus(1);
        commentMapper.insert(comment);

        if (comment.getMedia() != null && !comment.getMedia().isEmpty()) {
            for (CommentMedia media : comment.getMedia()) {
                media.setCommentId(comment.getId());
                mediaMapper.insert(media);
            }
        }

        return comment;
    }

    @Override
    public List<ProductComment> getCommentsByGoodsId(Long goodsId) {
        List<ProductComment> comments = commentMapper.listByGoodsId(goodsId);
        for (ProductComment comment : comments) {
            List<CommentMedia> mediaList = mediaMapper.listByCommentId(comment.getId());
            comment.setMedia(mediaList);
        }
        return comments;
    }

    @Override
    public List<CommentMedia> getMediaByCommentId(Long commentId) {
        return mediaMapper.listByCommentId(commentId);
    }

    @Override
    public List<CommentReply> getRepliesByCommentId(Long commentId) {
        return replyMapper.listByCommentId(commentId);
    }

    @Override
    public CommentReply replyToComment(Long commentId, Long userId, String content) {
        CommentReply reply = new CommentReply();
        reply.setCommentId(commentId);
        reply.setUserId(userId);
        reply.setContent(content);
        replyMapper.insert(reply);
        return reply;
    }
}
