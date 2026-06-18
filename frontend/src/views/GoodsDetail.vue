<script setup lang="ts">
import { onMounted, ref, inject, watch, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { api } from '../api';
import { ElMessage, ElDialog, ElInput, ElButton, ElRate } from 'element-plus';

const route = useRoute();
const router = useRouter();
const goods = ref<any>(null);
const cartItems = ref<any[]>([]);
const user = inject('user', ref<any>(null));
const dialogVisible = ref(false);
const messageContent = ref('');
const seller = ref<any>(null);
const comments = ref<any[]>([]);
const userMap = ref<Record<number, any>>({});
const BASE_URL = 'http://localhost:8081';
const replyDialogVisible = ref(false);
const replyForm = ref({
  commentId: null as number | null,
  content: ''
});
const couponDialogVisible = ref(false);
const couponAmount = ref(10);
const availableCoupon = ref<any>(null);
const availableCoupons = ref<any[]>([]);
const claimedCouponIds = ref<number[]>([]);

const editDialogVisible = ref(false);
const editForm = ref<any>({
  id: '',
  title: '',
  description: '',
  imageUrl: '',
  price: 0,
  freight: 0,
  category: '',
  brandName: '',
  brandColor: '#ff1744'
});
const uploading = ref(false);

// 团购活动相关
const groupBuyActivities = ref<any[]>([]);
const groupBuyParticipants = ref<Record<number, any[]>>({});
const groupBuyDialogVisible = ref(false);
const hasGroupBuyOrder = ref(false);

// 限时折扣活动
const discountActivity = ref<any>(null);

// 满减活动
const fullReduceActivity = ref<any>(null);

const currentGroupBuy = computed(() => {
  if (!goods.value) return null;
  return groupBuyActivities.value.find(
    (act: any) => act.goodsId === goods.value.id && act.status === '进行中'
  ) || null;
});

const canJoinGroupBuy = computed(() => {
  if (!currentGroupBuy.value || !user.value) return false;
  if (hasGroupBuyOrder.value) return false;
  const participants = groupBuyParticipants.value[currentGroupBuy.value.id] || [];
  return !participants.some((p: any) => p.userId === user.value.id);
});

const isInGroupBuy = computed(() => {
  if (!currentGroupBuy.value || !user.value) return false;
  const participants = groupBuyParticipants.value[currentGroupBuy.value.id] || [];
  return participants.some((p: any) => p.userId === user.value.id);
});

const groupBuyProgress = computed(() => {
  if (!currentGroupBuy.value) return { current: 0, min: 0, percentage: 0 };
  const participants = groupBuyParticipants.value[currentGroupBuy.value.id] || [];
  const current = participants.length;
  const min = currentGroupBuy.value.minGroupSize;
  return {
    current,
    min,
    percentage: Math.min((current / min) * 100, 100)
  };
});

// 计算商品折扣价
const discountedPrice = computed(() => {
  if (!goods.value) return 0;
  if (discountActivity.value) {
    return (goods.value.price * (discountActivity.value.ruleDetail?.discountRate || 1)).toFixed(2);
  }
  return goods.value.price;
});

const loadPromotions = async () => {
  if (!goods.value?.id) return;
  try {
    const res = await api.getGoodsPromotions(goods.value.id);
    if (res.data.code === 0) {
      const activities = res.data.data || [];
      
      // 限时折扣活动
      const discountAct = activities.find((a: any) => a.type === 2 && a.status === 1);
      if (discountAct) {
        discountActivity.value = {
          ...discountAct,
          discountRate: discountAct.ruleDetail?.discountRate || 0.8
        };
      }
      
      // 满减活动
      const fullReduceAct = activities.find((a: any) => a.type === 1 && a.status === 1);
      if (fullReduceAct) {
        fullReduceActivity.value = {
          ...fullReduceAct,
          thresholdAmount: fullReduceAct.ruleDetail?.thresholdAmount || 100,
          reductionAmount: fullReduceAct.ruleDetail?.reductionAmount || 10
        };
      }
      
      // 团购活动
      const groupActivities = activities.filter((a: any) => a.type === 3);
      groupBuyActivities.value = groupActivities.map((a: any) => ({
        ...a,
        groupPrice: a.ruleDetail?.groupPrice || 0,
        minGroupSize: a.ruleDetail?.minGroupSize || 2,
        maxGroupSize: a.ruleDetail?.maxGroupSize || 10,
        currentSize: a.ruleDetail?.currentSize || 0,
        status: a.status === 1 ? '进行中' : '已结束'
      }));
      
      // 加载团购参与人数
      for (const activity of groupActivities) {
        try {
          const pRes = await api.getGroupParticipants(activity.id);
          if (pRes.data.code === 0) {
            groupBuyParticipants.value[activity.id] = pRes.data.data?.participants || [];
          }
        } catch (e) {
          console.error('加载团购参与人数失败', e);
        }
      }

      // 检查用户是否已有该商品的团购订单
      if (user.value && groupActivities.length > 0) {
        try {
          const orderRes = await api.listBuyerOrdersWithDetails(user.value.id);
          if (orderRes.data.code === 0) {
            const orders = orderRes.data.data || [];
            hasGroupBuyOrder.value = orders.some((order: any) =>
              order.goodsId === goods.value.id &&
              order.promotionType === 3 &&
              order.status !== 4
            );
          }
        } catch (e) {
          console.error('检查团购订单失败', e);
        }
      }
    }
  } catch (e) {
    console.error('加载营销活动失败', e);
  }
};

// 兼容旧函数名
const loadGroupBuyActivities = loadPromotions;

const joinGroupBuy = async () => {
  if (!user.value) {
    ElMessage.warning('请先登录');
    router.push('/login');
    return;
  }
  if (!currentGroupBuy.value) return;

  try {
    const res = await api.joinGroupBuy({
      activityId: currentGroupBuy.value.id,
      userId: user.value.id,
      username: user.value.username || user.value.nickname || '用户'
    });
    
    if (res.data.code === 0) {
      const data = res.data.data;
      // 重新加载团购活动
      await loadGroupBuyActivities();
      
      if (data.isSuccess) {
        ElMessage.success(`团购成功！当前 ${data.currentSize}/${data.minGroupSize} 人，已达到成团人数`);
      } else {
        ElMessage.success(`已加入团购！当前 ${data.currentSize}/${data.minGroupSize} 人`);
      }
    } else {
      ElMessage.warning(res.data.msg || '加入团购失败');
    }
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.msg || '加入团购失败');
  }
};

