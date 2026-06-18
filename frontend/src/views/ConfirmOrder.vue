<script setup lang="ts">
import { ref, computed, onMounted, inject } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import { api } from '../api';
import AddressManager from '../components/AddressManager.vue';

const router = useRouter();
const route = useRoute();
const user = inject('user', ref<any>(null));

const cartItems = ref<any[]>([]);
const selectedAddress = ref<any>(null);
const showAddressManager = ref(false);
const remark = ref('');
const isPaying = ref(false);
const selectedPaymentMethod = ref('wechat');
const userCoupons = ref<any[]>([]);
const selectedCoupon = ref<any>(null);
const showCouponSelect = ref(false);

// 团购相关
const groupBuyInfo = ref<any>(null);
const isGroupBuyOrder = computed(() => !!groupBuyInfo.value);

// 限时折扣相关
const discountActivity = ref<any>(null);
const discountAmount = computed(() => {
  if (!discountActivity.value) return 0;
  const originalPrice = cartItems.value.reduce((sum, item) => sum + (item.price || 0) * item.quantity, 0);
  const discountedPrice = originalPrice * (discountActivity.value.ruleDetail?.discountRate || 1);
  return Math.max(0, originalPrice - discountedPrice);
});

// 满减相关
const fullReduceActivity = ref<any>(null);
const fullReduceAmount = computed(() => {
  if (!fullReduceActivity.value) return 0;
  
  const goodsTotal = cartItems.value.reduce((total, item) => {
    const price = item.price || 0;
    const memberPrice = price * getMemberDiscount();
    return total + memberPrice * item.quantity;
  }, 0);
  
  let finalGoodsTotal = goodsTotal;
  if (discountActivity.value) {
    finalGoodsTotal = goodsTotal * (discountActivity.value.ruleDetail?.discountRate || 1);
  }
  
  const threshold = fullReduceActivity.value.thresholdAmount || 100;
  const reduction = fullReduceActivity.value.reductionAmount || 10;
  
  // 满减门槛基于商品总价（不含运费）
  if (finalGoodsTotal >= threshold) {
    return reduction;
  }
  return 0;
});

const getCartKey = () => {
  if (user.value) {
    return `cart_${user.value.id}`;
  }
  return 'cart';
};

const getMemberPrice = (price: number) => {
  const discount = (user.value?.discount !== undefined && user.value?.discount !== null) ? user.value.discount : 1;
  return (price * discount).toFixed(2);
};

const getMemberDiscount = () => {
  return (user.value?.discount !== undefined && user.value?.discount !== null) ? user.value.discount : 1;
};

const showMemberPrice = computed(() => {
  const discount = user.value?.discount;
  return discount !== undefined && discount !== null && discount < 1;
});

const freight = computed(() => {
  return cartItems.value.reduce((total, item) => total + (item.freight || 0), 0);
});

const goodsOriginalTotal = computed(() => {
  return cartItems.value.reduce((total, item) => total + (item.price || 0) * item.quantity, 0);
});

const totalAmount = computed(() => {
  // 团购订单不享受折扣和满减
  if (isGroupBuyOrder.value) {
    const goodsTotal = cartItems.value.reduce((total, item) => {
      return total + (item.price || 0) * item.quantity;
    }, 0);
    const total = goodsTotal + freight.value;
    if (selectedCoupon.value) {
      return Math.max(0, total - selectedCoupon.value.amount);
    }
    return total;
  }

  const goodsTotal = cartItems.value.reduce((total, item) => {
    const price = item.price || 0;
    const memberPrice = price * getMemberDiscount();
    return total + memberPrice * item.quantity;
  }, 0);
  
  // 应用限时折扣
  let finalGoodsTotal = goodsTotal;
  if (discountActivity.value) {
    finalGoodsTotal = goodsTotal * (discountActivity.value.ruleDetail?.discountRate || 1);
  }
  
  let total = finalGoodsTotal + freight.value;
  
  // 应用满减
  if (fullReduceActivity.value && fullReduceAmount.value > 0) {
    total = total - fullReduceAmount.value;
  }
  
  // 应用优惠券
  if (selectedCoupon.value) {
    return Math.max(0, total - selectedCoupon.value.amount);
  }
  return total;
});

