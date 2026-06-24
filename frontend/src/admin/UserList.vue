<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { api } from '../api';
import { ElMessage, ElButton, ElMessageBox, ElDialog, ElInput, ElSelect, ElOption, ElTag, ElAvatar } from 'element-plus';

const userList = ref<any[]>([]);
// 编辑用户相关
const dialogVisible = ref(false);
const currentUser = ref<any>(null);
const editForm = ref({ nickname: '', role: 0 });
const searchKeyword = ref('');

// 统计数据
const stats = computed(() => ({
  total: userList.value.length,
  active: userList.value.filter(u => u.status === 0).length,
  banned: userList.value.filter(u => u.status === 1).length,
  admin: userList.value.filter(u => u.role === 1).length,
  merchant: userList.value.filter(u => u.role === 2).length,
  normalUser: userList.value.filter(u => u.role === 0).length,
}));

onMounted(() => { loadUserList(); });

const loadUserList = async () => {
  try {
    const res = await api.listUsers();
    if (res.data.code === 0) {
      let users = res.data.data || [];
      
      // 前端搜索过滤
      if (searchKeyword.value.trim()) {
        const keyword = searchKeyword.value.trim().toLowerCase();
        users = users.filter(user => {
          const username = String(user.username || '').toLowerCase();
          const nickname = String(user.nickname || '').toLowerCase();
          const userId = String(user.id || '').toLowerCase();
          return username.includes(keyword) || 
                 nickname.includes(keyword) || 
                 userId.includes(keyword);
        });
      }
      
      userList.value = users;
    }
  } catch (err) { ElMessage.error('加载用户列表失败'); }
};

// 封禁用户
const banUser = async (user: any) => {
  try {
    const { value: banHours } = await ElMessageBox.prompt('请输入封禁时长（小时）', '封禁用户', {
      confirmButtonText: '确定', cancelButtonText: '取消',
      inputPattern: /^\d+$/, inputErrorMessage: '请输入有效的数字'
    });
    await api.updateUserStatus(user.id, 1, parseInt(banHours));
    ElMessage.success('用户已封禁');
    loadUserList();
  } catch (err) { /* 取消 */ }
};

// 解除封禁
const unbanUser = async (user: any) => {
  try {
    await api.updateUserStatus(user.id, 0);
    ElMessage.success('已解除封禁');
    loadUserList();
  } catch (err) { ElMessage.error('操作失败'); }
};

// 编辑用户
const editUser = (user: any) => {
  currentUser.value = user;
  editForm.value = { nickname: user.nickname || '', role: user.role };
  dialogVisible.value = true;
};

// 保存用户信息
const saveUser = async () => {
  if (!editForm.value.nickname.trim()) { ElMessage.warning('请输入昵称'); return; }
  try {
    await api.updateUserInfo(currentUser.value.id, editForm.value.nickname, editForm.value.role);
    ElMessage.success('用户信息已更新');
    dialogVisible.value = false;
    loadUserList();
  } catch (err) { ElMessage.error('更新失败'); }
};

// 删除用户
const deleteUser = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定要删除这个用户吗？', '删除用户', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
    });
    ElMessage.info('删除功能待实现');
  } catch (err) { /* 取消 */ }
};

// 格式化封禁截止时间
const formatBannedUntil = (bannedUntil: string) => {
  const now = new Date();
  const bannedTime = new Date(bannedUntil);
  const diff = bannedTime.getTime() - now.getTime();
  const hours = Math.ceil(diff / (1000 * 60 * 60));
  return `剩余${hours}小时`;
};

// 格式化时间
const formatTime = (time: string) => {
  if (!time) return '-';
  const date = new Date(time);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hour = String(date.getHours()).padStart(2, '0');
  const minute = String(date.getMinutes()).padStart(2, '0');
  return `${year}-${month}-${day} ${hour}:${minute}`;
};

// 获取角色文字与标签类型 0普通用户 1管理员 2商家
const getRoleText = (role: number) => {
  if (role === 1) return '管理员';
  if (role === 2) return '商家';
  return '普通用户';
};
const getRoleTagType = (role: number) => {
  if (role === 1) return 'danger';
  if (role === 2) return 'warning';
  return 'info';
};
</script>

