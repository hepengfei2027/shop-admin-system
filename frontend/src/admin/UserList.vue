<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { api } from '../api';
import { ElMessage, ElButton, ElMessageBox, ElDialog, ElInput, ElSelect, ElOption, ElTag, ElAvatar } from 'element-plus';

const userList = ref<any[]>([]);

// 编辑用户相关
const dialogVisible = ref(false);
const currentUser = ref<any>(null);
const editForm = ref({
  nickname: '',
  role: 0
});

onMounted(() => {
  loadUserList();
});

const loadUserList = async () => {
  try {
    const res = await api.listUsers();
    if (res.data.code === 0) {
      userList.value = res.data.data || [];
    }
  } catch (err) {
    ElMessage.error('加载用户列表失败');
  }
};

// 封禁用户
const banUser = async (user: any) => {
  try {
    const { value: banHours } = await ElMessageBox.prompt('请输入封禁时长（小时）', '封禁用户', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPattern: /^\d+$/,
      inputErrorMessage: '请输入有效的数字'
    });
    
    await api.updateUserStatus(user.id, 1, parseInt(banHours));
    ElMessage.success('用户已封禁');
    loadUserList();
  } catch (err) {
    // 用户取消操作
  }
};

// 解除封禁
const unbanUser = async (user: any) => {
  try {
    await api.updateUserStatus(user.id, 0);
    ElMessage.success('已解除封禁');
    loadUserList();
  } catch (err) {
    ElMessage.error('操作失败');
  }
};

// 编辑用户
const editUser = (user: any) => {
  currentUser.value = user;
  editForm.value = {
    nickname: user.nickname || '',
    role: user.role
  };
  dialogVisible.value = true;
};

// 保存用户信息
const saveUser = async () => {
  if (!editForm.value.nickname.trim()) {
    ElMessage.warning('请输入昵称');
    return;
  }
  try {
    await api.updateUserInfo(currentUser.value.id, editForm.value.nickname, editForm.value.role);
    ElMessage.success('用户信息已更新');
    dialogVisible.value = false;
    loadUserList();
  } catch (err) {
    ElMessage.error('更新失败');
  }
};

// 删除用户
const deleteUser = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定要删除这个用户吗？', '删除用户', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    });
    ElMessage.info('删除功能待实现');
  } catch (err) {
    // 用户取消删除
  }
};

// 格式化封禁截止时间
const formatBannedUntil = (bannedUntil: string) => {
  const now = new Date();
  const bannedTime = new Date(bannedUntil);
  const diff = bannedTime.getTime() - now.getTime();
  const hours = Math.ceil(diff / (1000 * 60 * 60));
  return `剩余${hours}小时`;
};
</script>

