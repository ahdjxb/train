<template>
  <div class="admin-page">
    <div class="page-header">
      <h2>全部订单</h2>
    </div>

    <!-- 筛选区 -->
    <el-card style="margin-bottom: 16px">
      <el-form :inline="true" :model="queryForm" label-width="70px">
        <el-form-item label="订单号">
          <el-input v-model="queryForm.orderId" placeholder="订单编号" clearable style="width: 120px" />
        </el-form-item>
        <el-form-item label="车次号">
          <el-input v-model="queryForm.trainNo" placeholder="车次号" clearable style="width: 120px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.orderStatus" placeholder="全部" clearable style="width: 120px">
            <el-option label="待支付" value="PENDING" />
            <el-option label="已支付" value="PAID" />
            <el-option label="已改签" value="CHANGED" />
            <el-option label="已退票" value="REFUNDED" />
            <el-option label="已完成" value="FINISHED" />
            <el-option label="已取消" value="CANCELLED" />
          </el-select>
        </el-form-item>
        <el-form-item label="出发站">
          <el-select v-model="queryForm.startStationId" filterable clearable placeholder="出发站" style="width: 140px">
            <el-option v-for="s in stations" :key="s.stationId" :label="s.stationName" :value="s.stationId" />
          </el-select>
        </el-form-item>
        <el-form-item label="到达站">
          <el-select v-model="queryForm.endStationId" filterable clearable placeholder="到达站" style="width: 140px">
            <el-option v-for="s in stations" :key="s.stationId" :label="s.stationName" :value="s.stationId" />
          </el-select>
        </el-form-item>
        <el-form-item label="乘车日期">
          <el-date-picker v-model="queryForm.travelDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" style="width: 150px" />
        </el-form-item>
        <el-form-item label="车厢号">
          <el-input v-model="queryForm.carriageNo" placeholder="车厢号" clearable style="width: 100px" />
        </el-form-item>
        <el-form-item label="座位号">
          <el-input v-model="queryForm.seatNo" placeholder="座位号" clearable style="width: 100px" />
        </el-form-item>
        <el-form-item label="创建时间">
          <el-date-picker v-model="queryForm.createTimeStart" type="date" placeholder="开始" value-format="YYYY-MM-DD" style="width: 130px" />
          <span style="margin: 0 4px">-</span>
          <el-date-picker v-model="queryForm.createTimeEnd" type="date" placeholder="结束" value-format="YYYY-MM-DD" style="width: 130px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadOrders">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 订单列表 -->
    <el-card>
      <template #header><span>全部订单</span></template>
      <el-table :data="orders" border stripe>
        <el-table-column prop="orderId" label="订单号" width="80" />
        <el-table-column prop="username" label="用户" width="90" />
        <el-table-column prop="passengerName" label="乘车人" width="90" />
        <el-table-column prop="trainNo" label="车次" width="80" />
        <el-table-column prop="departTime" label="发车时间" width="160" />
        <el-table-column prop="startStationName" label="出发站" width="90" />
        <el-table-column prop="endStationName" label="到达站" width="90" />
        <el-table-column prop="carriageNo" label="车厢" width="60" />
        <el-table-column prop="seatNo" label="座位" width="60" />
        <el-table-column prop="carriageLevel" label="席位" width="60" />
        <el-table-column prop="ticketPrice" label="票价" width="70">
          <template #default="{ row }">¥{{ row.ticketPrice }}</template>
        </el-table-column>
        <el-table-column prop="orderStatus" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.orderStatus)">{{ statusText(row.orderStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" />
      </el-table>
    </el-card>
  </div>
</template>

<script>
import { adminQueryAllOrders, listStations } from '../../api'

export default {
  name: 'AllOrders',
  data() {
    return {
      stations: [],
      queryForm: {
        orderId: '', trainNo: '', orderStatus: '',
        startStationId: null, endStationId: null,
        travelDate: '', carriageNo: '', seatNo: '',
        createTimeStart: '', createTimeEnd: ''
      },
      orders: []
    }
  },
  async mounted() {
    const userId = localStorage.getItem('userId')
    const role = localStorage.getItem('role')
    if (!userId || role !== 'ADMIN') {
      this.$router.push('/admin/login')
      return
    }
    await this.loadStations()
    await this.loadOrders()
  },
  methods: {
    async loadStations() {
      const res = await listStations()
      this.stations = res.data
    },
    async loadOrders() {
      const data = { ...this.queryForm }
      Object.keys(data).forEach(k => {
        if (data[k] === '' || data[k] === null) delete data[k]
      })
      const res = await adminQueryAllOrders(data)
      this.orders = res.data
    },
    resetQuery() {
      this.queryForm = {
        orderId: '', trainNo: '', orderStatus: '',
        startStationId: null, endStationId: null,
        travelDate: '', carriageNo: '', seatNo: '',
        createTimeStart: '', createTimeEnd: ''
      }
    },
    statusText(status) {
      const map = { PENDING: '待支付', PAID: '已支付', CHANGED: '已改签', REFUNDED: '已退票', FINISHED: '已完成', CANCELLED: '已取消' }
      return map[status] || status
    },
    statusTagType(status) {
      const map = { PENDING: 'warning', PAID: 'success', CHANGED: '', REFUNDED: 'info', FINISHED: '', CANCELLED: 'danger' }
      return map[status] || ''
    }
  }
}
</script>

<style scoped>
.admin-page { padding: 20px 24px; }
.page-header { padding-bottom: 12px; }
.page-header h2 { margin: 0; color: #303133; }
</style>
