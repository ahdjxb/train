<template>
  <div class="my-orders">
    <div class="page-header">
      <h2>我的订单</h2>
      <el-radio-group v-model="statusFilter" @change="loadOrders" style="margin-left: auto">
        <el-radio-button label="">全部</el-radio-button>
        <el-radio-button label="PENDING">待支付</el-radio-button>
        <el-radio-button label="PAID">已支付</el-radio-button>
        <el-radio-button label="CANCELLED">已取消</el-radio-button>
        <el-radio-button label="CHANGED">已改签</el-radio-button>
        <el-radio-button label="REFUNDED">已退票</el-radio-button>
      </el-radio-group>
    </div>

    <el-table :data="orders" border stripe style="margin-top: 16px" v-loading="loading">
      <el-table-column prop="orderNo" label="订单号" width="180" />
      <el-table-column prop="trainNo" label="车次" width="100" />
      <el-table-column prop="passengerName" label="乘车人" width="100" />
      <el-table-column prop="departTime" label="发车时间" width="160" />
      <el-table-column prop="startStationName" label="始发站" width="100" />
      <el-table-column prop="endStationName" label="终点站" width="100" />
      <el-table-column prop="carriageLevel" label="席位" width="80" />
      <el-table-column prop="seatNo" label="座位号" width="80" />
      <el-table-column prop="price" label="票价" width="80">
        <template #default="{ row }">
          <span style="color: #f56c6c">￥{{ row.price }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.orderStatus)" size="small">
            {{ statusText(row.orderStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="倒计时" width="120" v-if="hasPendingOrders">
        <template #default="{ row }">
          <span v-if="row.orderStatus === 'PENDING'" class="countdown">
            {{ formatCountdown(row.orderId) }}
          </span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <template v-if="row.orderStatus === 'PENDING'">
            <el-button type="primary" size="small" @click="handlePay(row)">支付</el-button>
            <el-button type="info" size="small" @click="handleCancelOrder(row)">取消</el-button>
          </template>
          <template v-else-if="row.orderStatus === 'PAID'">
            <el-button type="warning" size="small" @click="handleChange(row)">改签</el-button>
            <el-button type="danger" size="small" @click="handleRefund(row)">退票</el-button>
          </template>
          <span v-else>-</span>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && orders.length === 0" description="暂无订单" />

    <!-- 支付弹窗 -->
    <el-dialog v-model="showPayDialog" title="订单支付" width="400px">
      <div class="pay-dialog-body">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="订单号">{{ currentOrder.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="车次">{{ currentOrder.trainNo }}</el-descriptions-item>
          <el-descriptions-item label="乘车人">{{ currentOrder.passengerName }}</el-descriptions-item>
          <el-descriptions-item label="始发站">{{ currentOrder.startStationName }}</el-descriptions-item>
          <el-descriptions-item label="终点站">{{ currentOrder.endStationName }}</el-descriptions-item>
          <el-descriptions-item label="席位">{{ currentOrder.carriageLevel }}</el-descriptions-item>
          <el-descriptions-item label="座位号">{{ currentOrder.seatNo }}</el-descriptions-item>
          <el-descriptions-item label="票价">
            <span style="color:#f56c6c; font-size:18px; font-weight:bold">￥{{ currentOrder.price }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="剩余支付时间">
            <span style="color:#f56c6c">{{ formatCountdown(currentOrder.orderId) }}</span>
          </el-descriptions-item>
        </el-descriptions>
        <div class="pay-methods">
          <el-radio-group v-model="payMethod">
            <el-radio label="wechat">微信支付</el-radio>
            <el-radio label="alipay">支付宝</el-radio>
          </el-radio-group>
        </div>
      </div>
      <template #footer>
        <el-button @click="showPayDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmPay" :loading="payLoading">确认支付</el-button>
      </template>
    </el-dialog>

    <!-- 改签弹窗 -->
    <el-dialog v-model="showChangeDialog" title="改签车次" width="600px" @open="loadChangeTrains">
      <el-form :inline="true" style="margin-bottom: 12px">
        <el-form-item label="始发城市">
          <el-input v-model="changeQuery.startCity" placeholder="始发城市" clearable style="width: 120px" />
        </el-form-item>
        <el-form-item label="终点城市">
          <el-input v-model="changeQuery.endCity" placeholder="终点城市" clearable style="width: 120px" />
        </el-form-item>
        <el-form-item label="车型">
          <el-select v-model="changeQuery.trainType" placeholder="全部" clearable style="width: 100px">
            <el-option label="高铁" value="高铁" />
            <el-option label="普通" value="普通" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadChangeTrains">查询</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="changeTrains" border stripe max-height="200" size="small">
        <el-table-column prop="trainNo" label="车次" width="80" />
        <el-table-column prop="departDatetime" label="发车时间" width="160" />
        <el-table-column prop="trainType" label="车型" width="60" />
        <el-table-column label="操作" width="80">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="selectChangeTrain(row)">选择</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="selectedChangeTrain" style="margin-top: 12px">
        <el-divider content-position="left">选择座位</el-divider>
        <el-table :data="availableSeats" border stripe size="small">
          <el-table-column prop="carriageNo" label="车厢" width="60" />
          <el-table-column prop="carriageLevel" label="等级" width="80" />
          <el-table-column prop="seatNo" label="座位号" width="80" />
          <el-table-column prop="price" label="票价" width="80">
            <template #default="{ row }">
              <span style="color:#f56c6c">￥{{ row.price }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80">
            <template #default="{ row }">
              <el-button type="primary" size="small" @click="doChange(row)">改签</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { queryOrders, payOrder, cancelOrder, changeOrder, refundOrder, listAvailableSeats, listTrainsForChange, timeoutCancelOrder } from '../api'
import { ElMessage, ElMessageBox } from 'element-plus'

export default {
  name: 'MyOrders',
  data() {
    return {
      userId: null,
      orders: [],
      loading: false,
      statusFilter: '',
      countdowns: {},
      timer: null,
      showPayDialog: false,
      currentOrder: {},
      payMethod: 'wechat',
      payLoading: false,
      showChangeDialog: false,
      changeQuery: { startCity: '', endCity: '', trainType: '' },
      changeTrains: [],
      selectedChangeTrain: null,
      availableSeats: [],
      currentChangeOrder: null
    }
  },
  computed: {
    hasPendingOrders() {
      return this.orders.some(o => o.orderStatus === 'PENDING')
    }
  },
  async mounted() {
    this.userId = localStorage.getItem('userId')
    if (!this.userId) {
      this.$router.push('/login')
      return
    }
    await this.loadOrders()
    this.startTimer()
  },
  beforeUnmount() {
    this.stopTimer()
  },
  methods: {
    async loadOrders() {
      this.loading = true
      try {
        const params = { userId: this.userId }
        if (this.statusFilter) {
          params.orderStatus = this.statusFilter
        }
        const res = await queryOrders(params)
        this.orders = res.data || []
        this.initCountdowns()
      } catch (e) {
        ElMessage.error('加载订单失败')
      } finally {
        this.loading = false
      }
    },
    initCountdowns() {
      const now = Date.now()
      this.countdowns = {}
      for (const order of this.orders) {
        if (order.orderStatus === 'PENDING' && order.createTime) {
          const createTime = new Date(order.createTime).getTime()
          const deadline = createTime + 30 * 60 * 1000
          const remaining = Math.max(0, deadline - now)
          this.countdowns[order.orderId] = remaining
        }
      }
    },
    startTimer() {
      this.stopTimer()
      this.timer = setInterval(() => {
        let needReload = false
        for (const key of Object.keys(this.countdowns)) {
          if (this.countdowns[key] > 0) {
            this.countdowns[key] -= 1000
            if (this.countdowns[key] <= 0) {
              this.countdowns[key] = 0
              needReload = true
              this.timeoutCancelOrder(key)
            }
          }
        }
        this.countdowns = { ...this.countdowns }
      }, 1000)
    },
    stopTimer() {
      if (this.timer) {
        clearInterval(this.timer)
        this.timer = null
      }
    },
    async timeoutCancelOrder(orderId) {
      try {
        await timeoutCancelOrder(orderId)
        ElMessage.warning('订单超时未支付，已自动取消')
        await this.loadOrders()
      } catch (e) {
        console.error('超时取消失败', e)
      }
    },
    formatCountdown(orderId) {
      const ms = this.countdowns[orderId]
      if (!ms || ms <= 0) return '已超时'
      const minutes = Math.floor(ms / 60000)
      const seconds = Math.floor((ms % 60000) / 1000)
      return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
    },
    handlePay(row) {
      this.currentOrder = row
      this.showPayDialog = true
    },
    async confirmPay() {
      this.payLoading = true
      try {
        await payOrder(this.currentOrder.orderId)
        ElMessage.success('支付成功')
        this.showPayDialog = false
        await this.loadOrders()
      } catch (e) {
        ElMessage.error(e.response?.data?.message || '支付失败')
      } finally {
        this.payLoading = false
      }
    },
    async handleCancelOrder(row) {
      try {
        await ElMessageBox.confirm('确定取消该订单？', '提示', { type: 'warning' })
        await cancelOrder(row.orderId)
        ElMessage.success('订单已取消')
        await this.loadOrders()
      } catch (e) {
        if (e !== 'cancel') {
          ElMessage.error(e.response?.data?.message || '取消失败')
        }
      }
    },
    handleChange(row) {
      this.currentChangeOrder = row
      this.changeQuery.startCity = row.startCity || ''
      this.changeQuery.endCity = row.endCity || ''
      this.showChangeDialog = true
      this.selectedChangeTrain = null
      this.availableSeats = []
    },
    async loadChangeTrains() {
      try {
        const res = await listTrainsForChange(this.changeQuery)
        this.changeTrains = res.data || []
      } catch (e) {
        ElMessage.error('查询车次失败')
      }
    },
    async selectChangeTrain(train) {
      this.selectedChangeTrain = train
      try {
        const res = await listAvailableSeats(train.trainId)
        this.availableSeats = res.data || []
      } catch (e) {
        ElMessage.error('加载座位失败')
      }
    },
    async doChange(seat) {
      try {
        await ElMessageBox.confirm('确定改签到该座位？', '提示', { type: 'warning' })
        await changeOrder(this.currentChangeOrder.orderId, {
          newTrainId: this.selectedChangeTrain.trainId,
          newSeatId: seat.seatId
        })
        ElMessage.success('改签成功')
        this.showChangeDialog = false
        await this.loadOrders()
      } catch (e) {
        if (e !== 'cancel') {
          ElMessage.error(e.response?.data?.message || '改签失败')
        }
      }
    },
    async handleRefund(row) {
      try {
        await ElMessageBox.confirm('确定退票？退票后将收取手续费。', '提示', { type: 'warning' })
        await refundOrder(row.orderId)
        ElMessage.success('退票成功')
        await this.loadOrders()
      } catch (e) {
        if (e !== 'cancel') {
          ElMessage.error(e.response?.data?.message || '退票失败')
        }
      }
    },
    statusTagType(status) {
      const map = { PENDING: 'warning', PAID: 'success', CANCELLED: 'info', CHANGED: 'primary', REFUNDED: 'danger' }
      return map[status] || 'info'
    },
    statusText(status) {
      const map = { PENDING: '待支付', PAID: '已支付', CANCELLED: '已取消', CHANGED: '已改签', REFUNDED: '已退票' }
      return map[status] || status
    }
  }
}
</script>

<style scoped>
.my-orders { padding: 20px 24px; }
.page-header {
  display: flex;
  align-items: center;
  padding-bottom: 12px;
}
.page-header h2 { margin: 0; color: #303133; }
.countdown {
  color: #f56c6c;
  font-weight: bold;
  font-family: 'Courier New', monospace;
}
.pay-dialog-body { padding: 0 20px; }
.pay-methods { margin-top: 16px; text-align: center; }
</style>