// 折扣后价格
const discountedGoodsTotal = computed(() => {
  const goodsTotal = cartItems.value.reduce((total, item) => {
    const price = item.price || 0;
    const memberPrice = price * getMemberDiscount();
    return total + memberPrice * item.quantity;
  }, 0);
  
  if (discountActivity.value) {
    return goodsTotal * (discountActivity.value.ruleDetail?.discountRate || 1);
  }
  return goodsTotal;
});

const paymentMethods = [
  { value: 'wechat', label: '微信支付', icon: '💳' },
  { value: 'alipay', label: '支付宝', icon: '📱' },
  { value: 'balance', label: '余额支付', icon: '💰' }
];

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

const loadCartItems = async () => {
  const userStr = localStorage.getItem('user');
  if (userStr) {
    user.value = JSON.parse(userStr);
  }

  const goodsId = route.query.goodsId;
  const goodsIds = route.query.goodsIds;
  const groupBuyId = route.query.groupBuyId;
  const groupPrice = route.query.groupPrice;

  // 加载团购信息
  if (groupBuyId && groupPrice) {
    groupBuyInfo.value = {
      groupBuyId: Number(groupBuyId),
      groupPrice: Number(groupPrice)
    };
  }

  if (goodsIds) {
    const idArray = (goodsIds as string).split(',').map(Number);
    const loadedItems: any[] = [];

    for (const id of idArray) {
      try {
        const res = await api.getGoodsDetail(id);
        if (res.data.code === 0 && res.data.data) {
          loadedItems.push({
            ...res.data.data,
            quantity: 1
          });
        }
      } catch (err) {
        console.error('加载商品信息失败', err);
      }
    }

    cartItems.value = loadedItems;
  } else if (goodsId) {
    try {
      const res = await api.getGoodsDetail(Number(goodsId));
      if (res.data.code === 0 && res.data.data) {
        const item = {
          ...res.data.data,
          quantity: 1
        };
        // 如果有团购价格，使用团购价格
        if (groupBuyInfo.value) {
          item.price = groupBuyInfo.value.groupPrice;
          item.isGroupBuy = true;
        }
        cartItems.value = [item];

        // 加载该商品的营销活动（限时折扣、满减）
        if (!groupBuyInfo.value) {
          await loadPromotionsForGoods(Number(goodsId));
        }
      }
    } catch (err) {
      ElMessage.error('加载商品信息失败');
    }
  } else {
    const cartStr = localStorage.getItem(getCartKey());
    if (cartStr) {
      cartItems.value = JSON.parse(cartStr);
      // 购物车商品也需要加载营销活动
      if (cartItems.value.length > 0 && !groupBuyInfo.value) {
        await loadPromotionsForGoods(cartItems.value[0].id);
      }
    }
  }

  loadDefaultAddress();
  loadUserCoupons();
};

