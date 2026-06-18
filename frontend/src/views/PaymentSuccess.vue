<script setup lang="ts">
import { ref, onMounted, nextTick, inject, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { api } from '../api';

const route = useRoute();
const router = useRouter();
const user = inject('user', ref<any>(null));

const orderId = ref('');
const orderInfo = ref<any>(null);
const showNotif = ref(false);
const showSuccessAnimation = ref(false);
const goodsInfo = ref<any>(null);

const statusMap: Record<number, string> = {
  0: '待付款',
  1: '待发货',
  2: '待收货',
  3: '已完成',
  4: '已取消'
};

const showMemberPrice = computed(() => {
  const discount = user.value?.discount;
  return discount !== undefined && discount !== null && discount < 1;
});

const getMemberDiscount = () => {
  return (user.value?.discount !== undefined && user.value?.discount !== null) ? user.value.discount : 1;
};

const loadGoodsInfo = async (goodsId: number) => {
  try {
    const res = await api.getGoodsDetail(goodsId);
    if (res.data.code === 0) {
      goodsInfo.value = res.data.data;
    }
  } catch (err) {
    console.error('加载商品信息失败', err);
  }
};

const loadOrderInfo = async () => {
  try {
    orderId.value = route.params.id as string;
    const res = await api.getOrderInfo(orderId.value);
    if (res.data.code === 0) {
      orderInfo.value = res.data.data;
      if (orderInfo.value?.goodsId) {
        await loadGoodsInfo(orderInfo.value.goodsId);
      }
    }
  } catch (err) {
    console.error('加载订单失败');
  }
};

const goToOrders = () => {
  router.push('/orders');
};

const goHome = () => {
  router.push('/');
};

onMounted(async () => {
  await loadOrderInfo();
  await nextTick();
  showSuccessAnimation.value = true;
  
  setTimeout(() => {
    showNotif.value = true;
    setTimeout(() => {
      showNotif.value = false;
    }, 4000);
  }, 500);
});
</script>

<template>
  <div class="success-page">
    <!-- 成功动画区域 -->
    <div class="success-animation-container">
      <div :class="['success-icon-wrapper', { show: showSuccessAnimation }]">
        <div class="success-circle">
          <svg class="success-checkmark" viewBox="0 0 52 52">
            <circle class="checkmark-circle" cx="26" cy="26" r="25" fill="none" />
            <path class="checkmark-check" fill="none" d="M14.1 27.2l7.1 7.2 16.7-16.8" />
          </svg>
        </div>
      </div>
      <h1 :class="['success-title', { show: showSuccessAnimation }]">支付成功</h1>
      <p :class="['success-subtitle', { show: showSuccessAnimation }]">感谢您的购买！订单已确认</p>
    </div>

    <!-- 支付金额显示 -->
  <div :class="['price-display', { show: showSuccessAnimation }]" v-if="orderInfo">
    <div class="order-number">订单号：{{ orderId }}</div>
    <div class="price-wrapper">
      <div v-if="goodsInfo && showMemberPrice" class="original-price-line">
        <span class="price-label-text">原价</span>
        <span class="original-amount">￥{{ goodsInfo.price.toFixed(2) }}</span>
      </div>
      <!-- 营销活动优惠 -->
      <div v-if="orderInfo.promotionDiscount && orderInfo.promotionDiscount > 0" class="promotion-discount-line">
        <span class="price-label-text">
          <span v-if="orderInfo.promotionType === 1">满减优惠</span>
          <span v-if="orderInfo.promotionType === 2">限时折扣</span>
          <span v-if="orderInfo.promotionType === 3">团购优惠</span>
        </span>
        <span class="promotion-discount">-￥{{ orderInfo.promotionDiscount.toFixed(2) }}</span>
      </div>
      <div v-if="orderInfo.couponAmount && orderInfo.couponAmount > 0" class="coupon-discount-line">
        <span class="price-label-text">优惠券抵扣</span>
        <span class="coupon-discount">-￥{{ orderInfo.couponAmount.toFixed(2) }}</span>
      </div>
      <div class="price-value">
        <span class="currency">￥</span>
        <span class="amount">{{ Number(orderInfo.amount).toFixed(2) }}</span>
      </div>
      <div v-if="showMemberPrice" class="member-price-info">
        <span class="member-tag">会员价</span>
        <span class="member-discount">({{ (getMemberDiscount() * 100).toFixed(0) }}折)</span>
      </div>
      <!-- 团购状态提示 -->
      <div v-if="orderInfo.promotionType === 3 && orderInfo.groupStatus === 0" class="group-status-info">
        <span class="group-tag">⏳ 等待拼团</span>
        <span class="group-hint">拼团成功后订单将生效</span>
      </div>
    </div>
  </div>
    
    <!-- 操作按钮 -->
    <div :class="['action-buttons', { show: showSuccessAnimation }]">
      <button class="btn btn-primary" @click="goHome">
        <span class="btn-icon"><</span>
        返回
      </button>
    </div>
    
    <!-- 成功通知 -->
    <Teleport to="body">
      <Transition name="notification">
        <div v-if="showNotif" class="notification">
          <div class="notification-icon">🎉</div>
          <div class="notification-content">
            <div class="notification-title">支付成功</div>
            <div class="notification-desc">您的订单已成功支付</div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
.success-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8ec 100%);
  padding: 60px 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

