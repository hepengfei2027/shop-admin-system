<script setup lang="ts">
import { reactive, inject } from 'vue';
import { api } from '../api';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';

const router = useRouter();
const updateUser = inject('updateUser') as (userData: any) => Promise<void>;

const form = reactive({
  username: '',
  password: ''
});

const onLogin = async () => {
  try {
    const res = await api.login(form);
    if (res.data.code === 0) {
      ElMessage.success('登录成功');
      const userData = res.data.data;
      localStorage.setItem('user', JSON.stringify(userData));
      
      // 更新用户状态
      if (updateUser) {
        await updateUser(userData);
      }

      // ✅ 这里全部换成 router.push，本地线上都兼容
      if (userData.role === 1) {
        router.push('/admin');
      } else {
        router.push('/');
      }
    } else {
      ElMessage.error(res.data.msg || '登录失败');
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.msg || '登录失败');
  }
};

// 跳转到注册页面
const goToRegister = () => {
  router.push('/register');
};
</script>

<template>
  <div class="login-wrapper">
    <el-card class="login-card">
      <h2>用户登录</h2>
      <el-form label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" />
        </el-form-item>

        <el-form-item>
          <!-- 登录按钮 -->
          <el-button type="primary" @click="onLogin">登录</el-button>

          <!-- 👇 这就是你要的 注册入口 👇 -->
          <el-button @click="goToRegister">立即注册</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.login-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 60vh;
}

.login-card {
  width: 400px;
}
</style>