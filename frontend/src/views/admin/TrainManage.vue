<template>
  <div class="train-manage">
    <div class="page-header">
      <h2>车次管理</h2>
      <el-button type="primary" @click="openAddDialog" :icon="Plus">新增车次</el-button>
    </div>

    <div class="filter-bar">
      <el-input v-model="searchTrainNo" placeholder="车次号" clearable style="width: 180px" />
      <el-select v-model="searchTrainType" placeholder="车型" clearable style="width: 120px">
        <el-option label="高铁" value="高铁" />
        <el-option label="动车" value="动车" />
        <el-option label="火车" value="火车" />
      </el-select>
      <el-button type="primary" @click="loadTrains">查询</el-button>
    </div>

    <el-table :data="trains" v-loading="loading" border stripe style="width: 100%">
      <el-table-column prop="trainNo" label="车次" width="100" />
      <el-table-column label="发车时间" width="160">
        <template #default="{ row }">
          {{ formatDateTime(row.departDatetime) }}
        </template>
      </el-table-column>
      <el-table-column label="到达时间" width="160">
        <template #default="{ row }">
          {{ formatDateTime(row.arriveDatetime) }}
        </template>
      </el-table-column>
      <el-table-column prop="startStationName" label="始发站" width="100" />
      <el-table-column prop="endStationName" label="终点站" width="100" />
      <el-table-column prop="trainType" label="车型" width="80" />
      <el-table-column label="售票状态" width="100">
        <template #default="{ row }">
          <el-tag :type="saleStatusTagType(row.saleStatus)">{{ saleStatusText(row.saleStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="售票开放时间" width="160">
        <template #default="{ row }">
          {{ formatDateTime(row.saleOpenTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <template v-if="!row.departed">
            <el-button size="small" type="primary" @click="openEditDialog(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
            <el-button
              v-if="row.saleStatus === 0"
              size="small"
              type="success"
              @click="handleSetSaleStatus(row, 1)"
            >开售</el-button>
            <el-button
              v-if="row.saleStatus === 1"
              size="small"
              type="warning"
              @click="handleSetSaleStatus(row, -1)"
            >关售</el-button>
          </template>
          <el-tag v-else type="info">已发车</el-tag>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/修改弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'add' ? '新增车次' : '编辑车次'"
      width="750px"
      @close="resetForm"
    >
      <el-form :model="form" label-width="120px" :rules="formRules" ref="formRef">
        <el-form-item label="车次号" prop="trainNo">
          <el-input v-model="form.trainNo" :disabled="isFieldDisabled('trainNo')" />
        </el-form-item>
        <el-form-item label="车型" prop="trainType">
          <el-select v-model="form.trainType" :disabled="isFieldDisabled('trainType')" style="width: 100%">
            <el-option label="高铁" value="高铁" />
            <el-option label="动车" value="动车" />
            <el-option label="火车" value="火车" />
          </el-select>
        </el-form-item>
        <el-form-item label="始发站" prop="startStationId">
          <el-select
            v-model="form.startStationId"
            :disabled="isFieldDisabled('startStationId')"
            style="width: 100%"
            placeholder="选择始发站"
          >
            <el-option
              v-for="s in stations"
              :key="s.stationId"
              :label="s.stationName"
              :value="s.stationId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="终点站" prop="endStationId">
          <el-select
            v-model="form.endStationId"
            :disabled="isFieldDisabled('endStationId')"
            style="width: 100%"
            placeholder="选择终点站"
          >
            <el-option
              v-for="s in stations"
              :key="s.stationId"
              :label="s.stationName"
              :value="s.stationId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="发车时间" prop="departDatetime">
          <el-date-picker
            v-model="form.departDatetime"
            type="datetime"
            placeholder="选择发车时间"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
            :disabled="isFieldDisabled('departDatetime')"
          />
        </el-form-item>
        <el-form-item label="到达时间" prop="arriveDatetime">
          <el-date-picker
            v-model="form.arriveDatetime"
            type="datetime"
            placeholder="选择到达时间"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
            :disabled="isFieldDisabled('arriveDatetime')"
          />
        </el-form-item>

        <!-- 售票状态：仅在新增时显示，编辑时灰色或隐藏 -->
        <el-form-item v-if="dialogMode === 'add'" label="售票状态">
          <el-select v-model="form.saleStatus" style="width: 100%">
            <el-option label="未开售" :value="0" />
            <el-option label="开售" :value="1" />
            <el-option label="关售" :value="-1" />
          </el-select>
        </el-form-item>
        <el-form-item v-else label="售票状态">
          <el-tag :type="saleStatusTagType(form.saleStatus)">{{ saleStatusText(form.saleStatus) }}</el-tag>
          <span class="hint-text">（不可修改）</span>
        </el-form-item>

        <el-divider content-position="left">途经站点</el-divider>

        <el-table :data="form.routeList" border style="width: 100%" size="small">
          <el-table-column label="序号" width="60" type="index" />
          <el-table-column label="站点" width="180">
            <template #default="{ row, $index }">
              <el-select
                v-model="row.stationId"
                :disabled="isRouteFieldDisabled($index)"
                style="width: 100%"
                size="small"
              >
                <el-option
                  v-for="s in stations"
                  :key="s.stationId"
                  :label="s.stationName"
                  :value="s.stationId"
                />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="到达时间" width="180">
            <template #default="{ row }">
              <el-date-picker
                v-model="row.arriveDatetime"
                type="datetime"
                placeholder="到达时间"
                value-format="YYYY-MM-DDTHH:mm:ss"
                style="width: 100%"
                size="small"
              />
            </template>
          </el-table-column>
          <el-table-column label="发车时间" width="180">
            <template #default="{ row }">
              <el-date-picker
                v-model="row.departDatetime"
                type="datetime"
                placeholder="发车时间"
                value-format="YYYY-MM-DDTHH:mm:ss"
                style="width: 100%"
                size="small"
              />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80">
            <template #default="{ $index }">
              <el-button
                v-if="!isRouteFieldDisabled($index)"
                size="small"
                type="danger"
                @click="removeRoute($index)"
              >删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-button
          v-if="!isAddOnly && canAddRoute"
          @click="addRoute"
          size="small"
          type="primary"
          plain
          style="margin-top: 10px"
        >添加途经站</el-button>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import axios from 'axios'

const trains = ref([])
const stations = ref([])
const loading = ref(false)
const searchTrainNo = ref('')
const searchTrainType = ref('')

const dialogVisible = ref(false)
const dialogMode = ref('add') // 'add' | 'edit'
const submitting = ref(false)
const formRef = ref(null)

const form = reactive({
  trainId: null,
  trainNo: '',
  trainType: '高铁',
  startStationId: null,
  endStationId: null,
  departDatetime: null,
  arriveDatetime: null,
  saleStatus: 0,
  routeList: []
})

// 当前编辑的车次原始数据
const editingTrain = ref(null)

const formRules = {
  trainNo: [{ required: true, message: '请输入车次号', trigger: 'blur' }],
  trainType: [{ required: true, message: '请选择车型', trigger: 'change' }],
  startStationId: [{ required: true, message: '请选择始发站', trigger: 'change' }],
  endStationId: [{ required: true, message: '请选择终点站', trigger: 'change' }],
  departDatetime: [{ required: true, message: '请选择发车时间', trigger: 'change' }],
  arriveDatetime: [{ required: true, message: '请选择到达时间', trigger: 'change' }]
}

const isAddOnly = computed(() => dialogMode.value === 'add')

// 正在售票时只允许修改时间类字段
const isEditingInSale = computed(() => {
  return dialogMode.value === 'edit'
    && editingTrain.value
    && editingTrain.value.inSaleWindow
    && editingTrain.value.saleStatus === 1
})

// 字段是否禁用
function isFieldDisabled(field) {
  if (dialogMode.value === 'add') return false
  if (!editingTrain.value) return false

  // 已发车：全部禁用（不应该进编辑）
  if (editingTrain.value.departed) return true

  // 正在售票：只允许改时间
  if (isEditingInSale.value) {
    // 允许修改：departDatetime, arriveDatetime
    if (field === 'departDatetime' || field === 'arriveDatetime') return false
    return true
  }

  // 非售票窗口期（未开售）：允许全部修改
  return false
}

// 途经站点的站点选择是否禁用（正在售票时已有站点不可改站点，只可改时间）
function isRouteFieldDisabled(index) {
  if (dialogMode.value === 'add') return false
  if (!editingTrain.value) return false
  if (editingTrain.value.departed) return true
  if (isEditingInSale.value) {
    // 正在售票：已有站点不可改站点，只可改时间；新添加的站点可改
    return index < (editingTrain.value.routeList?.length || 0)
  }
  return false
}

// 是否可以添加途经站点
const canAddRoute = computed(() => {
  if (dialogMode.value === 'add') return true
  if (!editingTrain.value) return false
  if (editingTrain.value.departed) return false
  if (isEditingInSale.value) return true // 正在售票时可添加
  return true // 未开售时可添加
})

function saleStatusText(status) {
  if (status === 1) return '正在售票'
  if (status === 0) return '未开售'
  if (status === -1) return '已关售'
  return '未知'
}

function saleStatusTagType(status) {
  if (status === 1) return 'success'
  if (status === 0) return 'info'
  if (status === -1) return 'danger'
  return 'info'
}

function formatDateTime(dt) {
  if (!dt) return '-'
  return dt.replace('T', ' ')
}

async function loadTrains() {
  loading.value = true
  try {
    const params = {}
    if (searchTrainNo.value) params.trainNo = searchTrainNo.value
    if (searchTrainType.value) params.trainType = searchTrainType.value
    const res = await axios.get('/api/train/list', { params })
    trains.value = res.data
  } catch (e) {
    ElMessage.error('加载车次列表失败')
  } finally {
    loading.value = false
  }
}

async function loadStations() {
  try {
    const res = await axios.get('/api/station/list')
    stations.value = res.data
  } catch (e) {
    ElMessage.error('加载站点列表失败')
  }
}

function openAddDialog() {
  dialogMode.value = 'add'
  editingTrain.value = null
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(row) {
  dialogMode.value = 'edit'
  editingTrain.value = row
  resetForm()

  form.trainId = row.trainId
  form.trainNo = row.trainNo
  form.trainType = row.trainType
  form.startStationId = row.startStationId
  form.endStationId = row.endStationId
  // 转换为 date-picker 需要的格式
  form.departDatetime = row.departDatetime
  form.arriveDatetime = row.arriveDatetime
  form.saleStatus = row.saleStatus
  form.routeList = (row.routeList || []).map(r => ({ ...r }))

  dialogVisible.value = true
}

function resetForm() {
  form.trainId = null
  form.trainNo = ''
  form.trainType = '高铁'
  form.startStationId = null
  form.endStationId = null
  form.departDatetime = null
  form.arriveDatetime = null
  form.saleStatus = 0
  form.routeList = []
}

function addRoute() {
  form.routeList.push({
    stationId: null,
    sort: form.routeList.length + 1,
    arriveDatetime: null,
    departDatetime: null
  })
}

function removeRoute(index) {
  form.routeList.splice(index, 1)
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      if (dialogMode.value === 'add') {
        await axios.post('/api/train', form)
        ElMessage.success('新增车次成功')
      } else {
        await axios.put('/api/train', form)
        ElMessage.success('修改车次成功')
      }
      dialogVisible.value = false
      loadTrains()
    } catch (e) {
      const msg = e.response?.data?.message || e.response?.data || '操作失败'
      ElMessage.error(msg)
    } finally {
      submitting.value = false
    }
  })
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除车次 ${row.trainNo} 吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await axios.delete(`/api/train/${row.trainId}`)
    ElMessage.success('删除成功')
    loadTrains()
  } catch (e) {
    if (e !== 'cancel') {
      const msg = e.response?.data?.message || e.response?.data || '删除失败'
      ElMessage.error(msg)
    }
  }
}

async function handleSetSaleStatus(row, status) {
  try {
    await axios.put(`/api/train/sale-status/${row.trainId}?saleStatus=${status}`)
    ElMessage.success('设置成功')
    loadTrains()
  } catch (e) {
    const msg = e.response?.data?.message || e.response?.data || '设置失败'
    ElMessage.error(msg)
  }
}

onMounted(() => {
  loadTrains()
  loadStations()
})
</script>

<style scoped>
.train-manage {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  font-size: 22px;
}

.filter-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}

.hint-text {
  color: #999;
  font-size: 12px;
  margin-left: 8px;
}
</style>
