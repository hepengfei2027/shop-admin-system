import axios from 'axios';

const instance = axios.create({
  baseURL: 'http://localhost:8081/api',
  timeout: 5000
});

export const api = {
  login(data: { username: string; password: string }) {
    return instance.post('/user/login', data);
  },
  register(data: any) {
    return instance.post('/user/register', data);
  },
  listGoods() {
    return instance.get('/goods/list');
  },
  searchGoods(keyword: string) {
    return instance.get('/goods/search', { params: { keyword } });
  },
  publishGoods(data: any) {
    return instance.post('/goods/publish', data);
  },
  uploadImage(file: File) {
    const formData = new FormData();
    formData.append('file', file);
    return instance.post('/upload/image', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
  },
  uploadVideo(file: File) {
    const formData = new FormData();
    formData.append('file', file);
    return instance.post('/upload/video', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
  },
  
  listMyGoods(userId: number) {
    return instance.get('/goods/my', { params: { userId } });
  },
  listMyOrders(userId: number) {
    return instance.get('/order/my', { params: { userId } });
  },
  listPendingGoods() {
    return instance.get('/goods/pending');
  },
  approveGoods(id: number) {
    return instance.post(`/goods/approve/${id}`);
  },
  rejectGoods(id: number) {
    return instance.post(`/goods/reject/${id}`);
  },
  deleteGoods(id: number) {
    return instance.delete(`/goods/delete/${id}`);
  },
  updateGoodsStatus(id: number, status: number) {
    return instance.post(`/goods/updateStatus/${id}`, null, { params: { status } });
  },
  getGoodsDetail(id: number) {
    return instance.get(`/goods/detail/${id}`);
  },
  listUsers() {
    return instance.get('/user/list');
  },
  updateUserStatus(id: number, status: number, banHours?: number) {
    return instance.post(`/user/updateStatus/${id}`, null, { params: { status, banHours } });
  },
  sendMessage(senderId: number, receiverId: number, content: string) {
    return instance.post('/message/send', null, { params: { senderId, receiverId, content } });
  },
  getMessages(userId: number) {
    return instance.get(`/message/list/${userId}`);
  },
  getAllMessages(userId: number) {
    return instance.get(`/message/all/${userId}`);
  },
  markAsRead(messageId: number) {
    return instance.post(`/message/read/${messageId}`);
  },
  countUnread(userId: number) {
    return instance.get(`/message/unread/${userId}`);
  },
  deleteMessage(messageId: number, senderId: number) {
    return instance.delete(`/message/delete/${messageId}`, { params: { senderId } });
  },
  getMessagesByConversationId(conversationId: number) {
    return instance.get(`/message/conversation/${conversationId}`);
  },
  deleteConversation(userId: number, conversationId: number) {
    return instance.delete(`/message/deleteConversation`, { params: { userId, conversationId } });
  },
  userDetail(userId: number) {
    return instance.get(`/user/${userId}`);
  },
  updateAvatar(userId: number, avatar: string) {
    return instance.post(`/user/${userId}/avatar`, null, { params: { avatar } });
  },
  updateUserInfo(id: number, nickname: string, role: number, phone?: string) {
    return instance.post(`/user/update/${id}`, null, { params: { nickname, role, phone } });
  },
  getAddresses(userId: number) {
    return instance.get('/address/list', { params: { userId } });
  },
  addAddress(data: any) {
    return instance.post('/address/add', data);
  },
  updateAddress(data: any) {
    return instance.post('/address/update', data);
  },
  deleteAddress(id: number, userId: number) {
    return instance.delete(`/address/delete/${id}`, { params: { userId } });
  },
  createOrder(goodsId: number, buyerId: number) {
    return instance.post('/order/create', null, { params: { goodsId, buyerId } });
  },
  getOrderInfo(orderId: string) {
    return instance.get(`/order/info/${orderId}`);
  },
  getAllOrders() {
    return instance.get('/order/all');
  },
  getOrders(status?: string) {
    return instance.get('/order/list', { params: { status } });
  },
  payOrder(orderId: string, method: string) {
    return instance.post('/order/pay', null, { params: { orderId, method } });
  },
  cancelOrder(orderId: string) {
    return instance.post(`/order/cancel/${orderId}`);
  },
  cancelOrderV2(orderId: number, userId: number) {
    return instance.post(`/order/${orderId}/cancel`, null, { params: { userId } });
  },
  payOrderV2(orderId: number, buyerId: number) {
    return instance.post(`/order/${orderId}/pay`, null, { params: { buyerId } });
  },
  shipOrder(orderId: number, sellerId: number) {
    return instance.post(`/order/${orderId}/ship`, null, { params: { sellerId } });
  },
  confirmReceive(orderId: number, buyerId: number) {
    return instance.post(`/order/${orderId}/confirm`, null, { params: { buyerId } });
  },
  listBuyerOrders(buyerId: number) {
    return instance.get(`/order/buyer/${buyerId}`);
  },
  listSellerOrders(sellerId: number) {
    return instance.get(`/order/seller/${sellerId}`);
  },
  createComment(data: any) {
    return instance.post('/comment/create', data);
  },
  getGoodsComments(goodsId: number) {
    return instance.get(`/comment/goods/${goodsId}`);
  },
  getCommentMedia(commentId: number) {
    return instance.get(`/comment/${commentId}/media`);
  },
  getCommentReplies(commentId: number) {
    return instance.get(`/comment/${commentId}/replies`);
  },
  replyComment(commentId: number, userId: number, content: string) {
    return instance.post(`/comment/${commentId}/reply`, null, { params: { userId, content } });
  },
  applyRefund(orderId: number, buyerId: number, remark?: string) {
    return instance.post(`/order/${orderId}/refund/apply`, null, { params: { buyerId, remark } });
  },
  approveRefund(orderId: number, sellerId: number) {
    return instance.post(`/order/${orderId}/refund/approve`, null, { params: { sellerId } });
  },
  rejectRefund(orderId: number, sellerId: number, remark?: string) {
    return instance.post(`/order/${orderId}/refund/reject`, null, { params: { sellerId, remark } });
  },
  confirmShipRefund(orderId: number, buyerId: number) {
    return instance.post(`/order/${orderId}/refund/ship`, null, { params: { buyerId } });
  },
  confirmReceiveRefund(orderId: number, sellerId: number) {
    return instance.post(`/order/${orderId}/refund/confirm`, null, { params: { sellerId } });
  },
  cancelRefund(orderId: number, buyerId: number) {
    return instance.post(`/order/${orderId}/refund/cancel`, null, { params: { buyerId } });
  },
  
  createDispute(orderId: number, buyerId: number, buyerContent?: string, buyerImages?: string) {
    return instance.post('/dispute/create', null, { params: { orderId, buyerId, buyerContent, buyerImages } });
  },
  
  getDisputeByOrderId(orderId: number) {
    return instance.get(`/dispute/order/${orderId}`);
  },
  
  getDisputeDetail(id: number) {
    return instance.get(`/dispute/detail/${id}`);
  },
  
  getAllDisputes() {
    return instance.get('/dispute/list');
  },
  
  sellerReplyDispute(id: number, sellerReply?: string, sellerImages?: string) {
    return instance.post('/dispute/seller/reply', null, { params: { id, sellerReply, sellerImages } });
  },
  
  adminDecision(id: number, adminDecision: string, adminRemark?: string, adminId: number, status: number) {
    return instance.post('/dispute/admin/decision', null, { params: { id, adminDecision, adminRemark, adminId, status } });
  },
  
  getStatisticsOverview() {
    return instance.get('/statistics/overview');
  },
  issueCoupon(goodsId: number, amount: number) {
    return instance.post('/coupon/issue', null, { params: { goodsId, amount } });
  },
  getAvailableCoupon(goodsId: number, userId: number) {
    return instance.get('/coupon/available', { params: { goodsId, userId } });
  },
  claimCoupon(couponId: number, userId: number) {
    return instance.post('/coupon/claim', null, { params: { couponId, userId } });
  },
  getUserCoupons(userId: number) {
    return instance.get('/coupon/user', { params: { userId } });
  },
  useCoupon(couponId: number) {
    return instance.post('/coupon/use', null, { params: { couponId } });
  }
};

