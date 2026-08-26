<template>
  <div class="profile-page">
    <div class="page-header">
      <h2>个人中心</h2>
    </div>
    <el-tabs v-model="activeTab" class="profile-tabs">
      <el-tab-pane label="个人信息" name="info">
        <el-card>
          <el-form :model="editForm" label-width="100px" style="max-width: 500px">
            <el-form-item label="用户名">
              <el-input v-model="editForm.username" />
            </el-form-item>
            <el-form-item label="账号">
              <el-input v-model="editForm.account" />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model="editForm.phone" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSaveInfo">保存修改</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-tab-pane>
      <el-tab-pane label="修改密码" name="password">
        <el-card>
          <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="100px" style="max-width: 500px">
            <el-form-item label="原密码" prop="oldPassword">
              <el-input v-model="pwdForm.oldPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="pwdForm.newPassword" type="password" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleChangePwd">修改密码</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-tab-pane>
      <el-tab-pane label="乘车人管理" name="passenger">
        <el-card>
          <div style="margin-bottom: 16px">
            <el-button type="primary" @click="showAddPassenger = true">新增乘车人</el-button>
          </div>
          <el-table :data="passengers" border stripe>
            <el-table-column prop="realName" label="姓名" />
            <el-table-column prop="idCard" label="身份证号" />
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button type="danger" size="small" @click="handleDeletePassenger(row.passengerId)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>
      <el-tab-pane label="账号注销" name="cancel">
        <el-card>
          <el-alert title="账号注销后不可恢复，请谨慎操作" type="warning" :closable="false" style="margin-bottom: 16px" />
          <el-button type="danger" @click="handleDeleteAccount">确认注销账号</el-button>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="showAddPassenger" title="新增乘车人" width="400px">
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
  </div>
</template>

<script>
import { getUserInfo, updateUserInfo, changePassword, deleteUser, listPassengers, addPassenger, deletePassenger } from '../api'
import { ElMessage, ElMessageBox } from 'element-plus'

export default {
  name: 'Profile',
  data() {
    return {
      activeTab: 'info',
      userId: null,
      editForm: { username: '', account: '', phone: '' },
      pwdForm: { oldPassword: '', newPassword: '' },
      pwdRules: {
        oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
        newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }]
      },
      passengers: [],
      showAddPassenger: false,
      passengerForm: { realName: '', idCard: '' },
      passengerRules: {
        realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
        idCard: [{ required: true, message: '请输入身份证号', trigger: 'blur' }]
      }
    }
  },
  async mounted() {
    this.userId = localStorage.getItem('userId')
    if (!this.userId) {
      this.$router.push('/login')
      return
    }
    await this.loadUserInfo()
    await this.loadPassengers()
  },
  methods: {
    async loadUserInfo() {
      const res = await getUserInfo(this.userId)
      this.editForm.username = res.data.username
      this.editForm.account = res.data.account
      this.editForm.phone = res.data.phone
    },
    async loadPassengers() {
      const res = await listPassengers(this.userId)
      this.passengers = res.data
    },
    async handleSaveInfo() {
      await updateUserInfo(this.userId, this.editForm)
      localStorage.setItem('username', this.editForm.username)
      ElMessage.success('修改成功')
      // 通知布局组件更新左侧导航用户名
      window.dispatchEvent(new CustomEvent('username-updated', { detail: this.editForm.username }))
      await this.loadUserInfo()
    },
    handleChangePwd() {
      this.$refs.pwdFormRef.validate(async (valid) => {
        if (!valid) return
        await changePassword(this.userId, this.pwdForm)
        ElMessage.success('密码修改成功')
        this.pwdForm.oldPassword = ''
        this.pwdForm.newPassword = ''
      })
    },
    handleAddPassenger() {
      this.$refs.passengerFormRef.validate(async (valid) => {
        if (!valid) return
        await addPassenger(this.userId, this.passengerForm)
        ElMessage.success('添加成功')
        this.showAddPassenger = false
        this.passengerForm.realName = ''
        this.passengerForm.idCard = ''
        await this.loadPassengers()
      })
    },
    async handleDeletePassenger(passengerId) {
      await ElMessageBox.confirm('确定删除该乘车人？', '提示', { type: 'warning' })
      await deletePassenger(this.userId, passengerId)
      ElMessage.success('删除成功')
      await this.loadPassengers()
    },
    handleDeleteAccount() {
      ElMessageBox.confirm('账号注销后不可恢复，确定继续？', '警告', { type: 'error' })
        .then(async () => {
          await deleteUser(this.userId)
          localStorage.clear()
          ElMessage.success('账号已注销')
          this.$router.push('/login')
        })
        .catch(() => {})
    }
  }
}
</script>

<style scoped>
.profile-page { padding: 20px 24px; }
.page-header { padding-bottom: 12px; }
.page-header h2 { margin: 0; color: #303133; }
.profile-tabs { background: #fff; padding: 16px; border-radius: 8px; }
</style>
