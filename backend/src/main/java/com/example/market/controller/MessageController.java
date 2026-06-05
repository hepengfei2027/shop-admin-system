package com.example.market.controller;

import com.example.market.dto.Result;
import com.example.market.entity.Message;
import com.example.market.service.MessageService;
import com.example.market.service.ConversationService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/api/message")
@CrossOrigin
public class MessageController {

    @Resource
    private MessageService messageService;

    @Resource
    private ConversationService conversationService;

    @PostMapping("/send")
    public Result<Message> sendMessage(@RequestParam java.lang.Long senderId, @RequestParam java.lang.Long receiverId, @RequestParam String content) {
        if (senderId.equals(receiverId)) {
            return Result.fail("不能给自己发送消息");
        }
        Message message = messageService.sendMessage(senderId, receiverId, content);
        return Result.ok(message);
    }

    @GetMapping("/list/{userId}")
    public Result<List<Message>> getMessages(@PathVariable java.lang.Long userId) {
        List<Message> messages = messageService.getMessagesByUserId(userId);
        return Result.ok(messages);
    }

    @GetMapping("/all/{userId}")
    public Result<List<Message>> getAllMessages(@PathVariable java.lang.Long userId) {
        List<Message> messages = messageService.getAllMessagesByUserId(userId);
        return Result.ok(messages);
    }

    @GetMapping("/conversation/{conversationId}")
    public Result<List<Message>> getMessagesByConversationId(@PathVariable java.lang.Long conversationId) {
        List<Message> messages = messageService.getMessagesByConversationId(conversationId);
        return Result.ok(messages);
    }

    @PostMapping("/read/{id}")
    public Result<Void> markAsRead(@PathVariable java.lang.Long id) {
        messageService.markAsRead(id);
        return Result.ok(null);
    }

    @GetMapping("/unread/{userId}")
    public Result<Integer> countUnread(@PathVariable java.lang.Long userId) {
        int count = messageService.countUnread(userId);
        return Result.ok(count);
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteMessage(@PathVariable java.lang.Long id, @RequestParam java.lang.Long senderId) {
        messageService.deleteMessage(id, senderId);
        return Result.ok(null);
    }

    @DeleteMapping("/deleteConversation")
    public Result<Void> deleteConversation(@RequestParam java.lang.Long userId, @RequestParam java.lang.Long conversationId) {
        conversationService.softDeleteConversation(userId, conversationId);
        return Result.ok(null);
    }
}