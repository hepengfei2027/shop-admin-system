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

// 地址相关
const addressList = ref<any[]>([]);
const selectedAddressId = ref<number | null>(null);

// 计算选中的地址对象
const selectedAddress = computed(() => {
  if (!selectedAddressId.value) return null;
  return addressList.value.find(addr => addr.id === selectedAddressId.value) || null;
});

// 限时折扣活动
const discountActivity = ref<any>(null);

// 满减活动
const fullReduceActivity = ref<any>(null);

// 计算商品折扣价（考虑会员折扣和限时折扣）
const discountedPrice = computed(() => {
  if (!goods.value) return 0;
  let price = goods.value.price;

  // 先应用会员折扣
  const memberDiscount = (user.value?.discount !== undefined && user.value?.discount !== null) ? user.value.discount : 1;
  price = price * memberDiscount;

  // 再应用限时折扣
  if (discountActivity.value) {
    price = price * (discountActivity.value.ruleDetail?.discountRate || 1);
  }

  return price.toFixed(2);
});

// 计算最终价格（考虑满减和优惠券）
const finalPrice = computed(() => {
  if (!goods.value) return 0;
  let price = parseFloat(discountedPrice.value);

  // 满减优惠
  if (fullReduceActivity.value && price >= fullReduceActivity.value.thresholdAmount) {
    price = price - fullReduceActivity.value.reductionAmount;
  }

  // 最大可用优惠券
  const maxCoupon = availableCoupons.value
    .filter((c: any) => price >= (c.minAmount || 0))
    .sort((a: any, b: any) => b.amount - a.amount)[0];
  if (maxCoupon) {
    price = price - maxCoupon.amount;
  }

  return Math.max(0, price).toFixed(2);
});

// 节省金额
const savedAmount = computed(() => {
  if (!goods.value) return 0;
  return Math.max(0, goods.value.price - parseFloat(finalPrice.value)).toFixed(2);
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
    }
  } catch (e) {
    console.error('加载营销活动失败', e);
  }
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

