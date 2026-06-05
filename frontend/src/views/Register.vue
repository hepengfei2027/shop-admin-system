<script setup lang="ts">
import { reactive } from 'vue';
import { api } from '../api';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';

const router = useRouter();

const form = reactive({
  username: '',
  password: '',
  nickname: '',
  phone: '',
  role: 0
});

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
  <div class="register-wrapper">
    <el-card class="register-card">
      <h2>用户注册</h2>
      <el-form label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="身份选择">
          <el-radio-group v-model="form.role">
            <el-radio :label="0">买家</el-radio>
            <el-radio :label="2">卖家</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onRegister">注册</el-button>
          <el-button @click="router.push('/login')">返回登录</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.register-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 70vh;
}
.register-card {
  width: 450px;
}
</style>