<template>
  <div class="login-container">
    <el-card class="login-card">
      <h2 class="title">火车票售票系统</h2>
      <p class="subtitle">用户登录</p>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="账号" prop="account">
          <el-input v-model="form.account" placeholder="请输入账号" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleLogin" style="width: 100%">登 录</el-button>
        </el-form-item>
        <el-form-item>
          <el-button @click="$router.push('/register')" style="width: 100%">注 册</el-button>
        </el-form-item>
        <el-form-item>
          <el-button text @click="$router.push('/admin/login')">管理员登录</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script>
import { userLogin } from '../api'
import { ElMessage } from 'element-plus'

export default {
  name: 'Login',
  data() {
    return {
      form: {
        account: '',
        password: ''
      },
      rules: {
        account: [{ required: true, message: '请输入账号', trigger: 'blur' }],
        password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
      }
    }
  },
  methods: {
    handleLogin() {
      this.$refs.formRef.validate(async (valid) => {
        if (!valid) return
        try {
          const res = await userLogin(this.form)
          localStorage.setItem('userId', res.data.userId)
          localStorage.setItem('username', res.data.username)
          localStorage.setItem('account', res.data.account)
          localStorage.setItem('role', res.data.role || 'NORMAL')
          ElMessage.success('登录成功')
          this.$router.push('/search')
        } catch (e) {
          // error handled by interceptor
        }
      })
    }
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.login-card {
  width: 420px;
  padding: 20px;
}
.title {
  text-align: center;
  margin-bottom: 8px;
}
.subtitle {
  text-align: center;
  color: #999;
  margin-bottom: 24px;
}
</style>
