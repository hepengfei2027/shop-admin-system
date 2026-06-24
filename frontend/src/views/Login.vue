<script setup lang="ts">
import { reactive, inject, ref } from 'vue';
import { api } from '../api';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';

const router = useRouter();
const updateUser = inject('updateUser') as (userData: any) => Promise<void>;

// 当前登录身份：0普通用户 1管理员 2商家
const currentLoginType = ref<0 | 1 | 2>(0);
const typeTextMap = {
  0: '用户',
  1: '管理员',
  2: '商家'
};
const placeholderMap = {
  0: '请输入手机号/用户名',
  1: '请输入管理员账号',
  2: '请输入商家入驻账号'
};

// 表单
const form = reactive({
  username: '',
  password: '',
  remember: false
});

// 找回密码对话框
const forgotDialogVisible = ref(false);
const forgotStep = ref(1); // 1: 输入账号 2: 输入验证码 3: 设置新密码
const forgotForm = reactive({
  username: '',
  phone: '',
  code: '',
  newPassword: '',
  confirmPassword: ''
});

// 切换登录身份
const changeLoginType = (type: 0 | 1 | 2) => {
  currentLoginType.value = type;
  // 切换清空输入
  form.username = '';
  form.password = '';
};

// 读取记住账号
const initForm = () => {
  const storageLogin = localStorage.getItem('loginInfo');
  if (storageLogin) {
    const info = JSON.parse(storageLogin);
    form.username = info.username;
    form.password = info.password;
    form.remember = true;
  }
};
initForm();

// 登录提交
const onLogin = async () => {
  if (!form.username) return ElMessage.warning(`请输入${typeTextMap[currentLoginType.value]}账号`);
  if (!form.password) return ElMessage.warning('请输入登录密码');

  try {
    // 传登录身份给接口
    const res = await api.login({ ...form, loginType: currentLoginType.value });
    if (res.data.code === 0) {
      ElMessage.success(`${typeTextMap[currentLoginType.value]}登录成功，欢迎回来！`);
      const userData = res.data.data;
      localStorage.setItem('user', JSON.stringify(userData));

      // 记住账号逻辑
      if (form.remember) {
        localStorage.setItem('loginInfo', JSON.stringify({
          username: form.username,
          password: form.password
        }));
      } else {
        localStorage.removeItem('loginInfo');
      }

      if (updateUser) await updateUser(userData);

      // 根据角色跳转对应页面
      if (userData.role === 1) {
        router.push('/admin');
      } else if (userData.role === 2) {
        router.push('/');
      } else {
        router.push('/');
      }
    } else {
      ElMessage.error(res.data.msg || '账号或密码错误');
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.msg || '网络异常，请稍后重试');
  }
};

// 跳转注册
const goToRegister = () => {
  router.push('/register');
};

// 打开找回密码对话框
const openForgotDialog = () => {
  forgotStep.value = 1;
  forgotForm.username = '';
  forgotForm.phone = '';
  forgotForm.code = '';
  forgotForm.newPassword = '';
  forgotForm.confirmPassword = '';
  forgotDialogVisible.value = true;
};

// 关闭找回密码对话框
const closeForgotDialog = () => {
  forgotDialogVisible.value = false;
};

// 发送验证码（模拟发送，直接进入下一步）
const sendVerifyCode = async () => {
  if (!forgotForm.username) {
    ElMessage.warning('请输入用户名');
    return;
  }
  if (!forgotForm.phone) {
    ElMessage.warning('请输入手机号');
    return;
  }

  ElMessage.success('验证码已发送');
  forgotStep.value = 2;
};

// 验证验证码（模拟验证，任意6位数字都可以）
const verifyCode = async () => {
  if (!forgotForm.code || forgotForm.code.length !== 6) {
    ElMessage.warning('请输入6位验证码');
    return;
  }

  ElMessage.success('验证成功');
  forgotStep.value = 3;
};

