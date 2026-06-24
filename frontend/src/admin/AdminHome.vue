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
  totalRefunds: 0,
  refundRate: 0,
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

// 加载全平台真实后端数据
const loadPlatformData = async () => {
  loading.value = true;
  // 每次加载先清空旧数据，杜绝残留假数据
  platformStats.value = {
    totalUsers: 0,
    totalGoods: 0,
    totalOrders: 0,
    totalRevenue: 0,
    pendingOrders: 0,
    completedOrders: 0,
    bannedUsers: 0,
    activeSellers: 0
  };
  salesTrend.value = [];
  topGoods.value = [];
  userAnalysis.value = {
    totalBuyers: 0,
    totalSellers: 0,
    newUsersToday: 0,
    activeUsers: 0,
    topSpendingUsers: [],
    topActiveUsers: []
  };
  orderAnalysis.value = {
    avgOrderValue: 0,
    refundRate: 0,
    completionRate: 0
  };

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
        totalRefunds: data.totalRefunds || 0,
        refundRate: data.refundRate || 0,
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

    // 重新统计买家和卖家（排除管理员）
    const allUsersRes = await api.listUsers();
    if (allUsersRes.data.code === 0) {
      const allUsers = allUsersRes.data.data || [];
      userAnalysis.value.totalBuyers = allUsers.filter(u => u.role === 0).length;
      userAnalysis.value.totalSellers = allUsers.filter(u => u.role === 2).length;
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
    ElMessage.error('加载统计数据失败，请检查接口或网络');
    // 【关键修改】异常不再加载模拟假数据，保持空白真实状态
    // initMockData(); // 注释掉模拟数据，彻底禁用假数据
  }
  loading.value = false;
};

// 仅保留，演示备用，页面不再自动调用
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
  const num = Number(amount) || 0;
  if (num >= 10000) {
    return '￥' + (num / 10000).toFixed(1) + '万';
  }
  return '￥' + num.toFixed(2);
};

// 格式化数字
const formatNumber = (num: number) => {
  const n = Number(num) || 0;
  if (n >= 10000) {
    return (n / 10000).toFixed(1) + '万';
  }
  return n.toLocaleString();
};

// 格式化百分比
const formatPercent = (value: number) => {
  const v = Number(value) || 0;
  return v.toFixed(1) + '%';
};

onMounted(() => {
  loadPlatformData();
});
</script>

<template>
  <div class="analytics-page">
    <h2>全平台数据概览</h2>

    <!-- 加载遮罩 -->
    <div v-if="loading" class="loading-mask">
      正在加载真实统计数据...
    </div>

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

      <div class="metric-card">
        <div class="metric-icon refunds">🔄</div>
        <div class="metric-content">
          <div class="metric-value">{{ formatNumber(platformStats.totalRefunds) }}</div>
          <div class="metric-label">退货申请数</div>
        </div>
      </div>

      <div class="metric-card">
        <div class="metric-icon rate">📊</div>
        <div class="metric-content">
          <div class="metric-value">{{ formatPercent(platformStats.refundRate) }}</div>
          <div class="metric-label">退货率</div>
        </div>
      </div>
    </div>

    <!-- 销售趋势 -->
    <div class="section-card">
      <h3>平台销售趋势（近7天）</h3>
      <div v-if="salesTrend.length === 0 && !loading" class="empty-state">暂无趋势数据</div>
      <div v-else class="trend-chart">
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

    <!-- 用户分析 移到商品排行上方 -->
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
          <div v-if="userAnalysis.topSpendingUsers.length === 0 && !loading" class="empty-state">
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
          <div v-if="userAnalysis.topActiveUsers.length === 0 && !loading" class="empty-state">
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
        <div v-if="topGoods.length === 0 && !loading" class="empty-state">
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
        <div v-if="
          platformStats.pendingOrders <= 100
          && orderAnalysis.completionRate < 90
          && orderAnalysis.refundRate >=5
          && userAnalysis.newUsersToday <=30
          && platformStats.bannedUsers === 0
        " class="suggestion-item">
          <span class="suggestion-icon">📋</span>
          <span>暂无特殊平台提示</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.analytics-page {
  max-width: 100%;
  margin: 0;
  padding: 16px;
}

.loading-mask {
  text-align: center;
  padding: 40px;
  color: #666;
  font-size: 14px;
}

h2 {
  margin: 0 0 16px 0;
  color: #333;
  font-size: 18px;
  font-weight: 600;
}

/* 核心指标卡片 简约扁平化统一风格 */
.metrics-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}

