<script setup lang="ts">
import { ref, onMounted, computed, provide } from 'vue';
import { ArrowDown, Menu, Close } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { api } from './api';

const user = ref<any>(null);
const unreadCount = ref(0);
const mobileMenuVisible = ref(false);

const memberLevels = [
  { level: 1, name: '普通会员', minExp: 0, maxExp: 100, discount: 1 },
  { level: 2, name: '银卡会员', minExp: 100, maxExp: 500, discount: 0.98 },
  { level: 3, name: '金卡会员', minExp: 500, maxExp: 2000, discount: 0.95 },
  { level: 4, name: '铂金会员', minExp: 2000, maxExp: 5000, discount: 0.92 },
  { level: 5, name: '钻石会员', minExp: 5000, maxExp: 10000, discount: 0.88 },
  { level: 6, name: '至尊会员', minExp: 10000, maxExp: Infinity, discount: 0.85 }
];

const getMemberDiscount = (experience: number) => {
  if (user.value && user.value.discount !== undefined) {
    return user.value.discount;
  }
  for (let i = memberLevels.length - 1; i >= 0; i--) {
    if (experience >= memberLevels[i].minExp) {
      return memberLevels[i].discount;
    }
  }
  return 1;
};

const getMemberLevel = (experience: number) => {
  if (user.value && user.value.memberLevel !== undefined) {
    const level = memberLevels.find(l => l.level === user.value.memberLevel);
    return level || memberLevels[0];
  }
  for (let i = memberLevels.length - 1; i >= 0; i--) {
    if (experience >= memberLevels[i].minExp) {
      return memberLevels[i];
    }
  }
  return memberLevels[0];
};

provide('memberLevels', memberLevels);
provide('getMemberDiscount', getMemberDiscount);
provide('getMemberLevel', getMemberLevel);

const loadUnreadCount = async () => {
  if (user.value) {
    try {
      const res = await api.countUnread(user.value.id);
      if (res.data.code === 0) {
        unreadCount.value = res.data.data;
      }
    } catch (err) {
      console.error('加载未读消息数失败', err);
    }
  }
};

const updateUser = async (userData: any) => {
  user.value = userData;
  await loadUnreadCount();
};

// 提供用户状态和更新方法给子组件
provide('user', user);
provide('updateUser', updateUser);

const isMobile = computed(() => {
  return window.innerWidth < 768;
});

const getUserRoleLabel = (role: number) => {
  switch (role) {
    case 1:
      return '管理员';
    case 2:
      return '卖家';
    default:
      return '买家';
  }
};

onMounted(async () => {
  const userStr = localStorage.getItem('user');
  if (userStr) {
    user.value = JSON.parse(userStr);
    await loadUnreadCount();
  }
});

const logout = () => {
  localStorage.removeItem('user');
  user.value = null;
  ElMessage.success('退出登录成功');
  window.location.reload();
};
</script>

<template>
  <el-container style="min-height: 100vh">