const onGroupBuyBuy = () => {
  if (!user.value) {
    ElMessage.warning('请先登录');
    router.push('/login');
    return;
  }
  if (!currentGroupBuy.value) return;

  const actId = currentGroupBuy.value.id;
  const participants = groupBuyParticipants.value[actId] || [];

  if (participants.length < currentGroupBuy.value.minGroupSize) {
    ElMessage.warning(`团购人数不足，当前 ${participants.length}/${currentGroupBuy.value.minGroupSize} 人，未达到成团条件`);
    return;
  }

  if (!participants.some((p: any) => p.userId === user.value.id)) {
    ElMessage.warning('您需要先加入团购才能下单');
    return;
  }

  // 跳转到确认订单页面，带上团购参数
  router.push(`/confirm-order?goodsId=${goods.value.id}&groupBuyId=${currentGroupBuy.value.id}&groupPrice=${currentGroupBuy.value.groupPrice}`);
};

const categories = [
  { label: '数码电子', value: '数码电子' },
  { label: '服装服饰', value: '服装服饰' },
  { label: '美妆护肤', value: '美妆护肤' },
  { label: '家居日用', value: '家居日用' },
  { label: '食品饮料', value: '食品饮料' },
  { label: '图书文具', value: '图书文具' },
  { label: '运动户外', value: '运动户外' },
  { label: '其他', value: '其他' }
];

const brandColors = [
  { label: '红色', value: '#ff1744' },
  { label: '粉色', value: '#ff4081' },
  { label: '紫色', value: '#e040fb' },
  { label: '蓝色', value: '#448aff' },
  { label: '青色', value: '#18ffff' },
  { label: '绿色', value: '#00e676' },
  { label: '黄色', value: '#ffea00' },
  { label: '橙色', value: '#ff9100' }
];

const getMemberPrice = (price: number) => {
  const discount = (user.value?.discount !== undefined && user.value?.discount !== null) ? user.value.discount : 1;
  return (price * discount).toFixed(2);
};

const showMemberPrice = computed(() => {
  const discount = user.value?.discount;
  return discount !== undefined && discount !== null && discount < 1;
});

const getCartKey = () => {
  if (user.value) {
    return `cart_${user.value.id}`;
  }
  return 'cart';
};

const loadCart = () => {
  const cartStr = localStorage.getItem(getCartKey());
  if (cartStr) {
    cartItems.value = JSON.parse(cartStr);
  } else {
    cartItems.value = [];
  }
};

watch(user, () => {
  loadCart();
}, { deep: true });

const loadDetail = async () => {
  loadCart();

  const id = Number(route.params.id);
  const res = await api.listGoods();
  const list = res.data.data || [];
  goods.value = list.find((item: any) => item.id === id);

  if (goods.value && goods.value.sellerId) {
    try {
      const sellerRes = await api.userDetail(goods.value.sellerId);
      if (sellerRes.data.code === 0) {
        seller.value = sellerRes.data.data;
      }
    } catch (err) {
      console.error('获取卖家信息失败', err);
    }
  }

  // 在商品数据加载后再获取营销活动信息
  if (goods.value) {
    await loadGroupBuyActivities();
  }

  await loadComments();
};

const mediaMap = ref<Record<number, any[]>>({});

const resolveMediaUrl = (url?: string) => {
  if (!url) return '';
  if (url.startsWith('http://') || url.startsWith('https://') || url.startsWith('data:')) {
    return url;
  }
  if (url.startsWith('/')) {
    return BASE_URL + url;
  }
  return `${BASE_URL}/${url}`;
};

const isImageMedia = (media: any) => {
  const type = (media.mediaType || '').toLowerCase();
  if (type.includes('image')) return true;
  const url = media.mediaUrl || '';
  return /^data:image\//i.test(url) || /\.(jpg|jpeg|png|gif|bmp|webp)(\?.*)?$/i.test(url);
};

const isVideoMedia = (media: any) => {
  const type = (media.mediaType || '').toLowerCase();
  if (type.includes('video')) return true;
  const url = media.mediaUrl || '';
  return /^data:video\//i.test(url) || /\.(mp4|webm|ogg|mov)(\?.*)?$/i.test(url);
};

const loadComments = async () => {
  if (!goods.value) return;
  try {
    const res = await api.getGoodsComments(goods.value.id);
    if (res.data.code === 0) {
      comments.value = res.data.data || [];
      const userIds = comments.value.map(c => c.userId);
      const uniqueIds = [...new Set(userIds)];
      for (const uid of uniqueIds) {
        if (!userMap.value[uid]) {
          const userRes = await api.userDetail(uid);
          if (userRes.data.code === 0) {
            userMap.value[uid] = userRes.data.data;
          }
        }
      }
      mediaMap.value = {};
      for (const comment of comments.value) {
        if (comment.media && comment.media.length > 0) {
          mediaMap.value[comment.id] = comment.media;
        } else {
          try {
            const mediaRes = await api.getCommentMedia(comment.id);
            if (mediaRes.data.code === 0 && mediaRes.data.data?.length > 0) {
              mediaMap.value[comment.id] = mediaRes.data.data;
            }
          } catch {
            // ignore missing media
          }
        }
      }
    }
  } catch (err) {
    console.error('加载评论失败', err);
  }
};

