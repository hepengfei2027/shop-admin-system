<script setup lang="ts">
import { onMounted, ref, computed, inject, watch } from 'vue';
import { api } from '../api';
import { ElMessage } from 'element-plus';

const goodsList = ref<any[]>([]);
const keyword = ref('');
const cartItems = ref<any[]>([]);
const user = inject('user', ref<any>(null));
const placeholderIndex = ref(0);
const showPlaceholder = ref(true);
const currentPlaceholder = ref('搜索宝贝');

const categories = [
  { label: '全部', value: '' },
  { label: '数码电子', value: '数码电子' },
  { label: '服装服饰', value: '服装服饰' },
  { label: '美妆护肤', value: '美妆护肤' },
  { label: '家居日用', value: '家居日用' },
  { label: '食品饮料', value: '食品饮料' },
  { label: '图书文具', value: '图书文具' },
  { label: '运动户外', value: '运动户外' },
  { label: '其他', value: '其他' }
];

const activeCategory = ref('');

const showFilterPanel = ref(false);
const brands = ref<any[]>([]);
const selectedBrand = ref('');
const priceRanges = [
  { label: '全部', min: 0, max: 0 },
  { label: '0-50', min: 0, max: 50 },
  { label: '50-100', min: 50, max: 100 },
  { label: '100-200', min: 100, max: 200 },
  { label: '200-500', min: 200, max: 500 },
  { label: '500+', min: 500, max: 0 }
];
const selectedPriceRange = ref('全部');
const customMinPrice = ref('');
const customMaxPrice = ref('');

const loadBrands = async () => {
  try {
    const res = await api.listBrands();
    if (res.data.code === 0) {
      brands.value = res.data.data || [];
    }
  } catch (err) {
    console.error("加载品牌列表失败", err);
  }
};

const getCartKey = () => {
  if (user.value) {
    return `cart_${user.value.id}`;
  }
  return 'cart';
};

const cartCount = computed(() => {
  return cartItems.value.reduce((total, item) => total + item.quantity, 0);
});

const getMemberPrice = (price: number) => {
  const discount = (user.value?.discount !== undefined && user.value?.discount !== null) ? user.value.discount : 1;
  return (price * discount).toFixed(2);
};

const showMemberPrice = computed(() => {
  const discount = user.value?.discount;
  return discount !== undefined && discount !== null && discount < 1;
});

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

const orders = ref<any[]>([]);

const loadOrders = async () => {
  if (!user.value?.id) return;
  try {
    const res = await api.listBuyerOrders(user.value.id);
    if (res.data.code === 0) {
      orders.value = res.data.data || [];
    } else if (Array.isArray(res.data)) {
      orders.value = res.data;
    } else {
      orders.value = [];
    }
  } catch (err) {
    console.error("加载订单失败", err);
    orders.value = [];
  }
};