// 完成重置密码（模拟成功）
const completeResetPassword = async () => {
  if (!forgotForm.newPassword) {
    ElMessage.warning('请输入新密码');
    return;
  }
  if (forgotForm.newPassword.length < 6) {
    ElMessage.warning('密码长度不能少于6位');
    return;
  }
  if (forgotForm.newPassword !== forgotForm.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致');
    return;
  }

  // 模拟重置成功
  ElMessage.success('密码重置成功，请使用新密码登录');
  closeForgotDialog();
};
</script>

<template>
  <div class="shop-login-page">
    <div class="login-container">
      <!-- 左侧电商宣传区 -->
      <div class="login-left">
        <div class="brand-box">
          <div class="brand-logo">SHOP商城</div>
          <p class="brand-slogan">正品好物 · 极速发货 · 售后无忧</p>
        </div>
        <div class="banner-desc">
          <p>百万商品随心选购</p>
          <p>新人专享大额优惠券</p>
          <p>全场满减限时特惠</p>
        </div>
      </div>

      <!-- 右侧登录表单区 -->
      <div class="login-right">
        <div class="login-card">
          <!-- 顶部身份切换导航 -->
          <div class="login-tab-nav">
            <div
                class="tab-item"
                :class="{ active: currentLoginType === 0 }"
                @click="changeLoginType(0)"
            >
              用户登录
            </div>
            <div
                class="tab-item"
                :class="{ active: currentLoginType === 2 }"
                @click="changeLoginType(2)"
            >
              商家登录
            </div>
            <div
                class="tab-item"
                :class="{ active: currentLoginType === 1 }"
                @click="changeLoginType(1)"
            >
              管理员登录
            </div>
          </div>

          <h2 class="login-title">{{ typeTextMap[currentLoginType.value] }}账号登录</h2>
          <el-form label-width="85px" :model="form">
            <el-form-item label="账号">
              <el-input
                  v-model="form.username"
                  :placeholder="placeholderMap[currentLoginType]"
                  size="large"
                  prefix-icon="User"
              />
            </el-form-item>

            <el-form-item label="密码">
              <el-input
                  v-model="form.password"
                  type="password"
                  placeholder="请输入登录密码"
                  size="large"
                  prefix-icon="Lock"
                  show-password
              />
            </el-form-item>

            <el-form-item>
              <div class="form-row">
                <el-checkbox v-model="form.remember">记住账号</el-checkbox>
                <span class="forget-pwd" @click="openForgotDialog">忘记密码？</span>
              </div>
            </el-form-item>

            <el-form-item>
              <el-button
                  type="primary"
                  size="large"
                  class="login-btn"
                  @click="onLogin"
              >
                立即登录
              </el-button>
            </el-form-item>

            <!-- 第三方快捷登录 -->
            <div class="other-login">
              <div class="line-text">
                <span class="line"></span>
                <span class="text">其他方式登录</span>
                <span class="line"></span>
              </div>
              <div class="icon-group">
                <div class="icon-item wechat">微信</div>
                <div class="icon-item alipay">支付宝</div>
                <div class="icon-item qq">QQ</div>
              </div>
            </div>

            <!-- 底部注册入口 -->
            <div class="register-tip">
              还没有{{ typeTextMap[currentLoginType.value] }}账号？
              <span class="register-link" @click="goToRegister">立即注册</span>
            </div>

            <!-- 用户协议 -->
            <div class="agreement">
              登录即代表你同意《用户服务协议》《隐私政策》
            </div>
          </el-form>
        </div>
      </div>
    </div>

    <!-- 找回密码对话框 -->
    <el-dialog
      v-model="forgotDialogVisible"
      :title="forgotStep === 1 ? '找回密码' : forgotStep === 2 ? '输入验证码' : '设置新密码'"
      width="400px"
      class="custom-dialog"
      @close="closeForgotDialog"
    >
      <!-- 步骤指示器 -->
      <div class="forgot-steps">
        <div class="step-item" :class="{ active: forgotStep >= 1, current: forgotStep === 1 }">
          <div class="step-circle">1</div>
          <div class="step-text">验证账号</div>
        </div>
        <div class="step-line" :class="{ active: forgotStep >= 2 }"></div>
        <div class="step-item" :class="{ active: forgotStep >= 2, current: forgotStep === 2 }">
          <div class="step-circle">2</div>
          <div class="step-text">输入验证码</div>
        </div>
        <div class="step-line" :class="{ active: forgotStep >= 3 }"></div>
        <div class="step-item" :class="{ active: forgotStep >= 3, current: forgotStep === 3 }">
          <div class="step-circle">3</div>
          <div class="step-text">重置密码</div>
        </div>
      </div>

      <!-- 步骤1：验证账号 -->
      <el-form v-if="forgotStep === 1" label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="forgotForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="forgotForm.phone" placeholder="请输入手机号" />
        </el-form-item>
      </el-form>

      <!-- 步骤2：输入验证码 -->
      <el-form v-if="forgotStep === 2" label-width="80px">
        <el-form-item label="验证码">
          <el-input v-model="forgotForm.code" placeholder="请输入6位验证码" maxlength="6" />
        </el-form-item>
      </el-form>

      <!-- 步骤3：设置新密码 -->
      <el-form v-if="forgotStep === 3" label-width="80px">
        <el-form-item label="新密码">
          <el-input v-model="forgotForm.newPassword" type="password" placeholder="请输入新密码" show-password />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input v-model="forgotForm.confirmPassword" type="password" placeholder="请再次输入新密码" show-password />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="closeForgotDialog">取消</el-button>
        <el-button v-if="forgotStep === 1" type="primary" @click="sendVerifyCode">发送验证码</el-button>
        <el-button v-if="forgotStep === 2" type="primary" @click="verifyCode">验证</el-button>
        <el-button v-if="forgotStep === 3" type="primary" @click="completeResetPassword">确认重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.shop-login-page {
  width: 100%;
  min-height: 100vh;
  background: linear-gradient(120deg, #fff5f0, #f0f7ff);
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px;
}

.login-container {
  width: 1080px;
  height: 560px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.08);
  display: flex;
  overflow: hidden;
}

