<script setup lang="ts">
import { ref, onMounted, inject, computed } from 'vue';
import { ElMessage, ElTable, ElTableColumn, ElImage, ElTag, ElDialog, ElInput, ElButton } from 'element-plus';
import { api } from '../api';

const user = inject('user', ref<any>(null));
const orderList = ref<any[]>([]);
const loading = ref(false);
const disputeMap = ref<Record<number, any>>({});
const showDisputeDetailDialog = ref(false);
const currentDispute = ref<any>(null);
const decisionForm = ref({
  decision: '',
  remark: ''
});
const searchKeyword = ref('');

// 统计计算：已完成订单、总交易额
const finishedOrderCount = computed(() => {
  // 若你的后端已完成状态不是4，自行修改此处数字
  return orderList.value.filter(order => order.status === 4).length;
});
const totalTransactionAmount = computed(() => {
  const total = orderList.value.reduce((sum, item) => sum + Number(item.amount || 0), 0);
  return total.toFixed(2); // 保留两位小数
});

const loadOrders = async () => {
  loading.value = true;
  try {
    const res = await api.getAllOrders();
    if (res.data.code === 0) {
      let orders = res.data.data || [];
      
      // 前端搜索过滤
      if (searchKeyword.value.trim()) {
        const keyword = searchKeyword.value.trim().toLowerCase();
        orders = orders.filter(order => {
          const orderId = String(order.id || '').toLowerCase();
          const goodsName = String(order.goodsName || '').toLowerCase();
          const buyerName = String(order.buyerNickname || order.buyerUsername || '').toLowerCase();
          const sellerName = String(order.sellerNickname || order.sellerUsername || '').toLowerCase();
          return orderId.includes(keyword) || 
                 goodsName.includes(keyword) || 
                 buyerName.includes(keyword) || 
                 sellerName.includes(keyword);
        });
      }
      
      orderList.value = orders;
      await loadAllDisputes();
    } else {
      ElMessage.error(res.data.msg || '加载订单失败');
    }
  } catch (err) {
    ElMessage.error('加载订单失败');
  } finally {
    loading.value = false;
  }
};

const loadAllDisputes = async () => {
  try {
    const res = await api.getAllDisputes();
    if (res.data.code === 0) {
      const disputes = res.data.data || [];
      for (const dispute of disputes) {
        disputeMap.value[dispute.orderId] = dispute;
      }
    }
  } catch (err) {
    console.error('加载纠纷列表失败', err);
  }
};

const getDisputeStatusLabel = (status: number) => {
  const statusMap: Record<number, { label: string; type: string }> = {
    1: { label: '买家申请平台介入', type: 'warning' },
    2: { label: '管理员处理中', type: 'info' },
    3: { label: '判买家退货', type: 'primary' },
    4: { label: '判卖家退款', type: 'success' }
  };
  return statusMap[status] || { label: '', type: 'info' };
};

const openDisputeDetail = async (orderId: number) => {
  try {
    const res = await api.getDisputeByOrderId(orderId);
    if (res.data.code === 0 && res.data.data) {
      currentDispute.value = res.data.data;
      decisionForm.value = { decision: '', remark: '' };
      showDisputeDetailDialog.value = true;
    } else {
      ElMessage.error('获取纠纷详情失败');
    }
  } catch (err) {
    ElMessage.error('获取纠纷详情失败');
  }
};

const handleAdminDecision = async (status: number) => {
  if (!user.value || !currentDispute.value) return;
  if (!decisionForm.value.decision.trim()) {
    ElMessage.warning('请填写判决内容');
    return;
  }

  try {
    await api.adminDecision(
        currentDispute.value.id,
        decisionForm.value.decision,
        decisionForm.value.remark,
        user.value.id,
        status
    );
    ElMessage.success('判决成功');
    showDisputeDetailDialog.value = false;
    await loadAllDisputes();
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.msg || '判决失败');
  }
};

const parseImages = (imagesStr: string) => {
  if (!imagesStr) return [];
  return imagesStr.split(',').filter((url: string) => url.trim());
};