const onBuy = async () => {
  if (!user.value) {
    ElMessage.warning('请先登录');
    router.push('/login');
    return;
  }

  // 跳转到确认订单页面
  router.push(`/confirm-order?goodsId=${goods.value.id}`);
};

const addToCart = () => {
  if (!user.value) {
    ElMessage.warning('请先登录');
    return;
  }

  const existingItem = cartItems.value.find(item => item.id === goods.value.id);
  if (existingItem) {
    existingItem.quantity += 1;
  } else {
    cartItems.value.push({
      id: goods.value.id,
      title: goods.value.title,
      price: goods.value.price,
      imageUrl: goods.value.imageUrl,
      quantity: 1
    });
  }

  localStorage.setItem(getCartKey(), JSON.stringify(cartItems.value));
  ElMessage.success('已加入购物车');
};

const openChat = () => {
  if (!user.value) {
    ElMessage.warning('请先登录');
    return;
  }
  if (user.value.id === goods.value.sellerId) {
    ElMessage.warning('不能给自己发消息');
    return;
  }
  dialogVisible.value = true;
};

const sendMessage = async () => {
  if (!messageContent.value.trim()) {
    ElMessage.warning('请输入消息内容');
    return;
  }
  try {
    await api.sendMessage(user.value.id, goods.value.sellerId, messageContent.value);
    ElMessage.success('消息已发送');
    dialogVisible.value = false;
    messageContent.value = '';
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.msg || '发送失败');
  }
};

const getUserName = (userId: number) => {
  if (userMap.value[userId]) {
    return userMap.value[userId].nickname || userMap.value[userId].username;
  }
  return '匿名用户';
};

const getUserAvatar = (userId: number) => {
  if (userMap.value[userId]?.avatar) {
    return userMap.value[userId].avatar;
  }
  return '';
};

const getCommentMedia = (commentId: number) => {
  const mediaList = mediaMap.value[commentId] || [];
  return mediaList.map((media: any) => ({
    ...media,
    fullUrl: resolveMediaUrl(media.mediaUrl)
  }));
};

const openReplyDialog = (commentId: number) => {
  replyForm.value.commentId = commentId;
  replyForm.value.content = '';
  replyDialogVisible.value = true;
};

const handleReply = async () => {
  if (!replyForm.value.content.trim()) {
    ElMessage.warning('请输入回复内容');
    return;
  }
  try {
    await api.replyComment(replyForm.value.commentId, user.value.id, replyForm.value.content);
    ElMessage.success('回复成功');
    replyDialogVisible.value = false;
    replyForm.value = { commentId: null, content: '' };
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.msg || '回复失败');
  }
};

const loadAvailableCoupon = async () => {
  if (!goods.value || !user.value) return;
  try {
    const res = await api.getAvailableCoupon(goods.value.id, user.value.id);
    if (res.data.code === 0) {
      availableCoupon.value = res.data.data;
    }
  } catch (err) {
    console.error('加载优惠券失败', err);
  }
};

const loadAllAvailableCoupons = async () => {
  if (!goods.value || !user.value) return;
  try {
    const res = await api.getAllAvailableCoupons(goods.value.id, goods.value.sellerId, user.value.id);
    if (res.data.code === 0 && res.data.data) {
      availableCoupons.value = res.data.data.map((coupon: any) => ({
        ...coupon,
        typeName: coupon.type === 0 ? '通用优惠券' : '商品优惠券',
        applicableTo: coupon.type === 0 ? '全店通用' : goods.value.title
      }));
    } else {
      availableCoupons.value = [];
    }
    
    const userCouponsRes = await api.getUserCoupons(user.value.id);
    if (userCouponsRes.data.code === 0 && userCouponsRes.data.data) {
      claimedCouponIds.value = userCouponsRes.data.data.map((c: any) => c.id);
    }
  } catch (err) {
    console.error('加载所有优惠券失败', err);
    availableCoupons.value = [];
  }
};

const openCouponDialog = () => {
  couponAmount.value = 10;
  couponDialogVisible.value = true;
};

const issueCoupon = async () => {
  if (!couponAmount.value || couponAmount.value <= 0) {
    ElMessage.warning('请输入有效的优惠券金额');
    return;
  }
  try {
    const res = await api.issueCoupon(goods.value.id, couponAmount.value);
    if (res.data.code === 0) {
      ElMessage.success('优惠券发放成功');
      couponDialogVisible.value = false;
      await loadAllAvailableCoupons();
    } else {
      ElMessage.error(res.data.msg || '发放失败');
    }
  } catch (err) {
    ElMessage.error('发放失败');
  }
};

const claimCoupon = async (couponId?: number) => {
  const id = couponId || availableCoupon.value?.id;
  if (!id) return;
  try {
    const res = await api.claimCoupon(id, user.value.id);
    if (res.data.code === 0) {
      ElMessage.success('优惠券领取成功');
      claimedCouponIds.value.push(id);
      loadAllAvailableCoupons();
    } else {
      ElMessage.error(res.data.msg || '领取失败');
    }
  } catch (err) {
    ElMessage.error('领取失败');
  }
};

const couponListDialogVisible = ref(false);

const openCouponListDialog = async () => {
  await loadAllAvailableCoupons();
  couponListDialogVisible.value = true;
};

const closeCouponListDialog = () => {
  couponListDialogVisible.value = false;
};