/* 左侧宣传区域 */
.login-left {
  width: 520px;
  background: linear-gradient(140deg, #ff6b35, #ff9559);
  color: #fff;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 60px;
}
.brand-logo {
  font-size: 42px;
  font-weight: bold;
  letter-spacing: 2px;
}
.brand-slogan {
  font-size: 16px;
  opacity: 0.9;
  margin-top: 8px;
}
.banner-desc {
  display: flex;
  flex-direction: column;
  gap: 16px;
  font-size: 18px;
}
.banner-desc p {
  padding-left: 24px;
  position: relative;
}
.banner-desc p::before {
  content: "✓";
  position: absolute;
  left: 0;
  width: 18px;
  height: 18px;
  background: #fff;
  color: #ff6b35;
  border-radius: 50%;
  text-align: center;
  font-size: 14px;
}

/* 右侧登录表单 */
.login-right {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
}
.login-card {
  width: 380px;
}

/* 身份切换导航栏 */
.login-tab-nav {
  display: flex;
  border-bottom: 1px solid #eee;
  margin-bottom: 24px;
}
.tab-item {
  flex: 1;
  text-align: center;
  padding: 12px 0;
  font-size: 16px;
  color: #666;
  cursor: pointer;
  position: relative;
  transition: all 0.2s;
}
.tab-item:hover {
  color: #ff6b35;
}
.tab-item.active {
  color: #ff6b35;
  font-weight: 500;
}
.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 50%;
  transform: translateX(-50%);
  width: 60%;
  height: 3px;
  background-color: #ff6b35;
  border-radius: 3px;
}

.login-title {
  text-align: center;
  font-size: 28px;
  color: #333;
  margin-bottom: 32px;
  font-weight: 500;
}

