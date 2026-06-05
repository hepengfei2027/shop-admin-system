<script setup lang="ts">
import { ref, onMounted, computed, inject } from 'vue';
import { ElMessage, ElDialog, ElInput, ElButton, ElRate } from 'element-plus';
import { api } from '../api';
import { useRouter } from 'vue-router';

const router = useRouter();
const user = inject('user', ref<any>(null));
const activeTab = ref('buyer');
const activeStatus = ref('all');
const buyerOrders = ref<any[]>([]);
const sellerOrders = ref<any[]>([]);
const goodsMap = ref<Record<number, any>>({});

const isBuyer = computed(() => user.value && user.value.role === 0);
const isSeller = computed(() => user.value && user.value.role === 2);

const tabs = computed(() => {
  const result = [];
  if (isBuyer.value) {
    result.push({ value: 'buyer', label: '我买的' });
  }
  if (isSeller.value) {
    result.push({ value: 'seller', label: '我卖的' });
  }
  return result;
});

const statusMap: Record<number, { label: string; type: string }> = {
  0: { label: '待付款', type: 'warning' },
  1: { label: '待发货', type: 'primary' },
  2: { label: '待收货', type: 'info' },
  3: { label: '已完成', type: 'success' },
  4: { label: '已取消', type: 'danger' }
};

const afterSaleStatusMap: Record<number, { label: string; type: string }> = {
  0: { label: '', type: '' },
  1: { label: '待商家确认', type: 'warning' },
  2: { label: '商家同意退货', type: 'primary' },
  3: { label: '买家已发货', type: 'info' },
  4: { label: '退款成功', type: 'success' },
  5: { label: '商家拒绝退货', type: 'danger' }
};

const statusFilterOptions = [
  { value: 'all', label: '全部' },
  { value: '0', label: '待付款' },
  { value: '1', label: '待发货' },
  { value: '2', label: '待收货' },
  { value: '3', label: '已完成' },
  { value: '4', label: '已取消' }
];

const currentOrders = computed(() => {
  const orders = activeTab.value === 'buyer' ? buyerOrders.value : sellerOrders.value;
  if (activeStatus.value === 'all') {
    return orders;
  }
  return orders.filter(order => order.status === Number(activeStatus.value));
});

const getOrderCount = (status: string) => {
  const orders = activeTab.value === 'buyer' ? buyerOrders.value : sellerOrders.value;
  if (status === 'all') {
    return orders.length;
  }
  return orders.filter(order => order.status === Number(status)).length;
};

const formatDateTime = (dateStr: string) => {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');
  const seconds = String(date.getSeconds()).padStart(2, '0');
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
};

const loadBuyerOrders = async () => {
  if (!user.value) return;
  try {
    const res = await api.listBuyerOrders(user.value.id);
    if (res.data.code === 0) {
      buyerOrders.value = res.data.data || [];
      const goodsIds = buyerOrders.value.map(o => o.goodsId);
      await loadGoodsDetails(goodsIds);
      for (const order of buyerOrders.value) {
        if (order.afterSaleStatus === 5) {
          await loadDisputeByOrderId(order.id);
        }
      }
    }
  } catch (err) {
    ElMessage.error('加载订单失败');
  }
};

const loadSellerOrders = async () => {
  if (!user.value) return;
  try {
    const res = await api.listSellerOrders(user.value.id);
    if (res.data.code === 0) {
      sellerOrders.value = res.data.data || [];
      const goodsIds = sellerOrders.value.map(o => o.goodsId);
      await loadGoodsDetails(goodsIds);
      for (const order of sellerOrders.value) {
        if (order.afterSaleStatus === 5) {
          await loadDisputeByOrderId(order.id);
        }
      }
    }
  } catch (err) {
    ElMessage.error('加载订单失败');
  }
};

const loadGoodsDetails = async (goodsIds: number[]) => {
  const uniqueIds = [...new Set(goodsIds)];
  for (const id of uniqueIds) {
    if (goodsMap.value[id]) continue;
    try {
      const res = await api.getGoodsDetail(id);
      if (res.data.code === 0 && res.data.data) {
        goodsMap.value[id] = res.data.data;
      }
    } catch (err) {
      console.error('加载商品详情失败', err);
    }
  }
};

const handleTabChange = (tab: string) => {
  if (tab === 'buyer' && !isBuyer.value) return;
  if (tab === 'seller' && !isSeller.value) return;
  
  activeTab.value = tab;
  if (tab === 'buyer') {
    loadBuyerOrders();
  } else {
    loadSellerOrders();
  }
};