// 加载用户地址
const loadAddresses = async () => {
  if (!user.value?.id) return;
  try {
    const res = await api.getAddresses(user.value.id);
    if (res.data.code === 0) {
      addressList.value = res.data.data || [];
      // 默认选择第一个地址
      if (addressList.value.length > 0 && !selectedAddressId.value) {
        selectedAddressId.value = addressList.value[0].id;
      }
    }
  } catch (err) {
    console.error('加载地址失败', err);
  }
};

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
    await loadPromotions();
  }

  await loadComments();
  await loadAddresses();
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

  // 检查是否选择了地址
  if (addressList.value.length > 0 && !selectedAddressId.value) {
    ElMessage.warning('请选择收货地址');
    return;
  }

  // 跳转到确认订单页面，传递地址ID
  const addressId = selectedAddressId.value || '';
  router.push(`/confirm-order?goodsId=${goods.value.id}${addressId ? `&addressId=${addressId}` : ''}`);
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
    <div class="container-wrap">
      <!-- 主商品区域 -->
      <div class="goods-main-row">
        <!-- 左侧图片区 -->
        <div class="goods-image-card">
          <div class="image-wrapper">
            <img
                v-if="goods.imageUrl"
                :src="resolveMediaUrl(goods.imageUrl)"
                alt="商品图片"
                class="goods-image"
            />
            <div v-else class="image-placeholder">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <rect x="3" y="3" width="18" height="18" rx="2"/>
                <circle cx="8.5" cy="8.5" r="1.5"/>
                <path d="M21 15l-5-5L5 21"/>
              </svg>
              <p>暂无商品图</p>
            </div>
            <!-- 营销角标 -->
            <div class="corner-tag" v-if="discountActivity">限时折扣</div>
          </div>
        </div>

        <!-- 右侧信息区 -->
        <div class="goods-info-card">
          <!-- 卖家卡片 -->
          <div class="seller-card">
            <div class="seller-avatar-wrap">
              <img
                  v-if="seller?.avatar"
                  :src="resolveMediaUrl(seller.avatar)"
                  :alt="seller?.nickname || seller?.username"
                  class="seller-avatar"
              />
              <div v-else class="seller-avatar-placeholder">
                {{ (seller?.nickname || seller?.username || '卖').charAt(0) }}
              </div>
            </div>
            <div class="seller-meta">
              <div class="seller-name">{{ seller?.nickname || seller?.username || '未知店铺' }}</div>
              <div class="seller-desc">官方认证卖家 · 极速发货</div>
            </div>
          </div>

          <!-- 标题描述 -->
          <div class="goods-header">
            <div class="title-row">
              <span v-if="goods.brandName" class="brand-tag" :style="{ backgroundColor: goods.brandColor || '#ff1744' }">
                {{ goods.brandName }}
              </span>
              <h1 class="goods-title">{{ goods.title }}</h1>
            </div>
            <p class="goods-desc">{{ goods.description }}</p>
          </div>

          <!-- 价格模块 -->
          <div class="price-block">
            <div class="price-main">
              <span class="price-symbol">¥</span>
              <span class="final-price">{{ finalPrice }}</span>
              <span class="arrive-price-tag">到手价</span>
              <span v-if="goods.price > Number(finalPrice)" class="origin-price-line">¥{{ goods.price }}</span>
            </div>

            <!-- 价格明细折叠面板 -->
            <div class="price-detail-panel" v-if="showMemberPrice || discountActivity || fullReduceActivity || availableCoupons.filter(c => Number(discountedPrice) >= (c.minAmount || 0)).length">
              <div class="detail-row" v-if="showMemberPrice">
                <span class="label">💜 会员专享价</span>
                <span class="val">¥{{ getMemberPrice(goods.price) }}</span>
              </div>
              <div class="detail-row" v-if="discountActivity">
                <span class="label">🔥 限时折扣</span>
                <span class="val red">{{ (discountActivity.discountRate * 10).toFixed(1) }}折</span>
              </div>
              <div class="detail-row" v-if="fullReduceActivity && Number(discountedPrice) >= fullReduceActivity.thresholdAmount">
                <span class="label">🎯 满减优惠</span>
                <span class="val green">-¥{{ fullReduceActivity.reductionAmount }}</span>
              </div>
              <div class="detail-row" v-if="availableCoupons.filter(c => Number(discountedPrice) >= (c.minAmount || 0)).length > 0">
                <span class="label">🎫 优惠券抵扣</span>
                <span class="val green">-¥{{ availableCoupons.filter(c => Number(discountedPrice) >= (c.minAmount || 0)).sort((a,b)=>b.amount-a.amount)[0]?.amount }}</span>
              </div>
              <div class="detail-row save-row" v-if="Number(savedAmount) > 0">
                <span class="label">💰 共省</span>
                <span class="val red save-amount">立省 ¥{{ savedAmount }}</span>
              </div>
            </div>

            <!-- 活动标签组 -->
            <div class="tag-group">
              <div v-if="discountActivity" class="tag tag-red">限时{{ (discountActivity.discountRate*10).toFixed(1) }}折</div>
              <div v-if="fullReduceActivity" class="tag tag-green">满{{ fullReduceActivity.thresholdAmount }}减{{ fullReduceActivity.reductionAmount }}</div>
            </div>

            <!-- 优惠券入口 -->
            <div class="coupon-entry-box" @click="openCouponListDialog" v-if="user && user.id !== goods.sellerId">
              <svg class="coupon-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"/>
                <line x1="7" y1="7" x2="7.01" y2="7"/>
              </svg>
              <span class="text">{{ availableCoupons.length > 0 ? `有${availableCoupons.length}张优惠券可领取` : '暂无可用优惠券' }}</span>
              <span class="arrow">></span>
            </div>

            <!-- 地址选择 -->
            <div class="address-select-box" v-if="user && user.role === 0">
              <div class="address-label">
                <svg class="addr-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/>
                  <circle cx="12" cy="10" r="3"/>
                </svg>
                <span>配送至</span>
              </div>
              <div class="address-content">
                <template v-if="addressList.length > 0">
                  <el-select
                    v-model="selectedAddressId"
                    placeholder="选择收货地址"
                    class="address-select"
                    size="large"
                  >
                    <el-option
                      v-for="addr in addressList"
                      :key="addr.id"
                      :label="`${addr.name} ${addr.phone} ${addr.province}${addr.city}${addr.district}${addr.detail}`"
                      :value="addr.id"
                    />
                  </el-select>
                </template>
                <div v-else class="no-address-tip">
                  <span>暂无收货地址</span>
                  <el-button type="primary" link @click="router.push('/profile?tab=address')">去添加</el-button>
                </div>
              </div>
            </div>

            <!-- 基础信息 -->
            <div class="base-info">
              <div class="info-item">
                <span class="info-label">运费</span>
                <span class="info-val" v-if="goods.freight > 0">¥{{ goods.freight }}</span>
                <span class="info-val free" v-else>包邮</span>
              </div>
              <div class="info-item">
                <span class="info-label">库存</span>
                <span class="info-val">{{ goods.stock }}件</span>
              </div>
              <div class="info-item">
                <span class="info-label">分类</span>
                <span class="info-val">{{ goods.category || '未分类' }}</span>
              </div>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="action-btn-group" v-if="user && user.role === 0">
            <el-button type="danger" size="large" class="buy-btn" @click="onBuy">
              <svg class="btn-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="9" cy="21" r="1"/>
                <circle cx="20" cy="21" r="1"/>
                <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"/>
              </svg>
              立即购买
            </el-button>
            <el-button size="large" class="cart-btn" @click="addToCart">
              <svg class="btn-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z"/>
                <line x1="3" y1="6" x2="21" y2="6"/>
                <path d="M16 10a4 4 0 0 1-8 0"/>
              </svg>
              加入购物车
            </el-button>
            <el-button size="large" class="chat-btn" @click="openChat">
              <svg class="btn-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
              </svg>
              联系卖家
            </el-button>
          </div>
        </div>
      </div>

      <!-- 评价区域 -->
      <div class="comment-container">
        <div class="comment-head">
          <h2 class="comment-title">商品评价</h2>
          <span class="count-tag">共{{ comments.length }}条评价</span>
        </div>

        <div class="comment-list-wrap">
          <div v-if="comments.length === 0" class="empty-comment-box">
            <svg class="empty-svg" viewBox="0 0 24 24" fill="none" stroke="#dcdcdc" stroke-width="1.5">
              <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
            </svg>
            <p>暂时还没有用户评价，期待你的第一条反馈~</p>
          </div>

          <div v-for="comment in comments" :key="comment.id" class="comment-card">
            <div class="comment-top">
              <div class="user-avatar">
                <img
                    v-if="getUserAvatar(comment.userId)"
                    :src="resolveMediaUrl(getUserAvatar(comment.userId))"
                    class="avatar-img"
                />
                <div v-else class="avatar-text">{{ getUserName(comment.userId).charAt(0) }}</div>
              </div>
              <div class="user-info">
                <div class="username">{{ getUserName(comment.userId) }}</div>
                <el-rate :model-value="comment.rating" disabled size="small" />
              </div>
              <div class="time-text">{{ new Date(comment.createTime).toLocaleString() }}</div>
            </div>

            <div class="comment-text">{{ comment.content }}</div>

            <!-- 评论图片视频 -->
            <div class="media-row" v-if="getCommentMedia(comment.id).length">
              <div v-for="(media, idx) in getCommentMedia(comment.id)" :key="idx" class="media-item">
                <img v-if="isImageMedia(media)" :src="media.fullUrl" class="media-img" />
                <video v-else-if="isVideoMedia(media)" :src="media.fullUrl" controls class="media-img"></video>
              </div>
            </div>

            <!-- 卖家回复按钮 -->
            <div class="reply-btn-wrap" v-if="user && user.role === 2 && user.id === goods.sellerId">
              <el-button text type="primary" @click="openReplyDialog(comment.id)">
                <svg class="reply-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="9 14 4 9 9 4"/>
                  <path d="M20 20v-7a4 4 0 0 0-4-4H4"/>
                </svg>
                回复这条评价
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 弹窗：发消息 -->
    <el-dialog v-model="dialogVisible" title="私信卖家" width="520px" class="common-dialog" :mask-closable="false">
      <el-input v-model="messageContent" type="textarea" :rows="5" maxlength="500" show-word-limit placeholder="输入你想咨询的问题..."/>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="danger" @click="sendMessage">发送消息</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 弹窗：回复评论 -->
    <el-dialog v-model="replyDialogVisible" title="回复用户评价" width="500px" class="common-dialog" :mask-closable="false">
      <el-input v-model="replyForm.content" type="textarea" :rows="4" placeholder="礼貌回复用户评价..."/>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="replyDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleReply">提交回复</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 弹窗：发放优惠券 -->
    <el-dialog v-model="couponDialogVisible" title="创建商品优惠券" width="440px" class="common-dialog">
      <div class="coupon-create-form">
        <el-input-number v-model="couponAmount" label="优惠金额" :min="1" :max="999" style="width:100%"/>
        <p class="tip-text">设置优惠券抵扣金额，用户下单直接减免</p>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="couponDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="issueCoupon">确认发放</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 弹窗：优惠券列表 -->
    <el-dialog v-model="couponListDialogVisible" title="全部可用优惠券" width="540px" class="common-dialog" @close="closeCouponListDialog">
      <div class="coupon-list-container">
        <div v-if="availableCoupons.length === 0" class="empty-coupon">
          <svg class="empty-svg" viewBox="0 0 24 24" fill="none" stroke="#ddd" stroke-width="2">
            <path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"/>
            <line x1="7" y1="7" x2="7.01" y2="7"/>
          </svg>
          <p>暂无优惠券，敬请期待</p>
        </div>
        <div v-for="coupon in availableCoupons" :key="coupon.id" class="coupon-card-item">
          <div class="coupon-left-block">
            <span class="money">¥{{ coupon.amount }}</span>
            <span v-if="coupon.minAmount" class="limit">满{{ coupon.minAmount }}可用</span>
          </div>
          <div class="coupon-right-block">
            <div class="coupon-type">{{ coupon.typeName }}</div>
            <div class="scope">适用：{{ coupon.applicableTo }}</div>
            <div class="meta-row">
              <span>剩余{{ coupon.totalCount - coupon.claimedCount }}张</span>
              <span>有效期至{{ new Date(coupon.expireTime).toLocaleDateString() }}</span>
            </div>
            <div v-if="Number(goods.price) >= (coupon.minAmount || 0)" class="save-tip">本商品使用可省¥{{ coupon.amount }}</div>
          </div>
          <div class="btn-block">
            <el-button v-if="!claimedCouponIds.includes(coupon.id)" type="primary" size="small" @click="claimCoupon(coupon.id)">立即领取</el-button>
            <span v-else class="claimed-text">已领取</span>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- 弹窗：编辑商品 -->
    <el-dialog v-model="editDialogVisible" title="编辑商品信息" width="640px" class="common-dialog">
      <el-form label-width="90px" class="edit-form">
        <el-form-item label="商品标题">
          <el-input v-model="editForm.title" placeholder="输入商品标题"/>
        </el-form-item>
        <el-form-item label="商品描述">
          <el-input v-model="editForm.description" type="textarea" :rows="4" placeholder="详细描述商品"/>
        </el-form-item>
        <el-form-item label="商品主图">
          <el-upload :show-file-list="false" :before-upload="beforeUpload" :on-change="onFileChange">
            <el-button :loading="uploading" type="primary">点击上传图片（≤2M）</el-button>
          </el-upload>
          <div v-if="editForm.imageUrl" class="img-preview-box">
            <img :src="resolveMediaUrl(editForm.imageUrl)" alt="预览图" class="preview-img"/>
          </div>
        </el-form-item>
        <el-form-item label="售价">
          <el-input-number v-model="editForm.price" :min="0" step="0.01"/>
        </el-form-item>
        <el-form-item label="运费">
          <el-input-number v-model="editForm.freight" :min="0"/>
          <span class="form-tip">0元为包邮</span>
        </el-form-item>
        <el-form-item label="商品分类">
          <el-select v-model="editForm.category" placeholder="选择分类">
            <el-option v-for="cat in categories" :key="cat.value" :label="cat.label" :value="cat.value"/>
          </el-select>
        </el-form-item>
        <el-form-item label="品牌名称">
          <el-input v-model="editForm.brandName"/>
        </el-form-item>
        <el-form-item label="品牌标签色">
          <el-select v-model="editForm.brandColor">
            <el-option v-for="color in brandColors" :key="color.value" :label="color.label" :value="color.value"/>
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="editDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveEdit">保存修改</el-button>
        </div>
      </template>
    </el-dialog>
  </div>

  <!-- 加载占位 -->
  <div v-else class="loading-page">
    <div class="loading-circle"></div>
    <p class="loading-text">商品加载中，请稍候...</p>
  </div>
