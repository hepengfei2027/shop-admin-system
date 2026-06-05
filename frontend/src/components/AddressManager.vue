<script setup lang="ts">
import { ref, watch, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { api } from '../api';

const props = defineProps<{
  modelValue: boolean;
  userId?: number;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void;
  (e: 'close'): void;
  (e: 'select', address: any): void;
}>();

const addresses = ref<any[]>([]);
const selectedAddressId = ref<number | null>(null);

const dialogVisible = ref(false);
const innerDialogVisible = ref(false);
const isEdit = ref(false);
const form = ref({
  id: null,
  name: '',
  phone: '',
  province: '',
  city: '',
  district: '',
  detail: '',
  isDefault: false
});

const provinces = [
  { value: '北京市', label: '北京市' },
  { value: '上海市', label: '上海市' },
  { value: '广东省', label: '广东省' },
  { value: '浙江省', label: '浙江省' },
  { value: '江苏省', label: '江苏省' },
  { value: '四川省', label: '四川省' },
  { value: '湖北省', label: '湖北省' },
  { value: '湖南省', label: '湖南省' }
];

const cities: Record<string, { value: string; label: string }[]> = {
  '北京市': [{ value: '东城区', label: '东城区' }, { value: '西城区', label: '西城区' }, { value: '朝阳区', label: '朝阳区' }, { value: '海淀区', label: '海淀区' }],
  '上海市': [{ value: '黄浦区', label: '黄浦区' }, { value: '徐汇区', label: '徐汇区' }, { value: '浦东新区', label: '浦东新区' }, { value: '静安区', label: '静安区' }],
  '广东省': [{ value: '广州市', label: '广州市' }, { value: '深圳市', label: '深圳市' }, { value: '东莞市', label: '东莞市' }, { value: '佛山市', label: '佛山市' }],
  '浙江省': [{ value: '杭州市', label: '杭州市' }, { value: '宁波市', label: '宁波市' }, { value: '温州市', label: '温州市' }, { value: '嘉兴市', label: '嘉兴市' }],
  '江苏省': [{ value: '南京市', label: '南京市' }, { value: '苏州市', label: '苏州市' }, { value: '无锡市', label: '无锡市' }, { value: '常州市', label: '常州市' }],
  '四川省': [{ value: '成都市', label: '成都市' }, { value: '绵阳市', label: '绵阳市' }, { value: '德阳市', label: '德阳市' }, { value: '南充市', label: '南充市' }],
  '湖北省': [{ value: '武汉市', label: '武汉市' }, { value: '宜昌市', label: '宜昌市' }, { value: '襄阳市', label: '襄阳市' }, { value: '荆州市', label: '荆州市' }],
  '湖南省': [{ value: '长沙市', label: '长沙市' }, { value: '株洲市', label: '株洲市' }, { value: '湘潭市', label: '湘潭市' }, { value: '衡阳市', label: '衡阳市' }]
};

const districts: Record<string, { value: string; label: string }[]> = {
  '广州市': [{ value: '天河区', label: '天河区' }, { value: '越秀区', label: '越秀区' }, { value: '海珠区', label: '海珠区' }, { value: '白云区', label: '白云区' }],
  '深圳市': [{ value: '福田区', label: '福田区' }, { value: '南山区', label: '南山区' }, { value: '宝安区', label: '宝安区' }, { value: '龙华区', label: '龙华区' }],
  '杭州市': [{ value: '上城区', label: '上城区' }, { value: '下城区', label: '下城区' }, { value: '西湖区', label: '西湖区' }, { value: '滨江区', label: '滨江区' }],
  '武汉市': [{ value: '江岸区', label: '江岸区' }, { value: '江汉区', label: '江汉区' }, { value: '硚口区', label: '硚口区' }, { value: '汉阳区', label: '汉阳区' }],
  '成都市': [{ value: '锦江区', label: '锦江区' }, { value: '青羊区', label: '青羊区' }, { value: '金牛区', label: '金牛区' }, { value: '武侯区', label: '武侯区' }],
  '南京市': [{ value: '玄武区', label: '玄武区' }, { value: '秦淮区', label: '秦淮区' }, { value: '鼓楼区', label: '鼓楼区' }, { value: '建邺区', label: '建邺区' }],
  '北京市': [{ value: '东城区', label: '东城区' }, { value: '西城区', label: '西城区' }, { value: '朝阳区', label: '朝阳区' }, { value: '海淀区', label: '海淀区' }],
  '上海市': [{ value: '黄浦区', label: '黄浦区' }, { value: '徐汇区', label: '徐汇区' }, { value: '浦东新区', label: '浦东新区' }, { value: '静安区', label: '静安区' }],
};

const currentCities = ref<{ value: string; label: string }[]>([]);
const currentDistricts = ref<{ value: string; label: string }[]>([]);

const loadAddresses = async () => {
  if (!props.userId) return;
  try {
    const res = await api.getAddresses(props.userId);
    if (res.data.code === 0) {
      addresses.value = res.data.data || [];
      const defaultAddr = addresses.value.find(a => a.isDefault);
      if (defaultAddr) {
        selectedAddressId.value = defaultAddr.id;
      }
    }
  } catch (err) {
    ElMessage.error('加载地址失败');
  }
};

const handleProvinceChange = () => {
  currentCities.value = cities[form.value.province] || [];
  form.value.city = '';
  form.value.district = '';
  currentDistricts.value = [];
};

const handleCityChange = () => {
  currentDistricts.value = districts[form.value.city] || [];
  form.value.district = '';
};

const openAddDialog = () => {
  isEdit.value = false;
  form.value = {
    id: null,
    name: '',
    phone: '',
    province: '',
    city: '',
    district: '',
    detail: '',
    isDefault: false
  };
  currentCities.value = [];
  currentDistricts.value = [];
  innerDialogVisible.value = true;
};

const openEditDialog = (address: any) => {
  isEdit.value = true;
  form.value = {
    id: address.id,
    name: address.name,
    phone: address.phone,
    province: address.province,
    city: address.city,
    district: address.district,
    detail: address.detail,
    isDefault: address.isDefault
  };
  currentCities.value = cities[address.province] || [];
  currentDistricts.value = districts[address.city] || [];
  innerDialogVisible.value = true;
};

const saveAddress = async () => {
  if (!form.value.name || !form.value.phone || !form.value.province || !form.value.city || !form.value.detail) {
    ElMessage.warning('请填写完整信息');
    return;
  }
  
  if (!props.userId) {
    ElMessage.warning('请先登录');
    return;
  }
  
  try {
    if (isEdit.value) {
      form.value.userId = props.userId;
      await api.updateAddress(form.value);
      ElMessage.success('地址更新成功');
    } else {
      form.value.userId = props.userId;
      await api.addAddress(form.value);
      ElMessage.success('地址添加成功');
    }
    innerDialogVisible.value = false;
    await loadAddresses();
  } catch (err) {
    ElMessage.error(isEdit.value ? '更新失败' : '添加失败');
  }
};

const deleteAddress = async (id: number) => {
  if (!props.userId) return;
  try {
    await api.deleteAddress(id, props.userId);
    ElMessage.success('地址已删除');
    await loadAddresses();
  } catch (err) {
    ElMessage.error('删除失败');
  }
};

const selectAddress = (address: any) => {
  selectedAddressId.value = address.id;
};

const confirmSelect = () => {
  if (!selectedAddressId.value) {
    ElMessage.warning('请选择收货地址');
    return;
  }
  const address = addresses.value.find(a => a.id === selectedAddressId.value);
  if (address) {
    emit('select', address);
  }
};

const closeDialog = () => {
  innerDialogVisible.value = false;
};

watch(() => props.modelValue, (val) => {
  dialogVisible.value = val;
  if (val) {
    loadAddresses();
  }
});

watch(dialogVisible, (val) => {
  emit('update:modelValue', val);
});

onMounted(() => {
  if (props.modelValue) {
    loadAddresses();
  }
});
</script>

<template>
  <el-dialog title="选择收货地址" v-model="dialogVisible" width="600px" @close="emit('close')">
    <div class="address-list">
      <div
        v-for="address in addresses"
        :key="address.id"
        :class="['address-item', { selected: selectedAddressId === address.id }]"
        @click="selectAddress(address)"
      >
        <div class="address-header">
          <span class="name">{{ address.name }}</span>
          <span class="phone">{{ address.phone }}</span>
          <span v-if="address.isDefault" class="default-tag">默认</span>
        </div>
        <div class="address-detail">
          {{ address.province }} {{ address.city }} {{ address.district }} {{ address.detail }}
        </div>
        <div class="address-actions">
          <button @click.stop="openEditDialog(address)">编辑</button>
          <button @click.stop="deleteAddress(address.id)">删除</button>
        </div>
      </div>
      
      <div v-if="addresses.length === 0" class="empty-address">
        暂无收货地址，请添加
      </div>
    </div>
    
    <div class="address-footer">
      <button class="add-btn" @click="openAddDialog">+ 新增收货地址</button>
      <button class="confirm-btn" @click="confirmSelect">确认选择</button>
    </div>
    
    <!-- 新增/编辑地址弹窗 -->
    <el-dialog :title="isEdit ? '编辑地址' : '新增地址'" v-model="innerDialogVisible" width="500px" @close="closeDialog">
      <el-form :model="form" label-width="80px">
        <el-form-item label="姓名">
          <el-input v-model="form.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="省份">
          <el-select v-model="form.province" placeholder="请选择省份" @change="handleProvinceChange">
            <el-option v-for="p in provinces" :key="p.value" :label="p.label" :value="p.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="城市">
          <el-select v-model="form.city" placeholder="请选择城市" @change="handleCityChange">
            <el-option v-for="c in currentCities" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="区县">
          <el-select v-model="form.district" placeholder="请选择区县">
            <el-option v-for="d in currentDistricts" :key="d.value" :label="d.label" :value="d.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="详细地址">
          <el-input v-model="form.detail" type="textarea" :rows="3" placeholder="请输入详细地址" />
        </el-form-item>
        <el-form-item>
          <label>
            <input type="checkbox" v-model="form.isDefault" /> 设为默认地址
          </label>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeDialog">取消</el-button>
        <el-button type="primary" @click="saveAddress">{{ isEdit ? '保存修改' : '添加地址' }}</el-button>
      </template>
    </el-dialog>
  </el-dialog>
</template>

<style scoped>
.address-list {
  max-height: 400px;
  overflow-y: auto;
  margin-bottom: 20px;
}

.address-item {
  padding: 15px;
  border: 2px solid #e4e7ed;
  border-radius: 4px;
  margin-bottom: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.address-item:hover {
  border-color: #409eff;
}

.address-item.selected {
  border-color: #409eff;
  background-color: #f0f5ff;
}

.address-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 5px;
}

.name {
  font-weight: bold;
}

.phone {
  color: #666;
}

.default-tag {
  background-color: #409eff;
  color: white;
  font-size: 12px;
  padding: 2px 6px;
  border-radius: 2px;
}

.address-detail {
  color: #666;
  font-size: 14px;
}

.address-actions {
  margin-top: 10px;
  display: flex;
  gap: 10px;
}

.address-actions button {
  padding: 4px 10px;
  font-size: 12px;
  border: none;
  background-color: #f5f7fa;
  cursor: pointer;
  color: #666;
}

.address-actions button:hover {
  background-color: #e4e7ed;
}

.empty-address {
  text-align: center;
  padding: 40px;
  color: #999;
}

.address-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.add-btn, .confirm-btn {
  padding: 8px 20px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.add-btn {
  background-color: #f5f7fa;
  color: #666;
}

.add-btn:hover {
  background-color: #e4e7ed;
}

.confirm-btn {
  background-color: #409eff;
  color: white;
}

.confirm-btn:hover {
  background-color: #66b1ff;
}
</style>