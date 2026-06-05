<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ArrowLeft } from '@element-plus/icons-vue';
import { api } from '../../api';
import { ElMessage, ElDialog, ElInput, ElButton, ElMessageBox, ElIcon } from 'element-plus';

const route = useRoute();
const router = useRouter();
const user = ref<any>(null);
const messages = ref<any[]>([]);
const users = ref<Map<number, any>>(new Map());
const dialogVisible = ref(false);
const replyContent = ref('');
const currentMessage = ref<any>(null);
const conversationId = ref<number | null>(null);
const otherUserId = ref<number | null>(null);
const otherUserName = ref('');

onMounted(() => {
  const userStr = localStorage.getItem('user');
  if (userStr) {
    user.value = JSON.parse(userStr);
    // 从路由参数中获取会话ID和对方用户ID
    const params = route.query;
    if (params.conversationId && params.userId) {
      conversationId.value = Number(params.conversationId);
      otherUserId.value = Number(params.userId);
      loadUserInfo(otherUserId.value);
      loadConversationMessages();
    } else {
      // 如果没有路由参数，重定向到消息列表页面
      router.push('/messages');
    }
  } else {
    ElMessage.error('请先登录');
    setTimeout(() => {
      window.location.href = '/login';
    }, 1000);
  }
});

const loadConversationMessages = async () => {
  if (!conversationId.value) return;
  try {
    const res = await api.getMessagesByConversationId(conversationId.value);
    if (res.data.code === 0) {
      messages.value = res.data.data || [];
      // 标记所有接收的消息为已读
      messages.value.forEach(message => {
        if (message.status === 0 && message.receiverId === user.value.id) {
          api.markAsRead(message.id);
        }
        // 加载发送者信息
        loadUserInfo(message.senderId);
        // 加载接收者信息（如果不是当前用户）
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
        // 如果是对方用户，设置对方用户名称
        if (userId === otherUserId.value) {
          otherUserName.value = res.data.data.nickname || res.data.data.username;
        }
      }
    } catch (err) {
      console.error('加载用户信息失败', err);
    }
  } else {
    // 如果用户信息已存在，检查是否是对方用户
    if (userId === otherUserId.value) {
      const userInfo = users.value.get(userId);
      otherUserName.value = userInfo.nickname || userInfo.username;
    }
  }
};

const getUserName = (userId: number) => {
  const userInfo = users.value.get(userId);
  return userInfo ? (userInfo.nickname || userInfo.username) : '未知用户';
};

const formatTime = (time: string) => {
  return new Date(time).toLocaleString();
};

const openReplyDialog = (message: any) => {
  // 检查是否是自己给自己发的消息
  const otherUserId = message.senderId === user.value.id ? message.receiverId : message.senderId;
  if (otherUserId === user.value.id) {
    ElMessage.warning('不能给自己发消息');
    return;
  }
  currentMessage.value = message;
  replyContent.value = '';
  dialogVisible.value = true;
};

const sendReply = async () => {
  if (!replyContent.value.trim()) {
    ElMessage.warning('请输入回复内容');
    return;
  }
  try {
    if (!otherUserId.value) {
      ElMessage.error('无法确定接收者');
      return;
    }
    await api.sendMessage(user.value.id, otherUserId.value, replyContent.value);
    ElMessage.success('消息已发送');
    replyContent.value = '';
    loadConversationMessages();
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.msg || '发送失败');
  }
};

const deleteMessage = async (messageId: number, senderId: number) => {
  try {
    await ElMessageBox.confirm('确定要删除这条消息吗？', '删除消息', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    });
    await api.deleteMessage(messageId, senderId);
    ElMessage.success('消息已删除');
    loadMessages();
  } catch (err) {
    // 用户取消删除
  }
};

const deleteConversation = async (conversationId: number) => {
  try {
    await ElMessageBox.confirm('确定要删除整个对话吗？', '删除对话', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    });
    await api.deleteConversation(user.value.id, conversationId);
    ElMessage.success('对话已删除');
    loadMessages();
  } catch (err) {
    // 用户取消删除
  }
};

