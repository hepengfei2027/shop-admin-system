<script setup lang="ts">
import { ref, onMounted, inject, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { api } from '../api';
import AddressManager from '../components/AddressManager.vue';

const route = useRoute();
const router = useRouter();
const user = inject('user', ref<any>(null));

const orderId = ref<number>();
const orderInfo = ref<any>(null);
const goodsInfo = ref<any>(null);
const selectedPaymentMethod = ref('wechat');
const isPaying = ref(false);
const selectedAddress = ref<any>(null);
const showAddressManager = ref(false);
const userCoupons = ref<any[]>([]);
const selectedCoupon = ref<any>(null);
const showCouponSelect = ref(false);

const freight = computed(() => {
  if (!goodsInfo.value) return 0;
  return goodsInfo.value.freight || 0;
});

const totalAmount = computed(() => {
  if (!orderInfo.value) return 0;
  const price = orderInfo.value.amount || 0;
  const total = price + freight.value;
  if (selectedCoupon.value) {
    return Math.max(0, total - selectedCoupon.value.amount);
  }
  return total;
});

const paymentMethods = [
  { value: 'wechat', label: '微信支付', icon: '💳' },
  { value: 'alipay', label: '支付宝', icon: '📱' },
  { value: 'balance', label: '余额支付', icon: '💰' }
];

const statusMap: Record<number, string> = {
  0: '待付款',
  1: '待发货',
  2: '待收货',
  3: '已完成',
  4: '已取消'
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

const loadOrderInfo = async () => {
  try {
    orderId.value = Number(route.params.id);
    const res = await api.getOrderInfo(orderId.value);
    if (res.data.code === 0) {
      orderInfo.value = res.data.data;
      
      // 检查订单状态
      if (orderInfo.value.status !== 0) {
        ElMessage.warning('订单状态：' + statusMap[orderInfo.value.status]);
        router.push('/orders');
        return;
      }
      
      // 加载商品信息
      if (orderInfo.value.goodsId) {
        const goodsRes = await api.getGoodsDetail(orderInfo.value.goodsId);
        if (goodsRes.data.code === 0) {
          goodsInfo.value = goodsRes.data.data;
        }
      }
    } else {
      ElMessage.error('订单不存在');
      router.push('/');
    }
  } catch (err) {
    ElMessage.error('加载订单失败');
    router.push('/');
  }
};

const loadDefaultAddress = async () => {
  if (!user.value) return;
  try {
    const res = await api.getAddresses(user.value.id);
    if (res.data.code === 0) {
      const addresses = res.data.data || [];
      const defaultAddr = addresses.find((a: any) => a.isDefault);
      if (defaultAddr) {
        selectedAddress.value = defaultAddr;
      }
    }
  } catch (err) {
    console.error('加载默认地址失败', err);
  }
};

const loadUserCoupons = async () => {
  if (!user.value) return;
  try {
    const res = await api.getUserCoupons(user.value.id);
    if (res.data.code === 0) {
      const coupons = res.data.data || [];
      // 只显示适用于当前商品的优惠券
      if (orderInfo.value && orderInfo.value.goodsId) {
        userCoupons.value = coupons.filter((c: any) => c.goodsId === orderInfo.value.goodsId);
      }
    }
  } catch (err) {
    console.error('加载优惠券失败', err);
  }
};

const selectCoupon = (coupon: any) => {
  selectedCoupon.value = coupon;
  showCouponSelect.value = false;
};

const clearCoupon = () => {
  selectedCoupon.value = null;
};

const handleAddressSelect = (address: any) => {
  selectedAddress.value = address;
  showAddressManager.value = false;
};

const handlePayment = async () => {
  if (isPaying.value || !user.value) return;
  
  isPaying.value = true;
  
  try {
    await api.payOrderV2(orderId.value!, user.value.id);
    // 如果使用了优惠券，标记为已使用
    if (selectedCoupon.value) {
      await api.useCoupon(selectedCoupon.value.id);
    }
    // 模拟支付处理延迟，让用户看到加载动画
    setTimeout(() => {
      router.push(`/payment-success/${orderId.value}`);
    }, 1500);
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.msg || '支付失败');
    isPaying.value = false;
  }
};

const cancelOrder = async () => {
  if (!user.value) return;
  try {
    await api.cancelOrderV2(orderId.value!, user.value.id);
    ElMessage.success('订单已取消');
    router.push('/orders');
  } catch (err) {
    ElMessage.error('取消失败');
  }
};

const goBack = async () => {
  // 先取消订单
  if (user.value && orderId.value) {
    try {
      await api.cancelOrderV2(orderId.value, user.value.id);
    } catch (err) {
      // 即使取消失败也返回
    }
  }
  // 返回上一页
  router.back();
};

onMounted(async () => {
  await loadOrderInfo();
  loadDefaultAddress();
  loadUserCoupons();
});
</script>

<template>
  <div class="payment-page">
    <!-- 全屏支付中加载动画 -->
    <div v-if="isPaying" class="payment-loading-overlay">
      <div class="payment-loading-content">
        <div class="loading-spinner-large">
          <svg viewBox="0 0 50 50">
            <circle cx="25" cy="25" r="20" fill="none" stroke-width="4" stroke-linecap="round" />
          </svg>
        </div>
        <div class="loading-text">
          <h3>正在处理支付</h3>
          <p>请稍候，支付正在处理中...</p>
        </div>
        <div class="loading-dots">
          <span></span>
          <span></span>
          <span></span>
        </div>
      </div>
    </div>
    
    <div class="header-section">
      <button class="back-btn" @click="goBack" :disabled="isPaying">
        <svg class="back-icon" width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path d="M15 18L9 12L15 6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
        返回
      </button>
      <h2>确认支付</h2>
      <div style="width: 80px;"></div>
    </div>
    
    <div class="content-wrapper">
      <!-- 左侧内容区域 -->
      <div class="left-section">
        <div class="order-info" v-if="orderInfo">
          <!-- 收货地址 -->
          <div class="address-section">
            <div class="section-header">
              <svg class="section-icon" width="20" height="20" viewBox="0 0 24 24" fill="none">
                <path d="M21 10C21 17 12 23 12 23S3 17 3 10C3 5.02944 7.02944 1 12 1C16.9706 1 21 5.02944 21 10Z" stroke="currentColor" stroke-width="2"/>
                <circle cx="12" cy="10" r="3" stroke="currentColor" stroke-width="2"/>
              </svg>
              <h3>收货地址</h3>
            </div>
            <div v-if="selectedAddress" class="selected-address" @click="showAddressManager = true">
              <div class="address-content">
                <div class="address-info">
                  <span class="name">{{ selectedAddress.name }}</span>
                  <span class="phone">{{ selectedAddress.phone }}</span>
                </div>
                <div class="address-detail">
                  {{ selectedAddress.province }} {{ selectedAddress.city }} {{ selectedAddress.district }} {{ selectedAddress.detail }}
                </div>
              </div>
              <svg class="address-arrow" width="20" height="20" viewBox="0 0 24 24" fill="none">
                <path d="M9 6L15 12L9 18" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </div>
            <div v-else class="no-address" @click="showAddressManager = true">
              <svg class="add-icon" width="24" height="24" viewBox="0 0 24 24" fill="none">
                <path d="M12 5V19M5 12H19" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
              </svg>
              <span>请选择收货地址</span>
            </div>
          </div>
          
          <!-- 商品信息 -->
          <div class="goods-section">
            <div class="section-header">
              <svg class="section-icon" width="20" height="20" viewBox="0 0 24 24" fill="none">
                <path d="M20 7H4C3.44772 7 3 7.44772 3 8V19C3 19.5523 3.44772 20 4 20H20C20.5523 20 21 19.5523 21 19V8C21 7.44772 20.5523 7 20 7Z" stroke="currentColor" stroke-width="2"/>
                <path d="M16 3V7M8 3V7M4 12H20" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
              </svg>
              <h3>商品信息</h3>
            </div>
            <div class="goods-info" v-if="goodsInfo">
              <img v-if="goodsInfo.imageUrl" :src="goodsInfo.imageUrl" class="goods-image" />
              <div class="goods-details">
                <h4>{{ goodsInfo.title }}</h4>
                <p class="goods-desc">{{ goodsInfo.description }}</p>
                <p class="goods-price">￥{{ orderInfo.amount }}</p>
              </div>
            </div>
          </div>
          
          <!-- 订单详情 -->
          <div class="order-details-section">
            <div class="section-header">
              <svg class="section-icon" width="20" height="20" viewBox="0 0 24 24" fill="none">
                <path d="M14 2H6C5.46957 2 4.96086 2.21071 4.58579 2.58579C4.21071 2.96086 4 3.46957 4 4V20C4 20.5304 4.21071 21.0391 4.58579 21.4142C4.96086 21.7893 5.46957 22 6 22H18C18.5304 22 19.0391 21.7893 19.4142 21.4142C19.7893 21.0391 20 20.5304 20 20V8L14 2Z" stroke="currentColor" stroke-width="2"/>
                <path d="M14 2V8H20" stroke="currentColor" stroke-width="2"/>
                <path d="M8 13H16M8 17H14" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
              </svg>
              <h3>订单详情</h3>
            </div>
            <div class="order-detail-row">
              <span class="label">订单号</span>
              <span class="value">{{ orderId }}</span>
            </div>
            <div class="order-detail-row">
              <span class="label">下单时间</span>
              <span class="value">{{ formatDateTime(orderInfo.createTime) }}</span>
            </div>
          </div>
        </div>
        
        <!-- 支付方式 -->
        <div class="payment-section">
          <div class="section-header">
            <svg class="section-icon" width="20" height="20" viewBox="0 0 24 24" fill="none">
              <rect x="2" y="5" width="20" height="14" rx="2" stroke="currentColor" stroke-width="2"/>
              <path d="M2 10H22" stroke="currentColor" stroke-width="2"/>
            </svg>
            <h3>支付方式</h3>
          </div>
          <div class="payment-list">
            <div
              v-for="method in paymentMethods"
              :key="method.value"
              :class="['payment-item', { selected: selectedPaymentMethod === method.value }]"
              @click="selectedPaymentMethod = method.value"
            >
              <div class="payment-icon-wrapper">
                <span class="payment-icon">{{ method.icon }}</span>
              </div>
              <span class="payment-name">{{ method.label }}</span>
              <div class="payment-check" v-if="selectedPaymentMethod === method.value">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                  <path d="M20 6L9 17L4 12" stroke="white" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
              </div>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 右侧内容区域（价格汇总和操作按钮） -->
      <div class="right-section">
        <!-- 优惠券选择 -->
        <div class="coupon-section">
          <div class="section-header">
            <svg class="section-icon" width="20" height="20" viewBox="0 0 24 24" fill="none">
              <path d="M2 7C2 5.89543 2.89543 5 4 5H20C21.1046 5 22 5.89543 22 7V17C22 18.1046 21.1046 19 20 19H4C2.89543 19 2 18.1046 2 17V7Z" stroke="currentColor" stroke-width="2"/>
              <path d="M2 12C4.20914 12 6 10.2091 6 8C6 10.2091 7.79086 12 10 12C7.79086 12 6 13.7909 6 16C6 13.7909 4.20914 12 2 12Z" stroke="currentColor" stroke-width="2"/>
            </svg>
            <h3>优惠券</h3>
          </div>
          <div class="coupon-select" @click="showCouponSelect = true">
            <div v-if="selectedCoupon" class="selected-coupon">
              <div class="coupon-badge">优惠券</div>
              <span class="coupon-amount">-￥{{ selectedCoupon.amount }}</span>
              <span class="change-text">更换</span>
            </div>
            <div v-else-if="userCoupons.length > 0" class="no-coupon-selected">
              <span class="coupon-count">{{ userCoupons.length }}张可用</span>
              <svg class="arrow-icon" width="20" height="20" viewBox="0 0 24 24" fill="none">
                <path d="M9 6L15 12L9 18" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </div>
            <div v-else class="no-coupon">
              <span>暂无可用优惠券</span>
            </div>
          </div>
        </div>
        
        <!-- 价格汇总 -->
        <div class="price-summary">
          <div class="price-row">
            <span class="price-label">商品价格</span>
            <span class="price-value">￥{{ orderInfo?.amount || 0 }}</span>
          </div>
          <div class="price-row">
            <span class="price-label">运费</span>
            <span class="price-value">{{ freight === 0 ? '免运费' : '￥' + freight }}</span>
          </div>
          <div class="price-row discount" v-if="selectedCoupon">
            <span class="price-label">优惠券</span>
            <span class="price-value">-￥{{ selectedCoupon.amount }}</span>
          </div>
          <div class="divider"></div>
          <div class="total-row">
            <span class="total-label">实付金额</span>
            <div class="total-price-wrapper">
              <span class="currency">￥</span>
              <span class="total-price">{{ totalAmount }}</span>
            </div>
          </div>
          
          <!-- 操作栏 -->
          <div class="action-section">
            <div class="btn-group">
              <button class="cancel-btn" @click="cancelOrder">取消订单</button>
              <button class="pay-btn" :disabled="isPaying" @click="handlePayment">
                <span v-if="isPaying" class="paying-text">
                  <svg class="loading-spinner" width="20" height="20" viewBox="0 0 24 24" fill="none">
                    <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-dasharray="50 50"/>
                  </svg>
                  支付中...
                </span>
                <span v-else>
                  立即支付
                </span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 地址选择弹窗 -->
    <AddressManager v-model="showAddressManager" :userId="user?.id" @close="showAddressManager = false" @select="handleAddressSelect" />
    
    <!-- 优惠券选择弹窗 -->
    <el-dialog v-model="showCouponSelect" title="选择优惠券" width="420px" class="coupon-dialog">
      <div class="coupon-list">
        <div 
          v-for="coupon in userCoupons" 
          :key="coupon.id"
          :class="['coupon-item', { selected: selectedCoupon?.id === coupon.id }]"
          @click="selectCoupon(coupon)"
        >
          <div class="coupon-left">
            <div class="coupon-amount-value">￥{{ coupon.amount }}</div>
            <div class="coupon-label-text">优惠券</div>
          </div>
          <div class="coupon-right">
            <div class="coupon-description">仅限本商品使用</div>
            <div class="coupon-status-text">
              {{ selectedCoupon?.id === coupon.id ? '已选择' : '立即使用' }}
            </div>
          </div>
          <div v-if="selectedCoupon?.id === coupon.id" class="coupon-check-mark">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
              <path d="M20 6L9 17L4 12" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
        </div>
        <div class="coupon-item no-use" @click="clearCoupon(); showCouponSelect = false">
          <div class="coupon-left">
            <div class="coupon-amount-value no-use-text">不使用</div>
          </div>
          <div class="coupon-right">
            <div class="coupon-description">不使用优惠券</div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.payment-page {
  max-width: 1000px;
  margin: 0 auto;
  min-height: 100vh;
  background: #f5f7fa;
  padding: 24px;
}

.header-section {
  display: flex;
  align-items: center;
  position: relative;
  padding: 20px 0;
  margin-bottom: 24px;
  border-bottom: 2px solid #e0e6ed;
}

.back-btn {
  position: relative;
  left: 0;
  top: 0;
  transform: none;
  background: white;
  border: 1px solid #dcdfe6;
  cursor: pointer;
  padding: 10px 16px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.2s ease;
  z-index: 10;
}

.back-btn:hover {
  border-color: #ff6700;
  color: #ff6700;
}

.back-icon {
  color: #606266;
  display: block;
}

.header-section h2 {
  flex: 1;
  text-align: center;
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  color: #303133;
}

.content-wrapper {
  padding: 0;
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

.left-section {
  flex: 0 0 650px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.right-section {
  flex: 1;
  min-width: 300px;
  position: sticky;
  top: 24px;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
  padding: 0 20px;
}

.section-icon {
  color: #ff6700;
}

h3 {
  font-size: 16px;
  font-weight: 600;
  margin: 0;
  color: #303133;
}

/* 地址区域 */
.address-section {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.selected-address {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 16px;
  background: #fafafa;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.selected-address:hover {
  border-color: #ff6700;
  background: #fffaf5;
}

.address-content {
  flex: 1;
}

.address-info {
  display: flex;
  gap: 20px;
  margin-bottom: 10px;
}

.address-info .name {
  font-weight: 600;
  color: #303133;
  font-size: 16px;
}

.address-info .phone {
  color: #606266;
  font-size: 16px;
}

.address-detail {
  color: #606266;
  font-size: 14px;
  line-height: 1.6;
}

.address-arrow {
  color: #909399;
  flex-shrink: 0;
  margin-left: 16px;
  margin-top: 4px;
}

.no-address {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 32px;
  border: 2px dashed #dcdfe6;
  border-radius: 6px;
  cursor: pointer;
  color: #909399;
  font-size: 15px;
  transition: all 0.2s ease;
}

.no-address:hover {
  border-color: #ff6700;
  color: #ff6700;
  background: #fffaf5;
}

.add-icon {
  color: inherit;
}

/* 商品区域 */
.goods-section {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.goods-info {
  display: flex;
  gap: 20px;
  padding: 20px;
  background: #fafafa;
  border-radius: 6px;
}

.goods-image {
  width: 120px;
  height: 120px;
  object-fit: cover;
  border-radius: 6px;
  background: #f5f7fa;
}

.goods-details {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.goods-details h4 {
  margin: 0 0 10px 0;
  font-size: 16px;
  font-weight: 500;
  color: #303133;
  line-height: 1.5;
}

.goods-desc {
  margin: 0 0 12px 0;
  font-size: 14px;
  color: #909399;
  display: block;
}

.goods-price {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #ff6700;
}

/* 订单详情 */
.order-details-section {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.order-detail-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  font-size: 14px;
}

.order-detail-row .label {
  color: #909399;
}

.order-detail-row .value {
  color: #303133;
  font-family: 'SF Mono', Monaco, monospace;
}

/* 优惠券区域 */
.coupon-section {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.coupon-select {
  padding: 16px 20px;
  background: #fffaf5;
  border: 1px solid #ffd5b3;
  border-radius: 6px;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: center;
  transition: all 0.2s ease;
}

.coupon-select:hover {
  border-color: #ff6700;
}

.selected-coupon {
  display: flex;
  align-items: center;
  gap: 12px;
}

.coupon-badge {
  background: #ff6700;
  color: white;
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 13px;
  font-weight: 500;
}

.coupon-amount {
  font-weight: 600;
  color: #ff6700;
  font-size: 18px;
}

.change-text {
  color: #909399;
  font-size: 14px;
  font-weight: 400;
}

.no-coupon-selected {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  color: #606266;
  font-size: 15px;
}

.coupon-count {
  font-weight: 400;
}

.arrow-icon {
  color: #c0c4cc;
}

.no-coupon {
  color: #909399;
  font-size: 15px;
}

/* 支付方式 */
.payment-section {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.payment-list {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.payment-item {
  display: flex;
  align-items: center;
  padding: 20px;
  background: white;
  border: 2px solid #e4e7ed;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;
}

.payment-item:last-child {
  border-bottom: 2px solid #e4e7ed;
}

.payment-item:hover {
  border-color: #ff6700;
  background: #fffaf5;
}

.payment-item.selected {
  border-color: #ff6700;
  background: #fffaf5;
}

.payment-icon-wrapper {
  width: 50px;
  height: 50px;
  background: #f5f7fa;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
}

.payment-icon {
  font-size: 28px;
}

.payment-name {
  flex: 1;
  font-size: 16px;
  font-weight: 500;
  color: #303133;
}

.payment-check {
  width: 24px;
  height: 24px;
  background: #ff6700;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 价格汇总 */
.price-summary {
  background: white;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.price-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  font-size: 15px;
}

.price-row.discount .price-value {
  color: #ff6700;
  font-weight: 600;
}

.price-label {
  color: #606266;
}

.price-value {
  color: #303133;
  font-weight: 500;
}

.divider {
  height: 1px;
  background: #ebeef5;
  margin: 16px 0;
}

.total-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
}

.total-label {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.total-price-wrapper {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.currency {
  font-size: 18px;
  font-weight: 600;
  color: #ff6700;
}

.total-price {
  font-size: 32px;
  font-weight: 700;
  color: #ff6700;
  letter-spacing: -1px;
}

/* 底部操作栏 */
.action-section {
  position: relative;
  bottom: auto;
  left: auto;
  transform: none;
  width: 100%;
  max-width: none;
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 0;
  margin-top: 20px;
  background: transparent;
  box-shadow: none;
  z-index: 100;
}

.btn-group {
  display: flex;
  gap: 16px;
}

.cancel-btn, .pay-btn {
  padding: 14px 32px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 16px;
  font-weight: 500;
  transition: all 0.2s ease;
  flex: 1;
}

.cancel-btn {
  background: white;
  color: #606266;
  border: 1px solid #dcdfe6;
}

.cancel-btn:hover {
  border-color: #909399;
  color: #303133;
}

.pay-btn {
  background: linear-gradient(135deg, #ff6700 0%, #ff5000 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(255, 103, 0, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.pay-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #ff5000 0%, #ff3d00 100%);
  box-shadow: 0 6px 16px rgba(255, 103, 0, 0.4);
}

.pay-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.paying-text {
  display: flex;
  align-items: center;
  gap: 10px;
}

.loading-spinner {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* 优惠券列表 */
.coupon-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-height: 500px;
  overflow-y: auto;
  padding: 8px;
}

.coupon-item {
  display: flex;
  align-items: center;
  padding: 20px;
  border-radius: 8px;
  cursor: pointer;
  position: relative;
  background: #fffaf5;
  border: 2px solid #ffd5b3;
  transition: all 0.2s ease;
}

.coupon-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(255, 103, 0, 0.15);
}

.coupon-item.selected {
  background: #e8f5e9;
  border-color: #81c784;
}

.coupon-item.no-use {
  background: #f5f7fa;
  border: 2px dashed #dcdfe6;
}

.coupon-left {
  min-width: 100px;
  text-align: center;
  padding-right: 20px;
  border-right: 2px dashed rgba(0, 0, 0, 0.08);
}

.coupon-amount-value {
  font-size: 30px;
  font-weight: 700;
  color: #ff6700;
}

.no-use-text {
  font-size: 16px;
  font-weight: 500;
  color: #606266;
}

.coupon-label-text {
  font-size: 13px;
  color: #ff6700;
  margin-top: 6px;
  font-weight: 500;
}

.coupon-right {
  flex: 1;
  padding-left: 20px;
}

.coupon-description {
  color: #606266;
  font-size: 15px;
  margin-bottom: 8px;
}

.coupon-status-text {
  color: #ff6700;
  font-size: 14px;
  font-weight: 500;
}

.coupon-check-mark {
  position: absolute;
  right: 20px;
  top: 50%;
  transform: translateY(-50%);
  color: #4caf50;
}

/* 对话框样式 */
:deep(.coupon-dialog .el-dialog__header) {
  padding: 20px 24px 16px;
  border-bottom: 1px solid #ebeef5;
}

:deep(.coupon-dialog .el-dialog__title) {
  font-size: 18px;
  font-weight: 600;
}

:deep(.coupon-dialog .el-dialog__body) {
  padding: 20px 24px 24px;
}

/* 支付中加载动画 */
.payment-loading-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 99999;
  backdrop-filter: blur(4px);
}

.payment-loading-content {
  background: white;
  padding: 48px 64px;
  border-radius: 16px;
  text-align: center;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  animation: scaleIn 0.3s ease-out;
}

@keyframes scaleIn {
  from {
    transform: scale(0.9);
    opacity: 0;
  }
  to {
    transform: scale(1);
    opacity: 1;
  }
}

.loading-spinner-large {
  margin-bottom: 24px;
}

.loading-spinner-large svg {
  width: 80px;
  height: 80px;
  animation: rotate 2s linear infinite;
}

.loading-spinner-large circle {
  stroke: #ff6700;
  stroke-dasharray: 88, 132;
  stroke-dashoffset: 0;
  animation: dash 1.5s ease-in-out infinite;
}

@keyframes rotate {
  100% {
    transform: rotate(360deg);
  }
}

@keyframes dash {
  0% {
    stroke-dasharray: 40, 132;
    stroke-dashoffset: 0;
  }
  50% {
    stroke-dasharray: 90, 132;
    stroke-dashoffset: -35;
  }
  100% {
    stroke-dasharray: 90, 132;
    stroke-dashoffset: -124;
  }
}

.loading-text h3 {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 8px 0;
}

.loading-text p {
  font-size: 14px;
  color: #909399;
  margin: 0;
}

.loading-dots {
  display: flex;
  justify-content: center;
  gap: 6px;
  margin-top: 20px;
}

.loading-dots span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #ff6700;
  animation: bounce 1.4s ease-in-out infinite both;
}

.loading-dots span:nth-child(1) {
  animation-delay: -0.32s;
}

.loading-dots span:nth-child(2) {
  animation-delay: -0.16s;
}

@keyframes bounce {
  0%, 80%, 100% {
    transform: scale(0);
    opacity: 0.5;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}
</style>