<!--导航栏高度-->
    <el-header style="height: 25px">
      <div class="nav">
        <div class="logo" @click="$router.push('/')">JD</div>
        <!-- 移动端菜单按钮 -->
        <div class="mobile-menu-button" v-if="isMobile" @click="mobileMenuVisible = !mobileMenuVisible">
          <el-icon v-if="!mobileMenuVisible"><Menu /></el-icon>
          <el-icon v-else><Close /></el-icon>
        </div>
        <!-- 桌面端导航 -->
        <div class="nav-right" v-if="!isMobile">
          <template v-if="user">
            <el-dropdown>
              <span class="user-dropdown" style="cursor: pointer; margin-right: 20px; display: flex; align-items: center; gap: 8px;">
                <span class="user-role-badge" :class="`role-${user.role}`">{{ getUserRoleLabel(user.role) }}</span>
                <el-avatar :size="24" :src="user.avatar || ''">{{ (user.nickname || user.username || 'U').charAt(0).toUpperCase() }}</el-avatar>
                <span>{{ user.nickname || user.username }}</span>
                <el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="$router.push('/login')">切换账号</el-dropdown-item>
                  <el-dropdown-item @click="logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button type="primary" text @click="$router.push('/login')">登录</el-button>
          </template>
          <el-button type="primary" text @click="$router.push('/')">首页</el-button>
          <el-button type="primary" text @click="$router.push('/Profile')">个人中心</el-button>
          <el-button v-if="user && user.role === 2" type="primary" text @click="$router.push('/publish')">发布商品</el-button>
          <el-button v-if="user && user.role === 2" type="primary" text @click="$router.push('/shop-management')">店铺管理</el-button>
          <el-button v-if="user && user.role === 2" type="primary" text @click="$router.push('/seller-analytics')">数据分析</el-button>
          <el-button v-if="!user || user.role !== 1" type="primary" text @click="$router.push('/orders')">我的订单</el-button>
          <el-button type="primary" text @click="$router.push('/messages')">
            消息
            <el-badge v-if="unreadCount > 0" :value="unreadCount" class="message-badge" />
          </el-button>
          <el-button v-if="user && user.role === 1" type="primary" text @click="$router.push('/admin')">管理员后台</el-button>
        </div>
      </div>
      <!-- 移动端导航菜单 -->
      <div class="mobile-menu" v-if="isMobile && mobileMenuVisible">
        <div class="mobile-menu-content">
          <template v-if="user">
            <div class="mobile-user-info">
              <div class="mobile-user-header">
                <span class="user-role-badge" :class="`role-${user.role}`">{{ getUserRoleLabel(user.role) }}</span>
                <el-avatar :size="32" :src="user.avatar || ''">{{ (user.nickname || user.username || 'U').charAt(0).toUpperCase() }}</el-avatar>
                <div class="mobile-user-details">
                  <span>{{ user.nickname || user.username }}</span>
                </div>
              </div>
              <div class="mobile-user-actions">
                <el-button type="primary" text @click="$router.push('/login')">切换账号</el-button>
                <el-button type="primary" text @click="logout">退出登录</el-button>
              </div>
            </div>
          </template>
          <template v-else>
            <el-button type="primary" text @click="$router.push('/login')">登录</el-button>
          </template>
          <el-button type="primary" text @click="$router.push('/')">首页</el-button>
          <el-button type="primary" text @click="$router.push('/Profile')">个人中心</el-button>
          <el-button v-if="user && user.role === 2" type="primary" text @click="$router.push('/publish')">发布商品</el-button>
          <el-button v-if="user && user.role === 2" type="primary" text @click="$router.push('/shop-management')">店铺管理</el-button>
          <el-button v-if="user && user.role === 2" type="primary" text @click="$router.push('/seller-analytics')">数据分析</el-button>
          <el-button v-if="!user || user.role !== 1" type="primary" text @click="$router.push('/orders')">我的订单</el-button>
          <el-button type="primary" text @click="$router.push('/messages')">
            消息
            <el-badge v-if="unreadCount > 0" :value="unreadCount" class="message-badge" />
          </el-button>
          <el-button v-if="user && user.role === 1" type="primary" text @click="$router.push('/admin')">管理员后台</el-button>
        </div>
      </div>
    </el-header>
    <el-main>
      <router-view />
    </el-main>
  </el-container>
</template>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body {
  width: 100%;
  min-width: 100%;
  margin: 0;
  padding: 0;
}

#app {
  width: 100%;
  min-width: 100%;
  margin: 0;
  padding: 0;
}
</style>

<style scoped>
.nav {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 10px;
}

.logo {
  font-size: 20px;
  font-weight: bold;
  cursor: pointer;
  color: #ff0000;
}

/* 移动端菜单按钮 */
.mobile-menu-button {
  font-size: 24px;
  cursor: pointer;
  padding: 5px;
}

/* 移动端菜单 */
.mobile-menu {
  position: fixed;
  top: 60px;
  left: 0;
  right: 0;
  background-color: white;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  z-index: 1000;
}

.mobile-menu-content {
  padding: 10px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.mobile-user-info {
  padding: 10px;
  border-bottom: 1px solid #e4e7ed;
  margin-bottom: 10px;
}

.mobile-user-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.mobile-user-details {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.mobile-user-actions {
  display: flex;
  gap: 10px;
  margin-top: 10px;
}

.user-role-badge {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  color: white;
}

.role-1 {
  background-color: #e74c3c;
}

.role-2 {
  background-color: #3498db;
}

.role-0 {
  background-color: #2ecc71;
}

/*导航栏字体颜色*/
.nav :deep(.el-button--primary.is-text) {
  color: #666;
}

/* 响应式样式 */
@media (max-width: 768px) {
  .logo {
    font-size: 16px;
  }
  
  .nav-right {
    display: none;
  }
  
  .mobile-menu-button {
    display: block;
  }
}

@media (min-width: 769px) {
  .mobile-menu-button {
    display: none;
  }
  
  .mobile-menu {
    display: none;
  }
}
</style>
