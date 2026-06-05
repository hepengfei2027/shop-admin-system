package com.example.market.service;

import com.example.market.entity.Message;

import java.util.List;

public interface MessageService {

    Message sendMessage(Long senderId, Long receiverId, String content);

    List<Message> getMessagesByUserId(Long userId);

    List<Message> getAllMessagesByUserId(Long userId);

    List<Message> getMessagesByConversationId(Long conversationId);

    void markAsRead(Long messageId);

    void deleteMessage(Long messageId, Long senderId);

    int countUnread(Long userId);
}