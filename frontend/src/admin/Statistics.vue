<template>
  <div class="statistics-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-left">
        <span class="page-icon">📊</span>
        <div class="page-info">
          <h1 class="page-title">平台营收统计</h1>
          <p class="page-desc">查看平台交易数据和营收概况</p>
        </div>
      </div>
      <div class="header-right">
        <el-button type="primary" @click="loadStatistics">
          🔄 刷新数据
        </el-button>
      </div>
    </div>

    <!-- 核心数据卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :xs="12" :sm="6">
        <div class="stat-card revenue-card">
          <div class="stat-icon-wrapper">
            <span class="stat-icon">💰</span>
          </div>
          <div class="stat-content">
            <div class="stat-label">总成交额</div>
            <div class="stat-value">¥{{ statistics.totalRevenue.toLocaleString() }}</div>
          </div>
          <div class="stat-trend up">
            <el-icon><TrendCharts /></el-icon>
            <span>+12.5%</span>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card orders-card">
          <div class="stat-icon-wrapper">
            <span class="stat-icon">📦</span>
          </div>
          <div class="stat-content">
            <div class="stat-label">总成交次数</div>
            <div class="stat-value">{{ statistics.totalOrders }}</div>
          </div>
          <div class="stat-trend up">
            <el-icon><TrendCharts /></el-icon>
            <span>+8.3%</span>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card refunds-card">
          <div class="stat-icon-wrapper">
            <span class="stat-icon">🔄</span>
          </div>
          <div class="stat-content">
            <div class="stat-label">退货申请数</div>
            <div class="stat-value">{{ statistics.totalRefunds }}</div>
          </div>
          <div class="stat-trend down">
            <el-icon><TrendCharts /></el-icon>
            <span>-3.2%</span>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card rate-card">
          <div class="stat-icon-wrapper">
            <span class="stat-icon">📈</span>
          </div>
          <div class="stat-content">
            <div class="stat-label">退货率</div>
            <div class="stat-value">{{ statistics.refundRate.toFixed(1) }}%</div>
          </div>
          <div class="stat-trend down">
            <el-icon><TrendCharts /></el-icon>
            <span>-0.5%</span>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 第二行数据卡片 -->
    <el-row :gutter="20" class="stats-row second-row">
      <el-col :xs="12" :sm="6">
        <div class="stat-card users-card">
          <div class="stat-icon-wrapper">
            <span class="stat-icon">👥</span>
          </div>
          <div class="stat-content">
            <div class="stat-label">用户总数</div>
            <div class="stat-value">{{ statistics.totalUsers }}</div>
          </div>
          <div class="stat-trend up">
            <el-icon><TrendCharts /></el-icon>
            <span>+15.2%</span>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card goods-card">
          <div class="stat-icon-wrapper">
            <span class="stat-icon">🏪</span>
          </div>
          <div class="stat-content">
            <div class="stat-label">商品总数</div>
            <div class="stat-value">{{ statistics.totalGoods }}</div>
          </div>
          <div class="stat-trend up">
            <el-icon><TrendCharts /></el-icon>
            <span>+6.8%</span>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 热门商品排行 -->
    <el-card class="popular-goods-card">
      <template #header>
        <div class="card-header">
          <div class="header-title">
            <span class="header-icon">🏆</span>
            <span>热门商品 TOP 10</span>
          </div>
        </div>
      </template>
      <el-table 
        :data="statistics.popularGoods" 
        style="width: 100%"
        stripe
      >
        <el-table-column label="排名" width="100" align="center">
          <template #default="scope">
            <span class="rank-badge" :class="'rank-' + (scope.$index + 1)">
              {{ scope.$index + 1 }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="商品图片" width="120">
          <template #default="scope">
            <el-image
              v-if="scope.row.image"
              :src="scope.row.image"
              fit="cover"
              class="goods-thumb"
              :preview-src-list="[scope.row.image]"
            />
            <div v-else class="goods-thumb-placeholder">
              📦
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="商品名称" min-width="200">
          <template #default="scope">
            <span class="goods-name">{{ scope.row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="sales" label="销量" width="120" align="center" sortable>
          <template #default="scope">
            <span class="sales-count">{{ scope.row.sales }}</span>
          </template>
        </el-table-column>
        <el-table-column label="价格" width="140" align="center">
          <template #default="scope">
            <span class="price-text">¥{{ scope.row.price?.toFixed(2) || '0.00' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'info'" size="small">
              {{ scope.row.status === 1 ? '已上架' : '已下架' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { TrendCharts } from '@element-plus/icons-vue';
import { api } from '../api';

interface PopularGoods {
  id: number;
  name: string;
  image: string;
  price: number;
  sales: number;
  status: number;
}

interface Statistics {
  totalRevenue: number;
  totalOrders: number;
  totalRefunds: number;
  refundRate: number;
  totalUsers: number;
  totalGoods: number;
  popularGoods: PopularGoods[];
}

const statistics = ref<Statistics>({
  totalRevenue: 0,
  totalOrders: 0,
  totalRefunds: 0,
  refundRate: 0,
  totalUsers: 0,
  totalGoods: 0,
  popularGoods: []
});

const loadStatistics = async () => {
  try {
    const res = await api.getStatisticsOverview();
    if (res.data.code === 0) {
      statistics.value = res.data.data;
    } else {
      ElMessage.error(res.data.msg || '加载统计数据失败');
    }
  } catch (err) {
    ElMessage.error('加载统计数据失败');
  }
};

onMounted(() => {
  loadStatistics();
});
</script>

<style scoped>
.statistics-page {
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

/* 统计卡片行 */
.stats-row {
  margin-bottom: 24px;
}

.stats-row.second-row {
  margin-bottom: 32px;
}

.stat-card {
  background: white;
  border-radius: 16px;
  padding: 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  transition: all 0.3s;
  margin-bottom: 20px;
  position: relative;
  overflow: hidden;
}

.stat-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 4px;
  height: 100%;
}

.revenue-card::before {
  background: linear-gradient(180deg, #10b981 0%, #059669 100%);
}

.orders-card::before {
  background: linear-gradient(180deg, #3b82f6 0%, #1d4ed8 100%);
}

.refunds-card::before {
  background: linear-gradient(180deg, #f59e0b 0%, #d97706 100%);
}

.rate-card::before {
  background: linear-gradient(180deg, #8b5cf6 0%, #7c3aed 100%);
}

.users-card::before {
  background: linear-gradient(180deg, #ec4899 0%, #be185d 100%);
}

.goods-card::before {
  background: linear-gradient(180deg, #06b6d4 0%, #0891b2 100%);
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12);
}

.stat-icon-wrapper {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.revenue-card .stat-icon-wrapper {
  background: linear-gradient(135deg, #d1fae5 0%, #a7f3d0 100%);
}

.orders-card .stat-icon-wrapper {
  background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%);
}

.refunds-card .stat-icon-wrapper {
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
}

.rate-card .stat-icon-wrapper {
  background: linear-gradient(135deg, #ede9fe 0%, #ddd6fe 100%);
}

.users-card .stat-icon-wrapper {
  background: linear-gradient(135deg, #fce7f3 0%, #fbcfe8 100%);
}

.goods-card .stat-icon-wrapper {
  background: linear-gradient(135deg, #cffafe 0%, #a5f3fc 100%);
}

.stat-icon {
  font-size: 32px;
}

.stat-content {
  flex: 1;
  margin-left: 16px;
}

.stat-label {
  font-size: 14px;
  color: #6b7280;
  margin-bottom: 6px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #1f2937;
}

.stat-trend {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
  font-weight: 600;
  padding: 6px 12px;
  border-radius: 8px;
}

.stat-trend.up {
  color: #10b981;
  background: #d1fae5;
}

.stat-trend.down {
  color: #ef4444;
  background: #fee2e2;
}

/* 热门商品卡片 */
.popular-goods-card {
  border-radius: 16px;
  border: none;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
}

.header-icon {
  font-size: 20px;
}

/* 排名徽章 */
.rank-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #f3f4f6;
  color: #6b7280;
  font-weight: 700;
  font-size: 16px;
}

.rank-1 {
  background: linear-gradient(135deg, #fbbf24 0%, #f59e0b 100%);
  color: white;
  box-shadow: 0 2px 8px rgba(251, 191, 36, 0.4);
}

.rank-2 {
  background: linear-gradient(135deg, #9ca3af 0%, #6b7280 100%);
  color: white;
  box-shadow: 0 2px 8px rgba(156, 163, 175, 0.4);
}

.rank-3 {
  background: linear-gradient(135deg, #fb923c 0%, #ea580c 100%);
  color: white;
  box-shadow: 0 2px 8px rgba(251, 146, 60, 0.4);
}

/* 商品缩略图 */
.goods-thumb {
  width: 64px;
  height: 64px;
  border-radius: 10px;
  border: 2px solid #f3f4f6;
}

.goods-thumb-placeholder {
  width: 64px;
  height: 64px;
  border-radius: 10px;
  background: #f9fafb;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
}

.goods-name {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
}

.sales-count {
  font-size: 16px;
  font-weight: 700;
  color: #3b82f6;
}

.price-text {
  font-size: 16px;
  font-weight: 700;
  color: #ef4444;
}
</style>
