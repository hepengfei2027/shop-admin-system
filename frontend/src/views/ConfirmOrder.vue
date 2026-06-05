<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { api } from '../api';
import AddressManager from '../components/AddressManager.vue';

const router = useRouter();

const cartItems = ref<any[]>([]);
const selectedAddress = ref<any>(null);
const showAddressManager = ref(false);
const remark = ref('');
const user = ref<any>(null);

const getCartKey = () => {
  if (user.value) {
    return `cart_${user.value.id}`;
  }
  return 'cart';
};

const totalPrice = computed(() => {
  return cartItems.value.reduce((total, item) => total + item.price * item.quantity, 0);
});

const loadCartItems = () => {
  const cartStr = localStorage.getItem(getCartKey());
  if (cartStr) {
    cartItems.value = JSON.parse(cartStr);
  }
  
  const userStr = localStorage.getItem('user');
  if (userStr) {
    user.value = JSON.parse(userStr);
  }
  
  // 加载默认地址
  loadDefaultAddress();
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

const handleAddressSelect = (address: any) => {
  selectedAddress.value = address;
  showAddressManager.value = false;
};

const submitOrder = async () => {
  if (!selectedAddress.value) {
    ElMessage.warning('请选择收货地址');
    return;
  }
  
  if (cartItems.value.length === 0) {
    ElMessage.warning('购物车为空');
    return;
  }
  
  try {
    const orderData = {
      items: cartItems.value.map(item => ({
        goodsId: item.id,
        quantity: item.quantity,
        price: item.price
      })),
      addressId: selectedAddress.value.id,
      remark: remark.value,
      totalAmount: totalPrice.value
    };
    
    const res = await api.createOrder(orderData);
    if (res.data.code === 0) {
      const orderId = res.data.data;
      localStorage.removeItem(getCartKey());
      router.push(`/payment/${orderId}`);
    } else {
      ElMessage.error(res.data.msg || '下单失败');
    }
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.msg || '下单失败');
  }
};

onMounted(() => {
  loadCartItems();
});
</script>

<template>
  <div class="confirm-order">
    <h2>确认订单</h2>
    
    <!-- 收货地址 -->
    <div class="address-section">
      <h3>收货地址</h3>
      <div v-if="selectedAddress" class="selected-address" @click="showAddressManager = true">
        <div class="address-info">
          <span class="name">{{ selectedAddress.name }}</span>
          <span class="phone">{{ selectedAddress.phone }}</span>
        </div>
        <div class="address-detail">
          {{ selectedAddress.province }} {{ selectedAddress.city }} {{ selectedAddress.district }} {{ selectedAddress.detail }}
        </div>
      </div>
      <div v-else class="no-address" @click="showAddressManager = true">
        <span>+ 请选择收货地址</span>
      </div>
    </div>
    
    <!-- 商品列表 -->
    <div class="goods-section">
      <h3>商品清单</h3>
      <div class="goods-list">
        <div v-for="item in cartItems" :key="item.id" class="goods-item">
          <img v-if="item.imageUrl" :src="item.imageUrl" alt="商品图片" class="goods-image" />
          <div class="goods-info">
            <h4>{{ item.title }}</h4>
            <p class="price">￥{{ item.price }}</p>
            <p class="quantity">数量：{{ item.quantity }}</p>
          </div>
          <div class="subtotal">￥{{ item.price * item.quantity }}</div>
        </div>
      </div>
    </div>
    
    <!-- 订单金额 -->
    <div class="amount-section">
      <h3>订单金额</h3>
      <div class="amount-row">
        <span>商品总额</span>
        <span>￥{{ totalPrice }}</span>
      </div>
      <div class="amount-row total">
        <span>实付金额</span>
        <span>￥{{ totalPrice }}</span>
      </div>
    </div>
    
    <!-- 备注 -->
    <div class="remark-section">
      <h3>备注留言</h3>
      <textarea v-model="remark" placeholder="请输入备注信息（选填）" rows="3"></textarea>
    </div>
    
    <!-- 提交订单 -->
    <div class="submit-section">
      <div class="order-summary">
        <span>共 {{ cartItems.length }} 件商品，实付</span>
        <span class="final-price">￥{{ totalPrice }}</span>
      </div>
      <button class="submit-btn" @click="submitOrder">提交订单</button>
    </div>
    
    <!-- 地址选择弹窗 -->
    <AddressManager v-model="showAddressManager" :userId="user?.id" @close="showAddressManager = false" @select="handleAddressSelect" />
  </div>
</template>

<style scoped>
.confirm-order {
  max-width: 600px;
  margin: 0 auto;
  padding: 20px;
}

h2 {
  text-align: center;
  margin-bottom: 30px;
}

h3 {
  font-size: 16px;
  font-weight: bold;
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 1px solid #e4e7ed;
}

.address-section {
  margin-bottom: 20px;
}

.selected-address {
  padding: 15px;
  border: 2px solid #409eff;
  border-radius: 4px;
  cursor: pointer;
}

.address-info {
  display: flex;
  gap: 15px;
  margin-bottom: 8px;
}

.name {
  font-weight: bold;
}

.phone {
  color: #666;
}

.address-detail {
  color: #666;
}

.no-address {
  padding: 30px;
  border: 2px dashed #e4e7ed;
  border-radius: 4px;
  text-align: center;
  cursor: pointer;
  color: #999;
}

.goods-section {
  margin-bottom: 20px;
}

.goods-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.goods-item {
  display: flex;
  gap: 15px;
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.goods-image {
  width: 80px;
  height: 80px;
  object-fit: cover;
}

.goods-info {
  flex: 1;
}

.goods-info h4 {
  margin: 0 0 10px 0;
  font-size: 14px;
}

.price {
  color: #f56c6c;
  font-weight: bold;
  margin: 0 0 5px 0;
}

.quantity {
  color: #999;
  font-size: 12px;
  margin: 0;
}

.subtotal {
  font-weight: bold;
  color: #f56c6c;
  font-size: 16px;
}

.amount-section {
  margin-bottom: 20px;
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.amount-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
}

.amount-row:last-child {
  margin-bottom: 0;
}

.amount-row.total {
  font-weight: bold;
  font-size: 16px;
  color: #f56c6c;
  padding-top: 10px;
  border-top: 1px dashed #e4e7ed;
}

.remark-section {
  margin-bottom: 20px;
}

.remark-section textarea {
  width: 100%;
  padding: 10px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  resize: none;
}

.submit-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px;
  background-color: #fff;
  border-top: 1px solid #e4e7ed;
  position: sticky;
  bottom: 0;
}

.order-summary {
  display: flex;
  align-items: baseline;
  gap: 5px;
}

.final-price {
  font-size: 24px;
  font-weight: bold;
  color: #f56c6c;
}

.submit-btn {
  padding: 12px 40px;
  background-color: #f56c6c;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 16px;
}

.submit-btn:hover {
  background-color: #f78989;
}
</style>