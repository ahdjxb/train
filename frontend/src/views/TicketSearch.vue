<template>
  <div class="ticket-search">
    <!-- 顶部搜索栏 -->
    <div class="search-banner">
      <div class="search-bar">
        <el-form :inline="true" :model="searchForm" class="search-form">
          <el-form-item>
            <el-select v-model="searchForm.startCity" filterable placeholder="始发城市" class="city-select" @change="onStartCityChange">
              <el-option v-for="c in cities" :key="c" :label="c" :value="c" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button circle size="small" @click="swapCities" class="swap-btn">
              <el-icon><Sort /></el-icon>
            </el-button>
          </el-form-item>
          <el-form-item>
            <el-select v-model="searchForm.endCity" filterable placeholder="终点城市" class="city-select" @change="onEndCityChange">
              <el-option v-for="c in cities" :key="c" :label="c" :value="c" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch" :loading="loading" size="large" class="search-btn">
              <el-icon><Search /></el-icon> 查询
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>

    <!-- 日期横条 + 筛选标签 -->
    <div v-if="searched" class="filter-section">
      <div class="date-strip">
        <div v-for="d in dateList" :key="d.value" class="date-item" :class="{ active: searchForm.date === d.value }" @click="changeDate(d.value)">
          <div class="date-weekday">{{ d.weekday }}</div>
          <div class="date-value">{{ d.label }}</div>
        </div>
      </div>
      <div class="filter-tags">
        <el-radio-group v-model="searchForm.filterType" @change="handleFilterChange">
          <el-radio-button label="direct">直达</el-radio-button>
          <el-radio-button label="">全部</el-radio-button>
          <el-radio-button label="transfer">中转</el-radio-button>
        </el-radio-group>
        <el-radio-group v-model="searchForm.trainType" @change="handleFilterChange" style="margin-left: 16px">
          <el-radio-button label="">全部车型</el-radio-button>
          <el-radio-button label="高铁">高铁</el-radio-button>
          <el-radio-button label="普通">普速</el-radio-button>
        </el-radio-group>
        <el-select v-model="searchForm.startStationId" filterable clearable placeholder="始发站点(全部)" style="margin-left: 16px; width: 160px" @change="handleFilterChange">
          <el-option v-for="s in startStations" :key="s.stationId" :label="s.stationName" :value="s.stationId" />
        </el-select>
        <el-select v-model="searchForm.endStationId" filterable clearable placeholder="终点站点(全部)" style="margin-left: 8px; width: 160px" @change="handleFilterChange">
          <el-option v-for="s in endStations" :key="s.stationId" :label="s.stationName" :value="s.stationId" />
        </el-select>
      </div>
    </div>

    <!-- 查询结果 -->
    <div v-if="searched" class="result-section">
      <template v-if="showDirect">
        <div v-if="directList.length === 0" class="empty-block"><el-empty description="未查询到直达车次" /></div>
        <div v-else class="train-list">
          <div v-for="train in directList" :key="train.trainId" class="train-card">
            <div class="train-card-main">
              <div class="train-time-block">
                <div class="train-depart">
                  <span class="time">{{ train.departTime?.split(' ')[1] || '--' }}</span>
                  <span class="station">{{ train.startStationName }}</span>
                </div>
                <div class="train-arrow">
                  <div class="arrow-line"></div>
                  <el-icon><ArrowRight /></el-icon>
                  <div class="arrow-line"></div>
                </div>
                <div class="train-arrive">
                  <span class="time">{{ train.arriveTime?.split(' ')[1] || '--' }}</span>
                  <span class="station">{{ train.endStationName }}</span>
                </div>
              </div>
              <div class="train-info-block">
                <div class="train-no">{{ train.trainNo }}</div>
                <div class="train-type">{{ train.trainType }}</div>
                <div class="train-duration">{{ train.duration || '' }}</div>
              </div>
              <div class="seat-list">
                <div v-for="seat in train.seatList" :key="seat.carriageLevel" class="seat-item">
                  <span class="seat-level">{{ seat.carriageLevel }}</span>
                  <span class="seat-price">￥{{ seat.price || '--' }}</span>
                  <span class="seat-count" :class="{ 'no-ticket': seat.availableCount === 0 }">
                    {{ seat.availableCount > 0 ? `余${seat.availableCount}` : '无' }}
                  </span>
                </div>
              </div>
              <div class="buy-block">
                <el-button :type="train.hasTicket ? 'primary' : 'info'" :disabled="!train.hasTicket" @click="handleBuyTicket(train)" class="buy-btn">
                  {{ train.hasTicket ? '购票' : '售罄' }}
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </template>
      <template v-if="showTransfer">
        <div class="section-title">中转方案</div>
        <div v-if="transferList.length === 0" class="empty-block"><el-empty description="未查询到中转方案" /></div>
        <div v-else class="transfer-list">
          <div v-for="(transfer, idx) in transferList" :key="idx" class="transfer-card">
            <div class="transfer-header">
              <el-tag type="warning" size="small">中转{{ transfer.transferCount }}次</el-tag>
              <span class="route-path">{{ transfer.stationNames?.join(' → ') }}</span>
              <span class="total-duration">总耗时 {{ transfer.totalDuration || '' }}</span>
            </div>
            <div v-for="(seg, sidx) in transfer.segments" :key="sidx" class="segment-row">
              <div class="seg-train-info">
                <span class="seg-train-no">{{ seg.trainNo }}</span>
                <span class="seg-type">{{ seg.trainType }}</span>
              </div>
              <div class="seg-stations">
                <div class="seg-from">
                  <span class="seg-time">{{ seg.departTime?.split(' ')[1] || '--' }}</span>
                  <span class="seg-name">{{ seg.startStationName }}</span>
                </div>
                <div class="seg-middle"><el-icon><ArrowRight /></el-icon></div>
                <div class="seg-to">
                  <span class="seg-time">{{ seg.arriveTime?.split(' ')[1] || '--' }}</span>
                  <span class="seg-name">{{ seg.endStationName }}</span>
                </div>
              </div>
              <el-tag :type="seg.hasTicket ? 'success' : 'info'" size="small">{{ seg.hasTicket ? '有余票' : '无票' }}</el-tag>
            </div>
          </div>
        </div>
      </template>
    </div>
    <div v-if="!searched" class="welcome-section"><el-empty description="请选择始发城市、终点城市进行查询" /></div>
  </div>
