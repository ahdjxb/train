<template>
  <div class="confirm-page">
    <div class="page-header">
      <el-button text @click="$router.back()" style="margin-right: 8px">
        <el-icon><ArrowLeft /></el-icon> 返回
      </el-button>
      <h2>确认订单</h2>
    </div>

    <div class="confirm-content">
      <el-card class="info-card">
        <template #header><span>车次信息</span></template>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="车次号">{{ trainInfo.trainNo }}</el-descriptions-item>
          <el-descriptions-item label="发车时间">{{ trainInfo.departTime }}</el-descriptions-item>
          <el-descriptions-item label="到达时间">{{ trainInfo.arriveTime }}</el-descriptions-item>
          <el-descriptions-item label="始发站">{{ trainInfo.startStationName }}</el-descriptions-item>
          <el-descriptions-item label="终点站">{{ trainInfo.endStationName }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <el-card class="info-card">
        <template #header><span>选择座位</span></template>
        <el-table :data="availableSeats" border stripe @row-click="selectSeat" size="small">
          <el-table-column label="" width="50">
            <template #default="{ row }">
              <el-radio :model-value="selectedSeatId" :value="row.seatId" @click.stop>
                <span></span>
              </el-radio>
            </template>
          </el-table-column>
          <el-table-column prop="carriageNo" label="车厢号" width="80" />
          <el-table-column prop="carriageLevel" label="席位等级" width="100" />
          <el-table-column prop="seatNo" label="座位号" width="100" />
          <el-table-column prop="price" label="票价">
            <template #default="{ row }">
              <span style="color: #f56c6c; font-weight: bold">￥{{ row.price }}</span>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <el-card class="info-card">
        <template #header>
          <div style="display:flex; justify-content:space-between; align-items:center">
            <span>选择乘车人</span>
            <el-button type="primary" size="small" @click="showAddPassenger = true">新建乘车人</el-button>
          </div>
        </template>
        <el-radio-group v-model="selectedPassengerId">
          <el-radio v-for="p in passengers" :key="p.passengerId" :value="p.passengerId" style="margin-bottom: 8px">
            {{ p.realName }} ({{ p.idCard }})
          </el-radio>
        </el-radio-group>
        <el-empty v-if="passengers.length === 0" description="暂无乘车人，请新建" :image-size="60" />
      </el-card>

      <el-card>
        <div v-if="orderInfo" class="order-status">
          <el-alert
            :title="'订单已创建！请在 ' + countdownText + ' 内完成支付'"
            type="warning"
            :closable="false"
            style="margin-bottom: 16px"
          />
          <el-button type="danger" @click="showPayDialog = true" style="margin-right: 12px">立即支付</el-button>
          <el-button @click="$router.push('/my-orders')">查看我的订单</el-button>
        </div>
        <div v-else>
          <el-button type="primary" @click="handleSubmit" style="margin-right: 12px">提交订单</el-button>
        </div>
      </el-card>
    </div>

    <el-dialog v-model="showAddPassenger" title="新建乘车人" width="400px">
      <el-form ref="passengerFormRef" :model="passengerForm" :rules="passengerRules" label-width="80px">
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="passengerForm.realName" />
        </el-form-item>
        <el-form-item label="身份证号" prop="idCard">
          <el-input v-model="passengerForm.idCard" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddPassenger = false">取消</el-button>
        <el-button type="primary" @click="handleAddPassenger">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showPayDialog" title="支付" width="400px">
      <div style="text-align: center; padding: 20px 0">
        <el-icon size="48" color="#67c23a"><CircleCheck /></el-icon>
        <p style="margin-top: 12px">确认支付该订单？</p>
        <p style="font-size: 24px; color: #f56c6c; font-weight: bold; margin-top: 8px">
          ￥{{ orderInfo ? orderInfo.ticketPrice : 0 }}
        </p>
        <p style="color: #999; margin-top: 8px">剩余支付时间：{{ countdownText }}</p>
      </div>
      <template #footer>
        <el-button @click="showPayDialog = false">取消</el-button>
        <el-button type="primary" @click="handlePay">确认支付</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { listAvailableSeats, createOrder, payOrder, listPassengers, addPassenger } from '../api'
import { ElMessage } from 'element-plus'
import { CircleCheck, ArrowLeft } from '@element-plus/icons-vue'

export default {
  name: 'OrderConfirm',
  components: { CircleCheck, ArrowLeft },
  data() {
    return {
      userId: null,
      trainInfo: {
        trainId: null, trainNo: '', departTime: '', arriveTime: '',
        startStationName: '', endStationName: ''
      },
      availableSeats: [],
      selectedSeatId: null,
      passengers: [],
      selectedPassengerId: null,
      orderInfo: null,
      countdownTimer: null,
      remainingSeconds: 0,
      showAddPassenger: false,
      showPayDialog: false,
      passengerForm: { realName: '', idCard: '' },
      passengerRules: {
        realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
        idCard: [{ required: true, message: '请输入身份证号', trigger: 'blur' }]
      }
    }
  },
  computed: {
    countdownText() {
      const m = Math.floor(this.remainingSeconds / 60)
      const s = this.remainingSeconds % 60
      return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
    }
  },
  async mounted() {
    this.userId = localStorage.getItem('userId')
    if (!this.userId) {
      this.$router.push('/login')
      return
    }
    this.trainInfo.trainId = this.$route.query.trainId
    this.trainInfo.trainNo = this.$route.query.trainNo
    this.trainInfo.departTime = this.$route.query.departTime
    this.trainInfo.arriveTime = this.$route.query.arriveTime
    this.trainInfo.startStationName = this.$route.query.startStationName
    this.trainInfo.endStationName = this.$route.query.endStationName
    await this.loadSeats()
    await this.loadPassengers()
  },
  beforeUnmount() {
    if (this.countdownTimer) clearInterval(this.countdownTimer)
  },
  methods: {
    async loadSeats() {
      const res = await listAvailableSeats(this.trainInfo.trainId)
      this.availableSeats = res.data || []
    },
    async loadPassengers() {
      const res = await listPassengers(this.userId)
      this.passengers = res.data
    },
    selectSeat(row) {
      this.selectedSeatId = row.seatId
    },
    async handleSubmit() {
      if (!this.selectedSeatId) {
        ElMessage.warning('请选择座位')
        return
      }
      if (!this.selectedPassengerId) {
        ElMessage.warning('请选择乘车人')
        return
      }
      try {
        const res = await createOrder({
          userId: this.userId,
          passengerId: this.selectedPassengerId,
          trainId: this.trainInfo.trainId,
          seatId: this.selectedSeatId
        })
        this.orderInfo = res.data
        this.remainingSeconds = 30 * 60
        this.startCountdown()
        ElMessage.success('订单创建成功，请尽快支付')
      } catch (e) {
        // handled by interceptor
      }
    },
    startCountdown() {
      if (this.countdownTimer) clearInterval(this.countdownTimer)
      this.countdownTimer = setInterval(() => {
        if (this.remainingSeconds > 0) {
          this.remainingSeconds--
        } else {
          clearInterval(this.countdownTimer)
          ElMessage.warning('支付超时，订单已取消')
          this.orderInfo = null
          this.showPayDialog = false
          this.loadSeats()
        }
      }, 1000)
    },
    async handlePay() {
      try {
        await payOrder(this.orderInfo.orderId)
        ElMessage.success('支付成功！')
        this.showPayDialog = false
        if (this.countdownTimer) clearInterval(this.countdownTimer)
        this.$router.push('/my-orders')
      } catch (e) {
        // handled by interceptor
      }
    },
    handleAddPassenger() {
      this.$refs.passengerFormRef.validate(async (valid) => {
        if (!valid) return
        await addPassenger(this.userId, this.passengerForm)
        ElMessage.success('添加成功')
        this.showAddPassenger = false
        this.passengerForm = { realName: '', idCard: '' }
        await this.loadPassengers()
      })
    }
  }
}
</script>

<style scoped>
.confirm-page { padding: 20px 24px; }
.page-header { display: flex; align-items: center; padding-bottom: 12px; }
.page-header h2 { margin: 0; color: #303133; }
.confirm-content { max-width: 800px; margin: 0 auto; }
.info-card { margin-bottom: 16px; }
.order-status { text-align: center; }
</style>
