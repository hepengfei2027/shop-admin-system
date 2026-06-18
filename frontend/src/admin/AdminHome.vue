<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { api } from '../api';
import { ElMessage } from 'element-plus';

const loading = ref(true);

// 全平台统计数据
const platformStats = ref({
  totalUsers: 0,
  totalGoods: 0,
  totalOrders: 0,
  totalRevenue: 0,
  pendingOrders: 0,
  completedOrders: 0,
  bannedUsers: 0,
  activeSellers: 0
});

// 销售趋势数据
const salesTrend = ref<{ date: string; amount: number; orders: number }[]>([]);

// 商品销售排行（平台TOP）
const topGoods = ref<any[]>([]);

// 用户分析
const userAnalysis = ref({
  totalBuyers: 0,
  totalSellers: 0,
  newUsersToday: 0,
  activeUsers: 0,
  topSpendingUsers: [] as any[],
  topActiveUsers: [] as any[]
});

// 订单分析
const orderAnalysis = ref({
  avgOrderValue: 0,
  refundRate: 0,
  completionRate: 0
});

// 加载全平台数据
const loadPlatformData = async () => {
  loading.value = true;
  try {
    // 获取统计数据概览
    const statsRes = await api.getStatisticsOverview();
    if (statsRes.data.code === 0) {
      const data = statsRes.data.data;
      platformStats.value = {
        totalUsers: data.totalUsers || 0,
        totalGoods: data.totalGoods || 0,
        totalOrders: data.totalOrders || 0,
        totalRevenue: data.totalRevenue || 0,
        pendingOrders: data.pendingOrders || 0,
        completedOrders: data.completedOrders || 0,
        bannedUsers: data.bannedUsers || 0,
        activeSellers: data.activeSellers || 0
      };
    }

    // 获取销售趋势
    const trendRes = await api.getPlatformTrend();
    if (trendRes.data.code === 0) {
      salesTrend.value = trendRes.data.data || [];
    }

    // 获取热门商品
    const goodsRes = await api.getTopGoods();
    if (goodsRes.data.code === 0) {
      topGoods.value = goodsRes.data.data || [];
    }

    // 获取用户分析
    const userRes = await api.getUserAnalysis();
    if (userRes.data.code === 0) {
      userAnalysis.value = userRes.data.data || {
        totalBuyers: 0,
        totalSellers: 0,
        newUsersToday: 0,
        activeUsers: 0,
        topSpendingUsers: [],
        topActiveUsers: []
      };
    }

    // 获取订单分析
    const orderRes = await api.getOrderAnalysis();
    if (orderRes.data.code === 0) {
      orderAnalysis.value = orderRes.data.data || {
        avgOrderValue: 0,
        refundRate: 0,
        completionRate: 0
      };
    }
  } catch (err) {
    console.error('加载平台数据失败', err);
    // 使用模拟数据
    initMockData();
  }
  loading.value = false;
};

// 初始化模拟数据（用于演示）
const initMockData = () => {
  platformStats.value = {
    totalUsers: 15234,
    totalGoods: 8934,
    totalOrders: 24567,
    totalRevenue: 1893456.78,
    pendingOrders: 156,
    completedOrders: 23456,
    bannedUsers: 23,
    activeSellers: 456
  };

  // 生成近7天趋势
  const today = new Date();
  salesTrend.value = [];
  for (let i = 6; i >= 0; i--) {
    const date = new Date(today);
    date.setDate(date.getDate() - i);
    salesTrend.value.push({
      date: `${date.getMonth() + 1}/${date.getDate()}`,
      amount: Math.floor(Math.random() * 50000) + 30000,
      orders: Math.floor(Math.random() * 200) + 100
    });
  }

  topGoods.value = [
    { goodsId: 1, title: 'iPhone 15 Pro Max', salesCount: 1234, revenue: 1234567 },
    { goodsId: 2, title: 'MacBook Pro 14寸', salesCount: 876, revenue: 876543 },
    { goodsId: 3, title: 'AirPods Pro 2', salesCount: 2345, revenue: 456789 },
    { goodsId: 4, title: 'iPad Air 5', salesCount: 654, revenue: 321456 },
    { goodsId: 5, title: 'Apple Watch S9', salesCount: 543, revenue: 234567 }
  ];

  userAnalysis.value = {
    totalBuyers: 12345,
    totalSellers: 2345,
    newUsersToday: 56,
    activeUsers: 8765,
    topSpendingUsers: [
      { id: 1, username: 'vip_user001', totalAmount: 156789, orderCount: 45 },
      { id: 2, username: 'shopping_king', totalAmount: 98765, orderCount: 67 },
      { id: 3, username: 'luxury_fan', totalAmount: 87654, orderCount: 34 }
    ],
    topActiveUsers: [
      { id: 4, username: 'daily_shopper', orderCount: 123, totalAmount: 45678 },
      { id: 5, username: 'frequent_buyer', orderCount: 98, totalAmount: 34567 },
      { id: 6, username: 'weekly_deal', orderCount: 87, totalAmount: 23456 }
    ]
  };

  orderAnalysis.value = {
    avgOrderValue: 77.08,
    refundRate: 3.2,
    completionRate: 95.6
  };
};

