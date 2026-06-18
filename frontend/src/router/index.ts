import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router';
import Home from '../views/Home.vue';
import Login from '../views/Login.vue';
import GoodsDetail from '../views/GoodsDetail.vue';
import PublishGoods from '../views/PublishGoods.vue';
import Register from '../views/Register.vue';
import Profile from '../views/Profile.vue';
import Cart from '../views/Cart.vue';
import AdminLayout from '../admin/AdminLayout.vue';
import AdminHome from '../admin/AdminHome.vue';
import PendingGoods from '../admin/PendingGoods.vue';
import GoodsList from '../admin/GoodsList.vue';
import UserList from '../admin/UserList.vue';
import OrderList from '../admin/OrderList.vue';
import Statistics from '../admin/Statistics.vue';
import MessageList from '../views/Review/MessageList.vue';
import Messages from '../views/Review/Messages.vue';
import Orders from '../views/Orders.vue';
import OrderDetail from '../views/OrderDetail.vue';
import ConfirmOrder from '../views/ConfirmOrder.vue';
import PaymentSuccess from '../views/PaymentSuccess.vue';
import SellerAnalytics from '../views/SellerAnalytics.vue';
import ShopManagement from '../views/ShopManagement.vue';
const routes: RouteRecordRaw[] = [
  { path: '/', name: 'Home', component: Home },
  { path: '/login', name: 'Login', component: Login },
  { path: '/goods/:id', name: 'GoodsDetail', component: GoodsDetail },
  { path: '/publish', name: 'PublishGoods', component: PublishGoods },
  { path: '/register', name: 'Register', component: Register },
  {path: '/profile', name: 'Profile', component: Profile},
  {path: '/cart', name: 'Cart', component: Cart},
  {path: '/seller-analytics', name: 'SellerAnalytics', component: SellerAnalytics},
  {path: '/shop-management', name: 'ShopManagement', component: ShopManagement},
  { 
    path: '/admin', 
    component: AdminLayout,
    children: [
      { path: 'home', name: 'AdminHome', component: AdminHome },
      { path: 'pending', name: 'PendingGoods', component: PendingGoods },
      { path: 'goods', name: 'GoodsList', component: GoodsList },
      { path: 'users', name: 'UserList', component: UserList },
      { path: 'orders', name: 'OrderList', component: OrderList },
      { path: 'statistics', name: 'Statistics', component: Statistics },
      { path: '', redirect: '/admin/home' }
    ]
  },
  {path: '/messages', name: 'Messages', component: MessageList},
  {path: '/message-list', name: 'MessageList', component: MessageList},
  {path: '/chat', name: 'Chat', component: Messages},
  {path: '/orders', name: 'Orders', component: Orders},
  {path: '/order/:id', name: 'OrderDetail', component: OrderDetail},
  {path: '/confirm-order', name: 'ConfirmOrder', component: ConfirmOrder},
  {path: '/payment-success/:id', name: 'PaymentSuccess', component: PaymentSuccess}
];

const router = createRouter({
  // 👇 直接写死，这是唯一能让子路由也正常的写法
  history: createWebHistory('/Web3/'),

  // 👇 关键！必须加这个！解决子页面 404
  scrollBehavior() {
    return { top: 0 };
  },

  routes
});


export default router;

