<script setup lang="ts">
import { ref, onMounted, computed, inject, watch } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { api } from '../api';

const router = useRouter();
const cartItems = ref<any[]>([]);
const user = inject('user', ref<any>(null));

// 改用响应式对象存储选中id，解决Set视图更新延迟、勾选动画不同步
const selectedMap = ref<Record<number, boolean>>({});

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
  // 清空选中
  selectedMap.value = {};
};

watch(user, () => {
  loadCart();
}, { deep: true });

// 购物车全部总价
const totalPrice = computed(() => {
  return cartItems.value.reduce((total, item) => total + item.price * item.quantity, 0).toFixed(2);
});

// 修改商品数量
const updateQuantity = (item: any, delta: number) => {
  item.quantity += delta;
  if (item.quantity < 1) item.quantity = 1;
  localStorage.setItem(getCartKey(), JSON.stringify(cartItems.value));
};

// 删除单个商品
const removeItem = (index: number) => {
  const item = cartItems.value[index];
  // 删除选中标记
  delete selectedMap.value[item.id];
  cartItems.value.splice(index, 1);
  localStorage.setItem(getCartKey(), JSON.stringify(cartItems.value));
  ElMessage.success('已从购物车中移除');
};

// 清空购物车
const clearCart = () => {
  cartItems.value = [];
  selectedMap.value = {};
  localStorage.removeItem(getCartKey());
  ElMessage.success('购物车已清空');
};

// 判断商品是否选中（实时响应）
const isSelected = (item: any) => !!selectedMap.value[item.id];

// 单选切换
const toggleSelect = (item: any) => {
  if (selectedMap.value[item.id]) {
    delete selectedMap.value[item.id];
  } else {
    selectedMap.value[item.id] = true;
  }
};

// 全选/取消全选
const selectAll = (isCheck: boolean) => {
  if (isCheck) {
    // 全部选中
    cartItems.value.forEach(item => {
      selectedMap.value[item.id] = true;
    });
  } else {
    // 清空选中
    selectedMap.value = {};
  }
};

// 选中商品数量
const selectedCount = computed(() => Object.keys(selectedMap.value).length);

// 选中商品总价
const selectedTotal = computed(() => {
  return cartItems.value
      .filter(item => selectedMap.value[item.id])
      .reduce((total, item) => total + item.price * item.quantity, 0)
      .toFixed(2);
});

// 删除选中商品
const removeSelectedItems = () => {
  if (selectedCount.value === 0) {
    ElMessage.warning('请选择要移除的商品');
    return;
  }
  cartItems.value = cartItems.value.filter(item => !selectedMap.value[item.id]);
  selectedMap.value = {};
  localStorage.setItem(getCartKey(), JSON.stringify(cartItems.value));
  ElMessage.success('已移除选中商品');
};

// 结算下单
const buySelected = async () => {
  if (!user.value) {
    ElMessage.warning('请先登录');
    router.push('/login');
    return;
  }
  if (selectedCount.value === 0) {
    ElMessage.warning('请选择商品');
    return;
  }
  const selectedIds = Object.keys(selectedMap.value).join(',');
  router.push(`/confirm-order?goodsIds=${selectedIds}`);
};

onMounted(loadCart);
</script>

