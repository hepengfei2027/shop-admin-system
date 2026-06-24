<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { api } from '../api';
import { ElMessage, ElButton, ElMessageBox, ElTag, ElDropdown, ElDropdownMenu, ElDropdownItem, ElInput } from 'element-plus';

const goodsList = ref<any[]>([]);
const userList = ref<any[]>([]);
const searchKeyword = ref('');

// 状态筛选
const statusFilter = ref<number | null>(null);
const statusOptions = [
  { value: null, label: '全部状态' },
  { value: 0, label: '审核中' },
  { value: 1, label: '已上架' },
  { value: 2, label: '已下架' },
  { value: 3, label: '已售出' }
];

// 筛选后的商品列表（倒序排列，最新在前）
const filteredGoodsList = computed(() => {
  let list = [...goodsList.value];
  
  // 搜索过滤
  if (searchKeyword.value.trim()) {
    const keyword = searchKeyword.value.trim().toLowerCase();
    list = list.filter(goods => {
      const title = String(goods.title || '').toLowerCase();
      const description = String(goods.description || '').toLowerCase();
      const goodsId = String(goods.id || '').toLowerCase();
      const sellerName = String(getUserById(goods.sellerId) || '').toLowerCase();
      return title.includes(keyword) || 
             description.includes(keyword) || 
             goodsId.includes(keyword) || 
             sellerName.includes(keyword);
    });
  }
  
  // 状态筛选
  if (statusFilter.value !== null) {
    list = list.filter(goods => goods.status === statusFilter.value);
  }
  
  return list.reverse();
});

// 获取当前筛选标签
const currentFilterLabel = computed(() => {
  const option = statusOptions.find(opt => opt.value === statusFilter.value);
  return option ? option.label : '全部状态';
});

// 计算属性：数据统计
const onSaleCount = computed(() => {
  return goodsList.value.filter(g => g.status === 1).length;
});

const thisMonthNewCount = computed(() => {
  const now = new Date();
  const currentMonth = now.getMonth();
  const currentYear = now.getFullYear();
  return goodsList.value.filter(g => {
    if (!g.createTime) return false;
    const createDate = new Date(g.createTime);
    return createDate.getMonth() === currentMonth && createDate.getFullYear() === currentYear;
  }).length;
});

const totalSalesAmount = ref('--');

onMounted(() => {
  loadGoodsList();
  loadUserList();
  loadSalesData();
});

const loadGoodsList = async () => {
  try {
    const res = await api.listAllGoods();
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

// 加载总销售额
const loadSalesData = async () => {
  try {
    const res = await api.getStatisticsOverview();
    if (res.data.code === 0) {
      totalSalesAmount.value = res.data.data?.totalRevenue ?? '--';
    }
  } catch (err) {
    // 销售数据获取失败，保持 '--'
  }
};

// 下架商品
const offShelfGoods = async (goods: any) => {
  try {
    await ElMessageBox.confirm('确定要下架这个商品吗？', '下架商品', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    });
    await api.updateGoodsStatus(goods.id, 0); // 0 表示下架
    ElMessage.success('商品已下架');
    loadGoodsList();
  } catch (err) {
    // 用户取消或请求失败
  }
};

// 上架商品
const onShelfGoods = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定要上架这个商品吗？', '上架商品', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info'
    });
    await api.updateGoodsStatus(id, 1); // 1 表示上架
    ElMessage.success('商品已上架');
    loadGoodsList();
  } catch (err) {
    // 用户取消或请求失败
  }
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
        <el-input
            v-model="searchKeyword"
            placeholder="搜索商品名称/描述/ID/卖家"
            size="small"
            class="search-input"
            @keyup.enter="loadGoodsList"
        >
          <template #prefix>
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16">
              <circle cx="11" cy="11" r="8"/>
              <path d="m21 21-4.35-4.35"/>
            </svg>
          </template>
        </el-input>
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
          <div class="stat-number">{{ onSaleCount }}</div>
          <div class="stat-label">在售商品</div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-box">
          <div class="stat-number">{{ thisMonthNewCount }}</div>
          <div class="stat-label">本月新增</div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-box">
          <div class="stat-number">¥{{ totalSalesAmount }}</div>
          <div class="stat-label">总销售额</div>
        </div>
      </el-col>
    </el-row>

    <!-- 商品列表 -->
    <el-card class="table-card">
      <el-table
        :data="filteredGoodsList"
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
        <el-table-column label="状态" width="130">
          <template #header>
            <el-dropdown trigger="click" @command="(val: number | null) => statusFilter = val">
              <span class="status-filter-header">
                状态
                <span class="filter-indicator" :class="{ active: statusFilter !== null }">
                  🔽
                </span>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item
                    v-for="opt in statusOptions"
                    :key="opt.label"
                    :command="opt.value"
                    :class="{ 'is-active': statusFilter === opt.value }"
                  >
                    {{ opt.label }}
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template #default="scope">
            <el-tag
              :type="scope.row.status === 1 ? 'success' : scope.row.status === 0 ? 'warning' : 'info'"
              size="small"
            >
              {{ scope.row.status === 0 ? '审核中' : scope.row.status === 1 ? '已上架' : scope.row.status === 2 ? '已下架' : '已售出' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="scope">
            <el-button
              v-if="scope.row.status === 1"
              type="warning"
              size="small"
              @click="offShelfGoods(scope.row)"
            >
              下架
            </el-button>
            <el-button
              v-else-if="scope.row.status === 2"
              type="success"
              size="small"
              @click="onShelfGoods(scope.row.id)"
            >
              上架
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

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}
.search-input {
  width: 240px;
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

/* 状态筛选表头 */
.status-filter-header {
  cursor: pointer;
  user-select: none;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.status-filter-header:hover {
  color: #409eff;
}

.filter-indicator {
  font-size: 10px;
  transition: transform 0.2s;
}

.filter-indicator.active {
  color: #409eff;
}

:deep(.el-dropdown-menu__item.is-active) {
  color: #409eff;
  font-weight: 600;
}
</style>