</template>

<style scoped>
/* 全局变量统一规范 */
:root {
  --color-main: #ff4757;
  --color-main-light: #ffecec;
  --color-green: #00b42a;
  --color-green-light: #e6ffed;
  --color-orange: #ff7d00;
  --color-purple: #722ed1;
  --color-gray-1: #f7f8fa;
  --color-gray-2: #e5e6eb;
  --color-gray-3: #c9cdd4;
  --color-gray-4: #86909c;
  --color-gray-5: #4e5969;
  --color-gray-6: #1d2129;
  --shadow-sm: 0 2px 8px rgba(0,0,0,0.06);
  --shadow-md: 0 4px 16px rgba(0,0,0,0.08);
  --shadow-lg: 0 8px 24px rgba(0,0,0,0.1);
  --radius-sm: 6px;
  --radius-md: 10px;
  --radius-lg: 16px;
  --radius-full: 999px;
  --transition: all 0.24s ease;
}

* {
  box-sizing: border-box;
}
.detail-page {
  min-height: 100vh;
  background-color: #f2f3f5;
  padding: 24px 16px;
}
.container-wrap {
  max-width: 1400px;
  margin: 0 auto;
}

/* ========== 商品主行：左图 + 右侧信息 分区隔离 ========== */
.goods-main-row {
  display: grid;
  grid-template-columns: 1fr 1.2fr;
  /* 原 gap: 24px; 减小为 12px/8px/0px 按需改 */
  gap: 12px !important;
  /* 商品区与评论区的垂直间距，原 margin-bottom: 32px; */
  margin-bottom: 16px !important;
}

