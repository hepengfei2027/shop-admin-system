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
  stock: 999
});

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
  <div class="publish-wrapper">
    <el-card class="publish-card">
      <h2>发布商品</h2>
      <el-form label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="商品图片">
          <el-upload
            :show-file-list="false"
            :before-upload="beforeUpload"
            :on-change="onFileChange"
          >
            <el-button :loading="uploading" type="primary">上传商品图片</el-button>
          </el-upload>
          <div v-if="form.imageUrl" style="margin-top: 10px">
            <span>预览：</span>
            <img
              :src="form.imageUrl"
              alt="预览图片"
              style="width: 120px; height: 120px; object-fit: cover; border-radius: 4px"
            />
          </div>
        </el-form-item>
        <el-form-item label="价格">
          <el-input-number v-model="form.price" :min="0" :step="1" />
        </el-form-item>
        <el-form-item label="运费">
          <el-input-number v-model="form.freight" :min="0" :step="1" />
          <span style="margin-left: 10px; color: #999;">不填默认包邮</span>
        </el-form-item>
        <el-form-item label="库存">
          <el-input-number v-model="form.stock" :min="1" :step="1" />
          <span style="margin-left: 10px; color: #999;">默认999</span>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onPublish">发布</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.publish-wrapper {
  display: flex;
  justify-content: center;
}

.publish-card {
  width: 600px;
}
</style>