.metric-card {
  background: #fff;
  border: 1px solid #eee;
  border-radius: 0;
  padding: 14px 10px;
  display: flex;
  align-items: center;
  gap: 10px;
  box-shadow: none;
  position: relative;
  overflow: hidden;
}
.metric-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  width: 3px;
  height: 100%;
}
.metric-card:nth-child(1)::before { background: #667eea; }
.metric-card:nth-child(2)::before { background: #f5576c; }
.metric-card:nth-child(3)::before { background: #00f2fe; }
.metric-card:nth-child(4)::before { background: #38f9d7; }
.metric-card:nth-child(5)::before { background: #f59e0b; }
.metric-card:nth-child(6)::before { background: #8b5cf6; }

.metric-icon {
  width: 44px;
  height: 44px;
  border-radius: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
}

.metric-icon.users { background: #eef2ff; }
.metric-icon.goods { background: #fef2f7; }
.metric-icon.orders { background: #e6fbfe; }
.metric-icon.revenue { background: #ecfdf7; }
.metric-icon.refunds { background: #fef3c7; }
.metric-icon.rate { background: #ede9fe; }

.metric-value {
  font-size: 20px;
  font-weight: 600;
  color: #222;
}

.metric-label {
  font-size: 12px;
  color: #666;
  margin-top: 3px;
}

/* 区块卡片 */
.section-card {
  background: white;
  border: 1px solid #eee;
  border-radius: 0;
  padding: 14px;
  margin-bottom: 12px;
  box-shadow: none;
}

.section-card h3 {
  margin: 0 0 12px 0;
  font-size: 16px;
  color: #333;
  font-weight: 600;
}

.section-card h4 {
  margin: 16px 0 8px 0;
  font-size: 14px;
  color: #555;
}

/* 销售趋势图表 */
.trend-chart {
  overflow-x: auto;
}

.chart-container {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  height: 180px;
  padding: 16px 0;
}

.chart-bar-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 50px;
}

.chart-bar {
  width: 100%;
  max-width: 40px;
  background: #667eea;
  border-radius: 0;
  display: flex;
  justify-content: center;
  padding-top: 6px;
  min-height: 10px;
}

.bar-value {
  font-size: 9px;
  color: white;
  font-weight: 500;
  writing-mode: vertical-rl;
}

.chart-label {
  font-size: 11px;
  color: #777;
  margin-top: 6px;
}

/* 商品表格 */
.goods-table {
  width: 100%;
}

.table-header, .table-row {
  display: flex;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #eee;
}

.table-header {
  font-weight: 600;
  color: #555;
  font-size: 13px;
}

.table-row:hover {
  background: #fafafa;
}

.col-rank { width: 50px; text-align: center; }
.col-name { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size:13px; }
.col-sales { width: 80px; text-align: center; font-size:13px; }
.col-revenue { width: 110px; text-align: right; font-size:13px; color:#e53e3e; font-weight:500; }

.rank-badge {
  display: inline-block;
  width: 22px;
  height: 22px;
  line-height: 22px;
  text-align: center;
  border-radius: 0;
  font-size: 11px;
  font-weight: 600;
  background: #f0f0f0;
  color: #666;
}

.rank-badge.rank-1 { background: #ffd700; color: #333; }
.rank-badge.rank-2 { background: #c0c0c0; color: #fff; }
.rank-badge.rank-3 { background: #cd7f32; color: #fff; }

/* 用户分析 */
.user-stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
  margin-bottom: 16px;
}

.user-stat-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px;
  background: #f9fafb;
  border:1px solid #eee;
  border-radius: 0;
}

.stat-icon {
  width: 36px;
  height: 36px;
  border-radius: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
}

.stat-icon.buyers { background: #fef2f7; color:#f5576c; }
.stat-icon.sellers { background: #e6fbfe; color:#00f2fe; }
.stat-icon.new { background: #ecfdf7; color:#38f9d7; }
.stat-icon.active { background: #fff3e0; color:#fa709a; }

.user-stat-item .stat-value {
  font-size: 18px;
  font-weight: 600;
  color: #222;
}

.user-stat-item .stat-label {
  font-size: 11px;
  color: #777;
  margin-top: 2px;
}

.user-lists {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.user-list-section {
  padding: 10px;
  background: #fafafa;
  border:1px solid #eee;
  border-radius: 0;
}

.user-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
  border-bottom: 1px solid #eee;
  font-size:13px;
}

.user-item:last-child {
  border-bottom: none;
}

.user-rank {
  width: 20px;
  height: 20px;
  line-height: 20px;
  text-align: center;
  background: #e5e7eb;
  border-radius: 0;
  font-size: 10px;
  font-weight: 600;
  color: #666;
}

.user-avatar {
  width: 28px;
  height: 28px;
  border-radius: 0;
  background: #667eea;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
}

.user-name {
  flex: 1;
  font-weight: 500;
}

.user-orders {
  color: #666;
  font-size: 11px;
}

.user-amount {
  color: #ff4757;
  font-weight: 600;
  font-size: 12px;
}

/* 订单分析 */
.order-stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}

.order-stat-item {
  text-align: center;
  padding: 12px;
  background: #f9fafb;
  border:1px solid #eee;
  border-radius: 0;
}

.order-stat-item .stat-value {
  font-size: 22px;
  font-weight: 600;
  color: #222;
}

.order-stat-item .stat-value.warning {
  color: #f59e0b;
}

.order-stat-item .stat-value.success {
  color: #10b981;
}

.order-stat-item .stat-label {
  font-size: 12px;
  color: #777;
  margin-top: 3px;
}

/* 平台健康度 */
.suggestions {
  background: #f8fbf8;
  border:1px solid #e0efe0;
}

.suggestion-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.suggestion-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  background: white;
  border:1px solid #eee;
  border-radius: 0;
  font-size: 13px;
  color: #444;
}

.suggestion-icon {
  font-size: 16px;
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 24px;
  color: #999;
  font-size: 12px;
}

/* 响应式适配 */
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

  .user-lists {
    grid-template-columns: 1fr;
  }
}
</style>