<script setup lang="ts">
import { onMounted, ref, computed, inject, watch } from 'vue';
import { api } from '../api';
import { ElMessage } from 'element-plus';

const goodsList = ref<any[]>([]);
const keyword = ref('');
const cartItems = ref<any[]>([]);
const user = inject('user', ref<any>(null));
const placeholderIndex = ref(0);
const orders = ref<any[]>([]);
const showPlaceholder = ref(true);
const currentPlaceholder = ref('搜索宝贝');

const getCartKey = () => {
  if (user.value) {
    return `cart_${user.value.id}`;
  }
  return 'cart';
};

const cartCount = computed(() => {
  return cartItems.value.reduce((total, item) => total + item.quantity, 0);
});

const updateCart = () => {
  localStorage.setItem(getCartKey(), JSON.stringify(cartItems.value));
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

const loadOrders = async () => {
  try {
    const res = await api.getAllOrders();
    if (res.data.code === 0) {
      orders.value = res.data.data || [];
    } else if (Array.isArray(res.data)) {
      orders.value = res.data;
    } else {
      orders.value = [];
    }
    console.log('Orders loaded:', orders.value);
    console.log('Order statuses:', orders.value.map(o => ({ goodsId: o.goodsId || o.goodId, status: o.status })));
  } catch (err) {
    console.error("加载订单失败", err);
    orders.value = [];
  }
};

const getSalesCount = (goodsId: number) => {
  const orderId = orders.value.map(o => o.goodsId || o.goodId);
  console.log('Goods ID:', goodsId, 'Order goodsIds:', orderId);
  return orders.value.filter(order => {
    const orderGoodsId = order.goodsId !== undefined ? order.goodsId : order.goodId;
    const statusMatch = order.status === 'COMPLETED' || order.status === 'completed' || order.status === 2;
    return orderGoodsId === goodsId && statusMatch;
  }).length;
};

const loadList = async () => {
  try {
    const res = await api.listGoods();
    if (res.data.code === 0) {
      goodsList.value = res.data.data || [];
    } else {
      goodsList.value = [];
      ElMessage.warning(res.data.msg || '获取商品失败');
    }
    if (Array.isArray(res.data)) {
      goodsList.value = res.data;
    }
  } catch (err) {
    console.error("加载商品失败", err);
    ElMessage.error("加载商品失败，请检查后端接口");
  }
};

const onSearch = async () => {
  if (!keyword.value.trim()) {
    return loadList();
  }
  try {
    const res = await api.searchGoods(keyword.value);
    goodsList.value = res.data.data || [];
  } catch (err) {
    ElMessage.error("搜索失败");
  }
};

const clearSearch = async () => {
  keyword.value = '';
  await loadList();
};

const getPlaceholderText = () => {
  if (goodsList.value.length > 0 && goodsList.value[placeholderIndex.value]) {
    return goodsList.value[placeholderIndex.value].title;
  }
  return '搜索宝贝';
};

const animatePlaceholder = () => {
  showPlaceholder.value = false;
  setTimeout(() => {
    if (goodsList.value.length > 0) {
      placeholderIndex.value = (placeholderIndex.value + 1) % goodsList.value.length;
      currentPlaceholder.value = getPlaceholderText();
    }
    showPlaceholder.value = true;
  }, 300);
};

const handleQuickSearch = () => {
  if (goodsList.value.length > 0) {
    keyword.value = goodsList.value[placeholderIndex.value].title;
    onSearch();
  }
};

onMounted(() => {
  loadList();
  loadCart();
  loadOrders();
  setInterval(() => {
    if (goodsList.value.length > 0) {
      animatePlaceholder();
    }
  }, 3000);
});
</script>

<template>
  <div class="home">
    <div class="home-inner">
      <div class="search-bar-wrapper">
        <div class="search-bar">
          <div class="search-input-wrapper">
            <input
                v-model="keyword"
                type="text"
                @keyup.enter="onSearch"
            />
            <span 
              v-if="!keyword" 
              :class="['animated-placeholder', { 'fade-out': !showPlaceholder }]"
              @click="handleQuickSearch"
            >
              {{ currentPlaceholder }}
            </span>
            <button v-if="keyword" class="clear-btn" @click="clearSearch">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M18 6L6 18M6 6l12 12"/>
              </svg>
            </button>
            <button class="search-btn" @click="onSearch">
              <svg class="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="11" cy="11" r="8"/>
                <path d="m21 21-4.35-4.35"/>
              </svg>
            </button>
          </div>
          <button class="cart-btn" @click="$router.push('/cart')">
            <svg class="cart-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="9" cy="21" r="1"/>
              <circle cx="20" cy="21" r="1"/>
              <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"/>
            </svg>
            <span v-if="cartCount > 0" class="cart-badge">{{ cartCount }}</span>
          </button>
        </div>
      </div>

      <div class="top-section">
        <div class="banner">
          <div class="banner-decoration banner-decoration-1"></div>
          <div class="banner-decoration banner-decoration-2"></div>
          <div class="banner-decoration banner-decoration-3"></div>
          <div class="banner-content">
            <div class="banner-badge">限时特惠</div>
            <h2 class="banner-title">发现超值好物</h2>
            <p class="banner-subtitle">精选宝贝，品质保障</p>
            <div class="banner-tags">
              <span class="banner-tag">正品保证</span>
              <span class="banner-tag">全场包邮</span>
              <span class="banner-tag">七天退换</span>
            </div>
          </div>
        </div>
      </div>

      <div class="container">
        <div class="section-header">
          <h2>热门商品</h2>
          <span class="section-subtitle">为你精选</span>
        </div>

        <div v-if="goodsList.length === 0" class="empty-state">
          <div class="empty-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
              <circle cx="12" cy="7" r="4"/>
            </svg>
          </div>
          <p>暂无商品</p>
        </div>

        <div class="goods-grid">
          <div class="goods-card" v-for="item in goodsList" :key="item.id" @click="$router.push(`/goods/${item.id}`)">
            <div class="goods-image-wrapper">
              <img
                  v-if="item.imageUrl"
                  :src="item.imageUrl"
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
              <span v-if="!item.freight || item.freight === 0" class="free-shipping-tag">包邮</span>
            </div>
            <div class="goods-info">
              <h3 class="goods-title">{{ item.title }} {{ item.description }}</h3>
              <div class="goods-footer">
                <span class="goods-price">￥{{ item.price.toFixed(2) }}</span>
                <span class="goods-stock">销量 {{ getSalesCount(item.id) }} 件</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.home {
  min-height: 100vh;
  background-color: #fff5f5;
  padding: 0;
  margin: 0;
  width: 100%;
  min-width: 100%;

}

.home-inner {
  padding: 20px;
}

.top-section {
  max-width: 1600px;
  margin: 0 auto;
  background-color: #fff;
  border-radius: 20px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
  overflow: hidden;
}

.search-bar-wrapper {
  background: transparent;
  padding: 0;
  max-width: 1600px;
  margin: 0 auto 20px;
}

.search-bar {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 15px;
  padding: 0 20px;
}

.search-input-wrapper {
  flex: 1;
  max-width: 600px;
  display: flex;
  position: relative;
  background: #fff;
  border-radius: 30px;
  border: 2px solid #e53935;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
  overflow: hidden;
  transition: box-shadow 0.3s ease;
}

.search-input-wrapper:focus-within {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
}

.search-input-wrapper input {
  flex: 1;
  height: 44px;
  padding: 0 20px;
  border: none;
  outline: none;
  font-size: 15px;
  color: #333;
  transition: all 0.3s ease;
}

.search-input-wrapper input::placeholder {
  color: #999;
  transition: opacity 0.3s ease;
}

.animated-placeholder {
  position: absolute;
  left: 20px;
  top: 50%;
  transform: translateY(-50%);
  color: #999;
  font-size: 15px;
  pointer-events: none;
  transition: all 0.3s ease;
  opacity: 1;
  white-space: nowrap;
  overflow: hidden;
  max-width: 400px;
}

.animated-placeholder.fade-out {
  opacity: 0;
  transform: translateY(-50%) translateX(-10px);
}

.animated-placeholder:not(.fade-out) {
  animation: slideIn 0.3s ease forwards;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(-50%) translateX(10px);
  }
  to {
    opacity: 1;
    transform: translateY(-50%) translateX(0);
  }
}