const handlePay = async (orderId: number) => {
  if (!user.value || !isBuyer.value) return;
  try {
    await api.payOrderV2(orderId, user.value.id);
    ElMessage.success('支付成功');
    if (isBuyer.value) loadBuyerOrders();
  } catch (err) {
    ElMessage.error('支付失败');
  }
};

const handleCancel = async (orderId: number) => {
  if (!user.value || !isBuyer.value) return;
  try {
    await api.cancelOrderV2(orderId, user.value.id);
    ElMessage.success('订单已取消');
    loadBuyerOrders();
  } catch (err) {
    ElMessage.error('取消失败');
  }
};

const handleShip = async (orderId: number) => {
  if (!user.value || !isSeller.value) return;
  try {
    await api.shipOrder(orderId, user.value.id);
    ElMessage.success('已发货');
    loadSellerOrders();
  } catch (err) {
    ElMessage.error('发货失败');
  }
};

const handleConfirm = async (orderId: number) => {
  if (!user.value || !isBuyer.value) return;
  try {
    await api.confirmReceive(orderId, user.value.id);
    ElMessage.success('确认收货成功');
    loadBuyerOrders();
  } catch (err) {
    ElMessage.error('确认收货失败');
  }
};

const goToPayment = (orderId: number) => {
  router.push(`/payment/${orderId}`);
};

const showCommentDialog = ref(false);
const currentOrder = ref<any>(null);
const commentForm = ref({
  content: '',
  rating: 5,
  isAnonymous: 0
});

const showRefundDialog = ref(false);
const refundForm = ref({
  orderId: 0,
  remark: ''
});

const showRejectDialog = ref(false);
const rejectForm = ref({
  orderId: 0,
  remark: ''
});
const uploadedFiles = ref<any[]>([]);
const commentSubmitting = ref(false);

const showDisputeDialog = ref(false);
const disputeForm = ref({
  orderId: 0,
  content: '',
  images: [] as string[]
});
const disputeUploading = ref(false);
const disputeMap = ref<Record<number, any>>({});

const openDisputeDialog = (orderId: number) => {
  disputeForm.value = { orderId, content: '', images: [] };
  showDisputeDialog.value = true;
};

const handleDisputeFileUpload = async (event: Event) => {
  const input = event.target as HTMLInputElement;
  const files = input.files;
  if (!files || files.length === 0) return;
  
  disputeUploading.value = true;
  try {
    const file = files[0];
    const res = await api.uploadImage(file);
    if (res.data.code === 0) {
      disputeForm.value.images.push(res.data.data);
      ElMessage.success('图片上传成功');
    } else {
      ElMessage.error('图片上传失败');
    }
  } catch (err) {
    ElMessage.error('图片上传失败');
  } finally {
    disputeUploading.value = false;
    input.value = '';
  }
};

const removeDisputeImage = (index: number) => {
  disputeForm.value.images.splice(index, 1);
};

const handleCreateDispute = async () => {
  if (!user.value || !isBuyer.value) return;
  if (!disputeForm.value.content.trim()) {
    ElMessage.warning('请描述纠纷内容');
    return;
  }
  
  try {
    await api.createDispute(
      disputeForm.value.orderId,
      user.value.id,
      disputeForm.value.content,
      disputeForm.value.images.join(',')
    );
    ElMessage.success('平台介入申请已提交');
    showDisputeDialog.value = false;
    loadBuyerOrders();
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.msg || '申请失败');
  }
};

const loadDisputeByOrderId = async (orderId: number) => {
  if (disputeMap.value[orderId] !== undefined) return;
  try {
    const res = await api.getDisputeByOrderId(orderId);
    if (res.data.code === 0) {
      disputeMap.value[orderId] = res.data.data || null;
    }
  } catch (err) {
    console.error('加载纠纷信息失败', err);
  }
};

const getDisputeStatusLabel = (status: number) => {
  const statusMap: Record<number, string> = {
    1: '买家申请平台介入',
    2: '管理员处理中',
    3: '判买家退货',
    4: '判卖家退款'
  };
  return statusMap[status] || '';
};

const canApplyDispute = (order: any) => {
  return order.afterSaleStatus === 5 && !disputeMap.value[order.id];
};

const showDisputeDetailDialog = ref(false);
const disputeDetailOrder = ref<any>(null);
const sellerReplyForm = ref({
  content: '',
  images: [] as string[]
});
const sellerReplyUploading = ref(false);