/* 输入框样式（透明无底色、字体放大、边框完整） */
:deep(.el-form-item__label) {
  font-size: 16px !important;
  color: #333;
}
:deep(.el-input__wrapper) {
  background-color: transparent !important;
  box-shadow: none !important;
  border: 1px solid #dcdcdc !important;
  border-radius: 6px;
}
:deep(.el-input__wrapper:hover) {
  border-color: #ff6b35 !important;
}
:deep(.el-input__wrapper.is-focus) {
  border-color: #ff6b35 !important;
  box-shadow: 0 0 0 2px rgba(255, 107, 53, 0.15) !important;
}
:deep(.el-input__inner) {
  background-color: transparent !important;
  color: #222 !important;
  font-size: 16px !important;
  height: 48px;
}
:deep(.el-input__inner::placeholder) {
  font-size: 15px;
  color: #999;
}

/* 清除浏览器自动填充黄色底色 */
:deep(.el-input__inner:-webkit-autofill),
:deep(.el-input__inner:-webkit-autofill:hover),
:deep(.el-input__inner:-webkit-autofill:focus),
:deep(.el-input__inner:-webkit-autofill:active) {
  -webkit-box-shadow: 0 0 0 1000px transparent inset !important;
  box-shadow: 0 0 0 1000px transparent inset !important;
  background-color: transparent !important;
  -webkit-text-fill-color: #222 !important;
  transition: background-color 9999s ease-out !important;
}

.form-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.forget-pwd {
  color: #ff6b35;
  cursor: pointer;
  font-size: 15px;
}
.forget-pwd:hover {
  text-decoration: underline;
}

.login-btn {
  width: 100%;
  background: #ff6b35;
  border: none;
  font-size: 17px;
  height: 48px;
}
.login-btn:hover {
  background: #ff581f !important;
}

.other-login {
  margin-top: 24px;
}
.line-text {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #999;
  font-size: 14px;
}
.line {
  flex: 1;
  height: 1px;
  background: #eee;
}
.icon-group {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-top: 16px;
}
.icon-item {
  width: 46px;
  height: 46px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  color: #fff;
  cursor: pointer;
}
.icon-item.wechat {
  background: #07c160;
}
.icon-item.alipay {
  background: #1677ff;
}
.icon-item.qq {
  background: #5094e6;
}

.register-tip {
  text-align: center;
  margin-top: 28px;
  font-size: 15px;
  color: #666;
}
.register-link {
  color: #ff6b35;
  cursor: pointer;
  margin-left: 6px;
  font-size: 15px;
}
.register-link:hover {
  text-decoration: underline;
}

.agreement {
  text-align: center;
  margin-top: 16px;
  font-size: 13px;
  color: #aaa;
  line-height: 1.6;
}

/* 找回密码对话框样式 */
.forgot-steps {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24px;
  padding: 0 20px;
}
.step-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}
.step-circle {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #ddd;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 500;
}
.step-item.active .step-circle {
  background: #ff6b35;
}
.step-item.current .step-circle {
  background: #ff6b35;
  box-shadow: 0 0 0 4px rgba(255, 107, 53, 0.2);
}
.step-text {
  font-size: 12px;
  color: #999;
}
.step-item.active .step-text {
  color: #ff6b35;
}
.step-line {
  flex: 1;
  height: 2px;
  background: #ddd;
  margin: 0 8px;
  margin-bottom: 20px;
}
.step-line.active {
  background: #ff6b35;
}

/* 对话框样式 */
.custom-dialog :deep(.el-dialog) {
  border-radius: 8px;
}
.custom-dialog :deep(.el-dialog__header) {
  background: #fff;
  border-bottom: 1px solid #eee;
  padding: 16px 20px;
}
.custom-dialog :deep(.el-dialog__title) {
  color: #333;
  font-weight: 600;
  font-size: 18px;
}
.custom-dialog :deep(.el-dialog__close) {
  color: #999;
}
.custom-dialog :deep(.el-dialog__body) {
  padding: 20px;
}
.custom-dialog :deep(.el-dialog__footer) {
  padding: 12px 20px;
  border-top: 1px solid #eee;
}
</style>