/* 左侧图片卡片：纯白底色 + 深色细边框，独立区块 */
.goods-image-card {
  background: #ffffff;
  /* 直接写圆角，替换 var(--radius-lg) */
  border-radius: 10px !important;
  box-shadow: var(--shadow-md);
  border: 1px solid var(--color-gray-2);
  overflow: hidden;
  position: relative;
}
.image-wrapper {
  width: 100%;
  height: 100%;
  min-height: 460px;
  position: relative;
}
.goods-image {
  width: 100%;
  height: 100%;
  object-fit: contain;
  transition: var(--transition);
}
.goods-image:hover {
  transform: scale(1.03);
}
.image-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--color-gray-3);
  background: #fafafa;
}
.image-placeholder svg {
  width: 140px;
  height: 140px;
  margin-bottom: 12px;
}
.corner-tag {
  position: absolute;
  top: 12px;
  left: 12px;
  background: var(--color-main);
  color: #fff;
  font-size: 12px;
  padding: 3px 10px;
  border-radius: var(--radius-full);
}

/* 右侧信息卡片：极浅灰底色，和左侧纯白形成色差区分 */
.goods-info-card {
  background: #fcfcfd;
  /* 直接写圆角，替换 var(--radius-lg) */
  border-radius: 10px !important;
  box-shadow: var(--shadow-md);
  border: 1px solid #e8e8eb;
  padding: 28px;
  display: flex;
  flex-direction: column;
}

