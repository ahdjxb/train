<template>
  <div class="admin-page">
    <div class="page-header">
      <h2>用户管理</h2>
    </div>

    <!-- 搜索栏 -->
    <el-card style="margin-bottom: 16px">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="用户名">
          <el-input v-model="searchForm.username" placeholder="用户名" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.isLock" placeholder="全部" clearable style="width: 120px">
            <el-option label="正常" :value="0" />
            <el-option label="已锁定" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadUsers">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 用户列表 -->
    <el-card>
      <el-table :data="users" border stripe>
        <el-table-column prop="userId" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="account" label="账号" />
        <el-table-column prop="phone" label="手机号" />
        <el-table-column prop="role" label="角色" width="80" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.isLock === 1 ? 'danger' : 'success'">
              {{ row.isLock === 1 ? '已锁定' : '正常' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button
              v-if="row.isLock === 0"
              type="danger" size="small"
              @click="handleLock(row.userId, 1)"
            >锁定</el-button>
            <el-button
              v-else
              type="success" size="small"
              @click="handleLock(row.userId, 0)"
            >解锁</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script>
import { listNormalUsers, setUserLockStatus } from '../../api'
import { ElMessage, ElMessageBox } from 'element-plus'

export default {
  name: 'UserManage',
  data() {
    return {
      searchForm: {
        username: '',
        isLock: null
      },
      users: []
    }
  },
  async mounted() {
    const userId = localStorage.getItem('userId')
    const role = localStorage.getItem('role')
    if (!userId || role !== 'ADMIN') {
      this.$router.push('/admin/login')
      return
    }
    await this.loadUsers()
  },
  methods: {
    async loadUsers() {
      const params = {}
      if (this.searchForm.username) params.username = this.searchForm.username
      if (this.searchForm.isLock !== null) params.isLock = this.searchForm.isLock
      const res = await listNormalUsers(params)
      this.users = res.data
    },
    async handleLock(userId, isLock) {
      const action = isLock === 1 ? '锁定' : '解锁'
      try {
        await ElMessageBox.confirm(`确认${action}该用户？`, '提示', { type: 'warning' })
        await setUserLockStatus(userId, isLock)
        ElMessage.success(`${action}成功`)
        await this.loadUsers()
      } catch (e) { /* cancelled */ }
    }
  }
}
</script>

<style scoped>
.admin-page { padding: 20px 24px; }
.page-header { padding-bottom: 12px; }
.page-header h2 { margin: 0; color: #303133; }
</style>
