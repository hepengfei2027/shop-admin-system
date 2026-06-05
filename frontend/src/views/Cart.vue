<script setup lang="ts">
import { ref, onMounted, computed, inject, watch } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { api } from '../api';

const router = useRouter();
const cartItems = ref<any[]>([]);
const user = inject('user', ref<any>(null));

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

// 监听用户状态变化，重新加载购物车
watch(user, () => {
  loadCart();
}, { deep: true });

const totalPrice = computed(() => {
  return cartItems.value.reduce((total, item) => total + item.price * item.quantity, 0).toFixed(2);
});

const updateQuantity = (item: any, delta: number) => {
  item.quantity += delta;
  if (item.quantity < 1) {
    item.quantity = 1;
  }
  localStorage.setItem(getCartKey(), JSON.stringify(cartItems.value));
};

const removeItem = (index: number) => {
  cartItems.value.splice(index, 1);
  localStorage.setItem(getCartKey(), JSON.stringify(cartItems.value));
  ElMessage.success('已从购物车中移除');
};

const clearCart = () => {
  cartItems.value = [];
  localStorage.removeItem(getCartKey());
  ElMessage.success('购物车已清空');
};

const buyNow = async (item: any) => {
  if (!user.value) {
    ElMessage.warning('请先登录');
    router.push('/login');
    return;
  }

  try {
    const res = await api.createOrder(item.id, user.value.id);
    if (res.data.code === 0) {
      const orderId = res.data.data.id;
      // 从购物车中移除该商品
      const index = cartItems.value.findIndex(i => i.id === item.id);
      if (index !== -1) {
        cartItems.value.splice(index, 1);
        localStorage.setItem(getCartKey(), JSON.stringify(cartItems.value));
      }
      router.push(`/payment/${orderId}`);
    } else {
      ElMessage.error(res.data.msg || '下单失败');
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.msg || '下单失败');
  }
};

onMounted(loadCart);
</script>

<template>
  <div class="cart">
    <h2>购物车</h2>
    
    <div v-if="cartItems.length === 0" style="text-align: center; margin-top: 50px">
      <p>购物车为空</p>
      <el-button type="primary" @click="$router.push('/')">去购物</el-button>
    </div>
    
    <div v-else>
      <el-table :data="cartItems" style="width: 100%">
        <el-table-column label="商品图片">
          <template #default="scope">
            <img v-if="scope.row.imageUrl" :src="scope.row.imageUrl" alt="商品图片" style="width: 80px; height: 80px; object-fit: cover" />
          </template>
        </el-table-column>
        <el-table-column prop="title" label="商品名称" />
        <el-table-column prop="price" label="单价" />
        <el-table-column label="数量">
          <template #default="scope">
            <div style="display: flex; align-items: center">
              <el-button @click="updateQuantity(scope.row, -1)" size="small">-</el-button>
              <span style="margin: 0 10px">{{ scope.row.quantity }}</span>
              <el-button @click="updateQuantity(scope.row, 1)" size="small">+</el-button>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="小计">
          <template #default="scope">
            ￥{{ (scope.row.price * scope.row.quantity).toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column label="操作">
          <template #default="scope">
            <el-button type="primary" size="small" @click="buyNow(scope.row)">立即购买</el-button>
            <el-button type="danger" size="small" @click="removeItem(scope.$index)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <div style="margin-top: 20px; display: flex; justify-content: space-between; align-items: center">
        <el-button type="warning" @click="clearCart">清空购物车</el-button>
        <div>
          <span style="font-size: 18px; margin-right: 20px">总计：￥{{ totalPrice }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.cart {
  max-width: 1000px;
  margin: 0 auto;
  padding: 20px;
}
</style>