<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
// 👇 引入 useRouter
import { useRouter } from 'vue-router';
import { api } from '../../api';
import { ElMessage, ElCard, ElButton, ElAvatar, ElEmpty, ElBadge } from 'element-plus';

// 👇 加上这一句
const router = useRouter();

const user = ref<any>(null);
const messages = ref<any[]>([]);
const users = ref<Map<number, any>>(new Map());

onMounted(() => {
  const userStr = localStorage.getItem('user');
  if (userStr) {
    user.value = JSON.parse(userStr);
    loadMessages();
  } else {
    ElMessage.error('请先登录');
    setTimeout(() => {
      // ✅ 这里也换成 router.push，自动适配本地/线上
      router.push('/login');
    }, 1000);
  }
});

const loadMessages = async () => {
  try {
    const res = await api.getAllMessages(user.value.id);
    if (res.data.code === 0) {
      messages.value = res.data.data || [];
      messages.value.forEach(message => {
        if (message.status === 0 && message.receiverId === user.value.id) {
          api.markAsRead(message.id);
        }
        loadUserInfo(message.senderId);
        if (message.receiverId !== user.value.id) {
          loadUserInfo(message.receiverId);
        }
      });
    }
  } catch (err) {
    ElMessage.error('加载消息失败');
  }
};

const loadUserInfo = async (userId: number) => {
  if (!users.value.has(userId)) {
    try {
      const res = await api.userDetail(userId);
      if (res.data.code === 0) {
        users.value.set(userId, res.data.data);
      }
    } catch (err) {
      console.error('加载用户信息失败', err);
    }
  }
};

const getUserName = (userId: number) => {
  const userInfo = users.value.get(userId);
  return userInfo ? (userInfo.nickname || userInfo.username) : '未知用户';
};

const formatTime = (time: string) => {
  const date = new Date(time);
  const now = new Date();
  const diff = now.getTime() - date.getTime();
  const days = Math.floor(diff / (1000 * 60 * 60 * 24));

  if (days === 0) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
  } else if (days === 1) {
    return '昨天';
  } else if (days < 7) {
    return days + '天前';
  } else {
    return date.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' });
  }
};

const groupedConversations = computed(() => {
  const groups = new Map<number, any[]>();
  messages.value.forEach(message => {
    if (!groups.has(message.conversationId)) {
      groups.set(message.conversationId, []);
    }
    groups.get(message.conversationId)?.push(message);
  });

  return Array.from(groups.entries())
      .map(([conversationId, msgs]) => {
        msgs.sort((a, b) => new Date(a.createTime).getTime() - new Date(b.createTime).getTime());
        const otherUserId = msgs[0].senderId === user.value.id ? msgs[0].receiverId : msgs[0].senderId;
        const latestMessage = msgs[msgs.length - 1];
        const unreadCount = msgs.filter(msg => msg.receiverId === user.value.id && msg.status === 0).length;

        return {
          conversationId,
          userId: otherUserId,
          userName: getUserName(otherUserId),
          latestMessage: latestMessage.content,
          latestMessageTime: latestMessage.createTime,
          unreadCount
        };
      })
      .sort((a, b) => {
        return new Date(b.latestMessageTime).getTime() - new Date(a.latestMessageTime).getTime();
      });
});

// ✅ 最终完美版跳转（本地/线上自动适配，不会丢 Web3）
const goToChat = (conversationId: number, userId: number) => {
  router.push({
    path: '/chat',
    query: { conversationId, userId }
  })
}
</script>

<template>
  <div class="message-list-container">
    <h2>消息</h2>
    <el-card v-if="groupedConversations.length === 0" class="no-messages">
      <el-empty description="暂无消息" />
    </el-card>
    <div v-else class="conversation-list">
      <el-card 
        v-for="conversation in groupedConversations" 
        :key="conversation.conversationId" 
        class="conversation-item"
        @click="goToChat(conversation.conversationId, conversation.userId)"
      >
        <div class="conversation-content">
          <div class="conversation-avatar">
            <el-avatar :size="50">{{ conversation.userName.charAt(0) }}</el-avatar>
            <el-badge v-if="conversation.unreadCount > 0" :value="conversation.unreadCount" class="unread-badge" />
          </div>
          <div class="conversation-info">
            <div class="conversation-header">
              <h3 class="conversation-name">{{ conversation.userName }}</h3>
              <span class="conversation-time">{{ formatTime(conversation.latestMessageTime) }}</span>
            </div>
            <p class="conversation-latest-message">{{ conversation.latestMessage }}</p>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.message-list-container {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
}

.no-messages {
  margin-top: 20px;
}

.conversation-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 20px;
}

.conversation-item {
  cursor: pointer;
  transition: all 0.3s ease;
}

.conversation-item:hover {
  box-shadow: 0 4px 12px 0 rgba(0, 0, 0, 0.15);
}

.conversation-content {
  display: flex;
  align-items: center;
}

.conversation-avatar {
  position: relative;
  margin-right: 15px;
}

.unread-badge {
  position: absolute;
  top: -5px;
  right: -5px;
  background-color: #f56c6c;
  color: white;
}

.conversation-info {
  flex: 1;
  min-width: 0;
}

.conversation-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 5px;
}

.conversation-name {
  margin: 0;
  font-size: 16px;
  font-weight: bold;
}

.conversation-time {
  font-size: 12px;
  color: #999;
}

.conversation-latest-message {
  margin: 0;
  font-size: 14px;
  color: #666;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>