const getSalesCount = (goodsId: number) => {
  return orders.value.filter(order => {
    const orderGoodsId = order.goodsId !== undefined ? order.goodsId : order.goodId;
    // status=3 表示已完成
    return orderGoodsId === goodsId && order.status === 3;
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
  if (!keyword.value.trim() && !selectedBrand.value && selectedPriceRange.value === '全部' && !customMinPrice.value && !customMaxPrice.value) {
    return loadList();
  }
  try {
    const priceRange = priceRanges.find(r => r.label === selectedPriceRange.value);
    const params: any = {
      keyword: keyword.value || undefined,
      brand: selectedBrand.value || undefined,
      minPrice: customMinPrice.value ? parseFloat(customMinPrice.value) : (priceRange?.min || undefined),
      maxPrice: customMaxPrice.value ? parseFloat(customMaxPrice.value) : (priceRange?.max || undefined)
    };
    if (params.maxPrice === 0) {
      delete params.maxPrice;
    }
    const res = await api.searchGoodsWithFilter(params);
    goodsList.value = res.data.data || [];
  } catch (err) {
    ElMessage.error("搜索失败");
  }
};

const onCategoryClick = async (category: string) => {
  activeCategory.value = category;
  if (!category && !selectedBrand.value && selectedPriceRange.value === '全部' && !customMinPrice.value && !customMaxPrice.value) {
    keyword.value = '';
    return loadList();
  }
  keyword.value = category;
  await onSearch();
};

const clearSearch = async () => {
  keyword.value = '';
  selectedBrand.value = '';
  selectedPriceRange.value = '全部';
  customMinPrice.value = '';
  customMaxPrice.value = '';
  showFilterPanel.value = false;
  await loadList();
};

const toggleFilterPanel = () => {
  if (!showFilterPanel.value) {
    loadBrands();
  }
  showFilterPanel.value = !showFilterPanel.value;
};

const applyFilter = async () => {
  await onSearch();
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
              搜索
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

      <div class="category-bar">
        <div 
          v-for="cat in categories" 
          :key="cat.value" 
          :class="['category-item', { active: activeCategory === cat.value }]"
          @click="onCategoryClick(cat.value)"
        >
          {{ cat.label }}
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
          <div class="section-header-content">
            <h2>热门商品</h2>
            <span class="section-subtitle">为你精选</span>
          </div>
          <button v-if="keyword || activeCategory || selectedBrand || selectedPriceRange !== '全部' || customMinPrice || customMaxPrice" 
                  class="filter-toggle-btn" 
                  @click="toggleFilterPanel">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="filter-icon">
              <polygon points="22 12 18 12 15 21 9 3 6 12 2 12"/>
            </svg>
            筛选
          </button>
        </div>

        <div v-if="showFilterPanel" class="filter-panel">
          <div class="filter-section">
            <h4>品牌</h4>
            <div class="filter-options">
              <label class="filter-option">
                <input type="radio" name="brand" value="" v-model="selectedBrand" />
                <span>全部</span>
              </label>
              <label class="filter-option" v-for="brand in brands" :key="brand.brandName">
                <input type="radio" name="brand" :value="brand.brandName" v-model="selectedBrand" />
                <span>{{ brand.brandName }}</span>
              </label>
            </div>
          </div>
          <div class="filter-section">
            <h4>价格区间</h4>
            <div class="filter-options">
              <label class="filter-option" v-for="range in priceRanges" :key="range.label">
                <input type="radio" name="priceRange" :value="range.label" v-model="selectedPriceRange" />
                <span>{{ range.label }}</span>
              </label>
            </div>
            <div class="custom-price">
              <input type="number" v-model="customMinPrice" placeholder="最低价" />
              <span>-</span>
              <input type="number" v-model="customMaxPrice" placeholder="最高价" />
            </div>
          </div>
          <div class="filter-actions">
            <button class="filter-btn filter-reset" @click="clearSearch">重置</button>
            <button class="filter-btn filter-apply" @click="applyFilter">应用</button>
          </div>
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
              <h3 class="goods-title">
                <span v-if="item.brandName" class="brand-tag-inline" :style="{ backgroundColor: item.brandColor || '#ff1744' }">
                  {{ item.brandName }}
                </span>
                {{ item.title }} {{ item.description }}
              </h3>
              <div class="tags-wrapper">
                <span v-if="item.promotionLabel" class="activity-tag" :class="'activity-' + item.promotionType">
                  {{ item.promotionLabel }}
                </span>
<!--                <span v-if="item.hasDiscount && item.promotionType !== 2" class="activity-tag activity-2">-->
<!--                  {{ ((item.discountRate || 0.8) * 10).toFixed(1) }}折-->
<!--                </span>-->
                <span v-if="item.hasFullReduce" class="activity-tag activity-1">
                  满{{ item.fullReduceThreshold }}减{{ item.fullReduceAmount }}
                </span>
              </div>
              <div class="goods-footer">
                <div class="price-wrapper">
                  <div class="price-column">
                    <span v-if="item.promotionalPrice !== undefined && item.promotionalPrice < item.price" class="original-price">￥{{ item.price.toFixed(2) }}</span>
                    <span v-if="showMemberPrice && item.promotionType !== 1 && (!item.promotionalPrice || item.promotionalPrice >= item.price)" class="original-price">￥{{ item.price.toFixed(2) }}</span>
                    <span class="goods-price" :class="{ 'member-price': showMemberPrice && item.promotionType !== 1 && (!item.promotionalPrice || item.promotionalPrice >= item.price) }">
                      <template v-if="item.promotionType === 1">￥{{ item.price.toFixed(2) }}</template>
                      <template v-else>￥{{ (item.promotionalPrice !== undefined ? item.promotionalPrice : item.price * getMemberDiscount()).toFixed(2) }}</template>
                    </span>
                  </div>
                </div>
                <span class="goods-stock">已售 {{ getSalesCount(item.id) }} 件</span>
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
/* 主体上下边距 */
.home-inner {
  padding: 50px 20px;
}
/* 广告牌上下边距 */
.top-section {
  max-width: 1600px;
  margin: 50px auto 0px;
  background-color: #fff;
  border-radius: 20px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
  overflow: hidden;
}

.search-bar-wrapper {
  background: transparent;
  padding: 0;
  max-width: 1600px;
  margin: 0px auto 20px;
}

.category-bar {
  display: flex;
  justify-content: center;
  gap: 16px;
  max-width: 1600px;
  margin: 0px auto 20px;
  padding: 0 20px;
  flex-wrap: wrap;
}
/*分类字体*/
.category-item {
  font-size: 16px;
  color: #666;
  cursor: pointer;
}

.category-item:hover {
  color: #ff1744;
}

.category-item.active {
  color: #ff1744;
  font-weight: 600;
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
  max-width: 900px;
  display: flex;
  position: relative;
  background: #fff;
  border-radius: 12px;
  border: 3px solid #ff1744;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
  overflow: hidden;
  transition: box-shadow 0.3s ease;
}

.search-input-wrapper:focus-within {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
}

.search-input-wrapper input {
  flex: 1;
  height: 56px;
  padding: 0 24px 0 12px;
  border: none;
  outline: none;
  font-size: 16px;
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
  width: 100px;
  height: 56px;
  background: #ff1744;
  border: none;
  border-radius: 0 8px 8px 0;
  cursor: pointer;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
  font-size: 20px;
  font-weight: 600;
}

.search-btn:hover {
  background: #c62828;
}

.clear-btn {
  width: 40px;
  height: 56px;
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
  background: #ff1744;
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
/*广告牌图片*/
.banner {
  background: linear-gradient(135deg, rgba(255, 103, 0, 0.1) 0%, rgba(255, 80, 0, 0.1) 100%),
              url('https://images.unsplash.com/photo-1552664730-d307ca884978?w=1920&q=80') center/cover no-repeat;
  padding: 50px 40px;
  position: relative;
  overflow: hidden;
}

.banner-decoration {
  position: absolute;
  border-radius: 50%;
  opacity: 0.15;
}

.banner-decoration-1 {
  width: 200px;
  height: 200px;
  background: #fff;
  top: -50px;
  right: 10%;
  animation: float 6s ease-in-out infinite;
}

.banner-decoration-2 {
  width: 150px;
  height: 150px;
  background: #fff;
  bottom: -30px;
  left: 5%;
  animation: float 8s ease-in-out infinite reverse;
}

.banner-decoration-3 {
  width: 100px;
  height: 100px;
  background: #fff;
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
  background: rgba(255, 255, 255, 0.25);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  padding: 6px 20px;
  border-radius: 20px;
  margin-bottom: 16px;
  letter-spacing: 2px;
  border: 1px solid rgba(255, 255, 255, 0.3);
}

.banner-title {
  font-size: 42px;
  margin: 0 0 12px 0;
  font-weight: 800;
  color: #fff;
  letter-spacing: -1px;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.3);
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
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 10px;
}

.section-header-content {
  display: flex;
  align-items: baseline;
  gap: 10px;
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
  background: #ff1744;
  color: #fff;
  font-size: 11px;
  padding: 3px 10px;
  border-radius: 12px;
}

.promotion-tag {
  position: absolute;
  top: 10px;
  font-size: 11px;
  padding: 1px 10px;
  border-radius: 12px;
  font-weight: 500;
  height: 18px;
  line-height: 18px;
}

.fullreduce-tag {
  left: 55px;
  background: linear-gradient(135deg, #ff9800, #ff5722);
  color: #fff;
}

.discount-tag {
  left: 55px;
  background: linear-gradient(135deg, #2196f3, #1976d2);
  color: #fff;
}

.groupbuy-tag {
  left: 55px;
  background: linear-gradient(135deg, #e91e63, #c2185b);
  color: #fff;
}

.brand-tag {
  display: inline-block;
  color: #fff;
  font-size: 12px;
  padding: 0px 8px;
  border-radius: 3px;
  height: 20px;
  line-height: 20px;
  flex-shrink: 0;
}

.brand-tag-inline {
  display: inline-block;
  color: #fff;
  font-size: 12px;
  padding: 0px 6px;
  border-radius: 3px;
  height: 18px;
  line-height: 18px;
  margin-right: 6px;
  vertical-align: middle;
}

.tags-wrapper {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}

.activity-tag {
  display: inline-block;
  font-size: 12px;
  padding: 0px 8px;
  border-radius: 3px;
  font-weight: 500;
  height: 18px;
  line-height: 18px;
}

.activity-1 {
  background-color: #fff3e0;
  color: #e65100;
  border: 1px solid #ffcc80;
}

.activity-2 {
  background-color: #e3f2fd;
  color: #1565c0;
  border: 1px solid #90caf9;
}

.activity-3 {
  background-color: #fce4ec;
  color: #e91e63;
  border: 1px solid #f8bbd9;
}

.goods-info {
  padding: 15px;
}

.goods-title {
  font-size: 18px;
  font-weight: 500;
  color: #000;
  margin: 0 0 8px 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}

.goods-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.price-wrapper {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.price-column {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.fullreduce-hint {
  font-size: 12px;
  color: #ff9800;
  background: #fff3e0;
  padding: 2px 6px;
  border-radius: 4px;
  height: 18px;
  line-height: 18px;
  display: inline-block;
}

.goods-price {
  font-size: 25px;
  font-weight: 700;
  color: #ff1744;
}

.member-price {
  color: #ff1744;
}

.original-price {
  font-size: 12px;
  color: #999;
  text-decoration: line-through;
}

.goods-stock {
  font-size: 12px;
  color: #999;
}

.filter-toggle-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  font-size: 14px;
  color: #666;
  background: #fff;
  border: 1px solid #ddd;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.filter-toggle-btn:hover {
  border-color: #ff1744;
  color: #ff1744;
}

.filter-icon {
  width: 16px;
  height: 16px;
}

.filter-panel {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.filter-section {
  margin-bottom: 16px;
}

.filter-section h4 {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
}

.filter-options {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.filter-option {
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  font-size: 13px;
  color: #666;
}

.filter-option input[type="radio"] {
  width: 14px;
  height: 14px;
}

.custom-price {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
}

.custom-price input {
  width: 80px;
  padding: 6px 8px;
  font-size: 13px;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.filter-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 12px;
  border-top: 1px solid #eee;
}

.filter-btn {
  padding: 8px 20px;
  font-size: 14px;
  border-radius: 4px;
  cursor: pointer;
  border: none;
  transition: all 0.3s ease;
}

.filter-reset {
  background: #f5f5f5;
  color: #666;
}

.filter-reset:hover {
  background: #eee;
}

.filter-apply {
  background: #ff1744;
  color: #fff;
}

.filter-apply:hover {
  background: #ff0033;
}
</style>