<template>
  <!-- 适配父admin-content，宽度100%，无额外外边距 -->
  <div class="user-manage-wrapper">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <span class="page-icon">👥</span>
        <div>
          <h1 class="page-title">用户管理</h1>
          <p class="page-desc">管理平台注册用户、商家、管理员账号</p>
        </div>
      </div>
      <div class="header-right">
        <el-input
            v-model="searchKeyword"
            placeholder="搜索用户名/昵称/ID"
            size="small"
            class="search-input"
            @keyup.enter="loadUserList"
        >
          <template #prefix>
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16">
              <circle cx="11" cy="11" r="8"/>
              <path d="m21 21-4.35-4.35"/>
            </svg>
          </template>
        </el-input>
        <el-button @click="loadUserList">🔄 刷新</el-button>
      </div>
    </div>

    <!-- 统计卡片 填满横向 -->
    <div class="stats-grid">
      <div class="stat-item">
        <div class="stat-num">{{ stats.total }}</div>
        <div class="stat-label">总账号</div>
      </div>
      <div class="stat-item">
        <div class="stat-num">{{ stats.normalUser }}</div>
        <div class="stat-label">普通用户</div>
      </div>
      <div class="stat-item">
        <div class="stat-num">{{ stats.merchant }}</div>
        <div class="stat-label">商家</div>
      </div>
      <div class="stat-item">
        <div class="stat-num">{{ stats.admin }}</div>
        <div class="stat-label">管理员</div>
      </div>
    </div>

    <!-- 用户表格 100%填充父容器 -->
    <div class="table-container">
      <el-table :data="userList" border size="default" style="width:100%">
        <el-table-column label="用户信息" min-width="300">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :src="row.avatar" size="44" class="user-avatar" />
              <div class="user-info">
                <div class="user-name">{{ row.nickname }}</div>
                <div class="user-meta">ID:{{ row.id }} | {{ row.username }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="角色" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getRoleTagType(row.role)" size="default">
              {{ getRoleText(row.role) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="150" align="center">
          <template #default="{ row }">
            <div class="status-cell">
              <el-tag :type="row.status === 0 ? 'success' : 'danger'" size="default">
                {{ row.status === 0 ? '正常' : '封禁' }}
              </el-tag>
              <span v-if="row.status === 1" class="ban-time">
                {{ formatBannedUntil(row.bannedUntil) }}
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="注册时间" width="180" align="center">
          <template #default="{ row }">
            <span class="time-text">{{ row.createTime ? formatTime(row.createTime) : '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" align="center" fixed="right">
          <template #default="{ row }">
            <el-button @click="editUser(row)">编辑</el-button>
            <el-button v-if="row.status === 0" type="warning" @click="banUser(row)">封禁</el-button>
            <el-button v-else type="success" @click="unbanUser(row)">解封</el-button>
            <el-button type="danger" @click="deleteUser(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="dialogVisible" title="编辑账号信息" width="560px" :show-close="false">
      <div class="edit-form">
        <div class="form-item">
          <label>昵称</label>
          <el-input v-model="editForm.nickname" placeholder="请输入昵称" />
        </div>
        <div class="form-item">
          <label>角色</label>
          <el-select v-model="editForm.role" placeholder="请选择角色">
            <el-option :value="0" label="普通用户" />
            <el-option :value="1" label="管理员" />
            <el-option :value="2" label="商家" />
          </el-select>
        </div>
      </div>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveUser">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
* { margin: 0; padding: 0; box-sizing: border-box; }

/* 关键：宽度100%继承父admin-content，无额外内外边距，填满父区域 */
.user-manage-wrapper {
  width: 100%;
  height: 100%;
  color: #333;
}

/* 页面头部 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 14px;
  border-bottom: 1px solid #eee;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.page-icon { font-size: 28px; }
.page-title {
  font-size: 22px;
  font-weight: 600;
}
.page-desc {
  font-size: 14px;
  color: #666;
  margin-top: 3px;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}
.search-input {
  width: 240px;
}
:deep(.el-button) { border-radius: 0; }

/* 统计网格自动均分填满整行 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}
.stat-item {
  background: #fff;
  border: 1px solid #eee;
  padding: 20px 10px;
  display: flex;
  flex-direction: column;
  align-items: center;
  border-radius: 0;
}
.stat-num {
  font-size: 28px;
  font-weight: 600;
  color: #222;
}
.stat-label {
  font-size: 14px;
  color: #666;
  margin-top: 6px;
}

/* 表格容器100%宽填满父盒子 */
.table-container {
  width: 100%;
  background: #fff;
  border: 1px solid #eee;
  border-radius: 0;
}
:deep(.el-table) {
  --el-table-header-text-color: #333;
  --el-table-row-hover-bg-color: #fafafa;
}
:deep(.el-table__header-wrapper) { border-bottom: 1px solid #eee; }
:deep(.el-table th),
:deep(.el-table td) {
  padding: 12px 14px;
}
:deep(.el-table--border) { border: none; }
:deep(.el-table--border::after),
:deep(.el-table--border::before) { display: none; }

/* 用户信息单元格 */
.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}
.user-avatar {
  border-radius: 0;
  border: 1px solid #eee;
}
.user-info { line-height: 1.4; }
.user-name {
  font-size: 16px;
  font-weight: 500;
}
.user-meta {
  font-size: 14px;
  color: #666;
}

/* 状态行 */
.status-cell {
  display: flex;
  flex-direction: column;
  gap: 5px;
  align-items: center;
}
.ban-time {
  font-size: 13px;
  color: #d97706;
}
:deep(.el-tag) { border-radius: 0; }

/* 弹窗表单 */
.edit-form {
  padding: 20px 0;
}
.form-item {
  margin-bottom: 20px;
}
.form-item label {
  display: block;
  font-size: 16px;
  margin-bottom: 8px;
  color: #333;
}
:deep(.el-input__inner),
:deep(.el-select__wrapper) {
  border-radius: 0;
  height: 40px;
  font-size: 15px;
}
:deep(.el-dialog) {
  border-radius: 0;
  box-shadow: none;
  border: 1px solid #eee;
}
:deep(.el-dialog__header) {
  border-bottom: 1px solid #eee;
  padding: 14px 20px;
}
:deep(.el-dialog__title) {
  font-size: 18px;
  font-weight: 500;
}
:deep(.el-dialog__footer) {
  border-top: 1px solid #eee;
  padding: 14px 20px;
  text-align: right;
}
</style>