const openDisputeDetail = async (orderId: number) => {
  disputeDetailOrder.value = { id: orderId };
  await loadDisputeByOrderId(orderId);
  showDisputeDetailDialog.value = true;
};

const parseDisputeImages = (imagesStr: string) => {
  if (!imagesStr) return [];
  return imagesStr.split(',').filter((url: string) => url.trim());
};

const canShowDisputeDetail = (order: any) => {
  const dispute = disputeMap.value[order.id];
  return dispute !== undefined && dispute !== null;
};

const handleSellerReplyFileUpload = async (event: Event) => {
  const input = event.target as HTMLInputElement;
  const files = input.files;
  if (!files || files.length === 0) return;
  
  sellerReplyUploading.value = true;
  try {
    const file = files[0];
    const res = await api.uploadImage(file);
    if (res.data.code === 0) {
      sellerReplyForm.value.images.push(res.data.data);
      ElMessage.success('图片上传成功');
    } else {
      ElMessage.error('图片上传失败');
    }
  } catch (err) {
    ElMessage.error('图片上传失败');
  } finally {
    sellerReplyUploading.value = false;
    input.value = '';
  }
};

const removeSellerReplyImage = (index: number) => {
  sellerReplyForm.value.images.splice(index, 1);
};

const handleSellerReply = async () => {
  if (!user.value || !disputeDetailOrder.value) return;
  if (!sellerReplyForm.value.content.trim() && sellerReplyForm.value.images.length === 0) {
    ElMessage.warning('请填写回复内容或上传图片');
    return;
  }
  
  try {
    const dispute = disputeMap.value[disputeDetailOrder.value.id];
    if (!dispute) {
      ElMessage.error('纠纷信息不存在');
      return;
    }
    
    await api.sellerReplyDispute(
      dispute.id,
      sellerReplyForm.value.content,
      sellerReplyForm.value.images.join(',')
    );
    ElMessage.success('回复已提交');
    sellerReplyForm.value = { content: '', images: [] };
    await loadDisputeByOrderId(disputeDetailOrder.value.id);
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.msg || '回复失败');
  }
};

const handleBuyerContinueDispute = async () => {
  if (!user.value || !disputeDetailOrder.value || !sellerReplyForm.value.content.trim()) {
    ElMessage.warning('请填写补充内容');
    return;
  }
  
  try {
    const dispute = disputeMap.value[disputeDetailOrder.value.id];
    if (!dispute) {
      ElMessage.error('纠纷信息不存在');
      return;
    }
    
    const newContent = dispute.buyerContent + '\n\n【买家补充】' + sellerReplyForm.value.content;
    const newImages = dispute.buyerImages 
      ? dispute.buyerImages + ',' + sellerReplyForm.value.images.join(',')
      : sellerReplyForm.value.images.join(',');
    
    await api.createDispute(
      disputeDetailOrder.value.id,
      user.value.id,
      newContent,
      newImages
    );
    ElMessage.success('补充内容已提交');
    sellerReplyForm.value = { content: '', images: [] };
    await loadDisputeByOrderId(disputeDetailOrder.value.id);
    loadBuyerOrders();
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.msg || '提交失败');
  }
};

const openRefundDialog = (orderId: number) => {
  refundForm.value = { orderId, remark: '' };
  showRefundDialog.value = true;
};

const handleApplyRefund = async () => {
  if (!user.value || !isBuyer.value) return;
  try {
    await api.applyRefund(refundForm.value.orderId, user.value.id, refundForm.value.remark);
    ElMessage.success('退货申请已提交');
    showRefundDialog.value = false;
    loadBuyerOrders();
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.msg || '申请失败');
  }
};

const openRejectDialog = (orderId: number) => {
  rejectForm.value = { orderId, remark: '' };
  showRejectDialog.value = true;
};

const handleApproveRefund = async (orderId: number) => {
  if (!user.value || !isSeller.value) return;
  try {
    await api.approveRefund(orderId, user.value.id);
    ElMessage.success('已同意退货');
    loadSellerOrders();
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.msg || '操作失败');
  }
};

const handleRejectRefund = async () => {
  if (!user.value || !isSeller.value) return;
  try {
    await api.rejectRefund(rejectForm.value.orderId, user.value.id, rejectForm.value.remark);
    ElMessage.success('已拒绝退货');
    showRejectDialog.value = false;
    loadSellerOrders();
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.msg || '操作失败');
  }
};

