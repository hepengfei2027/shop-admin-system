<script setup lang="ts">
import { ref, onMounted, computed, inject } from 'vue';
import { ElMessage, ElDialog, ElInput, ElButton, ElRate, ElSelect, ElOption, ElTag, ElImage } from 'element-plus';
import { api } from '../api';
import { useRouter } from 'vue-router';

const router = useRouter();
const user = inject('user', ref<any>(null));
const activeTab = ref('buyer');
const activeStatus = ref('all');
// 顶部分类标签
const activeFilterTab = ref('all');
const searchKeyword = ref('');
const buyerOrders = ref<any[]>([]);
const sellerOrders = ref<any[]>([]);

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
  4: { label: '已取消', type: 'danger' },
  5: { label: '退货中', type: 'warning' }
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
  { value: '2', label: '待收货' },
  { value: '3', label: '已完成' },
  { value: '4', label: '已取消' },
  { value: '5', label: '退货中' }
];

// 顶部订单分类标签配置
const topFilterTabs = [
  { value: 'all', label: '全部订单' },
  { value: '0', label: '待付款' },
  { value: '2', label: '待收货' },
  { value: '3', label: '待评价(已完成)' }
];

const currentOrders = computed(() => {
  let orders = activeTab.value === 'buyer' ? buyerOrders.value : sellerOrders.value;

  // 顶部标签筛选
  if (activeFilterTab.value !== 'all') {
    orders = orders.filter(order => order.status === Number(activeFilterTab.value));
  }

  // 状态下拉筛选
  if (activeStatus.value !== 'all') {
    orders = orders.filter(order => order.status === Number(activeStatus.value));
  }

  // 搜索过滤
  if (searchKeyword.value.trim()) {
    const keyword = searchKeyword.value.toLowerCase();
    orders = orders.filter(order => {
      const orderId = String(order.id).toLowerCase();
      const goodsName = (order.goodsName || '').toLowerCase();
      return orderId.includes(keyword) || goodsName.includes(keyword);
    });
  }

  return orders;
});

const getOrderCount = (status: string) => {
  let orders = activeTab.value === 'buyer' ? buyerOrders.value : sellerOrders.value;

  if (activeFilterTab.value !== 'all') {
    orders = orders.filter(order => order.status === Number(activeFilterTab.value));
  }

  if (status !== 'all') {
    orders = orders.filter(order => order.status === Number(status));
  }

  if (searchKeyword.value.trim()) {
    const keyword = searchKeyword.value.toLowerCase();
    orders = orders.filter(order => {
      const orderId = String(order.id).toLowerCase();
      const goodsName = (order.goodsName || '').toLowerCase();
      return orderId.includes(keyword) || goodsName.includes(keyword);
    });
  }

  return orders.length;
};

const handleSearch = () => {};

const formatDateTime = (dateStr: string) => {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');
  return `${year}-${month}-${day} ${hours}:${minutes}`;
};