const editGoods = async () => {
  try {
    const res = await api.getGoodsDetail(goods.value.id);
    if (res.data.code === 0) {
      editForm.value = res.data.data;
      editDialogVisible.value = true;
    }
  } catch (err) {
    ElMessage.error('获取商品详情失败');
  }
};

const saveEdit = async () => {
  if (!editForm.value.id) {
    ElMessage.error('商品信息异常，请刷新后重试');
    return;
  }
  try {
    const payload = {
      id: editForm.value.id,
      title: editForm.value.title,
      description: editForm.value.description,
      imageUrl: editForm.value.imageUrl,
      price: editForm.value.price,
      freight: editForm.value.freight,
      stock: editForm.value.stock,
      category: editForm.value.category,
      brandName: editForm.value.brandName,
      brandColor: editForm.value.brandColor,
      status: 0,
      sellerId: user.value.id
    };
    const res = await api.updateGoods(payload);
    if (res.data.code === 0) {
      ElMessage.success('商品已更新，等待审核');
      editDialogVisible.value = false;
      loadDetail();
    } else {
      ElMessage.error(res.data.msg || '更新失败');
    }
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.msg || '更新失败');
  }
};

const handleUploadSuccess = (url: string) => {
  editForm.value.imageUrl = url;
  ElMessage.success('图片上传成功');
};

const beforeUpload = (file: File) => {
  const isImage = file.type.startsWith('image/');
  const isLt2M = file.size / 1024 / 1024 < 2;
  if (!isImage) {
    ElMessage.error('只能上传图片文件');
  }
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过 2MB');
  }
  return isImage && isLt2M;
};

const onFileChange = async (file: any) => {
  if (!file || !file.raw) return;
  uploading.value = true;
  try {
    const res = await api.uploadImage(file.raw);
    if (res.data.code === 0) {
      handleUploadSuccess(res.data.data);
    } else {
      ElMessage.error(res.data.msg || '上传失败');
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.msg || '上传失败');
  } finally {
    uploading.value = false;
  }
};

watch(goods, () => {
  if (goods.value && user.value) {
    loadAvailableCoupon();
    loadAllAvailableCoupons();
  }
});

onMounted(() => {
  loadDetail();
});
</script>