const handleConfirmShipRefund = async (orderId: number) => {
  if (!user.value || !isBuyer.value) return;
  try {
    await api.confirmShipRefund(orderId, user.value.id);
    ElMessage.success('已确认发货');
    loadBuyerOrders();
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.msg || '操作失败');
  }
};

const handleConfirmReceiveRefund = async (orderId: number) => {
  if (!user.value || !isSeller.value) return;
  try {
    await api.confirmReceiveRefund(orderId, user.value.id);
    ElMessage.success('已确认收货，退款成功');
    loadSellerOrders();
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.msg || '操作失败');
  }
};

const handleCancelRefund = async (orderId: number) => {
  if (!user.value || !isBuyer.value) return;
  try {
    await api.cancelRefund(orderId, user.value.id);
    ElMessage.success('已取消退货申请');
    loadBuyerOrders();
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.msg || '操作失败');
  }
};

const canApplyRefund = (order: any) => {
  return (order.status === 1 || order.status === 2) && (!order.afterSaleStatus || order.afterSaleStatus === 0 || order.afterSaleStatus === 5);
};

const openCommentDialog = (order: any) => {
  currentOrder.value = order;
  commentForm.value = { content: '', rating: 5, isAnonymous: 0 };
  uploadedFiles.value = [];
  showCommentDialog.value = true;
};

const handleFileUpload = (event: Event) => {
  const input = event.target as HTMLInputElement;
  const files = input.files;
  if (files && files.length > 0) {
    for (let i = 0; i < files.length; i++) {
      const file = files[i];
      const isVideo = file.type.startsWith('video/');
      const reader = new FileReader();
      reader.onload = (e: ProgressEvent<FileReader>) => {
        uploadedFiles.value.push({
          name: file.name,
          url: e.target?.result as string,
          type: isVideo ? 'video' : 'image',
          raw: file
        });
      };
      reader.readAsDataURL(file);
    }
  }
  input.value = '';
};

const removeFile = (index: number) => {
  uploadedFiles.value.splice(index, 1);
};

const uploadCommentMedia = async (fileItem: any) => {
  const uploadRes = fileItem.type === 'video'
    ? await api.uploadVideo(fileItem.raw)
    : await api.uploadImage(fileItem.raw);
  if (uploadRes.data.code !== 0 || !uploadRes.data.data) {
    throw new Error(uploadRes.data.msg || '媒体上传失败');
  }
  return {
    mediaUrl: uploadRes.data.data,
    mediaType: fileItem.type
  };
};

const handleComment = async () => {
  if (!commentForm.value.content.trim() && uploadedFiles.value.length === 0) {
    ElMessage.warning('请输入评论内容或上传图片/视频');
    return;
  }
  commentSubmitting.value = true;
  try {
    const goods = goodsMap.value[currentOrder.value.goodsId];
    const media = [];
    for (const fileItem of uploadedFiles.value) {
      media.push(await uploadCommentMedia(fileItem));
    }
    const commentData = {
      orderId: currentOrder.value.id,
      goodsId: currentOrder.value.goodsId,
      userId: user.value.id,
      sellerId: goods.sellerId,
      content: commentForm.value.content,
      rating: commentForm.value.rating,
      isAnonymous: commentForm.value.isAnonymous,
      media
    };
    await api.createComment(commentData);
    ElMessage.success('评论成功');
    showCommentDialog.value = false;
    commentForm.value = { content: '', rating: 5, isAnonymous: 0 };
    uploadedFiles.value = [];
    currentOrder.value = null;
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.msg || e?.message || '评论失败');
  } finally {
    commentSubmitting.value = false;
  }
};

onMounted(() => {
  // 根据用户角色设置默认显示的标签页
  if (isBuyer.value) {
    activeTab.value = 'buyer';
    loadBuyerOrders();
  } else if (isSeller.value) {
    activeTab.value = 'seller';
    loadSellerOrders();
  }
});
</script>

