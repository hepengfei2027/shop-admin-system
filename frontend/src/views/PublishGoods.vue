<script setup lang="ts">
import { reactive, ref } from 'vue';
import { api } from '../api';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';

const router = useRouter();

const form = reactive({
  title: '',
  description: '',
  imageUrl: '',
  price: 0,
  freight: 0,
  stock: 999,
  category: '',
  brandName: '',
  brandColor: '#ff1744'
});

const categories = [
  { label: '数码电子', value: '数码电子' },
  { label: '服装服饰', value: '服装服饰' },
  { label: '美妆护肤', value: '美妆护肤' },
  { label: '家居日用', value: '家居日用' },
  { label: '食品饮料', value: '食品饮料' },
  { label: '图书文具', value: '图书文具' },
  { label: '运动户外', value: '运动户外' },
  { label: '其他', value: '其他' }
];

const brandColors = [
  { label: '红色', value: '#ff1744' },
  { label: '粉色', value: '#ff4081' },
  { label: '紫色', value: '#e040fb' },
  { label: '蓝色', value: '#448aff' },
  { label: '青色', value: '#18ffff' },
  { label: '绿色', value: '#00e676' },
  { label: '黄色', value: '#ffea00' },
  { label: '橙色', value: '#ff9100' }
];

const uploading = ref(false);

const handleUploadSuccess = (url: string) => {
  form.imageUrl = url;
  ElMessage.success('图片上传成功');
};

const beforeUpload = (file: File) => {
  const isImage = file.type.startsWith('image/');
  const isLt2M = file.size / 1024 / 1024 < 2;
  if (!isImage) {
    ElMessage.error('只能上传图片文件');
  }
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过 2MB');
  }
  return isImage && isLt2M;
};

const onFileChange = async (file: any) => {
  if (!file || !file.raw) return;
  uploading.value = true;
  try {
    const res = await api.uploadImage(file.raw);
    if (res.data.code === 0) {
      handleUploadSuccess(res.data.data);
    } else {
      ElMessage.error(res.data.msg || '上传失败');
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.msg || '上传失败');
  } finally {
    uploading.value = false;
  }
};

const onPublish = async () => {
  const userStr = localStorage.getItem('user');
  if (!userStr) {
    ElMessage.warning('请先登录');
    return;
  }
  const user = JSON.parse(userStr);
  try {
    const payload = {
      ...form,
      sellerId: user.id
    };
    const res = await api.publishGoods(payload);
    if (res.data.code === 0) {
      ElMessage.success('发布成功');
      router.push('/');
    } else {
      ElMessage.error(res.data.msg || '发布失败');
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.msg || '发布失败');
  }
};
</script>