// 加载商品的营销活动
const loadPromotionsForGoods = async (goodsId: number) => {
  try {
    const res = await api.getGoodsPromotions(goodsId);
    if (res.data.code === 0) {
      const activities = res.data.data || [];

      // 限时折扣活动
      const discountAct = activities.find((a: any) => a.type === 2 && a.status === 1);
      if (discountAct) {
        discountActivity.value = discountAct;
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
  } catch (err) {
    console.error('加载营销活动失败', err);
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
      } else if (addresses.length > 0) {
        selectedAddress.value = addresses[0];
      } else {
        ElMessage.warning('请先添加收货地址');
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
      const allCoupons = res.data.data || [];
      userCoupons.value = allCoupons.map((c: any) => {
        const isExpired = c.expireTime && new Date(c.expireTime) < new Date();
        const isCancelled = c.status === 2;
        const isUsed = c.userCouponStatus === 1;
        let isAvailable = !isExpired && !isCancelled && !isUsed;

        if (isAvailable && cartItems.value.length > 0) {
          if (c.type === 1) {
            isAvailable = cartItems.value.some(item => item.id === c.goodsId);
          }
          if (isAvailable && c.minAmount && c.minAmount > 0) {
            const totalPrice = cartItems.value.reduce((sum, item) => {
              return sum + item.price * getMemberDiscount() * item.quantity;
            }, 0);
            isAvailable = totalPrice >= c.minAmount;
          }
        }

        return {
          ...c,
          isAvailable,
          typeName: c.type === 0 ? '通用优惠券' : '商品优惠券',
          applicableTo: c.type === 0 ? '全店通用' : '指定商品'
        };
      });
    }
  } catch (err) {
    console.error('加载优惠券失败', err);
    userCoupons.value = [];
  }
};

const availableCouponCount = computed(() => {
  return userCoupons.value.filter((c: any) => c.isAvailable).length;
});

const handleAddressSelect = (address: any) => {
  selectedAddress.value = address;
  showAddressManager.value = false;
};

const selectCoupon = (coupon: any) => {
  selectedCoupon.value = coupon;
  showCouponSelect.value = false;
};

const clearCoupon = () => {
  selectedCoupon.value = null;
};

const goBack = () => {
  router.back();
};

const handleSubmit = async () => {
  if (isPaying.value || !user.value) return;

  if (!selectedAddress.value) {
    ElMessage.warning('请选择收货地址');
    return;
  }

  if (cartItems.value.length === 0) {
    ElMessage.warning('购物车为空');
    return;
  }

  isPaying.value = true;

  try {
    const couponId = selectedCoupon.value?.id || null;
    
    // 构建促销信息
    let promotionType = null;
    let promotionId = null;
    let promotionDiscount = null;
    
    if (groupBuyInfo.value) {
      promotionType = 3;
      promotionId = groupBuyInfo.value.groupBuyId;
      const originalTotal = cartItems.value.reduce((total, item) => {
        return total + (item.price || 0) * item.quantity;
      }, 0);
      const groupTotal = groupBuyInfo.value.groupPrice * cartItems.value[0].quantity;
      promotionDiscount = parseFloat((originalTotal - groupTotal).toFixed(2));
    } else if (discountActivity.value) {
      promotionType = 2;
      promotionId = discountActivity.value.id;
      promotionDiscount = parseFloat(((goodsOriginalTotal.value * getMemberDiscount() - discountedGoodsTotal.value)).toFixed(2));
    }
    
    // 满减活动可以与限时折扣叠加
    if (fullReduceActivity.value && fullReduceAmount.value > 0 && !groupBuyInfo.value) {
      promotionType = 1;
      promotionId = fullReduceActivity.value.id;
      promotionDiscount = fullReduceAmount.value;
    }
    
    const orderRes = await api.createOrder(
      cartItems.value[0].id, 
      user.value.id, 
      selectedAddress.value.id, 
      cartItems.value[0].quantity, 
      couponId,
      promotionType,
      promotionId,
      promotionDiscount
    );

    if (orderRes.data.code === 0) {
      const orderId = orderRes.data.data.id;

      await api.payOrderV2(orderId, user.value.id);

      if (!route.query.goodsId && !route.query.goodsIds) {
        localStorage.removeItem(getCartKey());
      }

      setTimeout(() => {
        router.push(`/payment-success/${orderId}`);
      }, 1500);
    } else {
      ElMessage.error(orderRes.data.msg || '下单失败');
      isPaying.value = false;
    }
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.msg || '支付失败');
    isPaying.value = false;
  }
};

onMounted(() => {
  loadCartItems();
});
</script>