<template>
  <div class="orders-page">
    <h2>我的订单</h2>
    
    <div class="tabs">
      <button
        v-for="tab in tabs"
        :key="tab.value"
        :class="['tab', { active: activeTab === tab.value }]"
        @click="handleTabChange(tab.value)"
      >
        {{ tab.label }}
      </button>
    </div>

    <div class="status-filters">
      <button
        v-for="option in statusFilterOptions"
        :key="option.value"
        :class="['status-filter', { active: activeStatus === option.value }]"
        @click="activeStatus = option.value"
      >
        {{ option.label }}
        <span class="count">({{ getOrderCount(option.value) }})</span>
      </button>
    </div>
    
    <div class="orders-list">
      <div 
        v-for="order in currentOrders" 
        :key="order.id" 
        class="order-card"
      >
        <div class="order-header">
          <span class="order-id">订单号：{{ order.id }}</span>
          <div class="status-tags">
            <el-tag :type="statusMap[order.status]?.type || 'info'">
              {{ statusMap[order.status]?.label || '未知状态' }}
            </el-tag>
            <el-tag 
              v-if="order.afterSaleStatus && order.afterSaleStatus !== 0" 
              :type="afterSaleStatusMap[order.afterSaleStatus]?.type || 'warning'"
              class="after-sale-tag"
            >
              <span class="after-sale-icon">🔄</span>{{ afterSaleStatusMap[order.afterSaleStatus]?.label }}
            </el-tag>
            <el-tag 
              v-if="disputeMap[order.id]" 
              type="danger"
              class="after-sale-tag"
            >
              <span class="after-sale-icon">⚖️</span>{{ getDisputeStatusLabel(disputeMap[order.id].status) }}
            </el-tag>
          </div>
        </div>
        
        <div class="order-items">
          <div class="order-item">
            <img 
              v-if="goodsMap[order.goodsId]?.imageUrl" 
              :src="goodsMap[order.goodsId]?.imageUrl" 
              alt="商品图片" 
              class="item-image" 
            />
            <div class="item-info">
              <h4>{{ goodsMap[order.goodsId]?.title || '商品信息加载中...' }}</h4>
              <p class="goods-desc">{{ goodsMap[order.goodsId]?.description || '' }}</p>
              <p class="order-time">下单时间：{{ formatDateTime(order.createTime) }}</p>
            </div>
          </div>
        </div>
        
        <div class="order-footer">
          <span class="total">金额：<span class="amount">￥{{ order.amount }}</span></span>
          <div class="actions">
            <button 
              v-if="activeTab === 'buyer' && order.status === 0" 
              class="action-btn pay" 
              @click="goToPayment(order.id)"
            >
              去支付
            </button>
            <button 
              v-if="activeTab === 'buyer' && order.status === 0" 
              class="action-btn cancel" 
              @click="handleCancel(order.id)"
            >
              取消订单
            </button>
            <button 
              v-if="activeTab === 'buyer' && canApplyRefund(order)" 
              class="action-btn refund" 
              @click="openRefundDialog(order.id)"
            >
              申请退货
            </button>
            <button 
              v-if="activeTab === 'buyer' && order.afterSaleStatus === 1" 
              class="action-btn cancel" 
              @click="handleCancelRefund(order.id)"
            >
              取消退货
            </button>
            <button 
              v-if="activeTab === 'buyer' && order.afterSaleStatus === 2" 
              class="action-btn confirm" 
              @click="handleConfirmShipRefund(order.id)"
            >
              确认发货（退货）
            </button>
            <button 
              v-if="activeTab === 'buyer' && canApplyDispute(order)" 
              class="action-btn danger" 
              @click="openDisputeDialog(order.id)"
            >
              平台介入
            </button>
            <button 
              v-if="activeTab === 'seller' && order.status === 1" 
              class="action-btn ship" 
              @click="handleShip(order.id)"
            >
              确认发货
            </button>
            <button 
              v-if="activeTab === 'buyer' && order.status === 2 && (!order.afterSaleStatus || order.afterSaleStatus === 0 || order.afterSaleStatus === 5)" 
              class="action-btn confirm" 
              @click="handleConfirm(order.id)"
            >
              确认收货
            </button>
            <button 
              v-if="activeTab === 'buyer' && order.status === 3" 
              class="action-btn comment" 
              @click="openCommentDialog(order)"
            >
              发表评论
            </button>
            <button 
              v-if="activeTab === 'seller' && order.afterSaleStatus === 1" 
              class="action-btn approve" 
              @click="handleApproveRefund(order.id)"
            >
              同意退货
            </button>
            <button 
              v-if="activeTab === 'seller' && order.afterSaleStatus === 1" 
              class="action-btn reject" 
              @click="openRejectDialog(order.id)"
            >
              拒绝退货
            </button>
            <button 
              v-if="activeTab === 'seller' && order.afterSaleStatus === 3" 
              class="action-btn approve" 
              @click="handleConfirmReceiveRefund(order.id)"
            >
              确认收货（退款）
            </button>
            <button 
              v-if="activeTab === 'seller' && order.afterSaleStatus === 3" 
              class="action-btn reject" 
              @click="openRejectDialog(order.id)"
            >
              拒绝退款
            </button>
            <button 
              v-if="canShowDisputeDetail(order)" 
              class="action-btn dispute" 
              @click="openDisputeDetail(order.id)"
            >
              售后详情
            </button>
          </div>
        </div>
      </div>
      
      <div 
        v-if="currentOrders.length === 0" 
        class="empty-state"
      >
        <p>暂无订单</p>
      </div>
    </div>
  </div>

  <el-dialog v-model="showRefundDialog" title="申请退货">
      <div class="refund-form">
        <el-input
          v-model="refundForm.remark"
          type="textarea"
          :rows="3"
          placeholder="请输入退货原因（选填）"
        />
      </div>
      <template #footer>
        <el-button @click="showRefundDialog = false">取消</el-button>
        <el-button type="primary" @click="handleApplyRefund">提交申请</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showRejectDialog" title="拒绝退货/退款">
      <div class="reject-form">
        <el-input
          v-model="rejectForm.remark"
          type="textarea"
          :rows="3"
          placeholder="请输入拒绝原因"
        />
      </div>
      <template #footer>
        <el-button @click="showRejectDialog = false">取消</el-button>
        <el-button type="danger" @click="handleRejectRefund">确认拒绝</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showCommentDialog" title="发表评论">
    <div class="comment-form">
      <div class="form-item">
        <span>评分：</span>
        <el-rate v-model="commentForm.rating" />
      </div>
      <div class="form-item">
        <el-input
          v-model="commentForm.content"
          type="textarea"
          :rows="4"
          placeholder="请输入评论内容"
        />
      </div>
      <div class="form-item">
        <span>图片/视频：</span>
        <div class="upload-area">
          <label class="upload-btn">
            <input type="file" multiple accept="image/*,video/*" @change="handleFileUpload" />
            <span>+ 添加图片/视频</span>
          </label>
        </div>
        <div v-if="uploadedFiles.length > 0" class="uploaded-files">
          <div v-for="(file, index) in uploadedFiles" :key="index" class="uploaded-item">
            <img v-if="file.type === 'image'" :src="file.url" :alt="file.name" class="preview-image" />
            <video v-else :src="file.url" class="preview-video" controls muted />
            <button class="remove-btn" @click="removeFile(index)">×</button>
          </div>
        </div>
      </div>
    </div>
    <template #footer>
      <el-button @click="showCommentDialog = false">取消</el-button>
      <el-button type="primary" :loading="commentSubmitting" @click="handleComment">提交评论</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="showDisputeDialog" title="申请平台介入" width="500px">
    <div class="dispute-form">
      <div class="form-item">
        <span>纠纷描述：</span>
        <el-input 
          v-model="disputeForm.content" 
          type="textarea" 
          :rows="4" 
          placeholder="请描述纠纷内容..."
        />
      </div>
      <div class="form-item">
        <span>图片证据：</span>
        <div class="upload-area">
          <label class="upload-btn">
            <input type="file" accept="image/*" @change="handleDisputeFileUpload" :disabled="disputeUploading" />
            <span>{{ disputeUploading ? '上传中...' : '+ 添加图片' }}</span>
          </label>
        </div>
        <div v-if="disputeForm.images.length > 0" class="uploaded-files">
          <div v-for="(url, index) in disputeForm.images" :key="index" class="uploaded-item">
            <img :src="url" :alt="`图片${index + 1}`" class="preview-image" />
            <button class="remove-btn" @click="removeDisputeImage(index)">×</button>
          </div>
        </div>
      </div>
    </div>
    <template #footer>
      <el-button @click="showDisputeDialog = false">取消</el-button>
      <el-button type="primary" :loading="disputeUploading" @click="handleCreateDispute">提交</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="showDisputeDetailDialog" title="售后详情" width="600px">
    <div v-if="disputeMap[disputeDetailOrder?.id]" class="dispute-detail">
      <div class="dispute-section buyer-section">
        <h4>买家申诉 <span class="badge">买家</span></h4>
        <p class="content">{{ disputeMap[disputeDetailOrder?.id].buyerContent || '无描述' }}</p>
        <div v-if="parseDisputeImages(disputeMap[disputeDetailOrder?.id].buyerImages).length > 0" class="image-list">
          <el-image 
            v-for="(url, index) in parseDisputeImages(disputeMap[disputeDetailOrder?.id].buyerImages)" 
            :key="'buyer-' + index"
            :src="url"
            :preview-src-list="parseDisputeImages(disputeMap[disputeDetailOrder?.id].buyerImages)"
            fit="cover"
            class="dispute-image"
          />
        </div>
      </div>
      
      <div v-if="disputeMap[disputeDetailOrder?.id].sellerReply" class="dispute-section seller-section">
        <h4>卖家回复 <span class="badge">卖家</span></h4>
        <p class="content">{{ disputeMap[disputeDetailOrder?.id].sellerReply }}</p>
        <div v-if="parseDisputeImages(disputeMap[disputeDetailOrder?.id].sellerImages).length > 0" class="image-list">
          <el-image 
            v-for="(url, index) in parseDisputeImages(disputeMap[disputeDetailOrder?.id].sellerImages)" 
            :key="'seller-' + index"
            :src="url"
            :preview-src-list="parseDisputeImages(disputeMap[disputeDetailOrder?.id].sellerImages)"
            fit="cover"
            class="dispute-image"
          />
        </div>
      </div>
      
      <div v-if="disputeMap[disputeDetailOrder?.id].adminDecision" class="dispute-section admin-section">
        <h4>平台判决 <span class="badge success">已判决</span></h4>
        <p class="content">{{ disputeMap[disputeDetailOrder?.id].adminDecision }}</p>
        <p v-if="disputeMap[disputeDetailOrder?.id].adminRemark" class="remark">
          备注：{{ disputeMap[disputeDetailOrder?.id].adminRemark }}
        </p>
      </div>
      
      <div v-if="!disputeMap[disputeDetailOrder?.id].adminDecision" class="dispute-section reply-section">
        <h4>{{ activeTab === 'seller' ? '提交回复' : '补充内容' }}</h4>
        <el-input 
          v-model="sellerReplyForm.content" 
          type="textarea" 
          :rows="3" 
          :placeholder="activeTab === 'seller' ? '请填写回复内容...' : '请补充纠纷说明...'"
        />
        <div class="upload-area-inline">
          <label class="upload-btn-small">
            <input type="file" accept="image/*" @change="handleSellerReplyFileUpload" :disabled="sellerReplyUploading" />
            <span>{{ sellerReplyUploading ? '上传中...' : '+ 添加图片' }}</span>
          </label>
        </div>
        <div v-if="sellerReplyForm.images.length > 0" class="image-list">
          <div v-for="(url, index) in sellerReplyForm.images" :key="'reply-' + index" class="image-item">
            <img :src="url" alt="图片" class="dispute-image-small" />
            <button class="remove-btn-small" @click="removeSellerReplyImage(index)">×</button>
          </div>
        </div>
        <div class="reply-actions">
          <el-button 
            v-if="activeTab === 'seller'" 
            type="primary" 
            size="small"
            @click="handleSellerReply"
          >
            提交回复
          </el-button>
          <el-button 
            v-if="activeTab === 'buyer'" 
            type="warning" 
            size="small"
            @click="handleBuyerContinueDispute"
          >
            补充内容
          </el-button>
        </div>
      </div>
    </div>
    <template #footer>
      <el-button @click="showDisputeDetailDialog = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.orders-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