<template>
  <div class="publish-page">
    <div class="publish-container">
      <div class="publish-header">
        <div class="header-content">
          <h1 class="page-title">发布商品</h1>
          <p class="page-subtitle">完善商品信息，让更多买家发现您的商品</p>
        </div>
      </div>

      <div class="publish-content">
        <!-- 左侧图片区域 -->
        <div class="image-panel">
          <div class="panel-title">
            <span class="title-icon">📷</span>
            商品图片
          </div>
          <div class="image-upload-area">
            <el-upload
              :show-file-list="false"
              :before-upload="beforeUpload"
              :on-change="onFileChange"
              class="upload-trigger"
            >
              <div v-if="form.imageUrl" class="image-preview">
                <img :src="form.imageUrl" alt="商品图片" />
                <div class="upload-overlay">
                  <span class="overlay-icon">📷</span>
                  <span class="overlay-text">点击更换图片</span>
                </div>
              </div>
              <div v-else class="upload-placeholder">
                <div class="placeholder-icon">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                    <path d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"/>
                  </svg>
                </div>
                <span class="placeholder-text">点击上传商品图片</span>
                <span class="placeholder-hint">支持 JPG、PNG 格式，建议尺寸 800x800</span>
              </div>
            </el-upload>
            <div v-if="uploading" class="uploading-overlay">
              <el-icon class="is-loading"><Loading /></el-icon>
              <span>上传中...</span>
            </div>
          </div>
          <div class="upload-tips">
            <div class="tip-item">
              <span class="tip-icon">💡</span>
              <span>图片清晰美观，能提高商品曝光率</span>
            </div>
          </div>
        </div>

        <!-- 右侧表单区域 -->
        <div class="form-panel">
          <el-form :model="form" label-position="top" class="publish-form">
            <div class="form-section">
              <div class="section-label">
                <span class="label-icon">📝</span>
                基本信息
              </div>

              <el-form-item label="商品标题" class="form-item">
                <el-input
                  v-model="form.title"
                  placeholder="请输入商品标题，描述商品核心卖点，吸引买家点击"
                  maxlength="50"
                  show-word-limit
                />
              </el-form-item>

              <el-form-item label="商品描述" class="form-item">
                <el-input
                  v-model="form.description"
                  type="textarea"
                  :rows="4"
                  placeholder="详细描述商品特点、规格、材质、用途等信息，让买家更了解商品"
                  maxlength="200"
                  show-word-limit
                />
              </el-form-item>
            </div>

            <div class="form-section">
              <div class="section-label">
                <span class="label-icon"></span>
                价格库存
              </div>

              <div class="form-row">
                <el-form-item label="价格" class="form-item-half">
                  <div class="price-input-wrapper">
                    <span class="currency-symbol">¥</span>
                    <el-input-number
                      v-model="form.price"
                      :min="0"
                      :precision="2"
                      :step="1"
                      placeholder="0.00"
                    />
                  </div>
                </el-form-item>

                <el-form-item label="库存" class="form-item-half">
                  <el-input-number
                    v-model="form.stock"
                    :min="1"
                    :step="1"
                    placeholder="库存数量"
                  />
                </el-form-item>
              </div>

              <el-form-item label="运费" class="form-item">
                <div class="freight-wrapper">
                  <el-input-number
                    v-model="form.freight"
                    :min="0"
                    :precision="2"
                    :step="1"
                    placeholder="0.00"
                  />
                  <span class="freight-hint">填0或不填表示包邮</span>
                </div>
              </el-form-item>
            </div>

            <div class="form-section">
              <div class="section-label">
                <span class="label-icon"></span>
                商品属性
              </div>

              <el-form-item label="商品分类" class="form-item">
                <el-select v-model="form.category" placeholder="请选择商品分类" style="width: 100%">
                  <el-option v-for="cat in categories" :key="cat.value" :label="cat.label" :value="cat.value" />
                </el-select>
              </el-form-item>

              <div class="form-row">
                <el-form-item label="品牌名称" class="form-item-half">
                  <el-input v-model="form.brandName" placeholder="选填，如：Apple、华为" />
                </el-form-item>

                <el-form-item label="品牌标签色" class="form-item-half">
                  <div class="color-picker">
                    <el-select v-model="form.brandColor" placeholder="选择颜色" style="width: 100%">
                      <el-option v-for="color in brandColors" :key="color.value" :label="color.label" :value="color.value">
                        <div class="color-option">
                          <span class="color-dot" :style="{ backgroundColor: color.value }"></span>
                          <span>{{ color.label }}</span>
                        </div>
                      </el-option>
                    </el-select>
                    <span v-if="form.brandColor" class="color-preview" :style="{ backgroundColor: form.brandColor }"></span>
                  </div>
                </el-form-item>
              </div>
            </div>
          </el-form>

          <div class="form-actions">
            <el-button @click="router.push('/')" class="cancel-btn">取消</el-button>
            <el-button type="primary" @click="onPublish" class="publish-btn" :loading="uploading">
              <span class="btn-icon"></span>
              发布商品
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Noto+Sans+SC:wght@400;500;600;700&display=swap');

.publish-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8f0 100%);
  padding: 40px 20px;
  font-family: 'Noto Sans SC', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.publish-container {
  max-width: 1100px;
  margin: 0 auto;
}

.publish-header {
  margin-bottom: 32px;
}

.header-content {
  text-align: center;
}

.page-title {
  font-size: 32px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 8px 0;
}

.page-subtitle {
  font-size: 15px;
  color: #666;
  margin: 0;
}

.publish-content {
  display: flex;
  gap: 28px;
  align-items: flex-start;
}

/* 图片区域 */
.image-panel {
  width: 320px;
  flex-shrink: 0;
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
}

.panel-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a2e;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.title-icon {
  font-size: 18px;
}

.image-upload-area {
  position: relative;
  background: #f8f9fa;
  border-radius: 12px;
  overflow: hidden;
}

.upload-trigger {
  display: block;
  width: 100%;
}

.image-preview {
  position: relative;
  aspect-ratio: 1;
  cursor: pointer;
}

.image-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.upload-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
  color: #fff;
}

.image-preview:hover .upload-overlay {
  opacity: 1;
}

.overlay-icon {
  font-size: 32px;
  margin-bottom: 8px;
}

.overlay-text {
  font-size: 14px;
}

.upload-placeholder {
  aspect-ratio: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  border: 2px dashed #d0d5dd;
  border-radius: 12px;
  background: #fff;
  cursor: pointer;
  transition: all 0.3s;
}

.upload-placeholder:hover {
  border-color: #667eea;
  background: #f8f9ff;
}

.placeholder-icon {
  width: 64px;
  height: 64px;
  margin-bottom: 16px;
  color: #c0c4cc;
}

.placeholder-icon svg {
  width: 100%;
  height: 100%;
}

