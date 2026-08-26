<template>
  <div class="admin-page">
    <div class="page-header">
      <h2>车票统计</h2>
    </div>

    <!-- 汇总卡片 -->
    <el-row :gutter="16" style="margin-bottom: 16px">
      <el-col :span="4">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-value">{{ stats.totalTrains || 0 }}</div>
            <div class="stat-label">总车次数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-value">{{ stats.totalSeats || 0 }}</div>
            <div class="stat-label">总座位数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-value" style="color: #67c23a">{{ stats.availableCount || 0 }}</div>
            <div class="stat-label">可售余票</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-value" style="color: #409eff">{{ stats.issuedCount || 0 }}</div>
            <div class="stat-label">出票量</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-value" style="color: #e6a23c">{{ stats.pendingCount || 0 }}</div>
            <div class="stat-label">待支付占用</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-value" style="color: #f56c6c">{{ stats.refundCount || 0 }}</div>
            <div class="stat-label">退票量</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 各车次统计列表 -->
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>各车次车票统计（点击列名排序）</span>
          <el-button type="primary" size="small" @click="loadStats">刷新</el-button>
        </div>
      </template>
      <el-table :data="sortedTrainStats" border stripe @sort-change="handleSortChange">
        <el-table-column prop="trainNo" label="车次号" width="100" />
        <el-table-column prop="trainType" label="车型" width="80" />
        <el-table-column prop="departDatetime" label="发车时间" width="160" />
        <el-table-column prop="startStationName" label="始发站" width="100" />
        <el-table-column prop="endStationName" label="终点站" width="100" />
        <el-table-column prop="totalSeats" label="总座位" width="80" sortable="custom" />
        <el-table-column prop="availableCount" label="余票" width="80" sortable="custom">
          <template #default="{ row }">
            <span :style="{ color: row.availableCount > 0 ? '#67c23a' : '#f56c6c' }">{{ row.availableCount }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="issuedCount" label="出票量" width="80" sortable="custom">
          <template #default="{ row }">
            <span style="color: #409eff">{{ row.issuedCount }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="pendingCount" label="待支付" width="80" sortable="custom">
          <template #default="{ row }">
            <span style="color: #e6a23c">{{ row.pendingCount }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="refundCount" label="退票量" width="80" sortable="custom">
          <template #default="{ row }">
            <span style="color: #f56c6c">{{ row.refundCount }}</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script>
import { getTicketStats } from '../../api'
import { ElMessage } from 'element-plus'

export default {
  name: 'TicketStats',
  data() {
    return {
      stats: {},
      sortProp: 'departDatetime',
      sortOrder: 'ascending'
    }
  },
  computed: {
    sortedTrainStats() {
      let list = (this.stats.trainStatsList || []).slice()
      const prop = this.sortProp
      const order = this.sortOrder
      list.sort((a, b) => {
        let va = a[prop] || 0
        let vb = b[prop] || 0
        if (typeof va === 'string') {
          return order === 'ascending' ? va.localeCompare(vb) : vb.localeCompare(va)
        }
        return order === 'ascending' ? va - vb : vb - va
      })
      return list
    }
  },
  async mounted() {
    const userId = localStorage.getItem('userId')
    const role = localStorage.getItem('role')
    if (!userId || role !== 'ADMIN') {
      this.$router.push('/admin/login')
      return
    }
    await this.loadStats()
  },
  methods: {
    async loadStats() {
      const res = await getTicketStats()
      this.stats = res.data
    },
    handleSortChange({ prop, order }) {
      this.sortProp = prop || 'departDatetime'
      this.sortOrder = order || 'ascending'
    }
  }
}
</script>

<style scoped>
.admin-page { padding: 20px 24px; }
.page-header { padding-bottom: 12px; }
.page-header h2 { margin: 0; color: #303133; }
.stat-card { text-align: center; padding: 12px 0; }
.stat-value { font-size: 32px; font-weight: bold; }
.stat-label { color: #999; margin-top: 4px; }
</style>
