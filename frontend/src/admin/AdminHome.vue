<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { api } from '../api';
import { ElMessage, ElMessageBox, ElDialog, ElInput, ElSelect, ElOption, ElButton } from 'element-plus';
import { ArrowLeft, User, ShoppingBag, TrendingUp, AlertCircle } from '@element-plus/icons-vue';

const user = ref<any>(null);
const goodsList = ref<any[]>([]);
const pendingGoodsList = ref<any[]>([]);
const userList = ref<any[]>([]);
const statistics = ref({
  totalUsers: 0,
  totalGoods: 0,
  pendingCount: 0,
  totalRevenue: 0
});

// 编辑用户相关
const dialogVisible = ref(false);
const currentUser = ref<any>(null);
const editForm = ref({
  nickname: '',
  role: 0
});

onMounted(() => {
  const userStr = localStorage.getItem('user');
  if (userStr) {
    user.value = JSON.parse(userStr);
    if (user.value.role !== 1) {
      ElMessage.error('没有管理员权限');
      window.location.href = '/';
    } else {
      loadGoodsList();
      loadPendingGoodsList();
      loadUserList();
      loadStatistics();
    }
  } else {
    ElMessage.error('请先登录');
    window.location.href = '/login';
  }
});

const loadStatistics = () => {
  // 模拟统计数据
  statistics.value = {
    totalUsers: userList.value.length,
    totalGoods: goodsList.value.length,
    pendingCount: pendingGoodsList.value.length,
    totalRevenue: Math.floor(Math.random() * 100000) + 10000
  };
};

const loadGoodsList = async () => {
  try {
    const res = await api.listGoods();
    if (res.data.code === 0) {
      goodsList.value = res.data.data || [];
      loadStatistics();
    }
  } catch (err) {
    ElMessage.error('加载商品列表失败');
  }
};

const loadPendingGoodsList = async () => {
  try {
    const res = await api.listPendingGoods();
    if (res.data.code === 0) {
      pendingGoodsList.value = res.data.data || [];
      loadStatistics();
    }
  } catch (err) {
    ElMessage.error('加载待审核商品列表失败');
  }
};

const loadUserList = async () => {
  try {
    const res = await api.listUsers();
    if (res.data.code === 0) {
      userList.value = res.data.data || [];
      loadStatistics();
    }
  } catch (err) {
    ElMessage.error('加载用户列表失败');
  }
};

// 封禁用户
const banUser = async (user: any) => {
  try {
    const { value: banHours } = await ElMessageBox.prompt('请输入封禁时长（小时）', '封禁用户', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPattern: /^\d+$/,
      inputErrorMessage: '请输入有效的数字'
    });
    
    await api.updateUserStatus(user.id, 1, parseInt(banHours));
    ElMessage.success('用户已封禁');
    loadUserList();
  } catch (err) {
    // 用户取消操作
  }
};

// 解除封禁
const unbanUser = async (user: any) => {
  try {
    await api.updateUserStatus(user.id, 0);
    ElMessage.success('已解除封禁');
    loadUserList();
  } catch (err) {
    ElMessage.error('操作失败');
  }
};

const approveGoods = async (id: number) => {
  try {
    await api.approveGoods(id);
    ElMessage.success('审核通过');
    loadPendingGoodsList();
    loadGoodsList();
  } catch (err) {
    ElMessage.error('审核失败');
  }
};

const rejectGoods = async (id: number) => {
  try {
    await api.rejectGoods(id);
    ElMessage.success('已拒绝');
    loadPendingGoodsList();
  } catch (err) {
    ElMessage.error('操作失败');
  }
};

// 编辑用户
const editUser = (user: any) => {
  currentUser.value = user;
  editForm.value = {
    nickname: user.nickname || '',
    role: user.role
  };
  dialogVisible.value = true;
};

// 保存用户信息
const saveUser = async () => {
  if (!editForm.value.nickname.trim()) {
    ElMessage.warning('请输入昵称');
    return;
  }
  try {
    await api.updateUserInfo(currentUser.value.id, editForm.value.nickname, editForm.value.role);
    ElMessage.success('用户信息已更新');
    dialogVisible.value = false;
    loadUserList();
  } catch (err) {
    ElMessage.error('更新失败');
  }
};

// 根据用户ID获取用户名
const getUserById = (userId: number) => {
  const user = userList.value.find(u => u.id === userId);
  return user ? user.username : '未知用户';
};

// 格式化封禁截止时间
const formatBannedUntil = (bannedUntil: string) => {
  const now = new Date();
  const bannedTime = new Date(bannedUntil);
  const diff = bannedTime.getTime() - now.getTime();
  const hours = Math.ceil(diff / (1000 * 60 * 60));
  return `剩余${hours}小时`;
};
</script>

