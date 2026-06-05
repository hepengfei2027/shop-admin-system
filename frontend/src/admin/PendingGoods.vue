<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { api } from '../api';
import { ElMessage, ElButton, ElTag } from 'element-plus';

const pendingGoodsList = ref<any[]>([]);
const userList = ref<any[]>([]);

onMounted(() => {
  loadPendingGoodsList();
  loadUserList();
});

const loadPendingGoodsList = async () => {
  try {
    const res = await api.listPendingGoods();
    if (res.data.code === 0) {
      pendingGoodsList.value = res.data.data || [];
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
    }
  } catch (err) {
    ElMessage.error('加载用户列表失败');
  }
};

const approveGoods = async (id: number) => {
  try {
    await api.approveGoods(id);
    ElMessage.success('审核通过');
    loadPendingGoodsList();
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

// 根据用户ID获取用户名
const getUserById = (userId: number) => {
  const user = userList.value.find(u => u.id === userId);
  return user ? user.username : '未知用户';
};
</script>

<template>
  <div class="pending-goods">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-left">
        <span class="page-icon">📋</span>
        <div class="page-info">
          <h1 class="page-title">待审核商品</h1>
          <p class="page-desc">审核商家新发布的商品</p>
        </div>
      </div>
      <div class="header-right">
        <div class="pending-count">
          <span class="count-number">{{ pendingGoodsList.length }}</span>
          <span class="count-label">件待审核</span>
        </div>
        <el-button type="primary" @click="loadPendingGoodsList">
          🔄 刷新
        </el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="quick-stats" v-if="pendingGoodsList.length > 0">
      <el-col :xs="12" :sm="8">
        <div class="stat-box warning">
          <div class="stat-icon">⏳</div>
          <div class="stat-content">
            <div class="stat-number">{{ pendingGoodsList.length }}</div>
            <div class="stat-label">待审核商品</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="8">
        <div class="stat-box info">
          <div class="stat-icon">📊</div>
          <div class="stat-content">
            <div class="stat-number">--</div>
            <div class="stat-label">今日新增</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="8">
        <div class="stat-box success">
          <div class="stat-icon">✅</div>
          <div class="stat-content">
            <div class="stat-number">--</div>
            <div class="stat-label">今日已审核</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 空状态 -->
    <div v-else class="empty-state">
      <div class="empty-icon">🎉</div>
      <h3 class="empty-title">暂无待审核商品</h3>
      <p class="empty-desc">所有商品都已审核通过啦</p>
    </div>

    <!-- 商品列表 -->
    <el-card class="table-card" v-if="pendingGoodsList.length > 0">
      <el-table 
        :data="pendingGoodsList" 
        style="width: 100%" 
        stripe
      >
        <el-table-column label="商品信息" width="340">
          <template #default="scope">
            <div class="goods-cell">
              <img v-if="scope.row.imageUrl" :src="scope.row.imageUrl" class="goods-thumb" />
              <div v-else class="goods-thumb-placeholder">
                <span>🏷️</span>
              </div>
              <div class="goods-details">
                <div class="goods-name">{{ scope.row.title }}</div>
                <div class="goods-id">ID: {{ scope.row.id }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="价格" width="120">
          <template #default="scope">
            <span class="price-text">¥{{ scope.row.price?.toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column label="卖家" width="140">
          <template #default="scope">
            <el-tag type="info" size="small">{{ getUserById(scope.row.sellerId) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="scope">
            <el-button 
              type="success" 
              size="small" 
              @click="approveGoods(scope.row.id)"
              class="action-btn"
            >
              ✅ 审核通过
            </el-button>
            <el-button 
              type="danger" 
              size="small" 
              @click="rejectGoods(scope.row.id)"
              class="action-btn"
            >
              ❌ 拒绝
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.pending-goods {
  padding: 0;
}

/* 页面标题 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.page-icon {
  font-size: 40px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: #1f2937;
  margin: 0 0 4px 0;
}

.page-desc {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.pending-count {
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
  padding: 12px 20px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.count-number {
  font-size: 24px;
  font-weight: 700;
  color: white;
}

.count-label {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.9);
}

/* 快速统计 */
.quick-stats {
  margin-bottom: 24px;
}

.stat-box {
  background: white;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  margin-bottom: 20px;
}

.stat-box.warning {
  border-left: 4px solid #f59e0b;
}

.stat-box.info {
  border-left: 4px solid #3b82f6;
}

.stat-box.success {
  border-left: 4px solid #10b981;
}

.stat-icon {
  font-size: 36px;
}

.stat-content {
  flex: 1;
}

.stat-number {
  font-size: 28px;
  font-weight: 700;
  color: #1f2937;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 13px;
  color: #6b7280;
}

/* 空状态 */
.empty-state {
  background: white;
  border-radius: 16px;
  padding: 80px 20px;
  text-align: center;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.empty-title {
  font-size: 20px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 8px 0;
}

.empty-desc {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
}

/* 表格卡片 */
.table-card {
  border-radius: 16px;
  border: none;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
}

/* 商品单元格 */
.goods-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.goods-thumb {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  object-fit: cover;
  border: 2px solid #f3f4f6;
}

.goods-thumb-placeholder {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  background: #f9fafb;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

.goods-details {
  flex: 1;
  min-width: 0;
}

.goods-name {
  font-size: 14px;
  font-weight: 500;
  color: #1f2937;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.goods-id {
  font-size: 12px;
  color: #9ca3af;
}

.price-text {
  font-size: 16px;
  font-weight: 700;
  color: #ef4444;
}

.action-btn {
  font-weight: 500;
}
</style>
