import request from '../utils/request'

// 注册
export function register(data) {
  return request.post('/user/register', data)
}

// 用户登录
export function userLogin(data) {
  return request.post('/user/login', data)
}

// 查询个人信息
export function getUserInfo(userId) {
  return request.get(`/user/info/${userId}`)
}

// 修改个人信息
export function updateUserInfo(userId, data) {
  return request.put(`/user/info/${userId}`, data)
}

// 修改密码
export function changePassword(userId, data) {
  return request.put(`/user/password/${userId}`, data)
}

// 账号注销
export function deleteUser(userId) {
  return request.delete(`/user/${userId}`)
}

// 乘车人列表
export function listPassengers(userId) {
  return request.get(`/passenger/list/${userId}`)
}

// 新增乘车人
export function addPassenger(userId, data) {
  return request.post(`/passenger/${userId}`, data)
}

// 删除乘车人
export function deletePassenger(userId, passengerId) {
  return request.delete(`/passenger/${userId}/${passengerId}`)
}

// 管理员登录
export function adminLogin(data) {
  return request.post('/admin/login', data)
}

// 管理员改密码
export function changeAdminPassword(userId, data) {
  return request.put(`/admin/password/${userId}`, data)
}

// 站点列表
export function listStations() {
  return request.get('/station/list')
}

// 车次列表
export function listTrains(params) {
  return request.get('/train/list', { params })
}

// 新增车次
export function addTrain(data) {
  return request.post('/train', data)
}

// 修改车次
export function updateTrain(data) {
  return request.put('/train', data)
}

// 查询单个车次
export function getTrain(trainId) {
  return request.get(`/train/${trainId}`)
}

// 删除车次
export function deleteTrain(trainId) {
  return request.delete(`/train/${trainId}`)
}

// 设置售票状态
export function setSaleStatus(trainId, saleStatus) {
  return request.put(`/train/sale-status/${trainId}`, null, { params: { saleStatus } })
}

// ==================== 第二轮：余票查询、购票、支付 ====================

// 余票查询（POST + 支持AbortSignal中断）
export function searchTickets(params, signal) {
  return request.post('/ticket/search', params, { signal })
}

// 查询城市列表
export function listCities() {
  return request.get('/ticket/cities')
}

// 查询某城市下的站点
export function listStationsByCity(city) {
  return request.get('/ticket/stations-by-city', { params: { city } })
}

// 改签查询：同始发/终点城市、今日之后、正在售票的车次
export function listTrainsForChange(params) {
  return request.get('/train/list-for-change', { params })
}

// 查询车次可用座位
export function listAvailableSeats(trainId) {
  return request.get(`/order/available-seats/${trainId}`)
}

// 购票下单
export function createOrder(data) {
  return request.post('/order', data)
}

// 支付订单
export function payOrder(orderId) {
  return request.put(`/order/pay/${orderId}`)
}

// 查询订单详情
export function getOrder(orderId) {
  return request.get(`/order/${orderId}`)
}

// 查询用户订单列表
export function listUserOrders(userId, orderStatus) {
  return request.get(`/order/list/${userId}`, { params: { orderStatus } })
}

// ==================== 第三轮：改签、退票、管理员查询 ====================

// 订单多条件筛选查询
export function queryOrders(data) {
  return request.post('/order/query', data)
}

// 改签
export function changeOrder(orderId, data) {
  return request.post(`/order/change/${orderId}`, data)
}

// 超时取消订单（前端倒计时到期后调用）
export function timeoutCancelOrder(orderId) {
  return request.put(`/order/timeout-cancel/${orderId}`)
}

// 取消订单（用户主动取消）
export function cancelOrder(orderId) {
  return request.put(`/order/cancel/${orderId}`)
}

// 退票
export function refundOrder(orderId) {
  return request.post(`/order/refund/${orderId}`)
}

// 管理员-查询全部订单
export function adminQueryAllOrders(data) {
  return request.post('/admin/query/orders', data)
}

// 管理员-查询全部车票
export function adminQueryAllTickets(data) {
  return request.post('/admin/query/tickets', data)
}

// 管理员-车票统计
export function getTicketStats() {
  return request.get('/admin/query/stats')
}

// 管理员-查询全部普通用户
export function listNormalUsers(params) {
  return request.get('/admin/query/users', { params })
}

// 管理员-锁定/解锁用户
export function setUserLockStatus(userId, isLock) {
  return request.put(`/admin/query/users/${userId}/lock`, null, { params: { isLock } })
}