/* 成功动画区域 */
.success-animation-container {
  text-align: center;
  margin-bottom: 40px;
}

.success-icon-wrapper {
  margin-bottom: 24px;
  opacity: 0;
  transform: scale(0.6);
  transition: all 0.5s cubic-bezier(0.68, -0.55, 0.265, 1.55);
}

.success-icon-wrapper.show {
  opacity: 1;
  transform: scale(1);
}

.success-circle {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  background: linear-gradient(135deg, #4caf50 0%, #45a049 100%);
  box-shadow: 0 10px 40px rgba(76, 175, 80, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
}

.success-checkmark {
  width: 80px;
  height: 80px;
}

.checkmark-circle {
  stroke: white;
  stroke-width: 3;
  stroke-dasharray: 166;
  stroke-dashoffset: 166;
  animation: stroke 0.6s cubic-bezier(0.65, 0, 0.45, 1) forwards;
}

.checkmark-check {
  stroke: white;
  stroke-width: 4;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-dasharray: 48;
  stroke-dashoffset: 48;
  animation: stroke 0.3s cubic-bezier(0.65, 0, 0.45, 1) forwards 0.5s;
}

@keyframes stroke {
  100% {
    stroke-dashoffset: 0;
  }
}

.success-title {
  font-size: 32px;
  font-weight: 700;
  color: #1f2937;
  margin: 0 0 8px 0;
  opacity: 0;
  transform: translateY(20px);
  transition: all 0.5s ease 0.3s;
}

.success-title.show {
  opacity: 1;
  transform: translateY(0);
}

.success-subtitle {
  font-size: 16px;
  color: #6b7280;
  margin: 0;
  opacity: 0;
  transform: translateY(20px);
  transition: all 0.5s ease 0.4s;
}

.success-subtitle.show {
  opacity: 1;
  transform: translateY(0);
}

/* 价格显示 */
.price-display {
  width: 100%;
  max-width: 420px;
  padding: 0;
  text-align: center;
  opacity: 0;
  transform: translateY(20px);
  transition: all 0.5s ease 0.5s;
  margin-bottom: 32px;
}

.price-display.show {
  opacity: 1;
  transform: translateY(0);
}

.order-number {
  font-size: 14px;
  color: #9ca3af;
  margin-bottom: 16px;
  font-weight: 400;
}

.price-label {
  font-size: 16px;
  color: #6b7280;
  margin-bottom: 12px;
}

.price-value {
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 6px;
  margin-bottom: 16px;
}

.price-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.original-price-line {
  display: flex;
  align-items: center;
  gap: 8px;
  opacity: 0.7;
}

.price-label-text {
  font-size: 14px;
  color: #6b7280;
}

.original-amount {
  font-size: 18px;
  color: #9ca3af;
  text-decoration: line-through;
}

.member-price-info {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 4px;
}

.member-tag {
  background: linear-gradient(135deg, #ff1744 0%, #ff5252 100%);
  color: white;
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 12px;
  font-weight: 600;
}

.member-discount {
  font-size: 14px;
  color: #ff1744;
  font-weight: 600;
}

.coupon-discount-line {
  display: flex;
  align-items: center;
  gap: 8px;
  opacity: 0.7;
}

.coupon-discount {
  font-size: 18px;
  color: #4caf50;
  font-weight: 600;
}

.promotion-discount-line {
  display: flex;
  align-items: center;
  gap: 8px;
  opacity: 0.7;
}

.promotion-discount {
  font-size: 18px;
  color: #ff6700;
  font-weight: 600;
}

.group-status-info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
}

.group-tag {
  background: linear-gradient(135deg, #ff9800 0%, #f57c00 100%);
  color: white;
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 12px;
  font-weight: 600;
}

.group-hint {
  font-size: 12px;
  color: #909399;
}

.currency {
  font-size: 28px;
  font-weight: 700;
  color: #1f2937;
}

.price-value .amount {
  font-size: 48px;
  font-weight: 800;
  color: #1f2937;
  line-height: 1;
}

.order-id-text {
  font-family: 'SF Mono', Monaco, monospace;
  font-size: 14px;
  color: #9ca3af;
}

/* 操作按钮 */
.action-buttons {
  display: flex;
  justify-content: center;
  gap: 16px;
  width: 100%;
  max-width: 560px;
  opacity: 0;
  transform: translateY(20px);
  transition: all 0.5s ease 0.7s;
}

.action-buttons.show {
  opacity: 1;
  transform: translateY(0);
}

.btn {
  padding: 16px 48px;
  border: none;
  border-radius: 12px;
  cursor: pointer;
  font-size: 16px;
  font-weight: 600;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.btn-icon {
  font-size: 18px;
}

.btn-primary {
  background: white;
  color: #374151;
  border: 2px solid #e5e7eb;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.btn-primary:hover {
  border-color: #d1d5db;
  background: #f9fafb;
  transform: translateY(-2px);
}

.btn-secondary {
  background: white;
  color: #374151;
  border: 2px solid #e5e7eb;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.btn-secondary:hover {
  border-color: #d1d5db;
  background: #f9fafb;
  transform: translateY(-2px);
}

/* 通知 */
.notification {
  position: fixed;
  top: 24px;
  right: 24px;
  background: white;
  padding: 18px 24px;
  border-radius: 12px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.15);
  z-index: 99999;
  display: flex;
  align-items: center;
  gap: 14px;
  border-left: 4px solid #4caf50;
}

.notification-icon {
  font-size: 32px;
}

.notification-content {
  flex: 1;
}

.notification-title {
  font-size: 16px;
  font-weight: 700;
  color: #1f2937;
  margin-bottom: 2px;
}

.notification-desc {
  font-size: 14px;
  color: #6b7280;
}

.notification-enter-active {
  animation: slideInFromRight 0.4s ease-out;
}

.notification-leave-active {
  animation: slideOutToRight 0.4s ease-in;
}

@keyframes slideInFromRight {
  from {
    transform: translateX(120px);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

@keyframes slideOutToRight {
  from {
    transform: translateX(0);
    opacity: 1;
  }
  to {
    transform: translateX(120px);
    opacity: 0;
  }
}

/* 响应式 */
@media (max-width: 640px) {
  .success-page {
    padding: 40px 16px;
  }
  
  .success-title {
    font-size: 26px;
  }
  
  .info-grid {
    grid-template-columns: 1fr;
  }
  
  .action-buttons {
    flex-direction: column;
  }
}
</style>