<template>
  <div class="cart-container">
    <!-- 页面标题 -->
    <div class="cart-header">
      <h2 class="cart-title">我的购物车</h2>
      <span class="cart-tips">挑选心仪商品，一键结算</span>
    </div>

    <!-- 空购物车 -->
    <div v-if="cartItems.length === 0" class="cart-empty">
      <div class="empty-icon">🛒</div>
      <p class="empty-text">购物车还是空的，快去挑选好物吧</p>
      <el-button type="primary" size="large" @click="$router.push('/')">去首页逛逛</el-button>
    </div>

    <!-- 购物车商品列表 -->
    <div v-else class="cart-content">
      <!-- 顶部操作栏 -->
      <div class="cart-top-action">
        <el-button type="danger" plain icon="Delete" @click="removeSelectedItems">
          删除选中商品
        </el-button>
      </div>

      <!-- 商品表格 -->
      <el-table
          :data="cartItems"
          class="cart-table"
          border
          stripe
      >
        <!-- 勾选框列 -->
        <el-table-column width="60" align="center">
          <template #header>
            <el-checkbox
                :model-value="selectedCount === cartItems.length && cartItems.length > 0"
                @update:model-value="selectAll($event)"
                label="全选"
            />
          </template>
          <template #default="scope">
            <!-- 使用 v-model 绑定以触发动画 -->
            <el-checkbox
                :model-value="isSelected(scope.row)"
                @update:model-value="toggleSelect(scope.row)"
            />
          </template>
        </el-table-column>

        <!-- 商品图片 -->
        <el-table-column label="商品图片" width="120" align="center">
          <template #default="scope">
            <div class="goods-img-wrap">
              <img
                  v-if="scope.row.imageUrl"
                  :src="scope.row.imageUrl"
                  alt="商品图"
                  class="goods-img"
              />
              <div v-else class="img-placeholder">暂无图片</div>
            </div>
          </template>
        </el-table-column>

        <!-- 商品名称 -->
        <el-table-column prop="title" label="商品名称" min-width="220" />

        <!-- 单价 -->
        <el-table-column label="单价" width="120" align="center">
          <template #default="scope">
            <span class="price-text">￥{{ scope.row.price }}</span>
          </template>
        </el-table-column>

        <!-- 数量操作 -->
        <el-table-column label="数量" width="160" align="center">
          <template #default="scope">
            <div class="num-control">
              <el-button
                  class="num-btn minus"
                  @click="updateQuantity(scope.row, -1)"
                  icon="Minus"
                  circle
                  size="small"
              />
              <span class="num-value">{{ scope.row.quantity }}</span>
              <el-button
                  class="num-btn plus"
                  @click="updateQuantity(scope.row, 1)"
                  icon="Plus"
                  circle
                  size="small"
                  type="primary"
              />
            </div>
          </template>
        </el-table-column>

        <!-- 小计 -->
        <el-table-column label="小计" width="130" align="center">
          <template #default="scope">
            <span class="sub-price">￥{{ (scope.row.price * scope.row.quantity).toFixed(2) }}</span>
          </template>
        </el-table-column>

        <!-- 操作 -->
        <el-table-column label="操作" width="100" align="center">
          <template #default="scope">
            <el-button text type="danger" icon="Delete" @click="removeItem(scope.$index)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 底部结算栏 -->
      <div class="cart-footer">
        <div class="footer-left">
          <el-button plain type="warning" icon="DeleteFilled" @click="clearCart">
            清空购物车
          </el-button>
        </div>
        <div class="footer-right">
          <div v-if="selectedCount > 0" class="selected-info">
            <span>已选 <b class="count">{{ selectedCount }}</b> 件商品</span>
            <span class="selected-total">合计：<b>￥{{ selectedTotal }}</b></span>
          </div>
          <el-button
              v-if="selectedCount > 0"
              type="primary"
              size="large"
              class="settle-btn"
              @click="buySelected"
          >
            去结算
          </el-button>
          <div class="all-total">
            购物车全部总价：<span class="total-money">￥{{ totalPrice }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.cart-container {
  max-width: 1200px;
  margin: 30px auto;
  padding: 0 20px;
}

/* 头部标题 */
.cart-header {
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #eee;
}
.cart-title {
  font-size: 26px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 6px;
}
.cart-tips {
  color: #909399;
  font-size: 14px;
}

/* 空购物车 */
.cart-empty {
  text-align: center;
  padding: 80px 20px;
  background: #f9fafa;
  border-radius: 12px;
}
.empty-icon {
  font-size: 72px;
  margin-bottom: 16px;
  opacity: 0.6;
}
.empty-text {
  font-size: 16px;
  color: #606266;
  margin-bottom: 24px;
}

/* 内容区域 */
.cart-content {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  padding: 20px;
}
.cart-top-action {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 16px;
}

/* 商品图片 */
.goods-img-wrap {
  width: 80px;
  height: 80px;
  border-radius: 6px;
  overflow: hidden;
  border: 1px solid #ebeef5;
}
.goods-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}
.goods-img:hover {
  transform: scale(1.08);
}
.img-placeholder {
  width: 100%;
  height: 100%;
  background: #f5f7fa;
  font-size: 12px;
  color: #c0c4cc;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 价格文字 */
.price-text {
  color: #606266;
}
.sub-price {
  color: #e64340;
  font-weight: 500;
}

/* 数量控制器 */
.num-control {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}
.num-value {
  width: 36px;
  text-align: center;
  font-size: 15px;
}
.num-btn.minus {
  border-color: #dcdfe6;
  color: #606266;
}

/* 底部结算栏 */
.cart-footer {
  margin-top: 20px;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}
.footer-right {
  display: flex;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
}
.selected-info {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 15px;
}
.count {
  color: #409eff;
  font-size: 18px;
}
.selected-total b {
  color: #e64340;
  font-size: 18px;
}
.settle-btn {
  padding: 0 26px;
  height: 44px;
  font-size: 16px;
}
.all-total {
  font-size: 16px;
  color: #606266;
}
.total-money {
  font-size: 22px;
  color: #e64340;
  font-weight: 600;
  margin-left: 8px;
}

/* 表格全局微调 */
:deep(.cart-table .el-table__row:hover > td) {
  background-color: #f5fafe !important;
}
:deep(.el-table__header th) {
  background-color: #fafafa;
}
</style>