h2 {
  text-align: center;
  margin-bottom: 30px;
}

.tabs {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}

.tab {
  padding: 8px 20px;
  border: 1px solid #e4e7ed;
  border-radius: 20px;
  background-color: #fff;
  cursor: pointer;
  transition: all 0.2s;
}

.tab:hover {
  border-color: #409eff;
}

.tab.active {
  background-color: #409eff;
  color: white;
  border-color: #409eff;
}

.status-filters {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.status-filter {
  padding: 6px 14px;
  border: 1px solid #e4e7ed;
  border-radius: 16px;
  background-color: #fff;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 13px;
}

.status-filter:hover {
  border-color: #409eff;
  color: #409eff;
}

.status-filter.active {
  background-color: #409eff;
  color: white;
  border-color: #409eff;
}

.status-filter .count {
  font-size: 12px;
  opacity: 0.8;
}

.orders-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.order-card {
  background-color: #fff;
  border-radius: 8px;
  padding: 15px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 10px;
  border-bottom: 1px solid #e4e7ed;
  margin-bottom: 15px;
}

.status-tags {
  display: flex;
  gap: 8px;
  align-items: center;
}

.after-sale-tag {
  font-size: 12px;
}

.after-sale-icon {
  margin-right: 4px;
}

.order-id {
  font-family: monospace;
}

.order-items {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.order-item {
  display: flex;
  gap: 15px;
}

.item-image {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 4px;
}

.item-info {
  flex: 1;
}

.item-info h4 {
  margin: 0 0 8px 0;
  font-size: 14px;
}

.item-info p {
  margin: 5px 0;
  color: #666;
  font-size: 13px;
}

.goods-desc {
  color: #999 !important;
  font-size: 12px !important;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 400px;
}

.order-time {
  color: #999 !important;
  font-size: 12px !important;
}

.order-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 15px;
  border-top: 1px solid #e4e7ed;
  margin-top: 15px;
}

.total {
  font-size: 16px;
}

.amount {
  font-weight: bold;
  color: #f56c6c;
  font-size: 18px;
}

.actions {
  display: flex;
  gap: 10px;
}

.action-btn {
  padding: 6px 16px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.action-btn.pay {
  background-color: #f56c6c;
  color: white;
}

.action-btn.pay:hover {
  background-color: #f78989;
}

.action-btn.cancel {
  background-color: #f5f5f5;
  color: #666;
}

.action-btn.cancel:hover {
  background-color: #e4e7ed;
}

.action-btn.refund {
  background-color: #f56c6c;
  color: white;
}

.action-btn.refund:hover {
  background-color: #f78989;
}

.action-btn.approve {
  background-color: #67c23a;
  color: white;
}

.action-btn.approve:hover {
  background-color: #85ce61;
}

.action-btn.reject {
  background-color: #f56c6c;
  color: white;
}

.action-btn.reject:hover {
  background-color: #f78989;
}

.action-btn.ship {
  background-color: #67c23a;
  color: white;
}

.action-btn.ship:hover {
  background-color: #85ce61;
}

.action-btn.confirm {
  background-color: #409eff;
  color: white;
}

.action-btn.confirm:hover {
  background-color: #66b1ff;
}

.empty-state {
  text-align: center;
  padding: 60px;
  color: #999;
}

.upload-area {
  margin-top: 10px;
}

.upload-btn {
  display: inline-block;
  padding: 10px 20px;
  border: 2px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  color: #999;
  transition: all 0.2s;
}

.upload-btn:hover {
  border-color: #409eff;
  color: #409eff;
}

.upload-btn input {
  display: none;
}

.uploaded-files {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 10px;
}

.uploaded-item {
  position: relative;
  width: 100px;
  height: 100px;
}

.preview-image,
.preview-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 4px;
}

.video-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f5f5f5;
  border-radius: 4px;
  font-size: 12px;
  overflow: hidden;
}