<template>
  <div v-if="goods" class="detail-page">
    <div class="detail-container">
      <div class="goods-main">
        <div class="goods-image-card">
          <div class="image-wrapper">
            <img
              v-if="goods.imageUrl"
              :src="goods.imageUrl"
              alt="商品图片"
              class="goods-image"
            />
            <div v-else class="image-placeholder">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1">
                <rect x="3" y="3" width="18" height="18" rx="2"/>
                <circle cx="8.5" cy="8.5" r="1.5"/>
                <path d="M21 15l-5-5L5 21"/>
              </svg>
            </div>
          </div>
        </div>

        <div class="goods-info-card">
          <div class="goods-header">
            <h1 class="goods-title">{{ goods.title }}</h1>
            <p class="goods-description">{{ goods.description }}</p>
          </div>

          <div class="goods-price-section">
            <div class="price-row">
              <span class="price-label">价格</span>
              <div class="price-display">
                <span v-if="showMemberPrice && !discountActivity" class="original-price">￥{{ goods.price }}</span>
                <span v-if="discountActivity" class="original-price">￥{{ goods.price }}</span>
                <span v-if="discountActivity" class="goods-price discount-price">￥{{ discountedPrice }}</span>
                <span v-else class="goods-price">￥{{ getMemberPrice(goods.price) }}</span>
                <span v-if="showMemberPrice" class="member-tag">会员价</span>
                <span v-if="discountActivity" class="discount-tag">{{ (discountActivity.discountRate * 10).toFixed(1) }}折</span>
              </div>
            </div>

            <!-- 限时折扣信息 -->
            <div v-if="discountActivity" class="discount-entry">
              <span class="discount-badge">🔥 限时折扣</span>
              <span class="discount-text">限时 {{ (discountActivity.discountRate * 10).toFixed(1) }} 折优惠</span>
            </div>

            <!-- 满减信息 -->
            <div v-if="fullReduceActivity" class="fullreduce-entry">
              <span class="fullreduce-badge">🎯 满减优惠</span>
              <span class="fullreduce-text">满 {{ fullReduceActivity.thresholdAmount }} 减 {{ fullReduceActivity.reductionAmount }}</span>
            </div>

            <!-- 团购信息 -->
            <div v-if="currentGroupBuy" class="groupbuy-entry">
              <div class="groupbuy-header">
                <span class="groupbuy-badge">👥 团购</span>
                <span class="groupbuy-price">¥{{ currentGroupBuy.groupPrice }}</span>
                <span class="groupbuy-original">¥{{ goods.price }}</span>
              </div>
              <div class="groupbuy-progress-bar">
                <div class="groupbuy-progress-fill" :style="{ width: groupBuyProgress.percentage + '%' }"></div>
              </div>
              <div class="groupbuy-status">
                <span>{{ groupBuyProgress.current }}/{{ groupBuyProgress.min }}人已成团</span>
                <span v-if="groupBuyProgress.current >= groupBuyProgress.min" class="groupbuy-success">已成团，可下单</span>
                <span v-else class="groupbuy-pending">还差 {{ groupBuyProgress.min - groupBuyProgress.current }} 人</span>
              </div>
            </div>

            <div v-if="user && user.id !== goods.sellerId" class="coupon-entry" @click="openCouponListDialog">
              <svg class="coupon-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"/>
                <line x1="7" y1="7" x2="7.01" y2="7"/>
              </svg>
              <span class="coupon-text">{{ availableCoupons.length > 0 ? `可领 ${availableCoupons.length} 张优惠券` : '暂无可用优惠券' }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">运费</span>
              <span v-if="goods.freight && goods.freight > 0" class="info-value">￥{{ goods.freight }}</span>
              <span v-else class="info-value free-shipping">包邮</span>
            </div>
            <div class="info-row">
              <span class="info-label">库存</span>
              <span class="info-value">{{ goods.stock }} 件</span>
            </div>
          </div>

          <div class="seller-section">
            <div class="seller-label">卖家信息</div>
            <div class="seller-info">
              <img
                v-if="seller?.avatar"
                :src="seller.avatar"
                :alt="seller?.nickname || seller?.username"
                class="seller-avatar"
              />
              <div v-else class="seller-avatar-placeholder">
                {{ (seller?.nickname || seller?.username || '卖').charAt(0) }}
              </div>
              <div class="seller-details">
                <span class="seller-name">{{ seller?.nickname || seller?.username || '未知' }}</span>
                <span class="seller-text">卖家</span>
              </div>
            </div>
          </div>

          <div class="action-section">
            <template v-if="user && user.id === goods.sellerId">
              <el-button type="primary" class="action-btn edit-btn" @click="editGoods">
                <svg class="btn-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
                  <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
                </svg>
                编辑商品
              </el-button>
              <el-button type="warning" class="action-btn coupon-btn" @click="openCouponDialog">
                <svg class="btn-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"/>
                  <line x1="7" y1="7" x2="7.01" y2="7"/>
                </svg>
                发放优惠券
              </el-button>
            </template>
            <template v-else>
              <!-- 团购按钮 -->
              <template v-if="currentGroupBuy">
                <el-button v-if="canJoinGroupBuy" type="warning" class="action-btn groupbuy-join-btn" @click="joinGroupBuy">
                  <svg class="btn-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                    <circle cx="9" cy="7" r="4"/>
                    <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
                    <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
                  </svg>
                  参加团购 ¥{{ currentGroupBuy.groupPrice }}
                </el-button>
                <el-button v-else-if="groupBuyProgress.current >= groupBuyProgress.min" type="danger" class="action-btn groupbuy-buy-btn" @click="onGroupBuyBuy">
                  <svg class="btn-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="9" cy="21" r="1"/>
                    <circle cx="20" cy="21" r="1"/>
                    <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"/>
                  </svg>
                  团购下单 ¥{{ currentGroupBuy.groupPrice }}
                </el-button>
                <el-button v-else-if="hasGroupBuyOrder" type="info" class="action-btn groupbuy-wait-btn" disabled>
                  <svg class="btn-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="12" cy="12" r="10"/>
                    <polyline points="12 6 12 12 16 14"/>
                  </svg>
                  已下单等待拼团
                </el-button>
                <el-button v-else type="info" class="action-btn groupbuy-wait-btn" disabled>
                  <svg class="btn-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="12" cy="12" r="10"/>
                    <polyline points="12 6 12 12 16 14"/>
                  </svg>
                  等待成团 {{ groupBuyProgress.current }}/{{ groupBuyProgress.min }}
                </el-button>
              </template>
              <el-button v-if="!currentGroupBuy" type="danger" class="action-btn buy-btn" @click="onBuy">
                <svg class="btn-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="9" cy="21" r="1"/>
                  <circle cx="20" cy="21" r="1"/>
                  <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"/>
                </svg>
                立即购买
              </el-button>
              <el-button class="action-btn cart-btn" @click="addToCart">
                <svg class="btn-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z"/>
                  <line x1="3" y1="6" x2="21" y2="6"/>
                  <path d="M16 10a4 4 0 0 1-8 0"/>
                </svg>
                加入购物车
              </el-button>
              <el-button class="action-btn chat-btn" @click="openChat">
                <svg class="btn-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
                </svg>
                私聊卖家
              </el-button>
            </template>
          </div>
        </div>
      </div>

      <div class="comment-section">
        <div class="section-header">
          <h2 class="section-title">商品评价</h2>
          <span class="comment-count">{{ comments.length }} 条评价</span>
        </div>

        <div class="comment-list">
          <div v-if="comments.length === 0" class="empty-comments">
            <svg class="empty-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
            </svg>
            <p>暂无评价</p>
          </div>
          <div v-for="comment in comments" :key="comment.id" class="comment-card">
            <div class="comment-header">
              <img
                v-if="getUserAvatar(comment.userId)"
                :src="getUserAvatar(comment.userId)"
                :alt="getUserName(comment.userId)"
                class="comment-avatar"
              />
              <div v-else class="comment-avatar-placeholder">
                {{ getUserName(comment.userId).charAt(0) }}
              </div>
              <div class="comment-user-info">
                <span class="comment-user">{{ getUserName(comment.userId) }}</span>
                <el-rate :model-value="comment.rating" disabled size="small" />
              </div>
              <span class="comment-time">{{ new Date(comment.createTime).toLocaleString() }}</span>
            </div>
            <div class="comment-content">{{ comment.content }}</div>
            <div v-if="getCommentMedia(comment.id)?.length > 0" class="comment-media">
              <div v-for="(media, index) in getCommentMedia(comment.id)" :key="index" class="media-item">
                <img v-if="isImageMedia(media)" :src="media.fullUrl" alt="评论图片" class="comment-image" />
                <video v-else-if="isVideoMedia(media)" :src="media.fullUrl" controls class="comment-image"></video>
              </div>
            </div>
            <div v-if="user && user.role === 2 && user.id === goods.sellerId" class="comment-actions">
              <el-button type="text" class="reply-btn" @click="openReplyDialog(comment.id)">
                <svg class="reply-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="9 14 4 9 9 4"/>
                  <path d="M20 20v-7a4 4 0 0 0-4-4H4"/>
                </svg>
                回复
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" title="发送消息" width="500px" class="message-dialog">
      <el-input
        v-model="messageContent"
        type="textarea"
        :rows="4"
        placeholder="请输入消息内容"
        maxlength="500"
        show-word-limit
      />
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="sendMessage">发送</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="replyDialogVisible" title="回复评论" width="500px" class="reply-dialog">
      <el-input
        v-model="replyForm.content"
        type="textarea"
        :rows="3"
        placeholder="请输入回复内容"
      />
      <template #footer>
        <el-button @click="replyDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleReply">提交回复</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="couponDialogVisible" title="发放优惠券" width="400px" class="coupon-dialog">
      <div class="coupon-form">
        <el-input-number v-model="couponAmount" :min="1" :max="1000" label="优惠券金额" style="width: 100%;" />
        <p class="coupon-tip">设置优惠券的优惠金额（单位：元）</p>
      </div>
      <template #footer>
        <el-button @click="couponDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="issueCoupon">发放</el-button>
      </template>
    </el-dialog>

    <el-dialog
        v-model="editDialogVisible"
        title="编辑商品"
        width="600px"
    >
      <el-form label-width="80px" class="edit-form">
        <el-form-item label="标题">
          <el-input v-model="editForm.title" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="editForm.description" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="商品图片">
          <el-upload
              :show-file-list="false"
              :before-upload="beforeUpload"
              :on-change="onFileChange"
          >
            <el-button :loading="uploading" type="primary">从电脑选择图片</el-button>
          </el-upload>
          <div v-if="editForm.imageUrl" class="image-preview">
            <img
                :src="editForm.imageUrl"
                alt="预览图片"
                class="preview-img"
            />
          </div>
        </el-form-item>
        <el-form-item label="价格">
          <el-input-number v-model="editForm.price" :min="0" :step="1" />
        </el-form-item>
        <el-form-item label="运费">
          <el-input-number v-model="editForm.freight" :min="0" :step="1" />
          <span class="form-tip">不填默认包邮</span>
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="editForm.category" placeholder="请选择商品分类">
            <el-option v-for="cat in categories" :key="cat.value" :label="cat.label" :value="cat.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="品牌名称">
          <el-input v-model="editForm.brandName" placeholder="请输入品牌名称" />
        </el-form-item>
        <el-form-item label="品牌底色">
          <el-select v-model="editForm.brandColor" placeholder="请选择品牌标签底色">
            <el-option v-for="color in brandColors" :key="color.value" :label="color.label" :value="color.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="editDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveEdit">保存</el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog v-model="couponListDialogVisible" title="可用优惠券" width="500px" class="coupon-list-dialog" @close="closeCouponListDialog">
      <div class="coupon-list-container">
        <div v-if="availableCoupons.length === 0" class="empty-coupon-list">
          <svg class="empty-coupon-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"/>
            <line x1="7" y1="7" x2="7.01" y2="7"/>
          </svg>
          <p>暂无可用优惠券</p>
        </div>
        <div v-for="coupon in availableCoupons" :key="coupon.id" class="coupon-card">
          <div class="coupon-left">
            <span class="coupon-amount">￥{{ coupon.amount }}</span>
            <span v-if="coupon.minAmount && coupon.minAmount > 0" class="coupon-condition">满{{ coupon.minAmount }}可用</span>
          </div>
          <div class="coupon-right">
            <div class="coupon-type">{{ coupon.typeName }}</div>
            <div class="coupon-applicable">适用：{{ coupon.applicableTo }}</div>
            <div class="coupon-info">
              <span class="coupon-count">剩余 {{ coupon.totalCount - coupon.claimedCount }} 张</span>
              <span class="coupon-expire">有效期至 {{ new Date(coupon.expireTime).toLocaleDateString() }}</span>
            </div>
            <div v-if="goods.price >= (coupon.minAmount || 0)" class="coupon-savings">
              本商品使用可省：<span class="savings-amount">￥{{ coupon.amount }}</span>
            </div>
          </div>
          <el-button 
            v-if="!claimedCouponIds.includes(coupon.id)" 
            type="primary" 
            class="coupon-claim-btn"
            @click="claimCoupon(coupon.id)"
          >
            立即领取
          </el-button>
          <span v-else class="coupon-claimed">已领取</span>
        </div>
      </div>
    </el-dialog>
  </div>
  <div v-else class="loading">
    <div class="loading-spinner"></div>
    <p>加载中...</p>
  </div>
</template>

<style scoped>
.detail-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8f0 100%);
  padding: 40px 20px;
}

