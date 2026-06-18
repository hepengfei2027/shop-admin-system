<script setup lang="ts">
import { ref, onMounted, computed, inject, watch } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { api } from '../api';

const router = useRouter();
const cartItems = ref<any[]>([]);
const user = inject('user', ref<any>(null));

const selectedItems = ref<Set<number>>(new Set());

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
  selectedItems.value.clear();
};

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
  const item = cartItems.value[index];
  selectedItems.value.delete(item.id);
  cartItems.value.splice(index, 1);
  localStorage.setItem(getCartKey(), JSON.stringify(cartItems.value));
  ElMessage.success('已从购物车中移除');
};

const clearCart = () => {
  cartItems.value = [];
  selectedItems.value.clear();
  localStorage.removeItem(getCartKey());
  ElMessage.success('购物车已清空');
};

const isSelected = (item: any) => selectedItems.value.has(item.id);

const toggleSelect = (item: any) => {
  if (selectedItems.value.has(item.id)) {
    selectedItems.value.delete(item.id);
  } else {
    selectedItems.value.add(item.id);
  }
};

const selectAll = () => {
  if (selectedItems.value.size === cartItems.value.length) {
    selectedItems.value.clear();
  } else {
    cartItems.value.forEach(item => selectedItems.value.add(item.id));
  }
};

const selectedCount = computed(() => selectedItems.value.size);

const selectedTotal = computed(() => {
  return cartItems.value
    .filter(item => selectedItems.value.has(item.id))
    .reduce((total, item) => total + item.price * item.quantity, 0)
    .toFixed(2);
});

const removeSelectedItems = () => {
  if (selectedItems.value.size === 0) {
    ElMessage.warning('请选择要移除的商品');
    return;
  }
  cartItems.value = cartItems.value.filter(item => !selectedItems.value.has(item.id));
  selectedItems.value.clear();
  localStorage.setItem(getCartKey(), JSON.stringify(cartItems.value));
  ElMessage.success('已移除选中商品');
};

const buySelected = async () => {
  if (!user.value) {
    ElMessage.warning('请先登录');
    router.push('/login');
    return;
  }
  if (selectedItems.value.size === 0) {
    ElMessage.warning('请选择商品');
    return;
  }
  const selectedIds = Array.from(selectedItems.value).join(',');
  router.push(`/confirm-order?goodsIds=${selectedIds}`);
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
      <div style="display: flex; justify-content: flex-end; margin-bottom: 10px">
        <el-button type="danger" @click="removeSelectedItems">移除选中</el-button>
      </div>
      <el-table :data="cartItems" style="width: 100%">
        <el-table-column width="50">
          <template #header>
            <el-checkbox :checked="selectedCount === cartItems.length && cartItems.length > 0" @change="selectAll" />
          </template>
          <template #default="scope">
            <el-checkbox :checked="isSelected(scope.row)" @change="toggleSelect(scope.row)" />
          </template>
        </el-table-column>
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

      </el-table>
      
      <div style="margin-top: 20px; display: flex; justify-content: space-between; align-items: center">
        <el-button type="warning" @click="clearCart">清空购物车</el-button>
        <div>
          <span v-if="selectedCount > 0" style="font-size: 16px; margin-right: 15px">已选 {{ selectedCount }} 件，共 ￥{{ selectedTotal }}</span>
          <el-button v-if="selectedCount > 0" type="primary" @click="buySelected">批量下单</el-button>
          <span style="font-size: 18px; margin-left: 20px">总计：￥{{ totalPrice }}</span>
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