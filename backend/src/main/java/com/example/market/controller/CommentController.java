package com.example.market.controller;

import com.example.market.dto.Result;
import com.example.market.entity.CommentMedia;
import com.example.market.entity.CommentReply;
import com.example.market.entity.ProductComment;
import com.example.market.service.CommentService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/api/comment")
@CrossOrigin
public class CommentController {

    @Resource
    private CommentService commentService;

    @PostMapping("/create")
    public Result<ProductComment> create(@RequestBody ProductComment comment) {
        ProductComment result = commentService.createComment(comment);
        return Result.ok(result);
    }

    @GetMapping("/goods/{goodsId}")
    public Result<List<ProductComment>> getByGoodsId(@PathVariable Long goodsId) {
        List<ProductComment> comments = commentService.getCommentsByGoodsId(goodsId);
        return Result.ok(comments);
    }

    @GetMapping("/{commentId}/media")
    public Result<List<CommentMedia>> getMedia(@PathVariable Long commentId) {
        List<CommentMedia> media = commentService.getMediaByCommentId(commentId);
        return Result.ok(media);
    }

    @GetMapping("/{commentId}/replies")
    public Result<List<CommentReply>> getReplies(@PathVariable Long commentId) {
        List<CommentReply> replies = commentService.getRepliesByCommentId(commentId);
        return Result.ok(replies);
    }

    @PostMapping("/{commentId}/reply")
    public Result<CommentReply> reply(@PathVariable Long commentId, @RequestParam Long userId, @RequestParam String content) {
        CommentReply reply = commentService.replyToComment(commentId, userId, content);
        return Result.ok(reply);
    }
}