<template>
  <div class="admin-home">
    <!-- 统计卡片区域 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :xs="12" :sm="6">
        <div class="stat-card blue">
          <div class="stat-icon-wrapper">
            <el-icon :size="32" class="stat-icon"><User /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ statistics.totalUsers }}</div>
            <div class="stat-label">用户总数</div>
          </div>
          <div class="stat-trend">
            <el-icon><TrendingUp /></el-icon>
            <span>+12%</span>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card green">
          <div class="stat-icon-wrapper">
            <el-icon :size="32" class="stat-icon"><ShoppingBag /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ statistics.totalGoods }}</div>
            <div class="stat-label">商品总数</div>
          </div>
          <div class="stat-trend">
            <el-icon><TrendingUp /></el-icon>
            <span>+8%</span>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card orange">
          <div class="stat-icon-wrapper">
            <el-icon :size="32" class="stat-icon"><AlertCircle /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ statistics.pendingCount }}</div>
            <div class="stat-label">待审核商品</div>
          </div>
          <div class="stat-trend">
            <span v-if="statistics.pendingCount > 0" style="color: #f59e0b">待处理</span>
            <span v-else style="color: #10b981">全部完成</span>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card purple">
          <div class="stat-icon-wrapper">
            <el-icon :size="32" class="stat-icon">💰</el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">¥{{ statistics.totalRevenue.toLocaleString() }}</div>
            <div class="stat-label">平台营收</div>
          </div>
          <div class="stat-trend">
            <el-icon><TrendingUp /></el-icon>
            <span>+25%</span>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 待审核商品卡片 -->
    <el-card class="section-card" v-if="pendingGoodsList.length > 0">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <span class="header-icon">📋</span>
            <span class="header-title">待审核商品</span>
            <el-tag type="warning" size="small">{{ pendingGoodsList.length }}件待处理</el-tag>
          </div>
          <el-button type="primary" size="small" @click="$router.push('/admin/pending')">
            查看全部
          </el-button>
        </div>
      </template>
      <div class="goods-preview">
        <div v-for="goods in pendingGoodsList.slice(0, 4)" :key="goods.id" class="goods-item">
          <img v-if="goods.imageUrl" :src="goods.imageUrl" class="goods-img" />
          <div v-else class="goods-img-placeholder">无图</div>
          <div class="goods-info">
            <div class="goods-title">{{ goods.title }}</div>
            <div class="goods-price">¥{{ goods.price?.toFixed(2) }}</div>
            <div class="goods-seller">卖家：{{ getUserById(goods.sellerId) }}</div>
          </div>
          <div class="goods-actions">
            <el-button type="success" size="small" @click="approveGoods(goods.id)">通过</el-button>
            <el-button type="danger" size="small" @click="rejectGoods(goods.id)">拒绝</el-button>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 快速操作区域 -->
    <el-row :gutter="20" class="quick-actions">
      <el-col :xs="12" :sm="6">
        <div class="action-card" @click="$router.push('/admin/pending')">
          <div class="action-icon">📋</div>
          <div class="action-title">商品审核</div>
          <div class="action-desc">审核新发布的商品</div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="action-card" @click="$router.push('/admin/users')">
          <div class="action-icon">👥</div>
          <div class="action-title">用户管理</div>
          <div class="action-desc">管理平台用户</div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="action-card" @click="$router.push('/admin/orders')">
          <div class="action-icon">📦</div>
          <div class="action-title">订单管理</div>
          <div class="action-desc">查看订单详情</div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="action-card" @click="$router.push('/admin/statistics')">
          <div class="action-icon">📊</div>
          <div class="action-title">数据统计</div>
          <div class="action-desc">查看营收数据</div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.admin-home {
  padding: 0;
}

/* 统计卡片 */
.stats-row {
  margin-bottom: 24px;
}

.stat-card {
  background: white;
  border-radius: 16px;
  padding: 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  transition: all 0.3s;
  margin-bottom: 20px;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12);
}

.stat-card.blue {
  border-left: 4px solid #3b82f6;
}

.stat-card.green {
  border-left: 4px solid #10b981;
}

.stat-card.orange {
  border-left: 4px solid #f59e0b;
}

.stat-card.purple {
  border-left: 4px solid #8b5cf6;
}

.stat-icon-wrapper {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-card.blue .stat-icon-wrapper {
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
}

.stat-card.green .stat-icon-wrapper {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
}

.stat-card.orange .stat-icon-wrapper {
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
}

.stat-card.purple .stat-icon-wrapper {
  background: linear-gradient(135deg, #8b5cf6 0%, #7c3aed 100%);
}

.stat-icon {
  color: white;
}

.stat-info {
  flex: 1;
  margin-left: 20px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #1f2937;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 14px;
  color: #6b7280;
}

.stat-trend {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
  color: #10b981;
}

/* 区域卡片 */
.section-card {
  margin-bottom: 24px;
  border-radius: 16px;
  border: none;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-icon {
  font-size: 20px;
}

.header-title {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
}

/* 商品预览 */
.goods-preview {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.goods-item {
  display: flex;
  gap: 12px;
  padding: 12px;
  background: #f9fafb;
  border-radius: 12px;
  transition: all 0.3s;
}

.goods-item:hover {
  background: #f3f4f6;
}

.goods-img {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  object-fit: cover;
}

.goods-img-placeholder {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  background: #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #9ca3af;
  font-size: 14px;
}

.goods-info {
  flex: 1;
  min-width: 0;
}

.goods-title {
  font-size: 14px;
  font-weight: 500;
  color: #1f2937;
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.goods-price {
  font-size: 16px;
  font-weight: 700;
  color: #ef4444;
  margin-bottom: 4px;
}

.goods-seller {
  font-size: 12px;
  color: #6b7280;
}

.goods-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

/* 快速操作 */
.quick-actions {
  margin-bottom: 24px;
}

.action-card {
  background: white;
  border-radius: 16px;
  padding: 24px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  margin-bottom: 20px;
}

.action-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12);
}

.action-icon {
  font-size: 40px;
  margin-bottom: 12px;
}

.action-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 4px;
}

.action-desc {
  font-size: 13px;
  color: #6b7280;
}
</style>
