<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
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

// 新增统计计算，填充--空白数字
const todayStr = new Date().toLocaleDateString();
// 今日新增待审核
const todayNewCount = computed(() => {
  return pendingGoodsList.value.filter(item => {
    const createDate = new Date(item.createTime).toLocaleDateString();
    return createDate === todayStr;
  }).length;
});
// 今日已审核（这里仅展示，如需后端接口替换逻辑）
const todayReviewedCount = computed(() => 0);
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
        <el-button size="small" @click="loadPendingGoodsList">🔄 刷新</el-button>
      </div>
    </div>

    <!-- 统计卡片 三列均分，数字自动计算 -->
    <div class="stats-grid" v-if="pendingGoodsList.length > 0">
      <div class="stat-box warning">
        <div class="stat-icon">⏳</div>
        <div class="stat-content">
          <div class="stat-number">{{ pendingGoodsList.length }}</div>
          <div class="stat-label">待审核商品</div>
        </div>
      </div>
      <div class="stat-box info">
        <div class="stat-icon">📊</div>
        <div class="stat-content">
          <div class="stat-number">{{ todayNewCount }}</div>
          <div class="stat-label">今日新增</div>
        </div>
      </div>
      <div class="stat-box success">
        <div class="stat-icon">✅</div>
        <div class="stat-content">
          <div class="stat-number">{{ todayReviewedCount }}</div>
          <div class="stat-label">今日已审核</div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else class="empty-state">
      <div class="empty-icon">🎉</div>
      <h3 class="empty-title">暂无待审核商品</h3>
      <p class="empty-desc">所有商品都已审核通过啦</p>
    </div>

    <!-- 商品表格 -->
    <div class="table-wrap" v-if="pendingGoodsList.length > 0">
      <el-table
          :data="pendingGoodsList"
          style="width: 100%"
          stripe
          size="small"
      >
        <el-table-column label="商品信息" min-width="260">
          <template #default="scope">
            <div class="goods-cell">
              <img v-if="scope.row.imageUrl" :src="scope.row.imageUrl" class="goods-thumb" />
              <div v-else class="goods-thumb-placeholder">🏷️</div>
              <div class="goods-details">
                <div class="goods-name">{{ scope.row.title }}</div>
                <div class="goods-id">ID: {{ scope.row.id }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="价格" width="100">
          <template #default="scope">
            <span class="price-text">¥{{ scope.row.price?.toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column label="卖家" width="120" align="center">
          <template #default="scope">
            <el-tag type="info" size="small">{{ getUserById(scope.row.sellerId) }}</el-tag>
          </template>
        </el-table-column>
        <!-- 操作列加宽适配长按钮 -->
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="scope">
            <div class="btn-group">
              <el-button size="mini" type="success" @click="approveGoods(scope.row.id)">通过</el-button>
              <el-button size="mini" type="danger" @click="rejectGoods(scope.row.id)">拒绝</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<style scoped>
* { margin: 0; padding: 0; box-sizing: border-box; }
.pending-goods {
  width: 100%;
  color: #333;
}

/* 页面头部 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #eee;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}
.page-icon { font-size: 24px; }
.page-title {
  font-size: 18px;
  font-weight: 600;
}
.page-desc {
  font-size: 12px;
  color: #666;
  margin-top: 2px;
}

/* 普通按钮以外全部0圆角 */
:deep(.el-button:not(.el-button--mini)) { border-radius: 0; }
/* mini操作按钮圆角 + 加宽左右内边距，按钮变长 */
:deep(.el-button--mini) {
  border-radius: 6px;
  padding: 2px 25px !important;
}

/* 操作按钮横向排列 */
.btn-group {
  display: flex;
  gap: 10px;
  justify-content: center;
}

/* 统计网格 三列均分，删除el-row栅格 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}
.stat-box {
  background: #fff;
  border: 1px solid #eee;
  border-radius: 0;
  padding: 40px 10px;
  display: flex;
  align-items: center;
  gap: 10px;
}
/* 顶部细彩色线条替代左侧粗边框 */
.stat-box.warning { border-top: 3px solid #f59e0b; }
.stat-box.info { border-top: 3px solid #3b82f6; }
.stat-box.success { border-top: 3px solid #10b981; }
.stat-icon { font-size: 24px; }
.stat-content { flex: 1; text-align: center; }
.stat-number { font-size: 20px; font-weight: 600; }
.stat-label { font-size: 12px; color: #666; margin-top: 3px; }

/* 空状态简约化 */
.empty-state {
  background: #fff;
  border: 1px solid #eee;
  padding: 40px 20px;
  text-align: center;
}
.empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
}
.empty-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 6px;
}
.empty-desc {
  font-size: 13px;
  color: #666;
}

/* 表格容器 */
.table-wrap {
  width: 100%;
  background: #fff;
  border: 1px solid #eee;
}
:deep(.el-table) {
  --el-table-row-hover-bg-color: #fafafa;
}
:deep(.el-table th),
:deep(.el-table td) {
  padding: 8px 10px;
}
:deep(.el-table__header-wrapper) {
  border-bottom: 1px solid #eee;
}

/* 商品单元格紧凑 */
.goods-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}
.goods-thumb,
.goods-thumb-placeholder {
  width: 48px;
  height: 48px;
  border-radius: 0;
  border: 1px solid #eee;
  flex-shrink: 0;
  object-fit: cover;
}
.goods-thumb-placeholder {
  background: #f8f8f8;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}
.goods-details {
  min-width: 0;
}
.goods-name {
  font-size: 14px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.goods-id {
  font-size: 11px;
  color: #888;
  margin-top: 2px;
}
.price-text {
  font-size: 14px;
  font-weight: 600;
  color: #e53e3e;
}

/* 标签统一直角 */
:deep(.el-tag) {
  border-radius: 0;
}
</style>