<template>
  <div class="payment-page">
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
      <h2>确认订单</h2>
      <div style="width: 80px;"></div>
    </div>

    <div class="content-wrapper">
      <div class="left-section">
        <div class="order-info" v-if="cartItems.length > 0">
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

          <div class="goods-section">
            <div class="section-header">
              <svg class="section-icon" width="20" height="20" viewBox="0 0 24 24" fill="none">
                <path d="M20 7H4C3.44772 7 3 7.44772 3 8V19C3 19.5523 3.44772 20 4 20H20C20.5523 20 21 19.5523 21 19V8C21 7.44772 20.5523 7 20 7Z" stroke="currentColor" stroke-width="2"/>
                <path d="M16 3V7M8 3V7M4 12H20" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
              </svg>
              <h3>商品信息</h3>
            </div>
            <div class="goods-list">
              <div class="goods-info" v-for="item in cartItems" :key="item.id">
                <img v-if="item.imageUrl" :src="item.imageUrl" class="goods-image" />
                <div class="goods-details">
                  <div class="title-row">
                    <h4>{{ item.title }}</h4>
                    <span v-if="item.isGroupBuy" class="groupbuy-tag">👥 团购</span>
                  </div>
                  <p class="goods-desc">{{ item.description }}</p>
                  <div class="goods-bottom">
                    <div class="price-wrapper">
                      <span class="goods-price member-price">￥{{ getMemberPrice(item.price) }}</span>
                      <span v-if="showMemberPrice" class="original-price">￥{{ item.price.toFixed(2) }}</span>
                    </div>
                    <div class="quantity-info">数量：{{ item.quantity }}</div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="remark-section">
            <div class="section-header">
              <svg class="section-icon" width="20" height="20" viewBox="0 0 24 24" fill="none">
                <path d="M11 4H4C3.46957 4 2.96086 4.21071 2.58579 4.58579C2.21071 4.96086 2 5.46957 2 6V20C2 20.5304 2.21071 21.0391 2.58579 21.4142C2.96086 21.7893 3.46957 22 4 22H20C20.5304 22 21.0391 21.7893 21.4142 21.4142C21.7893 21.0391 22 20.5304 22 20V13" stroke="currentColor" stroke-width="2"/>
                <path d="M20 2H8C7.46957 2 6.96086 2.21071 6.58579 2.58579C6.21071 2.96086 6 3.46957 6 4V20C6 20.5304 6.21071 21.0391 6.58579 21.4142C6.96086 21.7893 7.46957 22 8 22H20C20.5304 22 21.0391 21.7893 21.4142 21.4142C21.7893 21.0391 22 20.5304 22 20V4C22 3.46957 21.7893 2.96086 21.4142 2.58579C21.0391 2.21071 20.5304 2 20 2Z" stroke="currentColor" stroke-width="2"/>
                <path d="M16 2V6H20" stroke="currentColor" stroke-width="2"/>
              </svg>
              <h3>备注留言</h3>
            </div>
            <textarea
              v-model="remark"
              class="remark-input"
              placeholder="请输入备注信息（选填）"
              rows="3"
            ></textarea>
          </div>
        </div>

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

      <div class="right-section">
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
            <div v-else-if="availableCouponCount > 0" class="no-coupon-selected">
              <span class="coupon-count">{{ availableCouponCount }}张可用</span>
              <svg class="arrow-icon" width="20" height="20" viewBox="0 0 24 24" fill="none">
                <path d="M9 6L15 12L9 18" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </div>
            <div v-else class="no-coupon">
              <span>暂无可用优惠券</span>
            </div>
          </div>
        </div>

        <div class="price-summary">
          <div class="price-row" v-if="showMemberPrice">
            <span class="price-label">商品原价</span>
            <span class="price-value original">￥{{ goodsOriginalTotal.toFixed(2) }}</span>
          </div>
          <div class="price-row" v-if="showMemberPrice">
            <span class="price-label">会员折扣</span>
            <span class="price-value discount-text">{{ (getMemberDiscount() * 100).toFixed(0) }}折</span>
          </div>
          <div class="price-row">
            <span class="price-label">{{ showMemberPrice ? '会员价小计' : '商品小计' }}</span>
            <span class="price-value">￥{{ (goodsOriginalTotal * getMemberDiscount()).toFixed(2) }}</span>
          </div>
          <!-- 限时折扣优惠 -->
          <div class="price-row discount" v-if="discountActivity">
            <span class="price-label">限时折扣</span>
            <span class="price-value">-￥{{ (goodsOriginalTotal * getMemberDiscount() - discountedGoodsTotal).toFixed(2) }}</span>
          </div>
          <!-- 满减优惠 -->
          <div class="price-row discount" v-if="fullReduceActivity && fullReduceAmount > 0">
            <span class="price-label">满减优惠</span>
            <span class="price-value">-￥{{ fullReduceAmount }}</span>
          </div>
          <div class="price-row">
            <span class="price-label">运费</span>
            <span class="price-value">{{ freight === 0 ? '免运费' : '￥' + freight.toFixed(2) }}</span>
          </div>
          <div class="price-row discount" v-if="selectedCoupon">
            <span class="price-label">优惠券</span>
            <span class="price-value">-￥{{ selectedCoupon.amount }}</span>
          </div>
          <div class="divider"></div>
          <div class="total-row">
            <span class="total-label">
              实付金额
              <span v-if="isGroupBuyOrder" class="groupbuy-label">👥 团购价</span>
            </span>
            <div class="total-price-wrapper">
              <span class="currency">￥</span>
              <span class="total-price">{{ totalAmount.toFixed(2) }}</span>
            </div>
          </div>

          <div class="action-section">
            <button class="pay-btn" :disabled="isPaying || !selectedAddress" @click="handleSubmit">
              <span v-if="isPaying" class="paying-text">
                <svg class="loading-spinner" width="20" height="20" viewBox="0 0 24 24" fill="none">
                  <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-dasharray="50 50"/>
                </svg>
                支付中...
              </span>
              <span v-else>
                {{ isGroupBuyOrder ? '提交团购订单并支付' : '提交订单并支付' }}
              </span>
            </button>
          </div>
        </div>
      </div>
    </div>

    <AddressManager v-model="showAddressManager" :userId="user?.id" @close="showAddressManager = false" @select="handleAddressSelect" />

    <el-dialog v-model="showCouponSelect" title="选择优惠券" width="450px" class="coupon-dialog">
      <div class="coupon-list">
        <div
          v-for="coupon in userCoupons"
          :key="coupon.id"
          :class="['coupon-item', { selected: selectedCoupon?.id === coupon.id, disabled: !coupon.isAvailable }]"
          @click="coupon.isAvailable && selectCoupon(coupon)"
        >
          <div class="coupon-left">
            <div class="coupon-amount-value">￥{{ coupon.amount }}</div>
            <div v-if="coupon.minAmount && coupon.minAmount > 0" class="coupon-min-amount">满{{ coupon.minAmount }}可用</div>
            <div class="coupon-label-text">{{ coupon.typeName }}</div>
          </div>
          <div class="coupon-right">
            <div class="coupon-description">{{ coupon.applicableTo }}</div>
            <div class="coupon-expire">有效期至 {{ new Date(coupon.expireTime).toLocaleDateString() }}</div>
            <div v-if="!coupon.isAvailable" class="coupon-unavailable">
              {{ coupon.userCouponStatus === 1 ? '已使用' : coupon.status === 2 ? '已失效' : '未满足使用条件' }}
            </div>
            <div v-else class="coupon-status-text">
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