.search-btn {
  width: 50px;
  height: 44px;
  background: #fff;
  border: none;
  cursor: pointer;
  color: #e53935;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: color 0.2s;
}

.search-btn:hover {
  color: #c62828;
}

.clear-btn {
  width: 30px;
  height: 44px;
  background: transparent;
  border: none;
  cursor: pointer;
  color: #999;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: color 0.2s;
}

.clear-btn:hover {
  color: #666;
}

.clear-btn svg {
  width: 16px;
  height: 16px;
}

.search-icon {
  width: 20px;
  height: 20px;
}

.cart-btn {
  position: relative;
  width: 44px;
  height: 44px;
  background: #e53935;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
}

.cart-btn:hover {
  background: #c62828;
}

.cart-icon {
  width: 22px;
  height: 22px;
}

.cart-badge {
  position: absolute;
  top: -5px;
  right: -5px;
  background: #ff4757;
  color: #fff;
  font-size: 11px;
  min-width: 18px;
  height: 18px;
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
}

.banner {
  background: linear-gradient(120deg, #ffcdd2 0%, #ef9a9a 50%, #e57373 100%);
  padding: 50px 40px;
  position: relative;
  overflow: hidden;
}

.banner-decoration {
  position: absolute;
  border-radius: 50%;
  opacity: 0.1;
}

.banner-decoration-1 {
  width: 200px;
  height: 200px;
  background: #e53935;
  top: -50px;
  right: 10%;
  animation: float 6s ease-in-out infinite;
}

.banner-decoration-2 {
  width: 150px;
  height: 150px;
  background: #e53935;
  bottom: -30px;
  left: 5%;
  animation: float 8s ease-in-out infinite reverse;
}

.banner-decoration-3 {
  width: 100px;
  height: 100px;
  background: #e53935;
  top: 20%;
  right: 25%;
  animation: float 5s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-20px); }
}