</template>

<script>
import { searchTickets, listCities, listStationsByCity } from '../api'
import { ElMessage } from 'element-plus'
import { Search, ArrowRight, Sort } from '@element-plus/icons-vue'

export default {
  name: 'TicketSearch',
  components: { Search, ArrowRight, Sort },
  data() {
    return {
      cities: [],
      startStations: [],
      endStations: [],
      searchForm: {
        startCity: '', endCity: '', date: '',
        filterType: 'direct', trainType: '',
        startStationId: null, endStationId: null
      },
      directList: [],
      transferList: [],
      searched: false,
      loading: false,
      abortController: null
    }
  },
  computed: {
    showDirect() { return this.searchForm.filterType !== 'transfer' },
    showTransfer() { return this.searchForm.filterType !== 'direct' },
    dateList() {
      const list = []
      const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
      const today = new Date()
      for (let i = 0; i < 7; i++) {
        const d = new Date(today)
        d.setDate(d.getDate() + i)
        const y = d.getFullYear()
        const m = String(d.getMonth() + 1).padStart(2, '0')
        const day = String(d.getDate()).padStart(2, '0')
        list.push({
          value: `${y}-${m}-${day}`,
          label: `${m}-${day}`,
          weekday: i === 0 ? '今天' : weekdays[d.getDay()]
        })
      }
      return list
    }
  },
  async mounted() {
    const userId = localStorage.getItem('userId')
    if (!userId) { this.$router.push('/login'); return }
    await this.loadCities()
    // 从localStorage恢复查询条件
    const saved = localStorage.getItem('ticketSearchForm')
    if (saved) {
      try {
        const form = JSON.parse(saved)
        this.searchForm.startCity = form.startCity || ''
        this.searchForm.endCity = form.endCity || ''
        this.searchForm.date = form.date || this.getToday()
      } catch (e) { /* ignore */ }
    }
    if (!this.searchForm.date) this.searchForm.date = this.getToday()
    if (this.searchForm.startCity) await this.onStartCityChange(this.searchForm.startCity)
    if (this.searchForm.endCity) await this.onEndCityChange(this.searchForm.endCity)
    if (this.searchForm.startCity && this.searchForm.endCity) this.handleSearch()
  },
  beforeUnmount() {
    this.cancelSearch()
  },
  methods: {
    getToday() {
      const d = new Date()
      return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
    },
    cancelSearch() {
      if (this.abortController) {
        this.abortController.abort()
        this.abortController = null
      }
    },
    async loadCities() {
      const res = await listCities()
      this.cities = res.data || []
    },
    async onStartCityChange(city) {
      if (!city) { this.startStations = []; return }
      const res = await listStationsByCity(city)
      this.startStations = res.data || []
      this.searchForm.startStationId = null
    },
    async onEndCityChange(city) {
      if (!city) { this.endStations = []; return }
      const res = await listStationsByCity(city)
      this.endStations = res.data || []
      this.searchForm.endStationId = null
    },
    swapCities() {
      const tmp = this.searchForm.startCity
      this.searchForm.startCity = this.searchForm.endCity
      this.searchForm.endCity = tmp
      const tmpStations = this.startStations
      this.startStations = this.endStations
      this.endStations = tmpStations
      const tmpId = this.searchForm.startStationId
      this.searchForm.startStationId = this.searchForm.endStationId
      this.searchForm.endStationId = tmpId
    },
    changeDate(date) {
      this.searchForm.date = date
      this.handleSearch()
    },
    handleFilterChange() {
      if (this.searched) this.handleSearch()
    },
    saveSearchForm() {
      localStorage.setItem('ticketSearchForm', JSON.stringify({
        startCity: this.searchForm.startCity,
        endCity: this.searchForm.endCity,
        date: this.searchForm.date
      }))
    },
    async handleSearch() {
      if (!this.searchForm.startCity || !this.searchForm.endCity) {
        ElMessage.warning('请选择始发城市和终点城市'); return
      }
      if (this.searchForm.startCity === this.searchForm.endCity) {
        ElMessage.warning('始发城市和终点城市不能相同'); return
      }
      this.saveSearchForm()
      this.cancelSearch()
      this.abortController = new AbortController()
      this.loading = true
      this.searched = true
      try {
        const res = await searchTickets(this.searchForm, this.abortController.signal)
        this.directList = res.data.direct || []
        this.transferList = res.data.transfer || []
      } catch (e) {
        if (e.name !== 'AbortError') ElMessage.error('查询失败')
      } finally {
        this.loading = false
      }
    },
    handleBuyTicket(train) {
      if (!train.hasTicket) { ElMessage.warning('该车次已售罄'); return }
      this.$router.push({
        path: '/order/confirm',
        query: {
          trainId: train.trainId, trainNo: train.trainNo,
          departTime: train.departTime, arriveTime: train.arriveTime,
          startStationName: train.startStationName, endStationName: train.endStationName
        }
      })
    }
  }
}
</script>