.detail-container {
  max-width: 1200px;
  margin: 0 auto;
}

.goods-main {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 30px;
  margin-bottom: 40px;
}

.goods-image-card {
  background: #fff;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.08);
}

.image-wrapper {
  position: relative;
  width: 100%;
  padding-top: 100%;
  background: #f8f9fa;
}

.goods-image {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-placeholder {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ccc;
}

.image-placeholder svg {
  width: 80px;
  height: 80px;
}

.goods-info-card {
  background: #fff;
  border-radius: 20px;
  padding: 30px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.08);
  display: flex;
  flex-direction: column;
}

.goods-header {
  margin-bottom: 25px;
  padding-bottom: 20px;
  border-bottom: 1px solid #eee;
}

.goods-title {
  font-size: 26px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 15px 0;
  line-height: 1.4;
}

.goods-description {
  font-size: 15px;
  color: #666;
  line-height: 1.7;
  margin: 0;
}

.goods-price-section {
  background: linear-gradient(135deg, #fff5f5 0%, #fff 100%);
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 25px;
}

.price-row {
  display: flex;
  align-items: baseline;
  margin-bottom: 12px;
}

.price-label {
  font-size: 14px;
  color: #999;
  margin-right: 15px;
}

.goods-price {
  font-size: 36px;
  font-weight: 700;
  color: #ff4757;
}

.price-display {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.original-price {
  font-size: 16px;
  color: #999;
  text-decoration: line-through;
}

.member-tag {
  display: inline-block;
  font-size: 12px;
  color: #ff1744;
  background: linear-gradient(135deg, #ffe0e0 0%, #fff5f5 100%);
  padding: 2px 8px;
  border-radius: 4px;
  margin-top: 4px;
  width: fit-content;
}

.info-row {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.info-row:last-child {
  margin-bottom: 0;
}

.info-label {
  font-size: 14px;
  color: #999;
  margin-right: 15px;
  min-width: 50px;
}

.info-value {
  font-size: 15px;
  color: #333;
}

.free-shipping {
  color: #2ed573;
  font-weight: 500;
}

.seller-section {
  margin-bottom: 25px;
}

.seller-label {
  font-size: 14px;
  color: #999;
  margin-bottom: 12px;
}

.seller-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.seller-avatar {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid #667eea;
}

.seller-avatar-placeholder {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: bold;
  border: 3px solid #667eea;
}

.seller-details {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.seller-name {
  font-size: 16px;
  font-weight: 500;
  color: #333;
}

.seller-text {
  font-size: 12px;
  color: #999;
}

.action-section {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: auto;
}

.action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 14px 28px;
  border-radius: 50px;
  font-size: 15px;
  font-weight: 500;
  transition: all 0.3s ease;
  border: none;
}

.btn-icon {
  width: 18px;
  height: 18px;
}

.buy-btn {
  background: linear-gradient(135deg, #ff4757 0%, #ff6b81 100%);
  color: white;
}

.buy-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(255, 71, 87, 0.4);
}

.cart-btn {
  background: #fff;
  color: #333;
  border: 2px solid #ddd !important;
}

.cart-btn:hover {
  border-color: #667eea !important;
  color: #667eea;
  background: #f8f9ff;
}

.chat-btn {
  background: #fff;
  color: #333;
  border: 2px solid #ddd !important;
}

.chat-btn:hover {
  border-color: #2ed573 !important;
  color: #2ed573;
  background: #f0fff4;
}

.edit-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.edit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(102, 126, 234, 0.4);
}

.coupon-btn {
  background: linear-gradient(135deg, #ffa502 0%, #ff6348 100%);
  color: white;
}

.coupon-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(255, 165, 2, 0.4);
}

.comment-section {
  background: #fff;
  border-radius: 20px;
  padding: 30px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.08);
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 25px;
  padding-bottom: 15px;
  border-bottom: 2px solid #f0f0f0;
}

.section-title {
  font-size: 22px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0;
}

.comment-count {
  font-size: 14px;
  color: #999;
  background: #f5f5f5;
  padding: 6px 14px;
  border-radius: 20px;
}

.empty-comments {
  text-align: center;
  padding: 60px 20px;
  color: #999;
}

.empty-icon {
  width: 60px;
  height: 60px;
  margin-bottom: 15px;
  color: #ddd;
}

.empty-comments p {
  font-size: 15px;
  margin: 0;
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.comment-card {
  padding: 20px;
  background: #fafbfc;
  border-radius: 12px;
  border: 1px solid #f0f0f0;
  transition: all 0.3s ease;
}

.comment-card:hover {
  border-color: #e0e0e0;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.05);
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.comment-avatar {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #667eea;
}

.comment-avatar-placeholder {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: bold;
}

.comment-user-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.comment-user {
  font-weight: 500;
  color: #333;
  font-size: 15px;
}

.comment-time {
  font-size: 12px;
  color: #999;
}

.comment-content {
  font-size: 14px;
  color: #555;
  line-height: 1.7;
  margin-left: 54px;
  margin-bottom: 12px;
}

.comment-media {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-left: 54px;
}

.media-item {
  width: 100px;
  height: 100px;
  border-radius: 8px;
  overflow: hidden;
}

.comment-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  cursor: pointer;
  transition: transform 0.3s ease;
}

.comment-image:hover {
  transform: scale(1.05);
}

.comment-actions {
  margin-left: 54px;
  margin-top: 10px;
}

.reply-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #667eea;
  font-size: 13px;
}

.reply-icon {
  width: 14px;
  height: 14px;
}

.message-dialog :deep(.el-dialog) {
  border-radius: 16px;
}

.reply-dialog :deep(.el-dialog) {
  border-radius: 16px;
}

.coupon-dialog :deep(.el-dialog) {
  border-radius: 16px;
}

.coupon-form {
  padding: 10px 0;
}

.coupon-tip {
  margin-top: 12px;
  color: #999;
  font-size: 13px;
}

.edit-form :deep(.el-form-item__label) {
  font-weight: 500;
}

.form-tip {
  margin-left: 12px;
  color: #999;
  font-size: 13px;
}

.image-preview {
  margin-top: 15px;
}

.preview-img {
  width: 120px;
  height: 120px;
  object-fit: cover;
  border-radius: 8px;
  border: 1px solid #eee;
}

.loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
  color: #999;
}

.loading-spinner {
  width: 50px;
  height: 50px;
  border: 4px solid #f0f0f0;
  border-top-color: #667eea;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 20px;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 768px) {
  .goods-main {
    grid-template-columns: 1fr;
  }

  .goods-title {
    font-size: 22px;
  }

  .goods-price {
    font-size: 28px;
  }

  .action-section {
    flex-direction: column;
  }

  .action-btn {
    width: 100%;
  }
}

.coupon-entry {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: linear-gradient(135deg, #fff5f0 0%, #fff 100%);
  border: 1px dashed #ff9f43;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  margin-top: 8px;
}

.coupon-entry:hover {
  background: linear-gradient(135deg, #ffede0 0%, #fff5f0 100%);
  border-color: #ff6b6b;
  transform: translateX(4px);
}

.coupon-icon {
  width: 18px;
  height: 18px;
  color: #ff9f43;
}

.coupon-text {
  font-size: 14px;
  color: #ff6b6b;
  font-weight: 500;
}

.coupon-list-dialog :deep(.el-dialog) {
  border-radius: 16px;
}

.coupon-list-container {
  padding: 10px 0;
}

.empty-coupon-list {
  text-align: center;
  padding: 40px 20px;
  color: #999;
}

.empty-coupon-icon {
  width: 60px;
  height: 60px;
  margin-bottom: 15px;
  color: #ddd;
}

.coupon-card {
  display: flex;
  align-items: center;
  background: linear-gradient(135deg, #fff5f0 0%, #fff 100%);
  border: 1px solid #ffe0c0;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 15px;
  position: relative;
  overflow: hidden;
}

.coupon-card::before {
  content: '';
  position: absolute;
  left: 120px;
  top: 0;
  bottom: 0;
  width: 20px;
  background: #fff;
}

.coupon-card::after {
  content: '';
  position: absolute;
  left: 110px;
  top: 50%;
  transform: translateY(-50%);
  width: 20px;
  height: 20px;
  background: #fff;
  border-radius: 50%;
  box-shadow: 0 0 0 2px #ffe0c0;
}

.coupon-left {
  width: 120px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #ff6b6b 0%, #ff9f43 100%);
  padding: 15px 0;
  border-radius: 8px;
  margin-right: 20px;
}

.coupon-amount {
  font-size: 32px;
  font-weight: 700;
  color: #fff;
  line-height: 1;
}

.coupon-condition {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.9);
  margin-top: 4px;
}

.coupon-right {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.coupon-type {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.coupon-applicable {
  font-size: 13px;
  color: #666;
}

.coupon-info {
  display: flex;
  gap: 15px;
}

.coupon-count, .coupon-expire {
  font-size: 12px;
  color: #999;
}

.coupon-savings {
  font-size: 13px;
  color: #ff6b6b;
  margin-top: 4px;
}

.savings-amount {
  font-weight: 600;
  font-size: 16px;
}

.coupon-claim-btn {
  padding: 8px 20px;
  border-radius: 20px;
  font-size: 14px;
  background: linear-gradient(135deg, #ff6b6b 0%, #ff9f43 100%);
  border: none;
  color: #fff;
  height: auto;
  line-height: 1;
  min-width: auto;
}

.coupon-claim-btn:hover {
  background: linear-gradient(135deg, #ff5252 0%, #ff8f33 100%);
  color: #fff;
}

.coupon-claimed {
  padding: 8px 20px;
  font-size: 14px;
  color: #999;
  background: #f5f5f5;
  border-radius: 20px;
  display: inline-block;
}

/* 团购样式 */
.groupbuy-entry {
  background: linear-gradient(135deg, #fff8e1 0%, #fff 100%);
  border: 1px solid #ffe0b2;
  border-radius: 12px;
  padding: 15px;
  margin-bottom: 15px;
}

.groupbuy-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}

.groupbuy-badge {
  background: linear-gradient(135deg, #ff9800 0%, #f57c00 100%);
  color: white;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
}

.groupbuy-price {
  font-size: 24px;
  font-weight: 700;
  color: #ff6b00;
}

.groupbuy-original {
  font-size: 14px;
  color: #999;
  text-decoration: line-through;
}

.groupbuy-progress-bar {
  height: 8px;
  background: #e0e0e0;
  border-radius: 4px;
  overflow: hidden;
  margin-bottom: 8px;
}

.groupbuy-progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #ff9800, #ff5722);
  border-radius: 4px;
  transition: width 0.5s ease;
}

.groupbuy-status {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
}

.groupbuy-success {
  color: #4caf50;
  font-weight: 600;
}

.groupbuy-pending {
  color: #ff9800;
  font-weight: 600;
}

.groupbuy-join-btn {
  background: linear-gradient(135deg, #ff9800 0%, #f57c00 100%) !important;
  color: white !important;
  border: none !important;
}

.groupbuy-join-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(255, 152, 0, 0.4);
}

.groupbuy-buy-btn {
  background: linear-gradient(135deg, #ff4757 0%, #ff6b81 100%) !important;
  color: white !important;
  border: none !important;
}

.groupbuy-buy-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(255, 71, 87, 0.4);
}

.groupbuy-wait-btn {
  background: #f5f5f5 !important;
  color: #999 !important;
  border: none !important;
}

/* 限时折扣样式 */
.discount-entry {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  background: linear-gradient(135deg, #fff0f0 0%, #fff 100%);
  border: 1px solid #ffcdd2;
  border-radius: 8px;
  margin-top: 8px;
}

.discount-badge {
  background: linear-gradient(135deg, #f44336 0%, #e53935 100%);
  color: white;
  padding: 3px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.discount-text {
  font-size: 14px;
  color: #d32f2f;
  font-weight: 500;
}

.discount-price {
  color: #d32f2f !important;
}

.discount-tag {
  background: linear-gradient(135deg, #f44336 0%, #e53935 100%);
  color: white;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 600;
  margin-left: 8px;
}

/* 满减样式 */
.fullreduce-entry {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  background: linear-gradient(135deg, #e8f5e9 0%, #fff 100%);
  border: 1px solid #c8e6c9;
  border-radius: 8px;
  margin-top: 8px;
}

.fullreduce-badge {
  background: linear-gradient(135deg, #4caf50 0%, #388e3c 100%);
  color: white;
  padding: 3px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.fullreduce-text {
  font-size: 14px;
  color: #2e7d32;
  font-weight: 500;
}
</style>