.video-placeholder span {
  padding: 5px;
  text-align: center;
}

.remove-btn {
  position: absolute;
  top: -8px;
  right: -8px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background-color: #f56c6c;
  color: white;
  border: none;
  cursor: pointer;
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.dispute-detail {
  max-height: 500px;
  overflow-y: auto;
}

.dispute-section {
  padding: 15px;
  border-radius: 8px;
  margin-bottom: 15px;
  background-color: #f5f7fa;
}

.buyer-section {
  border-left: 4px solid #409eff;
}

.seller-section {
  border-left: 4px solid #67c23a;
}

.admin-section {
  border-left: 4px solid #e6a23c;
  background-color: #fdf6ec;
}

.reply-section {
  border-left: 4px solid #909399;
  background-color: #f4f4f5;
}

.dispute-section h4 {
  margin: 0 0 10px 0;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.badge {
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 4px;
  background-color: #409eff;
  color: white;
  font-weight: normal;
}

.badge.success {
  background-color: #67c23a;
}

.dispute-section .content {
  margin: 0;
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  white-space: pre-wrap;
}

.dispute-section .remark {
  margin: 8px 0 0 0;
  font-size: 13px;
  color: #909399;
}

.image-list {
  display: flex;
  gap: 8px;
  margin-top: 10px;
  flex-wrap: wrap;
}

.dispute-image {
  width: 60px;
  height: 60px;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
  cursor: pointer;
}

.dispute-image-small {
  width: 50px;
  height: 50px;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
}

.upload-area-inline {
  margin-top: 10px;
}

.upload-btn-small {
  display: inline-block;
  padding: 6px 12px;
  background-color: #409eff;
  color: white;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
}

.upload-btn-small input {
  display: none;
}

.image-item {
  position: relative;
  display: inline-block;
}

.remove-btn-small {
  position: absolute;
  top: -6px;
  right: -6px;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background-color: #f56c6c;
  color: white;
  border: none;
  cursor: pointer;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.reply-actions {
  margin-top: 15px;
  display: flex;
  gap: 10px;
}

.action-btn.dispute {
  background-color: #e6a23c;
  color: white;
  padding: 6px 16px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
}
</style>