const loadBuyerOrders = async () => {
  if (!user.value) return;
  try {
    const res = await api.listBuyerOrdersWithDetails(user.value.id);
    if (res.data.code === 0) {
      buyerOrders.value = res.data.data || [];
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
    const res = await api.listSellerOrdersWithDetails(user.value.id);
    if (res.data.code === 0) {
      sellerOrders.value = res.data.data || [];
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

const handleTabChange = (tab: string) => {
  if (tab === 'buyer' && !isBuyer.value) return;
  if (tab === 'seller' && !isSeller.value) return;

  activeTab.value = tab;
  activeFilterTab.value = 'all';
  activeStatus.value = 'all';
  searchKeyword.value = '';
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

const goToOrderDetail = (orderId: number) => {
  router.push(`/order/${orderId}`);
};

const goToTrash = () => {
  // 订单回收站路由自行替换
  router.push('/order-trash');
};

// ========== 评论弹窗 ==========
const showCommentDialog = ref(false);
const currentOrder = ref<any>(null);
const commentForm = ref({
  content: '',
  rating: 5,
  isAnonymous: 0
});
const uploadedFiles = ref<any[]>([]);
const commentSubmitting = ref(false);

// ========== 退货弹窗 ==========
const showRefundDialog = ref(false);
const refundForm = ref({
  orderId: 0,
  remark: ''
});

// ========== 拒绝退货弹窗 ==========
const showRejectDialog = ref(false);
const rejectForm = ref({
  orderId: 0,
  remark: ''
});

// ========== 平台介入弹窗 ==========
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

// 纠纷详情弹窗
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

// 【修复完成的函数，解决报错根源】
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
    const media = [];
    for (const fileItem of uploadedFiles.value) {
      media.push(await uploadCommentMedia(fileItem));
    }
    const commentData = {
      orderId: currentOrder.value.id,
      goodsId: currentOrder.value.goodsId,
      userId: user.value.id,
      sellerId: currentOrder.value.sellerId,
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
    <!-- 一、顶部标题栏 -->
    <div class="page-title-wrap">
      <h1 class="page-title">我的订单</h1>
    </div>

    <!-- 分割浅灰通栏 -->
    <div class="split-bar"></div>

    <!-- 二、筛选导航功能区 左右布局 -->
    <div class="filter-wrap">
      <div class="filter-left">
        <!-- 分类标签 -->
        <div class="top-tabs">
        <span
            v-for="tab in topFilterTabs"
            :key="tab.value"
            class="tab-item"
            :class="{ active: activeFilterTab === tab.value }"
            @click="activeFilterTab = tab.value"
        >
          {{ tab.label }}
        </span>
        </div>
        <span class="trash-link" @click="goToTrash"></span>
      </div>
      <div class="filter-right">
        <el-input
            v-model="searchKeyword"
            placeholder="商品名称 / 商品编号 / 订单号"
            clearable
            class="search-input"
            @keyup.enter="handleSearch"
        >
          <template #prefix>
            <i class="el-icon-search"></i>
          </template>
        </el-input>
      </div>
    </div>

    <!-- 三、列表表头通栏 -->
    <div class="table-header-row">
      <div class="col col-goods">订单详情</div>
      <div class="col col-receiver">收货人</div>
      <div class="col col-amount">金额</div>
      <div class="col col-status">
        状态
        <el-select v-model="activeStatus" size="small" class="status-select">
          <el-option v-for="opt in statusFilterOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
        </el-select>
      </div>
      <div class="col col-action">操作</div>
    </div>

    <!-- 四、订单列表主体 块状订单 -->
    <div class="order-list">
      <div v-if="currentOrders.length === 0" class="empty-tip">暂无订单</div>

      <div v-for="order in currentOrders" :key="order.id" class="order-block">
        <!-- 订单头部信息行 -->
        <div class="order-head">
          <div class="head-col head-goods">
            <span class="time">{{ formatDateTime(order.createTime) }}</span>
            <span class="order-id">订单号：{{ order.id }}</span>
          </div>
        </div>

        <!-- 商品明细行 -->
        <div class="goods-row">
          <div class="goods-info" @click="router.push(`/goods/${order.goodsId}`)">
            <el-image
                v-if="order.goodsImage"
                :src="order.goodsImage"
                fit="cover"
                class="goods-img"
            />
            <div class="goods-right">
              <h3 class="goods-name">{{ order.goodsName || '商品信息加载中...' }}</h3>
              <p class="goods-desc">{{ order.goodsDescription || '-' }}</p>
              <span class="goods-num">x{{ order.quantity || 1 }}</span>
            </div>
          </div>
          <div class="head-col head-receiver">
            <i class="el-icon-user"></i>
            {{ order.addressName || '-' }}
          </div>
          <div class="head-col head-amount">
            <span class="price">¥{{ order.amount.toFixed(2) }}</span>
            <span class="pay-type">在线支付</span>
          </div>
          <div class="head-col head-status">
            <el-tag v-if="order.promotionType === 3 && order.groupStatus === 0" type="warning" size="small">等待拼团</el-tag>
            <el-tag v-else :type="statusMap[order.status]?.type || 'info'" size="small">
              {{ statusMap[order.status]?.label || '未知' }}
            </el-tag>
            <el-tag v-if="order.promotionType === 1" type="success" size="small">满减</el-tag>
            <el-tag v-if="order.promotionType === 2" type="danger" size="small">折扣</el-tag>
            <el-tag v-if="order.promotionType === 3 && order.groupStatus === 1" type="success" size="small">团购</el-tag>
            <el-tag v-if="order.afterSaleStatus && order.afterSaleStatus !== 0" :type="afterSaleStatusMap[order.afterSaleStatus]?.type || 'warning'" size="small">
              {{ afterSaleStatusMap[order.afterSaleStatus]?.label }}
            </el-tag>
            <a class="detail-link" @click.stop="goToOrderDetail(order.id)">查看详情</a>
          </div>
          <div class="head-col head-action">
            <div class="actions">
              <button
                  v-if="activeTab === 'buyer' && order.status === 0"
                  class="action-btn pay"
                  @click.stop="goToPayment(order.id)"
              >去支付</button>
              <button
                  v-if="activeTab === 'buyer' && order.status === 0"
                  class="action-btn cancel"
                  @click.stop="handleCancel(order.id)"
              >取消</button>
              <button
                  v-if="activeTab === 'buyer' && canApplyRefund(order)"
                  class="action-btn refund"
                  @click.stop="openRefundDialog(order.id)"
              >退货</button>
              <button
                  v-if="activeTab === 'buyer' && order.afterSaleStatus === 1"
                  class="action-btn cancel"
                  @click.stop="handleCancelRefund(order.id)"
              >取消退货</button>
              <button
                  v-if="activeTab === 'seller' && order.afterSaleStatus === 1"
                  class="action-btn approve"
                  @click.stop="handleApproveRefund(order.id)"
              >同意退货</button>
              <button
                  v-if="activeTab === 'seller' && order.afterSaleStatus === 1"
                  class="action-btn reject"
                  @click.stop="openRejectDialog(order.id)"
              >拒绝</button>
              <button
                  v-if="activeTab === 'seller' && order.afterSaleStatus === 3"
                  class="action-btn approve"
                  @click.stop="handleConfirmReceiveRefund(order.id)"
              >确认退款</button>
              <button
                  v-if="activeTab === 'seller' && order.status === 1 && (!order.afterSaleStatus || order.afterSaleStatus === 0)"
                  class="action-btn ship"
                  @click.stop="handleShip(order.id)"
              >发货
              </button>
              <button
                  v-if="activeTab === 'buyer' && order.status === 2"
                  class="action-btn confirm"
                  @click.stop="handleConfirm(order.id)"
              >确认收货</button>
              <button
                  v-if="order.status === 3 && activeTab === 'buyer' && !order.hasCommented"
                  class="action-btn comment"
                  @click.stop="openCommentDialog(order)"
              >评价</button>
              <button
                  v-if="order.status === 3 && activeTab === 'buyer' && order.hasCommented"
                  class="action-btn detail"
                  disabled
              >已评价</button>
              <button
                  v-if="activeTab === 'buyer' && order.afterSaleStatus === 5 && disputeMap[order.id]?.status !== 1"
                  class="action-btn dispute"
                  @click.stop="openDisputeDialog(order.id)"
              >平台介入</button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 下方所有弹窗完全保留无修改 -->
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
  </div>
</template>

<style scoped>
/* 全局基础 */
.orders-page {
  background: #fff;
  padding: 24px;
  min-height: 100vh;
}

/* 一、顶部标题栏 */
.page-title-wrap {
  padding-bottom: 16px;
}
.page-title {
  font-size: 16px;
  font-weight: 500;
  color: #333;
  margin: 0;
}
.split-bar {
  height: 8px;
  background-color: #f5f5f5;
  margin: 0 -24px 16px;
}

/* 二、筛选导航区 左右布局 */
.filter-wrap {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  gap: 20px;
  flex-wrap: wrap;
}
.filter-left {
  display: flex;
  align-items: center;
  gap: 24px;
}
.top-tabs {
  display: flex;
  gap: 20px;
}
.tab-item {
  font-size: 14px;
  color: #666;
  cursor: pointer;
  padding: 6px 0;
  position: relative;
}
.tab-item.active {
  color: #e53935;
  font-weight: 500;
}
.tab-item.active::after {
  content: '';
  position: absolute;
  left: 0;
  bottom: 0;
  width: 100%;
  height: 2px;
  background: #e53935;
}
.trash-link {
  font-size: 13px;
  color: #999;
  cursor: pointer;
}
.trash-link:hover {
  color: #1890ff;
}
.filter-right .search-input {
  width: 320px;
}

/* 三、列表表头通栏 */
.table-header-row {
  display: grid;
  grid-template-columns: 42% 16% 14% 16% 12%;
  background: #f5f5f5;
  padding: 12px 16px;
  border-radius: 4px 4px 0 0;
}
.table-header-row .col {
  text-align: center;
  font-size: 14px;
  color: #333;
  font-weight: 500;
  border-right: 1px solid #d0d0d0;
}
.table-header-row .col:last-child {
  border-right: none;
}
.col-goods {
  text-align: left !important;
}
.status-select {
  width: 90px;
  margin-left: 6px;
}

/* 四、订单列表 */
.order-list {
  border-radius: 4px;
  margin-top: 12px;
}
.empty-tip {
  text-align: center;
  padding: 60px 0;
  color: #999;
}
.order-block {
  border: 1px solid #ccc;
  margin-bottom: 12px;
  border-radius: 4px;
}
.order-block:last-child {
  margin-bottom: 0;
}
/* 订单头部行 */
.order-head {
  display: flex;
  gap: 20px;
  padding: 6px 16px;
  background: #f0f0f0;
  border-bottom: 1px solid #ccc;
}
.head-goods {
  display: flex;
  flex-direction: row;
  gap: 16px;
  align-items: center;
}
.time {
  font-size: 12px;
  color: #999;
}
.order-id {
  font-size: 13px;
  color: #666;
}

/* 商品明细跨行区域 */
.goods-row {
  display: grid;
  grid-template-columns: 42% 15% 14% 14% 8%;
  align-items: stretch;
  padding: 0 16px;
  gap: 12px;
  cursor: pointer;
}
.goods-info {
  display: flex;
  align-items: center;
  gap: 12px;
  overflow: hidden;
  border-right: 1px solid #d0d0d0;
  padding: 20px 0;
}
.goods-img {
  width: 70px;
  height: 70px;
  border: 1px solid #eee;
  border-radius: 4px;
  flex-shrink: 0;
}
.goods-right {
  display: flex;
  flex-direction: column;
  gap: 4px;
  overflow: hidden;
}
.goods-name {
  font-size: 14px;
  margin: 0;
  color: #333;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.goods-desc {
  font-size: 12px;
  color: #999;
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.goods-num {
  font-size: 12px;
  color: #666;
}
.head-receiver {
  text-align: center;
  font-size: 14px;
  color: #333;
  border-right: 1px solid #d0d0d0;
  padding: 20px 0;
  display: flex;
  align-items: center;
  justify-content: center;
}
.head-amount {
  text-align: center;
  border-right: 1px solid #d0d0d0;
  padding: 20px 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.price {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  display: block;
}
.pay-type {
  font-size: 12px;
  color: #999;
}
.head-status {
  text-align: center;
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: center;
  border-right: 1px solid #d0d0d0;
  padding: 20px 0;
  justify-content: center;
}
.detail-link {
  font-size: 12px;
  color: #1890ff;
  cursor: pointer;
}
.head-action {
  text-align: right;
  padding: 20px 0;
  display: flex;
  align-items: center;
  justify-content: flex-end;
}
.actions {
  display: flex;
  flex-direction: column;
  gap: 6px;
  align-items: flex-end;
}
.action-btn {
  padding: 5px 10px;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  white-space: nowrap;
}
.action-btn.pay {
  background: #e53935;
  color: #fff;
  border-color: #e53935;
}
.action-btn.cancel {
  color: #666;
}
.action-btn.refund {
  background: #ff9800;
  color: #fff;
  border-color: #ff9800;
}
.action-btn.approve {
  background: #43a047;
  color: #fff;
  border-color: #43a047;
}
.action-btn.reject {
  background: #e53935;
  color: #fff;
  border-color: #e53935;
}
.action-btn.ship {
  background: #1e88e5;
  color: #fff;
  border-color: #1e88e5;
}
.action-btn.confirm {
  background: #43a047;
  color: #fff;
  border-color: #43a047;
}
.action-btn.comment {
  background: #7b1fa2;
  color: #fff;
  border-color: #7b1fa2;
}
.action-btn.dispute {
  background: #f57c00;
  color: #fff;
  border-color: #f57c00;
}
.action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}


.goods-img {
  width: 70px;
  height: 70px;
  border: 1px solid #eee;
  border-radius: 4px;
}
.goods-right {
  flex: 1;
}
.goods-name {
  font-size: 14px;
  margin: 0 0 4px;
  color: #333;
  font-weight: 500;
}
.goods-desc {
  font-size: 12px;
  color: #999;
  margin: 0 0 4px;
}
.goods-num {
  font-size: 12px;
  color: #666;
}

/* 弹窗样式复用原有，仅微调弹窗头部 */
:deep(.el-dialog__header) {
  background: #333;
}
:deep(.el-dialog__title) {
  color: #fff;
}
:deep(.el-dialog__close) {
  color: #fff;
}

/* 响应式适配 */
@media screen and (max-width: 768px) {
  .table-header-row, .order-head {
    grid-template-columns: 1fr;
    gap: 12px;
  }
  .table-header-row .col,
  .head-receiver, .head-amount, .head-status, .head-action {
    text-align: left !important;
  }
  .goods-row {
    padding-left: 16px;
  }
  .filter-wrap {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>