<template>
  <div class="user-list">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-left">
        <span class="page-icon">👥</span>
        <div class="page-info">
          <h1 class="page-title">用户管理</h1>
          <p class="page-desc">管理平台所有注册用户</p>
        </div>
      </div>
      <div class="header-right">
        <div class="user-summary">
          <div class="summary-item">
            <span class="summary-number">{{ userList.length }}</span>
            <span class="summary-label">总用户</span>
          </div>
        </div>
        <el-button type="primary" @click="loadUserList">
          🔄 刷新
        </el-button>
      </div>
    </div>

    <!-- 快速统计 -->
    <el-row :gutter="20" class="quick-stats">
      <el-col :xs="12" :sm="6">
        <div class="stat-box blue">
          <div class="stat-icon">👤</div>
          <div class="stat-content">
            <div class="stat-number">{{ userList.length }}</div>
            <div class="stat-label">注册用户</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-box success">
          <div class="stat-icon">✅</div>
          <div class="stat-content">
            <div class="stat-number">--</div>
            <div class="stat-label">正常用户</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-box warning">
          <div class="stat-icon">🚫</div>
          <div class="stat-content">
            <div class="stat-number">--</div>
            <div class="stat-label">封禁用户</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-box purple">
          <div class="stat-icon">⭐</div>
          <div class="stat-content">
            <div class="stat-number">--</div>
            <div class="stat-label">管理员</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 用户列表 -->
    <el-card class="table-card">
      <el-table 
        :data="userList" 
        style="width: 100%" 
        stripe
      >
        <el-table-column label="用户信息" width="280">
          <template #default="scope">
            <div class="user-cell">
              <el-avatar :size="48" :src="scope.row.avatar" class="user-avatar">
                {{ (scope.row.nickname || scope.row.username || 'U').charAt(0).toUpperCase() }}
              </el-avatar>
              <div class="user-details">
                <div class="user-name">{{ scope.row.nickname || scope.row.username }}</div>
                <div class="user-username">@{{ scope.row.username }}</div>
                <div class="user-id">ID: {{ scope.row.id }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="角色" width="120">
          <template #default="scope">
            <el-tag :type="scope.row.role === 1 ? 'danger' : 'info'" size="small">
              {{ scope.row.role === 1 ? '管理员' : '普通用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="160">
          <template #default="scope">
            <div class="status-cell">
              <el-tag :type="scope.row.status === 0 ? 'success' : 'danger'" size="small">
                {{ scope.row.status === 0 ? '正常' : '已封禁' }}
              </el-tag>
              <span v-if="scope.row.status === 1 && scope.row.bannedUntil" class="ban-time">
                {{ formatBannedUntil(scope.row.bannedUntil) }}
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="scope">
            <el-button 
              v-if="scope.row.status === 0" 
              type="danger" 
              size="small" 
              @click="banUser(scope.row)"
            >
              封禁
            </el-button>
            <el-button 
              v-else 
              type="success" 
              size="small" 
              @click="unbanUser(scope.row)"
            >
              解封
            </el-button>
            <el-button type="primary" size="small" @click="editUser(scope.row)">
              编辑
            </el-button>
            <el-button type="info" size="small" @click="deleteUser(scope.row.id)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 编辑用户对话框 -->
    <el-dialog v-model="dialogVisible" title="编辑用户信息" width="480px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="currentUser.username" disabled />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="editForm.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="editForm.role" placeholder="请选择角色">
            <el-option label="普通用户" :value="0" />
            <el-option label="管理员" :value="1" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveUser">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.user-list {
  padding: 0;
}

/* 页面标题 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.page-icon {
  font-size: 40px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: #1f2937;
  margin: 0 0 4px 0;
}

.page-desc {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-summary {
  display: flex;
  gap: 20px;
  background: white;
  padding: 12px 24px;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.summary-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.summary-number {
  font-size: 24px;
  font-weight: 700;
  color: #1f2937;
}

.summary-label {
  font-size: 13px;
  color: #6b7280;
  margin-top: 2px;
}

/* 快速统计 */
.quick-stats {
  margin-bottom: 24px;
}

.stat-box {
  background: white;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  margin-bottom: 20px;
}

.stat-box.blue {
  border-left: 4px solid #3b82f6;
}

.stat-box.success {
  border-left: 4px solid #10b981;
}

.stat-box.warning {
  border-left: 4px solid #f59e0b;
}

.stat-box.purple {
  border-left: 4px solid #8b5cf6;
}

.stat-icon {
  font-size: 36px;
}

.stat-content {
  flex: 1;
}

.stat-number {
  font-size: 28px;
  font-weight: 700;
  color: #1f2937;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 13px;
  color: #6b7280;
}

/* 表格卡片 */
.table-card {
  border-radius: 16px;
  border: none;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
}

/* 用户单元格 */
.user-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-avatar {
  border: 2px solid #f3f4f6;
}

.user-details {
  flex: 1;
  min-width: 0;
}

.user-name {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 2px;
}

.user-username {
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 2px;
}

.user-id {
  font-size: 12px;
  color: #9ca3af;
}

.status-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.ban-time {
  font-size: 12px;
  color: #f59e0b;
}
</style>
