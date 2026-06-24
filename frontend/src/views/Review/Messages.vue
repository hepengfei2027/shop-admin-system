<script setup lang="ts">
import { ref, onMounted, computed, nextTick, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ArrowLeft, Delete, ChatDotRound } from '@element-plus/icons-vue';
import { api } from '../../api';
import { ElMessage, ElMessageBox, ElIcon, ElCard, ElEmpty, ElAvatar, ElInput, ElButton } from 'element-plus';

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
// 聊天滚动容器ref
const messageListRef = ref<HTMLDivElement | null>(null);

// 页面挂载初始化
onMounted(async () => {
  const userStr = localStorage.getItem('user');
  if (!userStr) {
    ElMessage.error('请先登录');
    setTimeout(() => (window.location.href = '/login'), 1000);
    return;
  }
  user.value = JSON.parse(userStr);
  const params = route.query;
  if (!params.conversationId || !params.userId) {
    router.push('/messages');
    return;
  }
  conversationId.value = Number(params.conversationId);
  otherUserId.value = Number(params.userId);
  await loadUserInfo(otherUserId.value!);
  await loadConversationMessages();
});

// 监听消息变化，自动滚动到底部
watch(messages, async () => {
  await nextTick();
  scrollToBottom();
});

// 滚动聊天框至底部
const scrollToBottom = () => {
  if (!messageListRef.value) return;
  messageListRef.value.scrollTop = messageListRef.value.scrollHeight;
};

// 加载对话消息列表
const loadConversationMessages = async () => {
  if (!conversationId.value) return;
  try {
    const res = await api.getMessagesByConversationId(conversationId.value);
    if (res.data.code === 0) {
      messages.value = res.data.data || [];
      // 批量加载用户信息 + 标记已读
      messages.value.forEach((message) => {
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
    ElMessage.error('加载消息失败，请刷新重试');
  }
};

// 缓存加载用户信息，避免重复请求
const loadUserInfo = async (userId: number) => {
  if (users.value.has(userId)) {
    if (userId === otherUserId.value) {
      const info = users.value.get(userId);
      otherUserName.value = info.nickname || info.username;
    }
    return;
  }
  try {
    const res = await api.userDetail(userId);
    if (res.data.code === 0) {
      users.value.set(userId, res.data.data);
      if (userId === otherUserId.value) {
        otherUserName.value = res.data.data.nickname || res.data.data.username;
      }
    }
  } catch (err) {
    console.error('用户信息加载失败', err);
  }
};

// 获取用户昵称
const getUserName = (userId: number) => {
  const userInfo = users.value.get(userId);
  return userInfo ? userInfo.nickname || userInfo.username : '未知用户';
};

// 格式化时间
const formatTime = (time: string) => {
  const date = new Date(time);
  const h = String(date.getHours()).padStart(2, '0');
  const m = String(date.getMinutes()).padStart(2, '0');
  return `${h}:${m}`;
};

// 打开回复弹窗（当前页面已内置输入框，此函数保留备用）
const openReplyDialog = (message: any) => {
  const targetId = message.senderId === user.value.id ? message.receiverId : message.senderId;
  if (targetId === user.value.id) {
    ElMessage.warning('不能给自己发送消息');
    return;
  }
  currentMessage.value = message;
  replyContent.value = '';
  dialogVisible.value = true;
};

// 发送消息
const sendReply = async () => {
  const content = replyContent.value.trim();
  if (!content) {
    ElMessage.warning('请输入聊天内容');
    return;
  }
  if (!otherUserId.value) {
    ElMessage.error('接收用户异常');
    return;
  }
  try {
    await api.sendMessage(user.value.id, otherUserId.value, content);
    replyContent.value = '';
    await loadConversationMessages();
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.msg || '消息发送失败');
  }
};

// 删除单条消息
const deleteMessage = async (messageId: number, senderId: number) => {
  try {
    await ElMessageBox.confirm('确定删除本条消息？删除后不可恢复', '提示', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning',
      confirmButtonClass: 'del-msg-btn',
    });
    await api.deleteMessage(messageId, senderId);
    ElMessage.success('消息已删除');
    loadConversationMessages();
  } catch {
    // 取消操作不提示
  }
};

// 删除整个对话
const deleteConversation = async (cid: number) => {
  try {
    await ElMessageBox.confirm('删除整个对话后，所有聊天记录将清空，是否继续？', '危险操作', {
      confirmButtonText: '彻底删除',
      cancelButtonText: '取消',
      type: 'error',
    });
    await api.deleteConversation(user.value.id, cid);
    ElMessage.success('对话已删除');
    router.push('/message-list');
  } catch {}
};

// 原有分组计算属性（当前页面未使用，保留兼容）
const groupedMessages = computed(() => {
  const groups = new Map<number, any[]>();
  messages.value.forEach((msg) => {
    if (!groups.has(msg.conversationId)) groups.set(msg.conversationId, []);
    groups.get(msg.conversationId)!.push(msg);
  });
  return Array.from(groups.entries())
      .map(([cid, msgs]) => {
        msgs.sort((a, b) => new Date(a.createTime).getTime() - new Date(b.createTime).getTime());
        const targetUid = msgs[0].senderId === user.value.id ? msgs[0].receiverId : msgs[0].senderId;
        return {
          conversationId: cid,
          userId: targetUid,
          userName: getUserName(targetUid),
          messages: msgs,
        };
      })
      .sort((a, b) => {
        const t1 = new Date(a.messages.at(-1).createTime).getTime();
        const t2 = new Date(b.messages.at(-1).createTime).getTime();
        return t2 - t1;
      });
});
</script>

<template>
  <div class="chat-page-wrap">
    <!-- 顶部导航栏 -->
    <div class="chat-header">
      <el-button text class="back-btn" @click="router.push('/message-list')">
        <el-icon size="18"><ArrowLeft /></el-icon>
        <span>返回会话列表</span>
      </el-button>

      <div class="chat-title">
        <el-icon size="20" color="#409eff"><ChatDotRound /></el-icon>
        <h2>{{ otherUserName }}</h2>
      </div>

      <el-button text type="danger" class="del-conv-btn" @click="deleteConversation(conversationId!)">
        <el-icon><Delete /></el-icon>
        删除对话
      </el-button>
    </div>

    <!-- 聊天消息主体区域 -->
    <div class="chat-main">
      <!-- 空白消息状态 -->
      <el-card v-if="messages.length === 0" class="empty-chat">
        <el-empty description="暂无聊天记录，发送第一条消息开启对话" />
      </el-card>

      <!-- 消息滚动容器 -->
      <div v-else ref="messageListRef" class="message-scroll">
        <div v-for="msg in messages" :key="msg.id" class="message-item" :class="{ self: msg.senderId === user?.id }">
          <!-- 对方头像 -->
          <div class="avatar-box">
            <el-avatar :size="36" fit="cover">
              {{ msg.senderId === user?.id ? '我' : otherUserName.charAt(0) }}
            </el-avatar>
          </div>

          <!-- 消息气泡 -->
          <div class="bubble-wrap">
            <!-- 发送者昵称（仅对方展示） -->
            <div v-if="msg.senderId !== user?.id" class="sender-name">{{ otherUserName }}</div>
            <div class="msg-bubble">
              <p class="msg-text">{{ msg.content }}</p>
              <div class="msg-bottom">
                <span class="msg-time">{{ formatTime(msg.createTime) }}</span>
                <el-button
                    v-if="msg.senderId === user?.id"
                    text
                    size="small"
                    class="del-btn"
                    @click="deleteMessage(msg.id, msg.senderId)"
                >
                  删除
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部输入区域 -->
    <div class="chat-input-bar">
      <el-input
          v-model="replyContent"
          type="textarea"
          :rows="1"
          autosize
          placeholder="输入消息，回车发送..."
          class="chat-input"
          @keyup.enter="sendReply"
      />
      <el-button type="primary" class="send-btn" :disabled="!replyContent.trim()" @click="sendReply">
        发送
      </el-button>
    </div>
  </div>
</template>

<style scoped>
/* 全局容器 */
.chat-page-wrap {
  width: 100%;
  max-width: 900px;
  margin: 0 auto;
  height: 92vh;
  display: flex;
  flex-direction: column;
  background: #f7f8fa;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 2px 16px rgba(0, 0, 0, 0.06);
}

/* 头部导航 */
.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
}