.goods-section {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.goods-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
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

.price-wrapper {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.member-price {
  color: #ff1744;
}

.original-price {
  font-size: 14px;
  color: #999;
  text-decoration: line-through;
}

.goods-bottom {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-top: 12px;
  gap: 20px;
}

.quantity-info {
  font-size: 14px;
  color: #606266;
}

.remark-section {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.remark-input {
  width: 100%;
  padding: 12px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  font-size: 14px;
  color: #303133;
  resize: none;
  box-sizing: border-box;
}

.remark-input:focus {
  outline: none;
  border-color: #ff6700;
  background: #fffaf5;
}

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

.price-value.original {
  text-decoration: line-through;
  color: #999;
}

.discount-text {
  color: #ff1744;
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

.pay-btn {
  width: 100%;
  padding: 14px 32px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 16px;
  font-weight: 500;
  transition: all 0.2s ease;
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
  background: #ccc;
  box-shadow: none;
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

.coupon-item:hover:not(.disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(255, 103, 0, 0.15);
}

.coupon-item.selected {
  background: #e8f5e9;
  border-color: #81c784;
}

.coupon-item.disabled {
  opacity: 0.5;
  cursor: not-allowed;
  background: #f5f7fa;
  border-color: #dcdfe6;
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

.coupon-min-amount {
  font-size: 12px;
  color: rgba(255, 103, 0, 0.8);
  margin-top: 4px;
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

.coupon-expire {
  font-size: 13px;
  color: #909399;
  margin-bottom: 6px;
}

.coupon-unavailable {
  font-size: 13px;
  color: #f56c6c;
  font-weight: 500;
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

/* 团购样式 */
.title-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.groupbuy-tag {
  background: linear-gradient(135deg, #ff9800 0%, #f57c00 100%);
  color: white;
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.groupbuy-label {
  background: linear-gradient(135deg, #ff9800 0%, #f57c00 100%);
  color: white;
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
  margin-left: 8px;
}
</style>