const getStatusType = (status: number) => {
  const types = ['info', 'warning', 'primary', 'success', 'danger', 'warning'];
  return types[status] || 'info';
};

const formatDate = (dateStr: string) => {
  if (!dateStr) return '';
  return new Date(dateStr).toLocaleString('zh-CN');
};

onMounted(() => {
  loadOrders();
});
</script>

<template>
  <div class="order-list">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-left">
        <span class="page-icon">📦</span>
        <div class="page-info">
          <h1 class="page-title">订单管理</h1>
          <p class="page-desc">管理平台所有交易订单</p>
        </div>
      </div>
      <div class="header-right">
        <el-input
            v-model="searchKeyword"
            placeholder="搜索订单号/商品/买家/卖家"
            size="small"
            class="search-input"
            @keyup.enter="loadOrders"
        >
          <template #prefix>
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16">
              <circle cx="11" cy="11" r="8"/>
              <path d="m21 21-4.35-4.35"/>
            </svg>
          </template>
        </el-input>
        <el-button size="small" @click="loadOrders">🔄 刷新</el-button>
      </div>
    </div>

    <!-- 快速统计（修复空白--，显示已完成/总交易额） -->
    <div class="stats-grid">
      <div class="stat-box blue">
        <div class="stat-icon">📋</div>
        <div class="stat-content">
          <div class="stat-number">{{ orderList.length }}</div>
          <div class="stat-label">总订单</div>
        </div>
      </div>
      <div class="stat-box success">
        <div class="stat-icon">✅</div>
        <div class="stat-content">
          <div class="stat-number">{{ finishedOrderCount }}</div>
          <div class="stat-label">已完成</div>
        </div>
      </div>
      <div class="stat-box warning">
        <div class="stat-icon">⚖️</div>
        <div class="stat-content">
          <div class="stat-number">{{ Object.keys(disputeMap).length }}</div>
          <div class="stat-label">纠纷订单</div>
        </div>
      </div>
      <div class="stat-box purple">
        <div class="stat-icon">💰</div>
        <div class="stat-content">
          <div class="stat-number">¥{{ totalTransactionAmount }}</div>
          <div class="stat-label">总交易额</div>
        </div>
      </div>
    </div>

    <!-- 订单列表 -->
    <div class="table-wrap">
      <el-table
          :data="orderList"
          style="width: 100%"
          v-loading="loading"
          stripe
          size="small"
      >
        <el-table-column label="订单号" prop="id" width="90" />

        <el-table-column label="商品信息" min-width="280">
          <template #default="scope">
            <div class="goods-cell">
              <el-image
                  v-if="scope.row.goodsImage"
                  :src="scope.row.goodsImage"
                  :preview-src-list="[scope.row.goodsImage]"
                  fit="cover"
                  class="goods-image"
              />
              <div v-else class="goods-image-placeholder">
                📦
              </div>
              <div class="goods-details">
                <div class="goods-name">{{ scope.row.goodsName || '-' }}</div>
                <div class="goods-desc">{{ scope.row.goodsDescription || '-' }}</div>
                <div class="goods-price">¥{{ scope.row.goodsPrice }}</div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="买家" width="140">
          <template #default="scope">
            <div class="user-cell">
              <el-avatar :size="32" :src="scope.row.buyerAvatar" class="user-avatar">
                {{ (scope.row.buyerNickname || scope.row.buyerUsername || 'B').charAt(0).toUpperCase() }}
              </el-avatar>
              <div class="user-details">
                <div>{{ scope.row.buyerNickname || scope.row.buyerUsername || '-' }}</div>
                <div class="user-username">@{{ scope.row.buyerUsername }}</div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="卖家" width="140">
          <template #default="scope">
            <div class="user-cell">
              <el-avatar :size="32" :src="scope.row.sellerAvatar" class="user-avatar">
                {{ (scope.row.sellerNickname || scope.row.sellerUsername || 'S').charAt(0).toUpperCase() }}
              </el-avatar>
              <div class="user-details">
                <div>{{ scope.row.sellerNickname || scope.row.sellerUsername || '-' }}</div>
                <div class="user-username">@{{ scope.row.sellerUsername }}</div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="金额" width="100">
          <template #default="scope">
            <span class="price-text">¥{{ scope.row.amount }}</span>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="170">
          <template #default="scope">
            <div class="status-cell">
              <el-tag :type="getStatusType(scope.row.status)" size="small">
                {{ scope.row.statusText }}
              </el-tag>
              <el-tag v-if="scope.row.afterSaleStatusText" type="danger" size="small">
                {{ scope.row.afterSaleStatusText }}
              </el-tag>
              <el-tag
                  v-if="disputeMap[scope.row.id]"
                  :type="getDisputeStatusLabel(disputeMap[scope.row.id].status).type"
                  size="small"
              >
                ⚖️ {{ getDisputeStatusLabel(disputeMap[scope.row.id].status).label }}
              </el-tag>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="110" fixed="right">
          <template #default="scope">
            <el-button
                v-if="disputeMap[scope.row.id]"
                type="primary"
                size="mini"
                @click="openDisputeDetail(scope.row.id)"
            >
              纠纷详情
            </el-button>
          </template>
        </el-table-column>

        <el-table-column label="下单时间" width="160">
          <template #default="scope">
            {{ formatDate(scope.row.createTime) }}
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 纠纷详情对话框 -->
    <el-dialog v-model="showDisputeDetailDialog" title="纠纷详情" width="520px" :show-close="false">
      <div v-if="currentDispute" class="dispute-detail">
        <div class="section buyer-section">
          <h4>👤 买家申诉</h4>
          <p class="content">{{ currentDispute.buyerContent || '无描述' }}</p>
          <div v-if="parseImages(currentDispute.buyerImages).length > 0" class="image-list">
            <el-image
                v-for="(url, index) in parseImages(currentDispute.buyerImages)"
                :key="index"
                :src="url"
                :preview-src-list="parseImages(currentDispute.buyerImages)"
                fit="cover"
                class="dispute-image"
            />
          </div>
        </div>

        <div v-if="currentDispute.sellerReply" class="section seller-section">
          <h4>🏪 卖家回复</h4>
          <p class="content">{{ currentDispute.sellerReply }}</p>
          <div v-if="parseImages(currentDispute.sellerImages).length > 0" class="image-list">
            <el-image
                v-for="(url, index) in parseImages(currentDispute.sellerImages)"
                :key="index"
                :src="url"
                :preview-src-list="parseImages(currentDispute.sellerImages)"
                fit="cover"
                class="dispute-image"
            />
          </div>
        </div>

        <div v-if="currentDispute.adminDecision" class="section admin-section">
          <h4>⚖️ 管理员判决</h4>
          <p class="content">{{ currentDispute.adminDecision }}</p>
          <p v-if="currentDispute.adminRemark" class="remark">备注：{{ currentDispute.adminRemark }}</p>
        </div>

        <div v-if="currentDispute.status === 1 || currentDispute.status === 2" class="decision-section">
          <h4>📝 进行判决</h4>
          <el-input
              v-model="decisionForm.decision"
              type="textarea"
              :rows="2"
              placeholder="请填写判决内容..."
          />
          <el-input
              v-model="decisionForm.remark"
              type="textarea"
              :rows="1"
              placeholder="备注（可选）"
              style="margin-top:8px"
          />
          <div class="decision-buttons">
            <el-button size="small" type="primary" @click="handleAdminDecision(3)">判买家退货</el-button>
            <el-button size="small" type="success" @click="handleAdminDecision(4)">判卖家退款</el-button>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button size="small" @click="showDisputeDetailDialog = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