.placeholder-text {
  font-size: 15px;
  color: #606266;
  margin-bottom: 6px;
}

.placeholder-hint {
  font-size: 12px;
  color: #909399;
}

.uploading-overlay {
  position: absolute;
  inset: 0;
  background: rgba(255, 255, 255, 0.9);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #667eea;
  font-size: 14px;
}

.upload-tips {
  margin-top: 16px;
}

.tip-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #909399;
}

/* 表单区域 */
.form-panel {
  flex: 1;
  background: #fff;
  border-radius: 16px;
  padding: 28px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
}

.publish-form {
  max-width: 600px;
}

.form-section {
  margin-bottom: 28px;
}

.form-section:last-of-type {
  margin-bottom: 20px;
}

.section-label {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a2e;
  margin-bottom: 16px;
  padding-bottom: 10px;
  border-bottom: 1px solid #eee;
  display: flex;
  align-items: center;
  gap: 8px;
}

.label-icon {
  font-size: 16px;
}

.form-item {
  margin-bottom: 18px;
}

.form-item :deep(.el-form-item__label) {
  font-size: 14px;
  font-weight: 500;
  color: #374151;
  padding-bottom: 8px;
}

.form-item :deep(.el-input__wrapper),
.form-item :deep(.el-textarea__inner),
.form-item :deep(.el-select .el-input__wrapper) {
  border-radius: 10px;
  padding: 8px 14px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  border: 1px solid #e5e7eb;
  transition: all 0.2s;
}

.form-item :deep(.el-input__wrapper:hover),
.form-item :deep(.el-textarea__inner:hover),
.form-item :deep(.el-select .el-input__wrapper:hover) {
  border-color: #667eea;
}

.form-item :deep(.el-input__wrapper.is-focus),
.form-item :deep(.el-textarea__inner:focus),
.form-item :deep(.el-select .el-input__wrapper.is-focus) {
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.form-row {
  display: flex;
  gap: 16px;
}

.form-item-half {
  flex: 1;
}

.form-item-half :deep(.el-input-number) {
  width: 100%;
}

/* 价格输入 */
.price-input-wrapper {
  display: flex;
  align-items: center;
  background: #f9fafb;
  border-radius: 10px;
  padding-left: 14px;
  border: 1px solid #e5e7eb;
}

.price-input-wrapper:hover {
  border-color: #667eea;
}

.currency-symbol {
  color: #ef4444;
  font-size: 16px;
  font-weight: 600;
  margin-right: 4px;
}

.price-input-wrapper :deep(.el-input-number) {
  flex: 1;
}

.price-input-wrapper :deep(.el-input-number .el-input__wrapper) {
  background: transparent;
  box-shadow: none;
  border: none;
}

/* 运费输入 */
.freight-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
}

.freight-wrapper :deep(.el-input-number) {
  flex: 1;
  max-width: 200px;
}

.freight-hint {
  font-size: 13px;
  color: #909399;
}

/* 颜色选择 */
.color-picker {
  display: flex;
  align-items: center;
  gap: 12px;
}

.color-picker :deep(.el-select) {
  flex: 1;
}

.color-option {
  display: flex;
  align-items: center;
  gap: 10px;
}

.color-dot {
  width: 18px;
  height: 18px;
  border-radius: 5px;
  flex-shrink: 0;
}

.color-preview {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  border: 2px solid #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  flex-shrink: 0;
}

/* 操作按钮 */
.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 20px;
  border-top: 1px solid #eee;
  margin-top: 20px;
}

.cancel-btn {
  padding: 12px 28px;
  border-radius: 10px;
  font-size: 15px;
  border-color: #d0d5dd;
  color: #374151;
}

.cancel-btn:hover {
  border-color: #667eea;
  color: #667eea;
}

.publish-btn {
  padding: 12px 32px;
  border-radius: 10px;
  font-size: 15px;
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  border: none;
  box-shadow: 0 4px 14px rgba(239, 68, 68, 0.35);
  transition: all 0.3s;
}

.publish-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(239, 68, 68, 0.45);
  background: linear-gradient(135deg, #f87171 0%, #ef4444 100%);
}

.btn-icon {
  margin-right: 8px;
}

/* 响应式 */
@media (max-width: 900px) {
  .publish-content {
    flex-direction: column;
  }

  .image-panel {
    width: 100%;
  }

  .form-panel {
    width: 100%;
  }
}

@media (max-width: 600px) {
  .publish-page {
    padding: 20px 12px;
  }

  .page-title {
    font-size: 24px;
  }

  .form-row {
    flex-direction: column;
    gap: 0;
  }

  .form-actions {
    flex-direction: column-reverse;
  }

  .cancel-btn,
  .publish-btn {
    width: 100%;
  }
}
</style>
