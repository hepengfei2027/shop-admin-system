<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { api } from '../api';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useRouter } from 'vue-router';

const router = useRouter();
const user = ref<any>(null);
const myGoods = ref<any[]>([]);
const addresses = ref<any[]>([]);

const avatarUploading = ref(false);
const avatarPreview = ref('');

const profileDialogVisible = ref(false);
const profileForm = ref<any>({
  nickname: '',
  phone: '',
  avatar: ''
});
const profileAvatarUploading = ref(false);

const openProfileDialog = () => {
  if (user.value) {
    profileForm.value = {
      nickname: user.value.nickname || '',
      phone: user.value.phone || '',
      avatar: user.value.avatar || ''
    };
    profileDialogVisible.value = true;
  }
};

const handleProfileAvatarUpload = async (event: Event) => {
  const input = event.target as HTMLInputElement;
  const files = input.files;
  if (!files || files.length === 0) return;
  
  const file = files[0];
  const isImage = file.type.startsWith('image/');
  const isLt2M = file.size / 1024 / 1024 < 2;
  
  if (!isImage) {
    ElMessage.error('只能上传图片文件');
    return;
  }
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过 2MB');
    return;
  }
  
  profileAvatarUploading.value = true;
  try {
    const res = await api.uploadImage(file);
    if (res.data.code === 0) {
      profileForm.value.avatar = res.data.data;
      ElMessage.success('头像上传成功');
    } else {
      ElMessage.error(res.data.msg || '上传失败');
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.msg || '上传失败');
  } finally {
    profileAvatarUploading.value = false;
    input.value = '';
  }
};

const triggerProfileAvatarUpload = () => {
  const input = document.getElementById('profile-avatar-upload') as HTMLInputElement;
  if (input && !profileAvatarUploading.value) {
    input.click();
  }
};

const saveProfile = async () => {
  if (!user.value) return;
  
  if (!profileForm.value.nickname.trim()) {
    ElMessage.warning('请输入昵称');
    return;
  }
  
  try {
    if (profileForm.value.avatar && profileForm.value.avatar !== user.value.avatar) {
      await api.updateAvatar(user.value.id, profileForm.value.avatar);
    }
    
    await api.updateUserInfo(
      user.value.id,
      profileForm.value.nickname,
      user.value.role
    );
    
    user.value.nickname = profileForm.value.nickname;
    user.value.phone = profileForm.value.phone;
    if (profileForm.value.avatar) {
      user.value.avatar = profileForm.value.avatar;
    }
    localStorage.setItem('user', JSON.stringify(user.value));
    
    ElMessage.success('个人信息更新成功');
    profileDialogVisible.value = false;
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.msg || '更新失败');
  }
};

const triggerAvatarUpload = () => {
  const input = document.getElementById('avatar-upload') as HTMLInputElement;
  if (input && !avatarUploading.value) {
    input.click();
  }
};

const editDialogVisible = ref(false);
const editForm = ref<any>({
  id: '',
  title: '',
  description: '',
  imageUrl: '',
  price: 0,
  freight: 0
});
const uploading = ref(false);

