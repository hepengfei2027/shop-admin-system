<script setup lang="ts">
import { reactive, ref } from 'vue';
import { api } from '../api';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';

const router = useRouter();

// 当前激活tab：buyer / seller
const activeTab = ref<'buyer' | 'seller'>('buyer');

const form = reactive({
  username: '',
  password: '',
  nickname: '',
  phone: '',
  role: 0 // 0买家 2商家
});

// 切换注册身份tab
const changeTab = (tab: 'buyer' | 'seller') => {
  activeTab.value = tab;
  form.role = tab === 'buyer' ? 0 : 2;
};

const onRegister = async () => {
  try {
    const res = await api.register(form);
    if (res.data.code === 0) {
      ElMessage.success('注册成功！请登录');
      router.push('/login');
    } else {
      ElMessage.error(res.data.msg || '注册失败');
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.msg || '注册失败');
  }
};
</script>

<template>
  <div class="register-page">
    <!-- 顶部身份切换导航栏 -->
    <div class="tab-nav">
      <div
          class="tab-item"
          :class="{ active: activeTab === 'buyer' }"
          @click="changeTab('buyer')"
      >
        买家注册
      </div>
      <div
          class="tab-item"
          :class="{ active: activeTab === 'seller' }"
          @click="changeTab('seller')"
      >
        商家注册
      </div>
    </div>

    <!-- 注册卡片主体 -->
    <div class="register-wrapper">
      <el-card class="register-card" shadow="hover">
        <h2 class="card-title">
          {{ activeTab === 'buyer' ? '买家注册' : '商家注册' }}
        </h2>
        <el-form label-width="90px" label-position="right">
          <el-form-item label="用户名">
            <el-input
                v-model="form.username"
                placeholder="请输入4-16位用户名"
                size="large"
            />
          </el-form-item>
          <el-form-item label="密码">
            <el-input
                v-model="form.password"
                type="password"
                placeholder="请输入6-20位密码"
                size="large"
            />
          </el-form-item>
          <el-form-item label="昵称">
            <el-input
                v-model="form.nickname"
                placeholder="请输入您的昵称"
                size="large"
            />
          </el-form-item>
          <el-form-item label="手机号">
            <el-input
                v-model="form.phone"
                placeholder="请输入11位手机号码"
                size="large"
            />
          </el-form-item>

          <!-- 已完全删除底部身份单选框，由顶部tab控制role -->

          <el-form-item class="btn-group">
            <el-button type="primary" size="large" @click="onRegister">
              立即注册
            </el-button>
            <el-button size="large" @click="router.push('/login')">
              返回登录
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
/* 页面整体 */
.register-page {
  width: 100%;
  min-height: 100vh;
  background-color: #f5f7fa;
  padding-top: 40px;
}

/* 顶部tab导航栏 */
.tab-nav {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-bottom: 36px;
}
.tab-item {
  width: 160px;
  height: 48px;
  line-height: 48px;
  text-align: center;
  font-size: 16px;
  border-radius: 8px;
  background: #fff;
  border: 1px solid #e4e7ed;
  cursor: pointer;
  transition: all 0.25s ease;
}
.tab-item:hover {
  border-color: #409eff;
  color: #409eff;
}
.tab-item.active {
  background: #409eff;
  color: #fff;
  border-color: #409eff;
}

/* 卡片居中容器 */
.register-wrapper {
  display: flex;
  justify-content: center;
  align-items: flex-start;
}

/* 注册卡片 */
.register-card {
  width: 480px;
  border-radius: 12px;
}
.card-title {
  text-align: center;
  margin: 10px 0 30px;
  color: #303133;
  font-weight: 600;
}

/* 表单按钮区域 */
.btn-group {
  margin-top: 20px;
  display: flex;
  gap: 16px;
  justify-content: center;
}
</style>