.back-btn {
  color: #606266;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.chat-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.chat-title h2 {
  margin: 0;
  font-size: 17px;
  font-weight: 600;
  color: #303133;
}

.del-conv-btn {
  color: #f56c6c;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 聊天主体 */
.chat-main {
  flex: 1;
  padding: 16px 20px;
  overflow: hidden;
}

.empty-chat {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
}

/* 消息滚动区域 */
.message-scroll {
  height: 100%;
  overflow-y: auto;
  padding-right: 8px;
}
/* 自定义滚动条 */
.message-scroll::-webkit-scrollbar {
  width: 6px;
}
.message-scroll::-webkit-scrollbar-thumb {
  background: #dcdfe6;
  border-radius: 3px;
}
.message-scroll::-webkit-scrollbar-track {
  background: transparent;
}

/* 单条消息 */
.message-item {
  display: flex;
  gap: 10px;
  margin-bottom: 18px;
  align-items: flex-start;
}
/* 自己发的消息右对齐 */
.message-item.self {
  flex-direction: row-reverse;
}

.avatar-box {
  flex-shrink: 0;
}

.bubble-wrap {
  max-width: 68%;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

/* 对方昵称 */
.sender-name {
  font-size: 12px;
  color: #909399;
  padding-left: 6px;
}

/* 消息气泡 */
.msg-bubble {
  padding: 10px 14px;
  border-radius: 18px;
  background: #ffffff;
  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.05);
}
.message-item.self .msg-bubble {
  background: linear-gradient(135deg, #409eff, #66b1ff);
}

.msg-text {
  margin: 0;
  line-height: 1.6;
  font-size: 14px;
  word-break: break-all;
  color: #303133;
}
.message-item.self .msg-text {
  color: #ffffff;
}

/* 消息底部时间+删除 */
.msg-bottom {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 6px;
}

.msg-time {
  font-size: 11px;
  color: #c0c4cc;
}
.message-item.self .msg-time {
  color: rgba(255, 255, 255, 0.75);
}

.del-btn {
  font-size: 11px;
  color: #f56c6c;
  padding: 0 4px;
}
.message-item.self .del-btn {
  color: rgba(255, 255, 255, 0.85);
}

/* 底部输入栏 */
.chat-input-bar {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  padding: 16px 20px;
  background: #fff;
  border-top: 1px solid #ebeef5;
}

.chat-input {
  flex: 1;
}
.chat-input :deep(.el-textarea__inner) {
  border-radius: 22px;
  padding: 10px 16px;
  font-size: 14px;
  border: 1px solid #e4e7ed;
  resize: none;
}

.send-btn {
  height: 40px;
  padding: 0 22px;
  border-radius: 20px;
  font-weight: 500;
}
</style>