const addressDialogVisible = ref(false);
const isEditAddress = ref(false);
const addressForm = ref({
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
  { value: '天津市', label: '天津市' },
  { value: '河北省', label: '河北省' },
  { value: '山西省', label: '山西省' },
  { value: '内蒙古自治区', label: '内蒙古自治区' },
  { value: '辽宁省', label: '辽宁省' },
  { value: '吉林省', label: '吉林省' },
  { value: '黑龙江省', label: '黑龙江省' },
  { value: '上海市', label: '上海市' },
  { value: '江苏省', label: '江苏省' },
  { value: '浙江省', label: '浙江省' },
  { value: '安徽省', label: '安徽省' },
  { value: '福建省', label: '福建省' },
  { value: '江西省', label: '江西省' },
  { value: '山东省', label: '山东省' },
  { value: '河南省', label: '河南省' },
  { value: '湖北省', label: '湖北省' },
  { value: '湖南省', label: '湖南省' },
  { value: '广东省', label: '广东省' },
  { value: '广西壮族自治区', label: '广西壮族自治区' },
  { value: '海南省', label: '海南省' },
  { value: '重庆市', label: '重庆市' },
  { value: '四川省', label: '四川省' },
  { value: '贵州省', label: '贵州省' },
  { value: '云南省', label: '云南省' },
  { value: '西藏自治区', label: '西藏自治区' },
  { value: '陕西省', label: '陕西省' },
  { value: '甘肃省', label: '甘肃省' },
  { value: '青海省', label: '青海省' },
  { value: '宁夏回族自治区', label: '宁夏回族自治区' },
  { value: '新疆维吾尔自治区', label: '新疆维吾尔自治区' },
  { value: '台湾省', label: '台湾省' },
  { value: '香港特别行政区', label: '香港特别行政区' },
  { value: '澳门特别行政区', label: '澳门特别行政区' }
];

// 省份对应城市
const cities: Record<string, { value: string; label: string }[]> = {
  '北京市': [{ value: '东城区', label: '东城区' }, { value: '西城区', label: '西城区' }, { value: '朝阳区', label: '朝阳区' }, { value: '海淀区', label: '海淀区' }],
  '天津市': [{ value: '和平区', label: '和平区' }, { value: '南开区', label: '南开区' }, { value: '滨海新区', label: '滨海新区' }],
  '河北省': [{ value: '石家庄市', label: '石家庄市' }, { value: '唐山市', label: '唐山市' }, { value: '保定市', label: '保定市' }, { value: '邯郸市', label: '邯郸市' }],
  '山西省': [{ value: '太原市', label: '太原市' }, { value: '大同市', label: '大同市' }, { value: '晋中市', label: '晋中市' }],
  '内蒙古自治区': [{ value: '呼和浩特市', label: '呼和浩特市' }, { value: '包头市', label: '包头市' }, { value: '鄂尔多斯市', label: '鄂尔多斯市' }],
  '辽宁省': [{ value: '沈阳市', label: '沈阳市' }, { value: '大连市', label: '大连市' }, { value: '鞍山市', label: '鞍山市' }],
  '吉林省': [{ value: '长春市', label: '长春市' }, { value: '吉林市', label: '吉林市' }, { value: '四平市', label: '四平市' }],
  '黑龙江省': [{ value: '哈尔滨市', label: '哈尔滨市' }, { value: '大庆市', label: '大庆市' }, { value: '齐齐哈尔市', label: '齐齐哈尔市' }],
  '上海市': [{ value: '黄浦区', label: '黄浦区' }, { value: '徐汇区', label: '徐汇区' }, { value: '浦东新区', label: '浦东新区' }, { value: '静安区', label: '静安区' }],
  '江苏省': [{ value: '南京市', label: '南京市' }, { value: '苏州市', label: '苏州市' }, { value: '无锡市', label: '无锡市' }, { value: '常州市', label: '常州市' }],
  '浙江省': [{ value: '杭州市', label: '杭州市' }, { value: '宁波市', label: '宁波市' }, { value: '温州市', label: '温州市' }, { value: '嘉兴市', label: '嘉兴市' }],
  '安徽省': [{ value: '合肥市', label: '合肥市' }, { value: '芜湖市', label: '芜湖市' }, { value: '马鞍山市', label: '马鞍山市' }],
  '福建省': [{ value: '福州市', label: '福州市' }, { value: '厦门市', label: '厦门市' }, { value: '泉州市', label: '泉州市' }],
  '江西省': [{ value: '南昌市', label: '南昌市' }, { value: '九江市', label: '九江市' }, { value: '赣州市', label: '赣州市' }],
  '山东省': [{ value: '济南市', label: '济南市' }, { value: '青岛市', label: '青岛市' }, { value: '烟台市', label: '烟台市' }, { value: '潍坊市', label: '潍坊市' }],
  '河南省': [{ value: '郑州市', label: '郑州市' }, { value: '洛阳市', label: '洛阳市' }, { value: '开封市', label: '开封市' }],
  '湖北省': [{ value: '武汉市', label: '武汉市' }, { value: '宜昌市', label: '宜昌市' }, { value: '襄阳市', label: '襄阳市' }, { value: '荆州市', label: '荆州市' }],
  '湖南省': [{ value: '长沙市', label: '长沙市' }, { value: '株洲市', label: '株洲市' }, { value: '湘潭市', label: '湘潭市' }, { value: '衡阳市', label: '衡阳市' }],
  '广东省': [
    { value: '广州市', label: '广州市' },
    { value: '深圳市', label: '深圳市' },
    { value: '珠海市', label: '珠海市' },
    { value: '汕头市', label: '汕头市' },
    { value: '佛山市', label: '佛山市' },
    { value: '韶关市', label: '韶关市' },
    { value: '湛江市', label: '湛江市' },
    { value: '肇庆市', label: '肇庆市' },
    { value: '江门市', label: '江门市' },
    { value: '茂名市', label: '茂名市' },
    { value: '惠州市', label: '惠州市' },
    { value: '梅州市', label: '梅州市' },
    { value: '汕尾市', label: '汕尾市' },
    { value: '河源市', label: '河源市' },
    { value: '阳江市', label: '阳江市' },
    { value: '清远市', label: '清远市' },
    { value: '东莞市', label: '东莞市' },
    { value: '中山市', label: '中山市' },
    { value: '潮州市', label: '潮州市' },
    { value: '揭阳市', label: '揭阳市' },
    { value: '云浮市', label: '云浮市' }
  ],
  '广西壮族自治区': [{ value: '南宁市', label: '南宁市' }, { value: '柳州市', label: '柳州市' }, { value: '桂林市', label: '桂林市' }],
  '海南省': [{ value: '海口市', label: '海口市' }, { value: '三亚市', label: '三亚市' }],
  '重庆市': [{ value: '渝中区', label: '渝中区' }, { value: '江北区', label: '江北区' }, { value: '南岸区', label: '南岸区' }],
  '四川省': [{ value: '成都市', label: '成都市' }, { value: '绵阳市', label: '绵阳市' }, { value: '德阳市', label: '德阳市' }, { value: '南充市', label: '南充市' }],
  '贵州省': [{ value: '贵阳市', label: '贵阳市' }, { value: '遵义市', label: '遵义市' }, { value: '六盘水市', label: '六盘水市' }],
  '云南省': [{ value: '昆明市', label: '昆明市' }, { value: '大理市', label: '大理市' }, { value: '丽江市', label: '丽江市' }],
  '西藏自治区': [{ value: '拉萨市', label: '拉萨市' }],
  '陕西省': [{ value: '西安市', label: '西安市' }, { value: '宝鸡市', label: '宝鸡市' }, { value: '咸阳市', label: '咸阳市' }],
  '甘肃省': [{ value: '兰州市', label: '兰州市' }, { value: '天水市', label: '天水市' }],
  '青海省': [{ value: '西宁市', label: '西宁市' }],
  '宁夏回族自治区': [{ value: '银川市', label: '银川市' }],
  '新疆维吾尔自治区': [{ value: '乌鲁木齐市', label: '乌鲁木齐市' }],
  '台湾省': [{ value: '台北市', label: '台北市' }, { value: '高雄市', label: '高雄市' }],
  '香港特别行政区': [{ value: '香港岛', label: '香港岛' }, { value: '九龙', label: '九龙' }],
  '澳门特别行政区': [{ value: '澳门半岛', label: '澳门半岛' }]
};

// 城市对应区县
const districts: Record<string, { value: string; label: string }[]> = {
  '东城区': [{ value: '东城区', label: '东城区' }],
  '西城区': [{ value: '西城区', label: '西城区' }],
  '朝阳区': [{ value: '朝阳区', label: '朝阳区' }],
  '海淀区': [{ value: '海淀区', label: '海淀区' }],
  '和平区': [{ value: '和平区', label: '和平区' }],
  '南开区': [{ value: '南开区', label: '南开区' }],
  '滨海新区': [{ value: '滨海新区', label: '滨海新区' }],
  '石家庄市': [{ value: '长安区', label: '长安区' }, { value: '桥西区', label: '桥西区' }],
  '太原市': [{ value: '小店区', label: '小店区' }, { value: '迎泽区', label: '迎泽区' }],
  '呼和浩特市': [{ value: '新城区', label: '新城区' }, { value: '赛罕区', label: '赛罕区' }],
  '沈阳市': [{ value: '和平区', label: '和平区' }, { value: '沈河区', label: '沈河区' }],
  '长春市': [{ value: '南关区', label: '南关区' }, { value: '朝阳区', label: '朝阳区' }],
  '哈尔滨市': [{ value: '道里区', label: '道里区' }, { value: '南岗区', label: '南岗区' }],
  '黄浦区': [{ value: '黄浦区', label: '黄浦区' }],
  '徐汇区': [{ value: '徐汇区', label: '徐汇区' }],
  '浦东新区': [{ value: '浦东新区', label: '浦东新区' }],
  '静安区': [{ value: '静安区', label: '静安区' }],
  '南京市': [{ value: '玄武区', label: '玄武区' }, { value: '秦淮区', label: '秦淮区' }, { value: '鼓楼区', label: '鼓楼区' }, { value: '建邺区', label: '建邺区' }],
  '苏州市': [{ value: '姑苏区', label: '姑苏区' }, { value: '吴中区', label: '吴中区' }],
  '杭州市': [{ value: '上城区', label: '上城区' }, { value: '西湖区', label: '西湖区' }, { value: '滨江区', label: '滨江区' }],
  '宁波市': [{ value: '海曙区', label: '海曙区' }, { value: '鄞州区', label: '鄞州区' }],
  '合肥市': [{ value: '蜀山区', label: '蜀山区' }, { value: '包河区', label: '包河区' }],
  '福州市': [{ value: '鼓楼区', label: '鼓楼区' }, { value: '仓山区', label: '仓山区' }],
  '南昌市': [{ value: '东湖区', label: '东湖区' }, { value: '西湖区', label: '西湖区' }],
  '济南市': [{ value: '历下区', label: '历下区' }, { value: '市中区', label: '市中区' }],
  '郑州市': [{ value: '中原区', label: '中原区' }, { value: '二七区', label: '二七区' }],
  '武汉市': [{ value: '江岸区', label: '江岸区' }, { value: '江汉区', label: '江汉区' }, { value: '硚口区', label: '硚口区' }, { value: '汉阳区', label: '汉阳区' }],
  '长沙市': [{ value: '芙蓉区', label: '芙蓉区' }, { value: '天心区', label: '天心区' }],
  // 广州市
  '广州市': [
    { value: '荔湾区', label: '荔湾区' },
    { value: '越秀区', label: '越秀区' },
    { value: '海珠区', label: '海珠区' },
    { value: '天河区', label: '天河区' },
    { value: '白云区', label: '白云区' },
    { value: '黄埔区', label: '黄埔区' },
    { value: '番禺区', label: '番禺区' },
    { value: '花都区', label: '花都区' },
    { value: '南沙区', label: '南沙区' },
    { value: '从化区', label: '从化区' },
    { value: '增城区', label: '增城区' }
  ],
// 深圳市
  '深圳市': [
    { value: '罗湖区', label: '罗湖区' },
    { value: '福田区', label: '福田区' },
    { value: '南山区', label: '南山区' },
    { value: '宝安区', label: '宝安区' },
    { value: '龙岗区', label: '龙岗区' },
    { value: '盐田区', label: '盐田区' },
    { value: '龙华区', label: '龙华区' },
    { value: '坪山区', label: '坪山区' },
    { value: '光明区', label: '光明区' },
    { value: '大鹏新区', label: '大鹏新区' }
  ],
// 珠海市
  '珠海市': [
    { value: '香洲区', label: '香洲区' },
    { value: '斗门区', label: '斗门区' },
    { value: '金湾区', label: '金湾区' }
  ],
// 汕头市
  '汕头市': [
    { value: '金平区', label: '金平区' },
    { value: '龙湖区', label: '龙湖区' },
    { value: '濠江区', label: '濠江区' },
    { value: '潮阳区', label: '潮阳区' },
    { value: '潮南区', label: '潮南区' },
    { value: '澄海区', label: '澄海区' },
    { value: '南澳县', label: '南澳县' }
  ],
// 佛山市
  '佛山市': [
    { value: '禅城区', label: '禅城区' },
    { value: '南海区', label: '南海区' },
    { value: '顺德区', label: '顺德区' },
    { value: '三水区', label: '三水区' },
    { value: '高明区', label: '高明区' }
  ],
// 韶关市
  '韶关市': [
    { value: '武江区', label: '武江区' },
    { value: '浈江区', label: '浈江区' },
    { value: '曲江区', label: '曲江区' },
    { value: '始兴县', label: '始兴县' },
    { value: '仁化县', label: '仁化县' },
    { value: '翁源县', label: '翁源县' },
    { value: '乳源瑶族自治县', label: '乳源瑶族自治县' },
    { value: '新丰县', label: '新丰县' },
    { value: '乐昌市', label: '乐昌市' },
    { value: '南雄市', label: '南雄市' }
  ],
// 湛江市
  '湛江市': [
    { value: '赤坎区', label: '赤坎区' },
    { value: '霞山区', label: '霞山区' },
    { value: '坡头区', label: '坡头区' },
    { value: '麻章区', label: '麻章区' },
    { value: '遂溪县', label: '遂溪县' },
    { value: '徐闻县', label: '徐闻县' },
    { value: '廉江市', label: '廉江市' },
    { value: '雷州市', label: '雷州市' },
    { value: '吴川市', label: '吴川市' }
  ],
// 肇庆市
  '肇庆市': [
    { value: '端州区', label: '端州区' },
    { value: '鼎湖区', label: '鼎湖区' },
    { value: '高要区', label: '高要区' },
    { value: '广宁县', label: '广宁县' },
    { value: '怀集县', label: '怀集县' },
    { value: '封开县', label: '封开县' },
    { value: '德庆县', label: '德庆县' },
    { value: '四会市', label: '四会市' }
  ],
// 江门市
  '江门市': [
    { value: '蓬江区', label: '蓬江区' },
    { value: '江海区', label: '江海区' },
    { value: '新会区', label: '新会区' },
    { value: '台山市', label: '台山市' },
    { value: '开平市', label: '开平市' },
    { value: '鹤山市', label: '鹤山市' },
    { value: '恩平市', label: '恩平市' }
  ],
// 茂名市
  '茂名市': [
    { value: '茂南区', label: '茂南区' },
    { value: '电白区', label: '电白区' },
    { value: '高州市', label: '高州市' },
    { value: '化州市', label: '化州市' },
    { value: '信宜市', label: '信宜市' }
  ],
// 惠州市
  '惠州市': [
    { value: '惠城区', label: '惠城区' },
    { value: '惠阳区', label: '惠阳区' },
    { value: '博罗县', label: '博罗县' },
    { value: '龙门县', label: '龙门县' },
    { value: '惠东县', label: '惠东县' }
  ],
// 梅州市
  '梅州市': [
    { value: '梅江区', label: '梅江区' },
    { value: '梅县区', label: '梅县区' },
    { value: '大埔县', label: '大埔县' },
    { value: '丰顺县', label: '丰顺县' },
    { value: '五华县', label: '五华县' },
    { value: '平远县', label: '平远县' },
    { value: '蕉岭县', label: '蕉岭县' },
    { value: '兴宁市', label: '兴宁市' }
  ],
// 汕尾市
  '汕尾市': [
    { value: '城区', label: '城区' },
    { value: '海丰县', label: '海丰县' },
    { value: '陆河县', label: '陆河县' },
    { value: '陆丰市', label: '陆丰市' }
  ],
// 河源市
  '河源市': [
    { value: '源城区', label: '源城区' },
    { value: '东源县', label: '东源县' },
    { value: '和平县', label: '和平县' },
    { value: '龙川县', label: '龙川县' },
    { value: '紫金县', label: '紫金县' },
    { value: '连平县', label: '连平县' }
  ],
// 阳江市
  '阳江市': [
    { value: '江城区', label: '江城区' },
    { value: '阳东区', label: '阳东区' },
    { value: '阳西县', label: '阳西县' },
    { value: '阳春市', label: '阳春市' }
  ],
// 清远市
  '清远市': [
    { value: '清城区', label: '清城区' },
    { value: '清新区', label: '清新区' },
    { value: '佛冈县', label: '佛冈县' },
    { value: '阳山县', label: '阳山县' },
    { value: '连山壮族瑶族自治县', label: '连山壮族瑶族自治县' },
    { value: '连南瑶族自治县', label: '连南瑶族自治县' },
    { value: '连州市', label: '连州市' },
    { value: '英德市', label: '英德市' }
  ],
// 东莞市（无下辖区县，直接填本级）
  '东莞市': [
    { value: '东莞市', label: '东莞市' }
  ],
// 中山市（无下辖区县，直接填本级）
  '中山市': [
    { value: '中山市', label: '中山市' }
  ],
// 潮州市
  '潮州市': [
    { value: '湘桥区', label: '湘桥区' },
    { value: '潮安区', label: '潮安区' },
    { value: '饶平县', label: '饶平县' }
  ],
// 揭阳市
  '揭阳市': [
    { value: '榕城区', label: '榕城区' },
    { value: '揭东区', label: '揭东区' },
    { value: '揭西县', label: '揭西县' },
    { value: '惠来县', label: '惠来县' },
    { value: '普宁市', label: '普宁市' }
  ],
// 云浮市
  '云浮市': [
    { value: '云城区', label: '云城区' },
    { value: '云安区', label: '云安区' },
    { value: '新兴县', label: '新兴县' },
    { value: '郁南县', label: '郁南县' },
    { value: '罗定市', label: '罗定市' }
  ],
  '南宁市': [{ value: '青秀区', label: '青秀区' }, { value: '兴宁区', label: '兴宁区' }],
  '海口市': [{ value: '美兰区', label: '美兰区' }, { value: '龙华区', label: '龙华区' }],
  '渝中区': [{ value: '渝中区', label: '渝中区' }],
  '江北区': [{ value: '江北区', label: '江北区' }],
  '成都市': [{ value: '锦江区', label: '锦江区' }, { value: '青羊区', label: '青羊区' }, { value: '金牛区', label: '金牛区' }, { value: '武侯区', label: '武侯区' }],
  '贵阳市': [{ value: '云岩区', label: '云岩区' }, { value: '南明区', label: '南明区' }],
  '昆明市': [{ value: '五华区', label: '五华区' }, { value: '盘龙区', label: '盘龙区' }],
  '拉萨市': [{ value: '城关区', label: '城关区' }],
  '西安市': [{ value: '新城区', label: '新城区' }, { value: '碑林区', label: '碑林区' }],
  '兰州市': [{ value: '城关区', label: '城关区' }],
  '西宁市': [{ value: '城东区', label: '城东区' }],
  '银川市': [{ value: '兴庆区', label: '兴庆区' }],
  '乌鲁木齐市': [{ value: '天山区', label: '天山区' }],
  '台北市': [{ value: '中正区', label: '中正区' }],
  '香港岛': [{ value: '中环', label: '中环' }],
  '澳门半岛': [{ value: '花地玛堂区', label: '花地玛堂区' }]
};

const currentCities = ref<{ value: string; label: string }[]>([]);
const currentDistricts = ref<{ value: string; label: string }[]>([]);

const handleProvinceChange = () => {
  currentCities.value = cities[addressForm.value.province] || [];
  addressForm.value.city = '';
  addressForm.value.district = '';
  currentDistricts.value = [];
};

const handleCityChange = () => {
  currentDistricts.value = districts[addressForm.value.city] || [];
  addressForm.value.district = '';
};

const loadAddresses = async () => {
  if (!user.value) return;
  try {
    const res = await api.getAddresses(user.value.id);
    if (res.data.code === 0) {
      addresses.value = res.data.data || [];
    }
  } catch (err) {
    ElMessage.error('加载地址失败');
  }
};

const openAddAddressDialog = () => {
  isEditAddress.value = false;
  addressForm.value = {
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
  addressDialogVisible.value = true;
};

const openEditAddressDialog = (address: any) => {
  isEditAddress.value = true;
  addressForm.value = {
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
  addressDialogVisible.value = true;
};

const saveAddress = async () => {
  if (!addressForm.value.name || !addressForm.value.phone || !addressForm.value.province || !addressForm.value.city || !addressForm.value.detail) {
    ElMessage.warning('请填写完整信息');
    return;
  }
  
  if (!user.value) {
    ElMessage.warning('请先登录');
    return;
  }
  
  try {
    if (isEditAddress.value) {
      addressForm.value.userId = user.value.id;
      await api.updateAddress(addressForm.value);
      ElMessage.success('地址更新成功');
    } else {
      addressForm.value.userId = user.value.id;
      await api.addAddress(addressForm.value);
      ElMessage.success('地址添加成功');
    }
    addressDialogVisible.value = false;
    await loadAddresses();
  } catch (err) {
    ElMessage.error(isEditAddress.value ? '更新失败' : '添加失败');
  }
};

const deleteAddress = async (id: number) => {
  if (!user.value) return;
  try {
    await ElMessageBox.confirm('确定要删除这个地址吗？', '删除地址', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    });
    await api.deleteAddress(id, user.value.id);
    ElMessage.success('地址已删除');
    await loadAddresses();
  } catch (err) {
  }
};

const closeAddressDialog = () => {
  addressDialogVisible.value = false;
};

const loadProfile = () => {
  const userStr = localStorage.getItem('user');
  if (!userStr) {
    ElMessage.warning('请先登录');
    router.push('/login');
    return;
  }
  user.value = JSON.parse(userStr);
  loadMyGoods();
  loadAddresses();
};

const loadMyGoods = async () => {
  if (!user.value?.id) return;
  try {
    const res = await api.listMyGoods(user.value.id);
    myGoods.value = res.data.data || [];
  } catch (e) {
    ElMessage.error('加载我的商品失败');
  }
};

const offGoods = async (id: number) => {
  try {
    await api.updateGoodsStatus(id, 2);
    ElMessage.success('商品已下架');
    loadMyGoods();
  } catch (err) {
    ElMessage.error('下架失败');
  }
};

const deleteGoods = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定要删除这个商品吗？', '删除商品', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    });
    await api.deleteGoods(id);
    ElMessage.success('商品已删除');
    loadMyGoods();
  } catch (err) {
  }
};