<style scoped>
.ticket-search { min-height: 100%; background: #f0f2f5; }
.search-banner { background: linear-gradient(135deg, #1a8fff 0%, #0052d4 100%); padding: 20px 0; }
.search-bar { max-width: 1000px; margin: 0 auto; padding: 0 24px; }
.search-form { display: flex; justify-content: center; align-items: center; gap: 4px; }
.city-select { width: 180px; }
.swap-btn { margin: 0 4px; }
.search-btn { font-size: 16px; letter-spacing: 4px; }
.filter-section { background: #fff; padding: 12px 24px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
.date-strip { display: flex; gap: 8px; max-width: 1000px; margin: 0 auto 12px; overflow-x: auto; }
.date-item { flex: 1; min-width: 80px; padding: 8px 12px; border: 1px solid #e4e7ed; border-radius: 6px; text-align: center; cursor: pointer; transition: all 0.2s; }
.date-item:hover { border-color: #409eff; color: #409eff; }
.date-item.active { background: #409eff; border-color: #409eff; color: #fff; }
.date-weekday { font-size: 12px; opacity: 0.7; }
.date-value { font-size: 14px; font-weight: bold; margin-top: 2px; }
.filter-tags { max-width: 1000px; margin: 0 auto; display: flex; align-items: center; }
.result-section { max-width: 1100px; margin: 0 auto; padding: 16px 24px; }
.section-title { font-size: 16px; font-weight: bold; margin: 16px 0 8px; color: #303133; }
.train-card { background: #fff; border-radius: 8px; margin-bottom: 8px; overflow: hidden; transition: box-shadow 0.2s; }
.train-card:hover { box-shadow: 0 2px 12px rgba(0,0,0,0.1); }
.train-card-main { display: flex; align-items: center; padding: 16px 20px; gap: 20px; }
.train-time-block { display: flex; align-items: center; gap: 12px; min-width: 280px; }
.train-depart, .train-arrive { display: flex; flex-direction: column; align-items: center; }
.train-depart .time, .train-arrive .time { font-size: 20px; font-weight: bold; color: #303133; }
.train-depart .station, .train-arrive .station { font-size: 12px; color: #909399; margin-top: 4px; }
.train-arrow { display: flex; align-items: center; gap: 4px; color: #c0c4cc; }
.arrow-line { width: 40px; height: 1px; background: #dcdfe6; }
.train-info-block { text-align: center; min-width: 100px; }
.train-no { font-size: 16px; font-weight: bold; color: #303133; }
.train-type { font-size: 12px; color: #909399; margin-top: 4px; }
.train-duration { font-size: 12px; color: #c0c4cc; margin-top: 4px; }
.seat-list { flex: 1; display: flex; flex-wrap: wrap; gap: 12px; }
.seat-item { display: flex; flex-direction: column; align-items: center; padding: 4px 12px; border-right: 1px solid #f0f0f0; }
.seat-level { font-size: 13px; color: #606266; }
.seat-price { font-size: 14px; color: #f56c6c; font-weight: bold; margin: 2px 0; }
.seat-count { font-size: 12px; color: #67c23a; }
.seat-count.no-ticket { color: #f56c6c; }
.buy-block { min-width: 80px; text-align: center; }
.buy-btn { width: 80px; height: 36px; font-size: 14px; }
.transfer-card { background: #fff; border-radius: 8px; margin-bottom: 12px; padding: 16px 20px; }
.transfer-header { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; padding-bottom: 8px; border-bottom: 1px solid #f0f0f0; }
.route-path { font-size: 14px; color: #409eff; font-weight: bold; }
.total-duration { font-size: 12px; color: #909399; margin-left: auto; }
.segment-row { display: flex; align-items: center; gap: 16px; padding: 8px 0; border-bottom: 1px dashed #f0f0f0; }
.segment-row:last-child { border-bottom: none; }
.seg-train-info { min-width: 100px; }
.seg-train-no { font-size: 14px; font-weight: bold; color: #409eff; }
.seg-type { font-size: 12px; color: #909399; margin-left: 8px; }
.seg-stations { display: flex; align-items: center; gap: 12px; flex: 1; }
.seg-from, .seg-to { display: flex; flex-direction: column; }
.seg-time { font-size: 16px; font-weight: bold; }
.seg-name { font-size: 12px; color: #909399; }
.seg-middle { color: #c0c4cc; }
.empty-block, .welcome-section { display: flex; justify-content: center; padding: 40px 0; }
.welcome-section { padding: 80px 0; }
</style>
