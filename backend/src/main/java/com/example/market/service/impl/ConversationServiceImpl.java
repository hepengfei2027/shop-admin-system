package com.example.market.service.impl;

import com.example.market.entity.Conversation;
import com.example.market.entity.UserConversation;
import com.example.market.mapper.ConversationMapper;
import com.example.market.mapper.MessageMapper;
import com.example.market.mapper.UserConversationMapper;
import com.example.market.service.ConversationService;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ConversationServiceImpl implements ConversationService {

    @Resource
    private ConversationMapper conversationMapper;

    @Resource
    private UserConversationMapper userConversationMapper;

    @Resource
    private MessageMapper messageMapper;

    @Override
    public Conversation createConversation() {
        Conversation conversation = new Conversation();
        conversation.setCreateTime(LocalDateTime.now());
        conversationMapper.insert(conversation);
        return conversation;
    }

    @Override
    public UserConversation addUserToConversation(java.lang.Long userId, java.lang.Long conversationId) {
        UserConversation userConversation = new UserConversation();
        userConversation.setUserId(userId);
        userConversation.setConversationId(conversationId);
        userConversation.setStatus(1);
        userConversation.setLastReadTime(LocalDateTime.now());
        userConversation.setCreateTime(LocalDateTime.now());
        userConversationMapper.insert(userConversation);
        return userConversation;
    }

    @Override
    public List<UserConversation> getUserConversations(java.lang.Long userId) {
        return userConversationMapper.findByUserId(userId);
    }

    @Override
    public void softDeleteConversation(java.lang.Long userId, java.lang.Long conversationId) {
        userConversationMapper.softDelete(userId, conversationId);
    }

    @Override
    public UserConversation findByUserIdAndConversationId(java.lang.Long userId, java.lang.Long conversationId) {
        return userConversationMapper.findByUserIdAndConversationId(userId, conversationId);
    }

    @Override
    public void updateLastReadTime(java.lang.Long userId, java.lang.Long conversationId) {
        userConversationMapper.updateLastReadTime(userId, conversationId, LocalDateTime.now().toString());
    }

    @Override
    public java.lang.Long findOrCreateConversation(java.lang.Long userId1, java.lang.Long userId2) {
        // 查找两个用户之间的会话
        List<UserConversation> user1Conversations = userConversationMapper.findByUserId(userId1);
        for (UserConversation uc1 : user1Conversations) {
            List<UserConversation> user2Conversations = userConversationMapper.findByUserId(userId2);
            for (UserConversation uc2 : user2Conversations) {
                if (uc1.getConversationId().equals(uc2.getConversationId())) {
                    // 找到现有会话
                    return uc1.getConversationId();
                }
            }
        }
        // 不存在则创建新会话
        Conversation conversation = createConversation();
        addUserToConversation(userId1, conversation.getId());
        addUserToConversation(userId2, conversation.getId());
        return conversation.getId();
    }
}