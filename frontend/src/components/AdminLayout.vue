<template>
  <div class="layout-container">
    <el-aside width="200px" class="sidebar">
      <div class="logo">
        <el-icon size="24"><Setting /></el-icon>
        <span>管理后台</span>
      </div>
      <el-menu :default-active="activeMenu" router class="side-menu"
        background-color="#001529" text-color="rgba(255,255,255,0.65)" active-text-color="#fff">
        <el-menu-item index="/admin/dashboard">
          <el-icon><Tickets /></el-icon><span>车次管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/orders">
          <el-icon><List /></el-icon><span>全部订单</span>
        </el-menu-item>
        <el-menu-item index="/admin/stats">
          <el-icon><DataLine /></el-icon><span>出票统计</span>
        </el-menu-item>
        <el-menu-item index="/admin/users">
          <el-icon><User /></el-icon><span>用户管理</span>
        </el-menu-item>
      </el-menu>
      <div class="sidebar-footer">
        <div class="user-info">
          <el-avatar size="small" style="background:#f56c6c">A</el-avatar>
          <span class="username">{{ username }}</span>
        </div>
        <div class="footer-btns">
          <el-button text size="small" @click="showChangePwd = true" style="color:rgba(255,255,255,0.65)">
            <el-icon><Lock /></el-icon> 改密
          </el-button>
          <el-button text size="small" @click="logout" style="color:rgba(255,255,255,0.65)">
            <el-icon><SwitchButton /></el-icon> 退出
          </el-button>
        </div>
      </div>
    </el-aside>
    <el-main class="main-content">
      <router-view />
    </el-main>

    <el-dialog v-model="showChangePwd" title="修改密码" width="400px">
      <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="80px">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showChangePwd = false">取消</el-button>
        <el-button type="primary" @click="handleChangePwd">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { Setting, Tickets, List, DataLine, User, SwitchButton, Lock } from '@element-plus/icons-vue'
import { changeAdminPassword } from '../api'
import { ElMessage } from 'element-plus'

export default {
  name: 'AdminLayout',
  components: { Setting, Tickets, List, DataLine, User, SwitchButton, Lock },
  data() {
    return {
      username: '',
      showChangePwd: false,
      pwdForm: { oldPassword: '', newPassword: '' },
      pwdRules: {
        oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
        newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }]
      }
    }
  },
  computed: {
    activeMenu() { return this.$route.path }
  },
  mounted() {
    this.username = localStorage.getItem('username') || '管理员'
    const userId = localStorage.getItem('userId')
    const role = localStorage.getItem('role')
    if (!userId || role !== 'ADMIN') {
      this.$router.push('/admin/login')
    }
  },
  methods: {
    handleChangePwd() {
      this.$refs.pwdFormRef.validate(async (valid) => {
        if (!valid) return
        try {
          await changeAdminPassword(localStorage.getItem('userId'), this.pwdForm)
          ElMessage.success('密码修改成功')
          this.showChangePwd = false
          this.pwdForm = { oldPassword: '', newPassword: '' }
        } catch (e) {
          ElMessage.error(e.response?.data?.message || '修改失败')
        }
      })
    },
    logout() {
      localStorage.clear()
      this.$router.push('/admin/login')
    }
  }
}
</script>

<style scoped>
.layout-container { display: flex; height: 100vh; overflow: hidden; }
.sidebar { background: #001529; display: flex; flex-direction: column; height: 100vh; position: fixed; left: 0; top: 0; z-index: 100; }
.logo { height: 60px; display: flex; align-items: center; justify-content: center; gap: 8px; color: #fff; font-size: 18px; font-weight: bold; border-bottom: 1px solid rgba(255,255,255,0.1); }
.side-menu { flex: 1; border-right: none; }
.sidebar-footer { padding: 12px 16px; border-top: 1px solid rgba(255,255,255,0.1); display: flex; flex-direction: column; gap: 8px; align-items: center; }
.user-info { display: flex; align-items: center; gap: 8px; }
.username { color: rgba(255,255,255,0.85); font-size: 14px; }
.footer-btns { display: flex; gap: 8px; }
.main-content { margin-left: 200px; padding: 0; background: #f0f2f5; height: 100vh; overflow-y: auto; width: calc(100% - 200px); }
</style>
