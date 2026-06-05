<script setup lang="ts">
import { ref, onMounted, inject } from 'vue';
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

const loadOrders = async () => {
  loading.value = true;
  try {
    const res = await api.getAllOrders();
    if (res.data.code === 0) {
      orderList.value = res.data.data || [];
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
  const types = ['info', 'warning', 'primary', 'success', 'danger'];
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
        <div class="order-summary">
          <div class="summary-item">
            <span class="summary-number">{{ orderList.length }}</span>
            <span class="summary-label">总订单</span>
          </div>
        </div>
        <el-button type="primary" @click="loadOrders">
          🔄 刷新
        </el-button>
      </div>
    </div>

    <!-- 快速统计 -->
    <el-row :gutter="20" class="quick-stats">
      <el-col :xs="12" :sm="6">
        <div class="stat-box blue">
          <div class="stat-icon">📋</div>
          <div class="stat-content">
            <div class="stat-number">{{ orderList.length }}</div>
            <div class="stat-label">总订单</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-box success">
          <div class="stat-icon">✅</div>
          <div class="stat-content">
            <div class="stat-number">--</div>
            <div class="stat-label">已完成</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-box warning">
          <div class="stat-icon">⚖️</div>
          <div class="stat-content">
            <div class="stat-number">{{ Object.keys(disputeMap).length }}</div>
            <div class="stat-label">纠纷订单</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-box purple">
          <div class="stat-icon">💰</div>
          <div class="stat-content">
            <div class="stat-number">--</div>
            <div class="stat-label">总交易额</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 订单列表 -->
    <el-card class="table-card">
      <el-table 
        :data="orderList" 
        style="width: 100%" 
        v-loading="loading"
        stripe
      >
        <el-table-column label="订单号" prop="id" width="100" />
        
        <el-table-column label="商品信息" width="320">
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
        
        <el-table-column label="买家" width="160">
          <template #default="scope">
            <div class="user-cell">
              <el-avatar :size="36" :src="scope.row.buyerAvatar" class="user-avatar">
                {{ (scope.row.buyerNickname || scope.row.buyerUsername || 'B').charAt(0).toUpperCase() }}
              </el-avatar>
              <div class="user-details">
                <div>{{ scope.row.buyerNickname || scope.row.buyerUsername || '-' }}</div>
                <div class="user-username">@{{ scope.row.buyerUsername }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column label="卖家" width="160">
          <template #default="scope">
            <div class="user-cell">
              <el-avatar :size="36" :src="scope.row.sellerAvatar" class="user-avatar">
                {{ (scope.row.sellerNickname || scope.row.sellerUsername || 'S').charAt(0).toUpperCase() }}
              </el-avatar>
              <div class="user-details">
                <div>{{ scope.row.sellerNickname || scope.row.sellerUsername || '-' }}</div>
                <div class="user-username">@{{ scope.row.sellerUsername }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column label="订单金额" width="120">
          <template #default="scope">
            <span class="price-text">¥{{ scope.row.amount }}</span>
          </template>
        </el-table-column>
        
        <el-table-column label="订单状态" width="180">
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
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="scope">
            <el-button 
              v-if="disputeMap[scope.row.id]" 
              type="primary" 
              size="small"
              @click="openDisputeDetail(scope.row.id)"
            >
              纠纷详情
            </el-button>
          </template>
        </el-table-column>
        
        <el-table-column label="下单时间" width="180">
          <template #default="scope">
            {{ formatDate(scope.row.createTime) }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    
    <!-- 纠纷详情对话框 -->
    <el-dialog v-model="showDisputeDetailDialog" title="纠纷详情" width="600px">
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
            :rows="3" 
            placeholder="请填写判决内容..."
          />
          <el-input 
            v-model="decisionForm.remark" 
            type="textarea" 
            :rows="2" 
            placeholder="备注（可选）"
            style="margin-top: 12px"
          />
          <div class="decision-buttons">
            <el-button type="primary" size="large" @click="handleAdminDecision(3)">
              判买家退货
            </el-button>
            <el-button type="success" size="large" @click="handleAdminDecision(4)">
              判卖家退款
            </el-button>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="showDisputeDetailDialog = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.order-list {
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

.order-summary {
  display: flex;
  gap: 20px;
  background: white;
  padding: 12px 24px;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.summary-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.summary-number {
  font-size: 24px;
  font-weight: 700;
  color: #1f2937;
}

.summary-label {
  font-size: 13px;
  color: #6b7280;
  margin-top: 2px;
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

.stat-box.blue {
  border-left: 4px solid #3b82f6;
}

.stat-box.success {
  border-left: 4px solid #10b981;
}

.stat-box.warning {
  border-left: 4px solid #f59e0b;
}

.stat-box.purple {
  border-left: 4px solid #8b5cf6;
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

/* 表格卡片 */
.table-card {
  border-radius: 16px;
  border: none;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
}

/* 商品单元格 */
.goods-cell {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.goods-image {
  width: 64px;
  height: 64px;
  border-radius: 8px;
  flex-shrink: 0;
  border: 2px solid #f3f4f6;
}

.goods-image-placeholder {
  width: 64px;
  height: 64px;
  border-radius: 8px;
  background: #f9fafb;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  flex-shrink: 0;
}

.goods-details {
  flex: 1;
  min-width: 0;
}

.goods-name {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.goods-desc {
  font-size: 12px;
  color: #9ca3af;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.goods-price {
  font-size: 14px;
  color: #ef4444;
  font-weight: 700;
}

.price-text {
  font-size: 16px;
  font-weight: 700;
  color: #ef4444;
}

/* 用户单元格 */
.user-cell {
  display: flex;
  gap: 10px;
  align-items: center;
}

.user-avatar {
  border: 2px solid #f3f4f6;
}

.user-details {
  flex: 1;
  min-width: 0;
}

.user-details > div:first-child {
  font-size: 14px;
  color: #1f2937;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-username {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 2px;
}

.status-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

/* 纠纷详情 */
.dispute-detail {
  padding: 10px 0;
}

.section {
  padding: 16px;
  background: #f9fafb;
  border-radius: 12px;
  margin-bottom: 16px;
}

.section h4 {
  margin: 0 0 12px 0;
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
}

.section .content {
  margin: 0;
  font-size: 14px;
  color: #4b5563;
  line-height: 1.6;
}

.section .remark {
  margin: 10px 0 0 0;
  font-size: 13px;
  color: #6b7280;
}

.image-list {
  display: flex;
  gap: 12px;
  margin-top: 12px;
  flex-wrap: wrap;
}

.dispute-image {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  border: 2px solid #e5e7eb;
}

.decision-section {
  padding: 16px;
  border: 2px dashed #3b82f6;
  border-radius: 12px;
  background: #eff6ff;
}

.decision-section h4 {
  margin: 0 0 12px 0;
  font-size: 15px;
  font-weight: 600;
  color: #1d4ed8;
}

.decision-buttons {
  margin-top: 16px;
  display: flex;
  gap: 12px;
}
</style>