.banner-content {
  max-width: 1200px;
  margin: 0 auto;
  text-align: center;
  position: relative;
  z-index: 1;
}

.banner-badge {
  display: inline-block;
  background: #e53935;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  padding: 6px 20px;
  border-radius: 20px;
  margin-bottom: 16px;
  letter-spacing: 2px;
}

.banner-title {
  font-size: 42px;
  margin: 0 0 12px 0;
  font-weight: 800;
  color: #fff;
  letter-spacing: -1px;
}

.banner-subtitle {
  font-size: 18px;
  margin: 0 0 24px 0;
  color: rgba(255, 255, 255, 0.9);
  font-weight: 400;
}

.banner-tags {
  display: flex;
  justify-content: center;
  gap: 16px;
  flex-wrap: wrap;
}

.banner-tag {
  background: rgba(255, 255, 255, 0.15);
  color: #fff;
  font-size: 13px;
  font-weight: 500;
  padding: 8px 20px;
  border-radius: 25px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(10px);
  transition: all 0.3s ease;
}

.banner-tag:hover {
  background: #fff;
  color: #b71c1c;
}

.container {
  max-width: 1600px;
  margin: 20px auto 0;
  padding: 15px 20px;
  background-color: #fff;
  border-radius: 20px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
}

.section-header {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 10px;
}

.section-header h2 {
  font-size: 22px;
  margin: 0;
  color: #333;
}

.section-subtitle {
  font-size: 13px;
  color: #999;
}

.empty-state {
  text-align: center;
  padding: 80px 0;
  color: #999;
}

.empty-icon {
  width: 60px;
  height: 60px;
  margin: 0 auto 15px;
  color: #ddd;
}

.empty-icon svg {
  width: 100%;
  height: 100%;
}

.empty-state p {
  margin: 0;
  font-size: 15px;
}

.goods-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 12px;
}

.goods-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 2px solid transparent;
}

.goods-card:hover {
  background: #fff;
  transform: translateY(-4px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.12);
  border-color: #ff0000;
}

.goods-image-wrapper {
  position: relative;
  width: 100%;
  height: 280px;
  overflow: hidden;
  background: #f8f8f8;
  border-radius: 12px;
}

.goods-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.goods-card:hover .goods-image {
  transform: scale(1.05);
}

.image-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ccc;
}

.image-placeholder svg {
  width: 40px;
  height: 40px;
}

.free-shipping-tag {
  position: absolute;
  top: 10px;
  left: 10px;
  background: #e53935;
  color: #fff;
  font-size: 11px;
  padding: 3px 10px;
  border-radius: 12px;
}

.goods-info {
  padding: 15px;
}

.goods-title {
  font-size: 18px;
  font-weight: 500;
  color: #000;
  margin: 0 0 12px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.goods-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.goods-price {
  font-size: 22px;
  font-weight: 700;
  color: #e53935;
}

.goods-stock {
  font-size: 12px;
  color: #999;
}
</style>