* { margin: 0; padding: 0; box-sizing: border-box; }
.order-list {
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
  margin: 0;
}
.page-desc {
  font-size: 12px;
  color: #666;
  margin-top: 2px;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}
.search-input {
  width: 240px;
}
:deep(.el-button) { border-radius: 0; }

/* 统计网格 紧凑四等分 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4,1fr);
  gap: 12px;
  margin-bottom: 16px;
}
.stat-box {
  background: #fff;
  border:1px solid #eee;
  border-radius:0;
  padding:14px 10px;
  display:flex;
  align-items:center;
  gap:10px;
}
/* 顶部细线标识，取消粗左边框 */
.stat-box.blue { border-top:3px solid #3b82f6; }
.stat-box.success { border-top:3px solid #10b981; }
.stat-box.warning { border-top:3px solid #f59e0b; }
.stat-box.purple { border-top:3px solid #8b5cf6; }
.stat-icon { font-size:24px; }
.stat-content { flex:1; text-align:center; }
.stat-number { font-size:20px; font-weight:600; }
.stat-label { font-size:12px; color:#666; margin-top:3px; }

/* 表格容器 */
.table-wrap {
  width:100%;
  background:#fff;
  border:1px solid #eee;
}
:deep(.el-table) {
  --el-table-row-hover-bg-color:#fafafa;
}
:deep(.el-table th),:deep(.el-table td) {
  padding:8px 10px;
}
:deep(.el-table__header-wrapper) {
  border-bottom:1px solid #eee;
}

/* 商品单元格 */
.goods-cell {
  display:flex;
  gap:8px;
  align-items:flex-start;
}
.goods-image,.goods-image-placeholder {
  width:48px;
  height:48px;
  border-radius:0;
  border:1px solid #eee;
  flex-shrink:0;
}
.goods-image-placeholder {
  background:#f8f8f8;
  display:flex;
  align-items:center;
  justify-content:center;
  font-size:20px;
}
.goods-details { min-width:0; }
.goods-name {
  font-size:14px;
  font-weight:500;
  white-space:nowrap;
  overflow:hidden;
  text-overflow:ellipsis;
}
.goods-desc {
  font-size:12px;
  color:#888;
  margin:2px 0;
  white-space:nowrap;
  overflow:hidden;
  text-overflow:ellipsis;
}
.goods-price { font-size:13px; color:#e53e3e; font-weight:500; }
.price-text { font-size:14px; font-weight:600; color:#e53e3e; }

/* 用户单元格 */
.user-cell {
  display:flex;
  align-items:center;
  gap:8px;
}
.user-avatar {
  border-radius:0;
  border:1px solid #eee;
}
.user-details { min-width:0; }
.user-details > div:first-child {
  font-size:13px;
  white-space:nowrap;
  overflow:hidden;
  text-overflow:ellipsis;
}
.user-username { font-size:11px; color:#888; margin-top:2px; }

.status-cell {
  display:flex;
  flex-direction:column;
  gap:3px;
}
:deep(.el-tag) { border-radius:0; }

/* 纠纷弹窗 */
.dispute-detail { padding:8px 0; }
.section {
  padding:12px;
  background:#f8f8f8;
  border:1px solid #eee;
  border-radius:0;
  margin-bottom:12px;
}
.section h4 {
  font-size:14px;
  margin-bottom:8px;
  font-weight:600;
}
.content { font-size:13px; line-height:1.5; color:#444; }
.remark { font-size:12px; color:#666; margin-top:6px; }

.image-list {
  display:flex;
  gap:8px;
  flex-wrap:wrap;
  margin-top:8px;
}
.dispute-image {
  width:64px;
  height:64px;
  border-radius:0;
  border:1px solid #ddd;
}

.decision-section {
  padding:12px;
  border:1px dashed #3b82f6;
  background:#f7fbff;
}
.decision-section h4 { margin-bottom:8px; font-size:14px; }
.decision-buttons { margin-top:12px; display:flex; gap:10px; }

/* 弹窗全局穿透样式 */
:deep(.el-dialog) {
  border-radius:0;
  box-shadow:none;
  border:1px solid #eee;
}
:deep(.el-dialog__header) {
  border-bottom:1px solid #eee;
  padding:10px 16px;
}
:deep(.el-dialog__footer) {
  border-top:1px solid #eee;
  padding:10px 16px;
  text-align:right;
}
:deep(.el-input__inner),:deep(.el-textarea__inner) {
  border-radius:0;
}
</style>