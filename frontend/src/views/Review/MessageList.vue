<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import { api } from '../../api';
import { ElMessage, ElCard, ElAvatar, ElEmpty, ElBadge } from 'element-plus';

const router = useRouter();

type UserItem = {
  id: number;
  nickname?: string;
  username: string;
};
type MessageItem = {
  id: number;
  conversationId: number;
  senderId: number;
  receiverId: number;
  content: string;
  status: 0 | 1;
  createTime: string;
};

const user = ref<UserItem | null>(null);
const messages = ref<MessageItem[]>([]);
const users = ref<Map<number, UserItem>>(new Map());

onMounted(async () => {
  const userStr = localStorage.getItem('user');
  if (!userStr) {
    ElMessage.error('请先登录');
    setTimeout(() => router.push('/login'), 800);
    return;
  }
  user.value = JSON.parse(userStr);
  await loadMessages();
});

const loadMessages = async () => {
  if (!user.value) return;
  try {
    const res = await api.getAllMessages(user.value.id);
    if (res.data.code === 0) {
      messages.value = res.data.data || [];
      // 批量加载用户 & 已读标记
      const userIdSet = new Set<number>();
      messages.value.forEach(message => {
        if (message.status === 0 && message.receiverId === user.value!.id) {
          api.markAsRead(message.id);
        }
        userIdSet.add(message.senderId);
        userIdSet.add(message.receiverId);
      });
      // 统一请求用户信息，减少重复接口
      Array.from(userIdSet).forEach(id => loadUserInfo(id));
    }
  } catch (err) {
    ElMessage.error('加载消息失败');
  }
};

const loadUserInfo = async (userId: number) => {
  if (users.value.has(userId)) return;
  try {
    const res = await api.userDetail(userId);
    if (res.data.code === 0) {
      users.value.set(userId, res.data.data);
    }
  } catch (err) {
    console.error('加载用户信息失败', err);
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

  if (days === 0) return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
  if (days === 1) return '昨天';
  if (days < 7) return `${days}天前`;
  return date.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' });
};

// 会话分组计算属性
const groupedConversations = computed(() => {
  const groups = new Map<number, MessageItem[]>();
  messages.value.forEach(msg => {
    if (!groups.has(msg.conversationId)) groups.set(msg.conversationId, []);
    groups.get(msg.conversationId)!.push(msg);
  });

  return Array.from(groups.entries())
      .map(([cid, msgs]) => {
        msgs.sort((a, b) => new Date(a.createTime).getTime() - new Date(b.createTime).getTime());
        const lastMsg = msgs.at(-1)!;
        const otherUid = lastMsg.senderId === user.value!.id ? lastMsg.receiverId : lastMsg.senderId;
        const unreadCount = msgs.filter(m => m.receiverId === user.value!.id && m.status === 0).length;

        return {
          conversationId: cid,
          userId: otherUid,
          userName: getUserName(otherUid),
          latestMsg: lastMsg.content,
          latestTime: lastMsg.createTime,
          unreadCount
        };
      })
      .sort((a, b) => new Date(b.latestTime).getTime() - new Date(a.latestTime).getTime());
});

// 跳转聊天页
const goToChat = (conversationId: number, userId: number) => {
  router.push({ path: '/chat', query: { conversationId, userId } });
};
</script>

<template>
  <div class="message-wrap">
    <h2 class="page-title">消息</h2>

    <!-- 无消息空状态 -->
    <el-card v-if="groupedConversations.length === 0" class="empty-card" shadow="never">
      <el-empty description="暂无聊天消息" image-size="80" />
    </el-card>

    <!-- 会话列表 -->
    <div v-else class="conv-list">
      <el-card
          v-for="item in groupedConversations"
          :key="item.conversationId"
          class="conv-item"
          shadow="never"
          @click="goToChat(item.conversationId, item.userId)"
      >
        <div class="conv-row">
          <!-- 头像+未读红点 -->
          <div class="avatar-box">
            <el-avatar size="40">{{ item.userName.charAt(0) }}</el-avatar>
            <el-badge
                v-if="item.unreadCount > 0"
                :value="item.unreadCount"
                class="dot-badge"
            />
          </div>

          <!-- 聊天信息 -->
          <div class="conv-text">
            <div class="conv-head">
              <span class="name">{{ item.userName }}</span>
              <span class="time">{{ formatTime(item.latestTime) }}</span>
            </div>
            <p class="last-msg">{{ item.latestMsg }}</p>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
/* 外层容器极致压缩边距 */
.message-wrap {
  padding: 12px 16px;
  max-width: 720px;
  margin: 0 auto;
}

.page-title {
  font-size: 18px;
  margin: 0 0 12px;
  font-weight: 500;
  color: #303133;
}

/* 空状态卡片 */
.empty-card {
  padding: 20px 0;
}

/* 会话列表容器 */
.conv-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

/* 单条会话卡片 紧凑核心 */
.conv-item {
  padding: 10px 14px !important;
  cursor: pointer;
  border-radius: 6px;
  transition: background 0.2s ease;
}
.conv-item:hover {
  background-color: #f7f8fa;
}

/* 一行布局 */
.conv-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

/* 头像容器+角标 */
.avatar-box {
  position: relative;
  flex-shrink: 0;
}
.dot-badge {
  position: absolute;
  top: -3px;
  right: -3px;
}
:deep(.dot-badge .el-badge__content) {
  min-width: 14px;
  height: 14px;
  font-size: 10px;
  padding: 0 3px;
  background: #ef4747;
}

/* 右侧文字区域 */
.conv-text {
  flex: 1;
  min-width: 0;
}
.conv-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 3px;
}
.name {
  font-size: 15px;
  color: #1f2937;
  font-weight: 500;
}
.time {
  font-size: 12px;
  color: #a0a0a0;
}
.last-msg {
  margin: 0;
  font-size: 13px;
  color: #747474;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>