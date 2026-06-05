package com.example.market.service;

import com.example.market.entity.Conversation;
import com.example.market.entity.UserConversation;

import java.util.List;

public interface ConversationService {

    Conversation createConversation();

    UserConversation addUserToConversation(Long userId, Long conversationId);

    List<UserConversation> getUserConversations(Long userId);

    void softDeleteConversation(Long userId, Long conversationId);

    UserConversation findByUserIdAndConversationId(Long userId, Long conversationId);

    void updateLastReadTime(Long userId, Long conversationId);

    Long findOrCreateConversation(Long userId1, Long userId2);
}