const editGoods = async (goods: any) => {
  try {
    const res = await api.getGoodsDetail(goods.id);
    if (res.data.code === 0) {
      editForm.value = res.data.data;
      editDialogVisible.value = true;
    }
  } catch (err) {
    ElMessage.error('获取商品详情失败');
  }
};

const saveEdit = async () => {
  try {
    const payload = {
      ...editForm.value,
      status: 0,
      sellerId: user.value.id
    };
    await api.publishGoods(payload);
    ElMessage.success('商品已更新，等待审核');
    editDialogVisible.value = false;
    loadMyGoods();
  } catch (err) {
    ElMessage.error('更新失败');
  }
};

const handleAvatarUpload = async (event: Event) => {
  const input = event.target as HTMLInputElement;
  const files = input.files;
  if (!files || files.length === 0) return;
  
  const file = files[0];
  const isImage = file.type.startsWith('image/');
  const isLt2M = file.size / 1024 / 1024 < 2;
  
  if (!isImage) {
    ElMessage.error('只能上传图片文件');
    return;
  }
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过 2MB');
    return;
  }
  
  avatarUploading.value = true;
  try {
    const res = await api.uploadImage(file);
    if (res.data.code === 0) {
      const avatarUrl = res.data.data;
      await api.updateAvatar(user.value.id, avatarUrl);
      user.value.avatar = avatarUrl;
      localStorage.setItem('user', JSON.stringify(user.value));
      ElMessage.success('头像上传成功');
    } else {
      ElMessage.error(res.data.msg || '上传失败');
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.msg || '上传失败');
  } finally {
    avatarUploading.value = false;
    input.value = '';
  }
};

