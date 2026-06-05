<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { api } from '../api';
import { ElMessage, ElButton, ElMessageBox, ElTag } from 'element-plus';

const goodsList = ref<any[]>([]);
const userList = ref<any[]>([]);

onMounted(() => {
  loadGoodsList();
  loadUserList();
});

const loadGoodsList = async () => {
  try {
    const res = await api.listGoods();
    if (res.data.code === 0) {
      goodsList.value = res.data.data || [];
    }
  } catch (err) {
    ElMessage.error('加载商品列表失败');
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

// 编辑商品
const editGoods = (goods: any) => {
  ElMessage.info('编辑功能待实现');
};

// 删除商品
const deleteGoods = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定要删除这个商品吗？', '删除商品', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    });
    await api.deleteGoods(id);
    ElMessage.success('商品已删除');
    loadGoodsList();
  } catch (err) {
    // 用户取消删除
  }
};

// 根据用户ID获取用户名
const getUserById = (userId: number) => {
  const user = userList.value.find(u => u.id === userId);
  return user ? user.username : '未知用户';
};
</script>

<template>
  <div class="goods-list">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-left">
        <span class="page-icon">🏪</span>
        <div class="page-info">
          <h1 class="page-title">商品管理</h1>
          <p class="page-desc">管理平台所有上架商品</p>
        </div>
      </div>
      <div class="header-right">
        <el-button type="primary" @click="loadGoodsList">
          🔄 刷新列表
        </el-button>
      </div>
    </div>

    <!-- 数据统计 -->
    <el-row :gutter="20" class="quick-stats">
      <el-col :xs="12" :sm="6">
        <div class="stat-box">
          <div class="stat-number">{{ goodsList.length }}</div>
          <div class="stat-label">商品总数</div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-box">
          <div class="stat-number">--</div>
          <div class="stat-label">在售商品</div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-box">
          <div class="stat-number">--</div>
          <div class="stat-label">本月新增</div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-box">
          <div class="stat-number">--</div>
          <div class="stat-label">总销售额</div>
        </div>
      </el-col>
    </el-row>

    <!-- 商品列表 -->
    <el-card class="table-card">
      <el-table 
        :data="goodsList" 
        style="width: 100%" 
        stripe
        v-loading="false"
      >
        <el-table-column label="商品信息" width="320">
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
            <div class="seller-cell">
              <el-tag type="info" size="small">{{ getUserById(scope.row.sellerId) }}</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="scope">
            <el-tag type="success" size="small">已上架</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="scope">
            <el-button type="primary" size="small" @click="editGoods(scope.row)">
              编辑
            </el-button>
            <el-button type="danger" size="small" @click="deleteGoods(scope.row.id)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.goods-list {
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

/* 快速统计 */
.quick-stats {
  margin-bottom: 24px;
}

.stat-box {
  background: white;
  border-radius: 12px;
  padding: 20px;
  text-align: center;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  margin-bottom: 20px;
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
</style>