/* 卖家卡片 内部二级区分 */
.seller-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  background: linear-gradient(135deg, #f6f7ff 0%, #f0f4ff 100%);
  border-radius: var(--radius-md);
  border: 1px solid #dde4ff;
  margin-bottom: 20px;
}
.seller-avatar-wrap {
  flex-shrink: 0;
}
.seller-avatar {
  width: 42px;
  height: 42px;
  border-radius: var(--radius-sm);
  object-fit: cover;
}
.seller-avatar-placeholder {
  width: 42px;
  height: 42px;
  border-radius: var(--radius-sm);
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 600;
}
.seller-meta .seller-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-gray-6);
}
.seller-meta .seller-desc {
  font-size: 12px;
  color: var(--color-gray-4);
  margin-top: 2px;
}

/* 标题区域分割线强化 */
.goods-header {
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 2px dashed var(--color-gray-2);
}
.title-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 10px;
}
.brand-tag {
  display: inline-block;
  padding: 4px 10px;
  border-radius: var(--radius-full);
  font-size: 12px;
  font-weight: 600;
  color: #fff;
  white-space: nowrap;
}
.goods-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--color-gray-6);
  margin: 0;
  line-height: 1.4;
}
.goods-desc {
  font-size: 14px;
  color: var(--color-gray-5);
  line-height: 1.7;
  margin: 0;
}