const handleUploadSuccess = (url: string) => {
  editForm.value.imageUrl = url;
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

const logout = () => {
  localStorage.removeItem('user');
  ElMessage.success('退出成功');
  router.push('/login');
};

onMounted(loadProfile);
</script>

<template>
  <div class="profile-container">
    <div class="profile-header">
      <div class="profile-card">
        <div class="user-info-section">
          <div class="avatar-wrapper">
            <img 
              :src="user?.avatar || 'https://via.placeholder.com/150'" 
              :alt="user?.nickname || '头像'" 
              class="avatar"
            />
            <div class="avatar-overlay" @click="triggerAvatarUpload">
              <input 
                type="file" 
                id="avatar-upload" 
                accept="image/*" 
                @change="handleAvatarUpload" 
                :disabled="avatarUploading"
                style="display: none"
              />
              <span class="upload-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M12 10v6m0 0l-3-3m3 3l3-3"/>
                  <circle cx="12" cy="12" r="10"/>
                </svg>
              </span>
            </div>
            <el-button 
              v-if="!avatarUploading"
              class="upload-btn"
              :loading="avatarUploading" 
              type="primary" 
              size="small"
              @click="triggerAvatarUpload"
            >
              {{ avatarUploading ? '上传中...' : '更换头像' }}
            </el-button>
          </div>
          
          <div class="user-details">
            <h2 class="user-name">{{ user?.nickname || user?.username || '未设置' }}</h2>
            <p class="user-role" :class="user?.role === 2 ? 'seller' : 'buyer'">
              {{ user?.role === 2 ? '卖家' : '买家' }}
            </p>
            <div class="user-meta">
              <div class="meta-item">
                <span class="meta-label">用户名</span>
                <span class="meta-value">{{ user?.username || '未设置' }}</span>
              </div>
              <div class="meta-item">
                <span class="meta-label">手机号</span>
                <span class="meta-value">{{ user?.phone || '未设置' }}</span>
              </div>
            </div>
            <div class="user-actions">
              <el-button type="primary" @click="openProfileDialog">编辑个人信息</el-button>
              <el-button type="danger" @click="logout">退出登录</el-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="profile-content">
      <el-card v-if="user && user.role === 2" class="content-card">
        <div class="card-header">
          <h3 class="card-title">我发布的商品</h3>
          <span class="card-count">{{ myGoods.length }} 件</span>
        </div>
        <el-table 
          :data="myGoods" 
          border 
          class="goods-table"
          :empty-text="myGoods.length === 0 ? '暂无商品' : ''"
        >
          <el-table-column label="商品图片" width="120">
            <template #default="scope">
              <div class="goods-image-wrapper">
                <img 
                  v-if="scope.row.imageUrl" 
                  :src="scope.row.imageUrl" 
                  alt="商品图片" 
                  class="goods-image"
                />
                <span v-else class="no-image">无图片</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="商品标题" prop="title" min-width="150" />
          <el-table-column label="价格" prop="price" width="100">
            <template #default="scope">
              <span class="price">¥{{ scope.row.price }}</span>
            </template>
          </el-table-column>
          <el-table-column label="库存" prop="stock" width="80" />
          <el-table-column label="是否包邮" width="100">
            <template #default="scope">
              <el-tag :type="(!scope.row.freight || scope.row.freight === 0) ? 'success' : 'info'" size="small">
                {{ (!scope.row.freight || scope.row.freight === 0) ? '包邮' : '不包邮' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="scope">
              <el-tag 
                :type="scope.row.status === 0 ? 'warning' : scope.row.status === 1 ? 'success' : 'danger'"
                size="small"
              >
                {{ scope.row.status === 0 ? '审核中' : scope.row.status === 1 ? '已上架' : '已下架' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200">
            <template #default="scope">
              <div class="table-actions">
                <el-button 
                  v-if="scope.row.status === 1" 
                  size="small" 
                  type="warning" 
                  @click="offGoods(scope.row.id)"
                >下架</el-button>
                <el-button size="small" type="primary" @click="editGoods(scope.row)">编辑</el-button>
                <el-button size="small" type="danger" @click="deleteGoods(scope.row.id)">删除</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <el-card class="content-card">
        <div class="card-header">
          <h3 class="card-title">我的地址</h3>
          <el-button type="primary" size="small" @click="openAddAddressDialog" class="add-btn">
            + 新增地址
          </el-button>
        </div>
        <div v-if="addresses.length === 0" class="empty-state">
          <div class="empty-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/>
              <circle cx="12" cy="10" r="3"/>
            </svg>
          </div>
          <p>暂无收货地址</p>
          <el-button type="primary" size="small" @click="openAddAddressDialog">添加地址</el-button>
        </div>
        <div v-else class="address-grid">
          <div
            v-for="address in addresses"
            :key="address.id"
            class="address-card"
            :class="{ 'default-address': address.isDefault }"
          >
            <div class="address-header">
              <div class="address-user">
                <span class="name">{{ address.name }}</span>
                <span class="phone">{{ address.phone }}</span>
              </div>
              <el-tag v-if="address.isDefault" type="primary" size="small">默认</el-tag>
            </div>
            <div class="address-detail">
              {{ address.province }} {{ address.city }} {{ address.district }} {{ address.detail }}
            </div>
            <div class="address-actions">
              <el-button size="mini" type="primary" @click="openEditAddressDialog(address)">编辑</el-button>
              <el-button size="mini" type="danger" @click="deleteAddress(address.id)">删除</el-button>
            </div>
          </div>
        </div>
      </el-card>
    </div>

    <el-dialog
        v-model="editDialogVisible"
        title="编辑商品"
        width="600px"
        class="custom-dialog"
    >
      <el-form label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="editForm.title" placeholder="请输入商品标题" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="editForm.description" type="textarea" :rows="4" placeholder="请输入商品描述" />
        </el-form-item>
        <el-form-item label="商品图片">
          <el-upload
              :show-file-list="false"
              :before-upload="beforeUpload"
              :on-change="onFileChange"
          >
            <el-button :loading="uploading" type="primary">选择图片</el-button>
          </el-upload>
          <div v-if="editForm.imageUrl" class="image-preview">
            <span>预览：</span>
            <img
                :src="editForm.imageUrl"
                alt="预览图片"
                class="preview-image"
            />
          </div>
        </el-form-item>
        <el-form-item label="价格">
          <el-input-number v-model="editForm.price" :min="0" :step="1" />
        </el-form-item>
        <el-form-item label="运费">
          <el-input-number v-model="editForm.freight" :min="0" :step="1" />
          <span class="hint-text">不填默认包邮</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="editDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveEdit">保存</el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog v-model="profileDialogVisible" title="编辑个人信息" width="500px" class="custom-dialog">
      <el-form label-width="80px">
        <el-form-item label="头像">
          <div class="profile-avatar-edit">
            <div class="avatar-preview-wrapper">
              <img 
                v-if="profileForm.avatar" 
                :src="profileForm.avatar" 
                alt="头像预览" 
                class="avatar-preview"
              />
              <div v-else class="avatar-preview-placeholder">
                {{ profileForm.nickname?.charAt(0) || '头' }}
              </div>
            </div>
            <input 
              type="file" 
              id="profile-avatar-upload" 
              accept="image/*" 
              @change="handleProfileAvatarUpload" 
              :disabled="profileAvatarUploading"
              style="display: none"
            />
            <el-button 
              :loading="profileAvatarUploading" 
              type="primary" 
              size="small"
              @click="triggerProfileAvatarUpload"
            >
              {{ profileAvatarUploading ? '上传中...' : '选择头像' }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="昵称" required>
          <el-input v-model="profileForm.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="profileForm.phone" placeholder="请输入手机号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="profileDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveProfile">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog 
      :title="isEditAddress ? '编辑地址' : '新增地址'" 
      v-model="addressDialogVisible" 
      width="500px" 
      class="custom-dialog"
      @close="closeAddressDialog"
    >
      <el-form :model="addressForm" label-width="80px">
        <el-form-item label="姓名">
          <el-input v-model="addressForm.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="addressForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="省份">
          <el-select v-model="addressForm.province" placeholder="请选择省份" @change="handleProvinceChange">
            <el-option v-for="p in provinces" :key="p.value" :label="p.label" :value="p.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="城市">
          <el-select v-model="addressForm.city" placeholder="请选择城市" @change="handleCityChange">
            <el-option v-for="c in currentCities" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="区县">
          <el-select v-model="addressForm.district" placeholder="请选择区县">
            <el-option v-for="d in currentDistricts" :key="d.value" :label="d.label" :value="d.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="详细地址">
          <el-input v-model="addressForm.detail" type="textarea" :rows="3" placeholder="请输入详细地址" />
        </el-form-item>
        <el-form-item>
          <label class="checkbox-label">
            <input type="checkbox" v-model="addressForm.isDefault" /> 设为默认地址
          </label>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeAddressDialog">取消</el-button>
        <el-button type="primary" @click="saveAddress">{{ isEditAddress ? '保存修改' : '添加地址' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.profile-container {
  min-height: 100vh;
  background: #f5f5f5;
  padding: 40px 20px;
}

.profile-card {
  background: white;
  border-radius: 20px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
  overflow: hidden;
  max-width: 900px;
  margin: 0 auto;
}

.user-info-section {
  display: flex;
  align-items: flex-start;
  padding: 40px;
  gap: 40px;
}

.avatar-wrapper {
  position: relative;
  flex-shrink: 0;
}

.avatar {
  width: 150px;
  height: 150px;
  border-radius: 50%;
  object-fit: cover;
  border: 4px solid #f0f0f0;
  transition: transform 0.3s ease, border-color 0.3s ease;
}

.avatar:hover {
  transform: scale(1.05);
  border-color: #409eff;
}

.avatar-overlay {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s ease;
  cursor: pointer;
}

.avatar-wrapper:hover .avatar-overlay {
  opacity: 1;
}

.upload-icon {
  color: white;
  font-size: 24px;
}

.upload-btn {
  position: absolute;
  bottom: -35px;
  left: 50%;
  transform: translateX(-50%);
  white-space: nowrap;
}

.user-details {
  flex: 1;
  padding-top: 20px;
}

.user-name {
  font-size: 28px;
  font-weight: 700;
  color: #1f2937;
  margin: 0 0 8px 0;
}

.user-role {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 20px;
}

.user-role.seller {
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
  color: white;
}

.user-role.buyer {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: white;
}

.user-meta {
  display: flex;
  gap: 30px;
  margin-bottom: 25px;
}

.meta-item {
  display: flex;
  flex-direction: column;
}

.meta-label {
  font-size: 13px;
  color: #9ca3af;
  margin-bottom: 4px;
}

.meta-value {
  font-size: 16px;
  color: #374151;
  font-weight: 500;
}

.user-actions {
  display: flex;
  gap: 12px;
}

.user-actions .el-button {
  padding: 10px 24px;
  border-radius: 8px;
  font-weight: 500;
}

.profile-content {
  max-width: 900px;
  margin: 30px auto 0;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.content-card {
  background: white;
  border-radius: 16px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.1);
  border: none;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px 16px;
  border-bottom: 1px solid #f3f4f6;
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}

.card-count {
  font-size: 14px;
  color: #6b7280;
  background: #f3f4f6;
  padding: 4px 12px;
  border-radius: 20px;
}

.add-btn {
  border-radius: 8px;
  padding: 6px 16px;
}

.goods-table {
  --el-table-header-text-color: #6b7280;
  --el-table-row-hover-bg-color: #f9fafb;
}

.goods-table :deep(.el-table__header) {
  background: #f9fafb;
}

.goods-table :deep(.el-table__header th) {
  border-bottom: 2px solid #f3f4f6;
  font-weight: 600;
}

.goods-image-wrapper {
  width: 100px;
  height: 100px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  overflow: hidden;
  background: #f9fafb;
}

.goods-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.no-image {
  color: #9ca3af;
  font-size: 12px;
}

.price {
  color: #ef4444;
  font-weight: 600;
  font-size: 15px;
}

.table-actions {
  display: flex;
  gap: 8px;
}

.empty-state {
  text-align: center;
  padding: 48px 20px;
  color: #9ca3af;
}

.empty-icon {
  width: 64px;
  height: 64px;
  margin: 0 auto 16px;
  color: #d1d5db;
}

.address-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
  padding: 16px 24px 24px;
}

.address-card {
  background: #fafafa;
  border-radius: 12px;
  padding: 20px;
  border: 2px solid transparent;
  transition: all 0.3s ease;
}

.address-card:hover {
  background: #f3f4f6;
  border-color: #e5e7eb;
}

.address-card.default-address {
  border-color: #409eff;
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
}

.address-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.address-user {
  display: flex;
  align-items: center;
  gap: 12px;
}

.address-user .name {
  font-weight: 600;
  color: #1f2937;
}

.address-user .phone {
  color: #6b7280;
  font-size: 14px;
}

.address-detail {
  color: #4b5563;
  font-size: 14px;
  line-height: 1.6;
  margin-bottom: 16px;
}

.address-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

.image-preview {
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.preview-image {
  width: 120px;
  height: 120px;
  object-fit: cover;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
}

.hint-text {
  margin-left: 12px;
  color: #9ca3af;
  font-size: 13px;
}

.checkbox-label {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #374151;
}

.profile-avatar-edit {
  display: flex;
  align-items: center;
  gap: 15px;
}

.avatar-preview-wrapper {
  flex-shrink: 0;
}

.avatar-preview {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #e5e7eb;
}

.avatar-preview-placeholder {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  font-weight: bold;
}

.custom-dialog :deep(.el-dialog__header) {
  background: #409eff;
  padding: 20px 24px;
  border-radius: 12px 12px 0 0;
}

.custom-dialog :deep(.el-dialog__title) {
  color: white;
  font-weight: 600;
  font-size: 16px;
}

.custom-dialog :deep(.el-dialog__close) {
  color: white;
}

.custom-dialog :deep(.el-dialog__body) {
  padding: 24px;
}

@media (max-width: 768px) {
  .profile-container {
    padding: 20px 15px;
  }
  
  .user-info-section {
    flex-direction: column;
    align-items: center;
    text-align: center;
    padding: 30px 20px;
    gap: 30px;
  }
  
  .upload-btn {
    position: static;
    transform: none;
    margin-top: 15px;
  }
  
  .user-meta {
    justify-content: center;
  }
  
  .user-actions {
    justify-content: center;
  }
  
  .address-grid {
    grid-template-columns: 1fr;
  }
  
  .card-header {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }
  
  .goods-table :deep(.el-table) {
    font-size: 13px;
  }
}
</style>