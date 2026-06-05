<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { api } from '../api';

const route = useRoute();
const router = useRouter();

const orderId = ref('');
const orderInfo = ref<any>(null);
const showNotif = ref(false);
const showSuccessAnimation = ref(false);

const statusMap: Record<number, string> = {
  0: '待付款',
  1: '待发货',
  2: '待收货',
  3: '已完成',
  4: '已取消'
};

const loadOrderInfo = async () => {
  try {
    orderId.value = route.params.id as string;
    const res = await api.getOrderInfo(orderId.value);
    if (res.data.code === 0) {
      orderInfo.value = res.data.data;
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
     
      <div class="price-value">
        <span class="currency">￥</span>
        <span class="amount">{{ orderInfo.totalAmount || orderInfo.amount || 0 }}</span>
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