/* 价格模块加深底色，和信息卡片主体区分 */
.price-block {
  background: #f5f7fa;
  border-radius: var(--radius-md);
  border: 1px solid #e2e6ed;
  padding: 20px;
  margin-bottom: 20px;
}
.price-main {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 14px;
}
.price-symbol {
  font-size: 18px;
  color: var(--color-main);
  font-weight: 600;
}
.final-price {
  font-size: 34px;
  font-weight: 700;
  color: #f5222d;
  line-height: 1;
}
.origin-price-line {
  font-size: 14px;
  color: var(--color-gray-4);
  text-decoration: line-through;
}
.save-badge {
  background: var(--color-main);
  color: #fff;
  font-size: 12px;
  padding: 3px 10px;
  border-radius: var(--radius-full);
}
.arrive-price-tag {
  background: #ff4757;
  color: #fff;
  font-size: 12px;
  padding: 3px 10px;
  margin-left: 8px;
}

/* 价格明细面板 */
.price-detail-panel {
  background: #ffffff;
  border-radius: var(--radius-sm);
  border: 1px solid var(--color-gray-2);
  padding: 12px 14px;
  margin-bottom: 14px;
}
.detail-row {
  display: flex;
  justify-content: space-between;
  padding: 6px 0;
  font-size: 13px;
}
.detail-row .label {
  color: var(--color-gray-5);
}
.detail-row .val {
  font-weight: 500;
}
.val.red { color: var(--color-main); }
.val.green { color: var(--color-green); }
.save-amount {
  color: #ff4757 !important;
  font-weight: 700;
  font-size: 16px;
}

/* 活动标签组 */
.tag-group {
  display: flex;
  gap: 8px;
  margin-bottom: 14px;
}
.tag {
  padding: 4px 12px;
  border-radius: var(--radius-full);
  font-size: 12px;
  font-weight: 500;
}
.tag-red {
  background: var(--color-main-light);
  color: var(--color-main);
  border: 1px solid #ffc2c5;
}
.tag-green {
  background: var(--color-green-light);
  color: var(--color-green);
  border: 1px solid #b6efc8;
}

/* 优惠券入口 */
.coupon-entry-box {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border: 1px dashed #ffb870;
  background: #fffaf5;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: var(--transition);
  margin-bottom: 16px;
}
.coupon-entry-box:hover {
  background: #fff0e0;
  border-color: var(--color-orange);
}
.coupon-icon {
  width: 18px;
  height: 18px;
  color: var(--color-orange);
  margin-right: 8px;
}
.coupon-entry-box .text {
  flex: 1;
  font-size: 14px;
  color: #e65100;
}
.arrow {
  color: var(--color-gray-4);
}

/* 地址选择器 */
.address-select-box {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  background: #fff;
  border: 1px solid var(--color-gray-2);
  border-radius: var(--radius-md);
  margin-bottom: 16px;
}
.address-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: var(--color-gray-5);
  white-space: nowrap;
}
.addr-icon {
  width: 16px;
  height: 16px;
  color: var(--color-main);
}
.address-content {
  flex: 1;
}
.address-select {
  width: 100%;
}
.no-address-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--color-gray-4);
}

/* 基础信息行 */
.base-info {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
}
.info-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}
.info-label {
  color: var(--color-gray-4);
}
.info-val {
  color: var(--color-gray-6);
}
.info-val.free {
  color: var(--color-green);
  font-weight: 500;
}

/* 操作按钮组 */
.action-btn-group {
  display: flex;
  gap: 12px;
  margin-top: auto;
  padding-top: 24px;
  border-top: 1px solid var(--color-gray-2);
}
.action-btn-group .el-button {
  flex: 1;
  height: 48px;
  font-size: 15px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  transition: var(--transition);
}
.buy-btn {
  background: linear-gradient(135deg, #ff4757, #ff6b77) !important;
  border: none !important;
}
.buy-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 14px rgba(255,71,87,0.25);
}
.cart-btn {
  background: linear-gradient(135deg, #ff4757, #ff6b77) !important;
  border: none !important;
  color: #fff !important;
}
.cart-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 14px rgba(255,71,87,0.25);
}
.chat-btn {
  border: 1px solid var(--color-gray-3) !important;
  color: var(--color-gray-5);
}
.chat-btn:hover {
  border-color: var(--color-main) !important;
  color: var(--color-main);
  transform: translateY(-2px);
}
.btn-icon {
  width: 16px;
  height: 16px;
}