// 格式化金额
const formatMoney = (amount: number) => {
  if (amount >= 10000) {
    return '￥' + (amount / 10000).toFixed(1) + '万';
  }
  return '￥' + (amount || 0).toFixed(2);
};

// 格式化数字
const formatNumber = (num: number) => {
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + '万';
  }
  return num.toLocaleString();
};

// 格式化百分比
const formatPercent = (value: number) => {
  return (value || 0).toFixed(1) + '%';
};

onMounted(() => {
  loadPlatformData();
});
</script>

<template>
  <div class="analytics-page">
    <h2>全平台数据概览</h2>
    
    <!-- 核心指标卡片 -->
    <div class="metrics-grid">
      <div class="metric-card">
        <div class="metric-icon users">👥</div>
        <div class="metric-content">
          <div class="metric-value">{{ formatNumber(platformStats.totalUsers) }}</div>
          <div class="metric-label">注册用户</div>
        </div>
      </div>
      
      <div class="metric-card">
        <div class="metric-icon goods">📦</div>
        <div class="metric-content">
          <div class="metric-value">{{ formatNumber(platformStats.totalGoods) }}</div>
          <div class="metric-label">上架商品</div>
        </div>
      </div>
      
      <div class="metric-card">
        <div class="metric-icon orders">🛒</div>
        <div class="metric-content">
          <div class="metric-value">{{ formatNumber(platformStats.totalOrders) }}</div>
          <div class="metric-label">总订单数</div>
        </div>
      </div>
      
      <div class="metric-card">
        <div class="metric-icon revenue">💰</div>
        <div class="metric-content">
          <div class="metric-value">{{ formatMoney(platformStats.totalRevenue) }}</div>
          <div class="metric-label">平台营收</div>
        </div>
      </div>
    </div>

    <!-- 销售趋势 -->
    <div class="section-card">
      <h3>平台销售趋势（近7天）</h3>
      <div class="trend-chart">
        <div class="chart-container">
          <div 
            v-for="(item, index) in salesTrend" 
            :key="index" 
            class="chart-bar-wrapper"
          >
            <div 
              class="chart-bar" 
              :style="{ height: (item.amount / Math.max(...salesTrend.map(s => s.amount), 1) * 150) + 'px' }"
            >
              <span class="bar-value">{{ formatMoney(item.amount) }}</span>
            </div>
            <div class="chart-label">{{ item.date }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 平台商品销售排行 -->
    <div class="section-card">
      <h3>平台商品销售排行 TOP10</h3>
      <div class="goods-table">
        <div class="table-header">
          <span class="col-rank">排名</span>
          <span class="col-name">商品名称</span>
          <span class="col-sales">销量</span>
          <span class="col-revenue">销售额</span>
        </div>
        <div v-if="topGoods.length === 0" class="empty-state">
          暂无销售数据
        </div>
        <div 
          v-for="(item, index) in topGoods.slice(0, 10)" 
          :key="item.goodsId" 
          class="table-row"
        >
          <span class="col-rank">
            <span :class="['rank-badge', `rank-${index + 1}`]">{{ index + 1 }}</span>
          </span>
          <span class="col-name">{{ item.title }}</span>
          <span class="col-sales">{{ item.salesCount }} 件</span>
          <span class="col-revenue">{{ formatMoney(item.revenue) }}</span>
        </div>
      </div>
    </div>

    <!-- 用户分析 -->
    <div class="section-card">
      <h3>用户分析</h3>
      <div class="user-stats-grid">
        <div class="user-stat-item">
          <div class="stat-icon buyers">🛍️</div>
          <div class="stat-info">
            <div class="stat-value">{{ formatNumber(userAnalysis.totalBuyers) }}</div>
            <div class="stat-label">买家总数</div>
          </div>
        </div>
        <div class="user-stat-item">
          <div class="stat-icon sellers">🏪</div>
          <div class="stat-info">
            <div class="stat-value">{{ formatNumber(userAnalysis.totalSellers) }}</div>
            <div class="stat-label">卖家总数</div>
          </div>
        </div>
        <div class="user-stat-item">
          <div class="stat-icon new">🆕</div>
          <div class="stat-info">
            <div class="stat-value">{{ formatNumber(userAnalysis.newUsersToday) }}</div>
            <div class="stat-label">今日新增</div>
          </div>
        </div>
        <div class="user-stat-item">
          <div class="stat-icon active">⚡</div>
          <div class="stat-info">
            <div class="stat-value">{{ formatNumber(userAnalysis.activeUsers) }}</div>
            <div class="stat-label">活跃用户</div>
          </div>
        </div>
      </div>
      
      <div class="user-lists">
        <div class="user-list-section">
          <h4>消费金额排行</h4>
          <div v-if="userAnalysis.topSpendingUsers.length === 0" class="empty-state">
            暂无数据
          </div>
          <div 
            v-for="(user, index) in userAnalysis.topSpendingUsers" 
            :key="user.id" 
            class="user-item"
          >
            <span class="user-rank">{{ index + 1 }}</span>
            <span class="user-avatar">{{ user.username.charAt(0).toUpperCase() }}</span>
            <span class="user-name">{{ user.username }}</span>
            <span class="user-orders">{{ user.orderCount }} 单</span>
            <span class="user-amount">{{ formatMoney(user.totalAmount) }}</span>
          </div>
        </div>
        
        <div class="user-list-section">
          <h4>活跃度排行</h4>
          <div v-if="userAnalysis.topActiveUsers.length === 0" class="empty-state">
            暂无数据
          </div>
          <div 
            v-for="(user, index) in userAnalysis.topActiveUsers" 
            :key="user.id" 
            class="user-item"
          >
            <span class="user-rank">{{ index + 1 }}</span>
            <span class="user-avatar">{{ user.username.charAt(0).toUpperCase() }}</span>
            <span class="user-name">{{ user.username }}</span>
            <span class="user-orders">{{ user.orderCount }} 单</span>
            <span class="user-amount">{{ formatMoney(user.totalAmount) }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 订单分析 -->
    <div class="section-card">
      <h3>订单分析</h3>
      <div class="order-stats-grid">
        <div class="order-stat-item">
          <div class="stat-value">{{ formatMoney(orderAnalysis.avgOrderValue) }}</div>
          <div class="stat-label">平均客单价</div>
        </div>
        <div class="order-stat-item">
          <div class="stat-value warning">{{ formatPercent(orderAnalysis.refundRate) }}</div>
          <div class="stat-label">退款率</div>
        </div>
        <div class="order-stat-item">
          <div class="stat-value success">{{ formatPercent(orderAnalysis.completionRate) }}</div>
          <div class="stat-label">完成率</div>
        </div>
        <div class="order-stat-item">
          <div class="stat-value">{{ platformStats.pendingOrders }}</div>
          <div class="stat-label">待处理订单</div>
        </div>
      </div>
    </div>

    <!-- 平台健康度 -->
    <div class="section-card suggestions">
      <h3>平台健康度</h3>
      <div class="suggestion-list">
        <div class="suggestion-item" v-if="platformStats.pendingOrders > 100">
          <span class="suggestion-icon">⚠️</span>
          <span>待处理订单较多 ({{ platformStats.pendingOrders }} 个)，建议关注</span>
        </div>
        <div class="suggestion-item" v-if="orderAnalysis.completionRate >= 90">
          <span class="suggestion-icon">✅</span>
          <span>订单完成率良好 ({{ formatPercent(orderAnalysis.completionRate) }})</span>
        </div>
        <div class="suggestion-item" v-if="orderAnalysis.refundRate < 5">
          <span class="suggestion-icon">👍</span>
          <span>退款率控制良好 ({{ formatPercent(orderAnalysis.refundRate) }})</span>
        </div>
        <div class="suggestion-item" v-if="userAnalysis.newUsersToday > 30">
          <span class="suggestion-icon">📈</span>
          <span>用户增长良好，今日新增 {{ userAnalysis.newUsersToday }} 人</span>
        </div>
        <div class="suggestion-item" v-if="platformStats.bannedUsers > 0">
          <span class="suggestion-icon">🔒</span>
          <span>当前封禁用户 {{ platformStats.bannedUsers }} 人</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.analytics-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

h2 {
  text-align: center;
  margin-bottom: 30px;
  color: #333;
}

/* 核心指标卡片 */
.metrics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
  margin-bottom: 30px;
}

.metric-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 15px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.metric-icon {
  width: 50px;
  height: 50px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

.metric-icon.users { background: linear-gradient(135deg, #667eea, #764ba2); }
.metric-icon.goods { background: linear-gradient(135deg, #f093fb, #f5576c); }
.metric-icon.orders { background: linear-gradient(135deg, #4facfe, #00f2fe); }
.metric-icon.revenue { background: linear-gradient(135deg, #43e97b, #38f9d7); }

.metric-value {
  font-size: 24px;
  font-weight: 700;
  color: #333;
}

.metric-label {
  font-size: 14px;
  color: #999;
  margin-top: 4px;
}

/* 区块卡片 */
.section-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.section-card h3 {
  margin: 0 0 20px 0;
  font-size: 18px;
  color: #333;
}

.section-card h4 {
  margin: 20px 0 12px 0;
  font-size: 15px;
  color: #666;
}

/* 销售趋势图表 */
.trend-chart {
  overflow-x: auto;
}

.chart-container {
  display: flex;
  align-items: flex-end;
  gap: 15px;
  height: 200px;
  padding: 20px 0;
}

.chart-bar-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 60px;
}

.chart-bar {
  width: 100%;
  max-width: 50px;
  background: linear-gradient(180deg, #667eea 0%, #764ba2 100%);
  border-radius: 6px 6px 0 0;
  display: flex;
  justify-content: center;
  padding-top: 8px;
  min-height: 20px;
  transition: height 0.3s ease;
}

.bar-value {
  font-size: 10px;
  color: white;
  font-weight: 600;
}

.chart-label {
  font-size: 12px;
  color: #999;
  margin-top: 8px;
}

/* 商品表格 */
.goods-table {
  width: 100%;
}

.table-header, .table-row {
  display: flex;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.table-header {
  font-weight: 600;
  color: #666;
  font-size: 14px;
}

.table-row:hover {
  background: #fafafa;
}

.col-rank { width: 60px; text-align: center; }
.col-name { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.col-sales { width: 100px; text-align: center; }
.col-revenue { width: 120px; text-align: right; }

.rank-badge {
  display: inline-block;
  width: 24px;
  height: 24px;
  line-height: 24px;
  text-align: center;
  border-radius: 50%;
  font-size: 12px;
  font-weight: 600;
  background: #f0f0f0;
  color: #666;
}

.rank-badge.rank-1 { background: linear-gradient(135deg, #ffd700, #ffec8b); color: #8b6914; }
.rank-badge.rank-2 { background: linear-gradient(135deg, #c0c0c0, #e8e8e8); color: #666; }
.rank-badge.rank-3 { background: linear-gradient(135deg, #cd7f32, #daa06d); color: #5c3d1e; }

/* 用户分析 */
.user-stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.user-stat-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: #f9fafb;
  border-radius: 8px;
}

.stat-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.stat-icon.buyers { background: linear-gradient(135deg, #f093fb, #f5576c); }
.stat-icon.sellers { background: linear-gradient(135deg, #4facfe, #00f2fe); }
.stat-icon.new { background: linear-gradient(135deg, #43e97b, #38f9d7); }
.stat-icon.active { background: linear-gradient(135deg, #fa709a, #fee140); }

.user-stat-item .stat-value {
  font-size: 22px;
  font-weight: 700;
  color: #333;
}

.user-stat-item .stat-label {
  font-size: 12px;
  color: #999;
  margin-top: 2px;
}

.user-lists {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 20px;
}

.user-list-section {
  padding: 16px;
  background: #fafafa;
  border-radius: 8px;
}

.user-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;
}

.user-item:last-child {
  border-bottom: none;
}

.user-rank {
  width: 22px;
  height: 22px;
  line-height: 22px;
  text-align: center;
  background: #e5e7eb;
  border-radius: 50%;
  font-size: 11px;
  font-weight: 600;
  color: #666;
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
}

.user-name {
  flex: 1;
  font-weight: 500;
  font-size: 14px;
}

.user-orders {
  color: #666;
  font-size: 13px;
}

.user-amount {
  color: #ff4757;
  font-weight: 600;
  font-size: 14px;
}

/* 订单分析 */
.order-stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 16px;
}

.order-stat-item {
  text-align: center;
  padding: 20px;
  background: #f9fafb;
  border-radius: 8px;
}

.order-stat-item .stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #333;
}

.order-stat-item .stat-value.warning {
  color: #f59e0b;
}

.order-stat-item .stat-value.success {
  color: #10b981;
}

.order-stat-item .stat-label {
  font-size: 13px;
  color: #999;
  margin-top: 4px;
}

/* 平台健康度 */
.suggestions {
  background: linear-gradient(135deg, #e8f5e9 0%, #fff 100%);
}

.suggestion-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.suggestion-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  background: white;
  border-radius: 8px;
  font-size: 14px;
  color: #666;
}

.suggestion-icon {
  font-size: 18px;
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 30px;
  color: #999;
  font-size: 13px;
}

/* 响应式 */
@media (max-width: 768px) {
  .metrics-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .user-stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .order-stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .chart-container {
    gap: 8px;
  }

  .user-lists {
    grid-template-columns: 1fr;
  }
}
</style>
