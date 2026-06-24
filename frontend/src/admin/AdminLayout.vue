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
  { path: '/admin/home', label: '数据概览', icon: '' },
  { path: '/admin/pending', label: '待审核商品', icon: '' },
  { path: '/admin/goods', label: '商品管理', icon: '' },
  { path: '/admin/users', label: '用户管理', icon: '' },
  { path: '/admin/orders', label: '订单管理', icon: '' }
];

const activePath = computed(() => route.path);
</script>

<template>
  <div class="admin-layout">
    <!-- 顶部导航栏 -->
    <div class="admin-header">
      <div class="header-left">
        <div class="logo">
          <span class="logo-icon"></span>
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
            background-color="#222222"
            text-color="#cccccc"
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
/* 根容器铺满全屏 */
.admin-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

/* 顶部栏固定吸附顶部，滚动永远可见 */
.admin-header {
  background: #333333;
  color: white;
  padding: 0 32px;
  height: 64px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: none;
  z-index: 100;
  position: sticky;
  top: 0;
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
  background: #444444;
  padding: 8px 16px;
  border-radius: 0;
  backdrop-filter: none;
}

.user-avatar {
  border: 2px solid #666666;
  border-radius: 0;
}

.user-name {
  font-size: 14px;
  font-weight: 500;
}

.logout-btn {
  background: #555555;
  border: none;
  color: white;
  padding: 8px 20px;
  border-radius: 0;
  font-weight: 500;
  transition: background 0.2s;
}

.logout-btn:hover {
  background: #777777;
}

/* 主体使用grid布局，左右两栏高度自动等高，侧边深色背景自动铺满底部，无断裂 */
.admin-body {
  flex: 1;
  display: grid;
  grid-template-columns: 240px 1fr;
  min-height: calc(100vh - 64px);
}

/* 左侧侧边栏 移除fixed，等高拉伸，内部滚动 */
.admin-sidebar {
  background-color: #222222;
  border-right: 1px solid #444444;
  overflow-y: auto;
}

.admin-sidebar::-webkit-scrollbar {
  width: 6px;
}

.admin-sidebar::-webkit-scrollbar-track {
  background: #222222;
}

.admin-sidebar::-webkit-scrollbar-thumb {
  background: #444444;
  border-radius: 0;
}

.admin-menu {
  border-right: none;
  min-height: 100%;
}

.menu-item {
  margin: 4px 0;
  border-radius: 0;
  transition: background 0.2s;
}

.menu-item:hover {
  background-color: #333333;
}

.menu-item.is-active {
  background: #555555;
}

.menu-icon {
  margin-right: 10px;
  font-size: 18px;
}

/* 右侧内容区域独立滚动 */
.admin-content {
  padding: 24px;
  overflow-y: auto;
  background-color: #ffffff;
}

.admin-content::-webkit-scrollbar {
  width: 8px;
}

.admin-content::-webkit-scrollbar-track {
  background: #ffffff;
}

.admin-content::-webkit-scrollbar-thumb {
  background: #dddddd;
  border-radius: 0;
}

/* 身份标签样式 - 直角矩形 */
.user-role-badge {
  padding: 4px 10px;
  border-radius: 0;
  font-size: 12px;
  font-weight: 600;
  color: white;
}

.role-1 {
  background-color: #dd3333;
}
</style>