// 按会话分组
const groupedMessages = computed(() => {
  const groups = new Map<number, any[]>();
  messages.value.forEach(message => {
    // 按会话ID分组
    if (!groups.has(message.conversationId)) {
      groups.set(message.conversationId, []);
    }
    groups.get(message.conversationId)?.push(message);
  });
  // 转换为数组并按最新消息时间排序
  return Array.from(groups.entries())
    .map(([conversationId, msgs]) => {
      // 按时间正序排列消息
      msgs.sort((a, b) => new Date(a.createTime).getTime() - new Date(b.createTime).getTime());
      // 确定对话的对方用户ID
      const otherUserId = msgs[0].senderId === user.value.id ? msgs[0].receiverId : msgs[0].senderId;
      return {
        conversationId,
        userId: otherUserId,
        userName: getUserName(otherUserId),
        messages: msgs
      };
    })
    .sort((a, b) => {
      // 按最新消息时间倒序排列对话
      const timeA = new Date(a.messages[a.messages.length - 1].createTime).getTime();
      const timeB = new Date(b.messages[b.messages.length - 1].createTime).getTime();
      return timeB - timeA;
    });
});
</script>

<template>
  <div class="messages-container">
    <!-- 聊天头部 -->
    <div class="chat-header">
      <el-button type="primary" text @click="router.push('/message-list')">
        <el-icon><ArrowLeft /></el-icon> 返回
      </el-button>
      <h2>{{ otherUserName || '聊天' }}</h2>
      <div class="chat-actions">
        <el-button type="danger" size="small" @click="deleteConversation(conversationId)">删除对话</el-button>
      </div>
    </div>
    
    <!-- 消息列表 -->
    <el-card v-if="messages.length === 0" class="no-messages">
      <el-empty description="暂无消息" />
    </el-card>
    <div v-else class="message-list">
      <div v-for="message in messages" :key="message.id" class="message-item" :class="{ 'sent-message': message.senderId === user?.id }">
        <div class="message-avatar">
          <el-avatar :size="30">{{ message.senderId === user?.id ? '我' : otherUserName.charAt(0) }}</el-avatar>
        </div>
        <div class="message-content">
          <div class="message-sender">{{ message.senderId === user?.id ? '我' : otherUserName }}</div>
          <div class="message-text">{{ message.content }}</div>
          <div class="message-footer">
            <span class="message-time">{{ formatTime(message.createTime) }}</span>
            <el-button v-if="message.senderId === user?.id" type="danger" size="small" @click="deleteMessage(message.id, message.senderId)">删除</el-button>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 消息输入框 -->
    <div class="message-input-container">
      <el-input
        v-model="replyContent"
        type="textarea"
        :rows="2"
        placeholder="请输入消息内容"
        class="message-input"
      />
      <el-button type="primary" @click="sendReply" :disabled="!replyContent.trim()">发送</el-button>
    </div>
  </div>
</template>

<style scoped>
.messages-container {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  min-height: 70vh;
}

.chat-header {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 1px solid #e4e7ed;
}

.chat-header h2 {
  margin: 0;
  font-size: 18px;
  font-weight: bold;
  flex: 1;
  text-align: center;
}

.chat-actions {
  margin-left: auto;
}

.no-messages {
  margin-top: 20px;
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.message-list {
  padding: 10px 0;
  flex: 1;
  overflow-y: auto;
  margin-bottom: 20px;
}

.message-item {
  display: flex;
  margin-bottom: 15px;
  align-items: flex-start;
}

.message-item.sent-message {
  flex-direction: row-reverse;
}

.message-avatar {
  margin: 0 10px;
}

.message-content {
  max-width: 70%;
  padding: 10px 15px;
  border-radius: 18px;
  background-color: #f0f0f0;
  position: relative;
}

.message-item.sent-message .message-content {
  background-color: #409eff;
  color: white;
}

.message-sender {
  font-size: 12px;
  font-weight: bold;
  margin-bottom: 5px;
  color: #666;
}

.message-item.sent-message .message-sender {
  color: rgba(255, 255, 255, 0.8);
  text-align: right;
}

.message-text {
  line-height: 1.5;
  margin-bottom: 5px;
  word-break: break-word;
}

.message-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 5px;
}

.message-time {
  font-size: 10px;
  color: #999;
}

.message-item.sent-message .message-time {
  color: rgba(255, 255, 255, 0.6);
}

.message-input-container {
  display: flex;
  gap: 10px;
  padding-top: 10px;
  border-top: 1px solid #e4e7ed;
}

.message-input {
  flex: 1;
}

.message-input textarea {
  resize: none;
}
</style>