/* ========== 评论区块：独立浅蓝色底色，和上方商品区彻底区分 ========== */
.comment-container {
  background: #f7fbff;
  border-radius: 12px !important;
  box-shadow: var(--shadow-md);
  border: 1px solid #d8e8fb;
  padding: 24px 28px;
}
.comment-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid #d8e8fb;
}
.comment-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-gray-6);
  margin: 0;
}
.count-tag {
  font-size: 13px;
  color: var(--color-gray-4);
  background: #e8f3ff;
  padding: 4px 12px;
  border-radius: var(--radius-full);
}

.comment-list-wrap {
  max-height: 400px;
  overflow-y: auto;
  padding-right: 8px;
}
.comment-list-wrap::-webkit-scrollbar {
  width: 4px;
}
.comment-list-wrap::-webkit-scrollbar-thumb {
  background: var(--color-gray-3);
  border-radius: var(--radius-full);
}

.empty-comment-box {
  text-align: center;
  padding: 40px 20px;
  color: var(--color-gray-4);
}
.empty-svg {
  width: 56px;
  height: 56px;
  margin-bottom: 12px;
}

.comment-card {
  padding: 16px;
  background: #ffffff;
  border-radius: var(--radius-md);
  border: 1px solid #ddecfc;
  margin-bottom: 12px;
  transition: var(--transition);
}
.comment-card:hover {
  box-shadow: var(--shadow-sm);
}
.comment-top {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}
.user-avatar {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-full);
  flex-shrink: 0;
  overflow: hidden;
}
.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.avatar-text {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
}
.user-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.username {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-gray-6);
}
.time-text {
  font-size: 11px;
  color: var(--color-gray-4);
}
.comment-text {
  font-size: 14px;
  color: var(--color-gray-5);
  line-height: 1.7;
  margin-left: 46px;
}
.media-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin: 10px 0 0 46px;
}
.media-item {
  width: 72px;
  height: 72px;
  border-radius: var(--radius-sm);
  overflow: hidden;
  border: 1px solid var(--color-gray-2);
}
.media-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.reply-btn-wrap {
  margin-left: 46px;
  margin-top: 8px;
}
.reply-svg {
  width: 12px;
  height: 12px;
  margin-right: 4px;
}

/* 弹窗通用样式 */
:deep(.common-dialog .el-dialog__header) {
  padding: 20px 24px;
}
:deep(.common-dialog .el-dialog__body) {
  padding: 0 24px 20px;
}
:deep(.common-dialog .el-dialog__footer) {
  padding: 12px 24px 20px;
}
:deep(.el-dialog) {
  border-radius: var(--radius-lg);
}
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
.tip-text {
  font-size: 12px;
  color: var(--color-gray-4);
  margin: 6px 0 0;
}
.img-preview-box {
  margin-top: 10px;
}
.preview-img {
  width: 120px;
  height: auto;
  border-radius: var(--radius-sm);
  border: 1px solid var(--color-gray-2);
}
.form-tip {
  font-size: 12px;
  color: var(--color-gray-4);
  margin-left: 8px;
}

/* 优惠券弹窗卡片 */
.coupon-list-container {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.empty-coupon {
  text-align: center;
  padding: 30px 0;
  color: var(--color-gray-4);
}
.coupon-card-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px;
  border: 1px solid #ffe8d6;
  border-radius: var(--radius-md);
  background: #fffaf5;
}
.coupon-left-block {
  flex-shrink: 0;
  text-align: center;
  min-width: 70px;
}
.money {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-orange);
}
.limit {
  font-size: 11px;
  color: var(--color-gray-4);
}
.coupon-right-block {
  flex: 1;
}
.coupon-type {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-gray-6);
}
.scope {
  font-size: 12px;
  color: var(--color-gray-4);
  margin: 4px 0;
}
.meta-row {
  display: flex;
  gap: 12px;
  font-size: 11px;
  color: var(--color-gray-4);
}
.save-tip {
  font-size: 12px;
  color: var(--color-green);
  margin-top: 4px;
}
.btn-block {
  flex-shrink: 0;
}
.claimed-text {
  font-size: 13px;
  color: var(--color-gray-4);
}

/* 加载页 */
.loading-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  color: var(--color-gray-4);
}
.loading-circle {
  width: 48px;
  height: 48px;
  border: 3px solid var(--color-gray-2);
  border-top-color: var(--color-main);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}
.loading-text {
  font-size: 14px;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}

</style>