<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';

const router = useRouter();
const route = useRoute();
const user = ref<any>(null);

onMounted(() => {
  const userStr = localStorage.getItem('user');
  if (userStr) {
    user.value = JSON.parse(userStr);
    if (user.value.role !== 1) {
      ElMessage.error('没有管理员权限');
      window.location.href = '/';
    }
  } else {
    ElMessage.error('请先登录');
    window.location.href = '/login';
  }
});

const logout = () => {
  localStorage.removeItem('user');
  ElMessage.success('退出登录成功');
  window.location.href = '/';
};

const navItems = [
  { path: '/admin/home', label: '数据概览', icon: '📊' },
  { path: '/admin/pending', label: '待审核商品', icon: '📋' },
  { path: '/admin/goods', label: '商品管理', icon: '🏪' },
  { path: '/admin/users', label: '用户管理', icon: '👥' },
  { path: '/admin/orders', label: '订单管理', icon: '📦' },
  { path: '/admin/statistics', label: '营收统计', icon: '💰' }
];

const activePath = computed(() => route.path);
</script>

<template>
  <div class="admin-layout">
    <!-- 顶部导航栏 -->
    <div class="admin-header">
      <div class="header-left">
        <div class="logo">
          <span class="logo-icon">🛒</span>
          <span class="logo-text">电商管理后台</span>
        </div>
      </div>
      <div class="header-right">
        <div class="user-info">
          <span class="user-role-badge role-1">管理员</span>
          <el-avatar :size="36" :src="user?.avatar || ''" class="user-avatar">
            {{ (user?.nickname || user?.username || '管').charAt(0).toUpperCase() }}
          </el-avatar>
          <span class="user-name">{{ user?.nickname || user?.username }}</span>
        </div>
        <el-button type="danger" size="small" @click="logout" class="logout-btn">
          退出登录
        </el-button>
      </div>
    </div>
    
    <!-- 主体区域 -->
    <div class="admin-body">
      <!-- 左侧导航菜单 -->
      <div class="admin-sidebar">
        <el-menu
          :default-active="activePath"
          class="admin-menu"
          router
          background-color="#1f2937"
          text-color="#9ca3af"
          active-text-color="#ffffff"
        >
          <el-menu-item
            v-for="item in navItems"
            :key="item.path"
            :index="item.path"
            class="menu-item"
          >
            <span class="menu-icon">{{ item.icon }}</span>
            <span>{{ item.label }}</span>
          </el-menu-item>
        </el-menu>
      </div>
      
      <!-- 右侧内容区域 -->
      <div class="admin-content">
        <router-view />
      </div>
    </div>
  </div>
</template>

<style scoped>
.admin-layout {
  min-height: 100vh;
  background-color: #f3f4f6;
  display: flex;
  flex-direction: column;
}

/* 顶部导航栏 */
.admin-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 0 32px;
  height: 64px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  z-index: 100;
}

.header-left {
  display: flex;
  align-items: center;
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-icon {
  font-size: 32px;
}

.logo-text {
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 1px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  background: rgba(255, 255, 255, 0.15);
  padding: 8px 16px;
  border-radius: 30px;
  backdrop-filter: blur(10px);
}

.user-avatar {
  border: 2px solid rgba(255, 255, 255, 0.3);
}

.user-name {
  font-size: 14px;
  font-weight: 500;
}

.logout-btn {
  background: rgba(255, 255, 255, 0.2);
  border: none;
  color: white;
  padding: 8px 20px;
  border-radius: 6px;
  font-weight: 500;
  transition: all 0.3s;
}

.logout-btn:hover {
  background: rgba(255, 255, 255, 0.3);
}

/* 主体区域 */
.admin-body {
  flex: 1;
  display: flex;
  overflow: hidden;
}

/* 左侧导航菜单 */
.admin-sidebar {
  width: 240px;
  background-color: #1f2937;
  border-right: 1px solid #374151;
  height: calc(100vh - 64px);
  overflow-y: auto;
}

.admin-sidebar::-webkit-scrollbar {
  width: 6px;
}

.admin-sidebar::-webkit-scrollbar-track {
  background: #1f2937;
}

.admin-sidebar::-webkit-scrollbar-thumb {
  background: #374151;
  border-radius: 3px;
}

.admin-menu {
  border-right: none;
  height: 100%;
}

.menu-item {
  margin: 4px 12px;
  border-radius: 8px;
  transition: all 0.3s;
}

.menu-item:hover {
  background-color: #374151;
}

.menu-item.is-active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.menu-icon {
  margin-right: 10px;
  font-size: 18px;
}

/* 右侧内容区域 */
.admin-content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
  background-color: #f3f4f6;
}

.admin-content::-webkit-scrollbar {
  width: 8px;
}

.admin-content::-webkit-scrollbar-track {
  background: #f3f4f6;
}

.admin-content::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 4px;
}

/* 身份标签样式 */
.user-role-badge {
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
  color: white;
}

.role-1 {
  background-color: #ef4444;
}
</style>
