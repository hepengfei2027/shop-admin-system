package com.example.market.service.impl;

import com.example.market.entity.Message;
import com.example.market.mapper.MessageMapper;
import com.example.market.service.ConversationService;
import com.example.market.service.MessageService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class MessageServiceImpl implements MessageService {

    @Resource
    private MessageMapper messageMapper;

    @Resource
    private ConversationService conversationService;

    @Override
    public Message sendMessage(java.lang.Long senderId, java.lang.Long receiverId, String content) {
        // 查找或创建会话
        java.lang.Long conversationId = conversationService.findOrCreateConversation(senderId, receiverId);
        
        Message message = new Message();
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setConversationId(conversationId);
        message.setContent(content);
        message.setStatus(0); // 未读
        messageMapper.insert(message);
        return message;
    }

    @Override
    public List<Message> getMessagesByUserId(java.lang.Long userId) {
        return messageMapper.findByReceiverId(userId);
    }

    @Override
    public List<Message> getAllMessagesByUserId(java.lang.Long userId) {
        return messageMapper.findAllByUserId(userId);
    }

    @Override
    public List<Message> getMessagesByConversationId(java.lang.Long conversationId) {
        return messageMapper.findByConversationId(conversationId);
    }

    @Override
    public void markAsRead(java.lang.Long messageId) {
        messageMapper.markAsRead(messageId);
    }

    @Override
    public void deleteMessage(java.lang.Long messageId, java.lang.Long senderId) {
        messageMapper.delete(messageId, senderId);
    }

    @Override
    public int countUnread(java.lang.Long userId) {
        return messageMapper.countUnread(userId);
    }
}