<template>
  <div class="login-container">
    <el-card class="login-card">
      <h2 class="title">用户注册</h2>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="账号" prop="account">
          <el-input v-model="form.account" placeholder="请输入账号" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleRegister" style="width: 100%">注 册</el-button>
        </el-form-item>
        <el-form-item>
          <el-button @click="$router.push('/login')" style="width: 100%">返回登录</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script>
import { register } from '../api'
import { ElMessage } from 'element-plus'

export default {
  name: 'Register',
  data() {
    return {
      form: {
        username: '',
        account: '',
        password: '',
        phone: ''
      },
      rules: {
        username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
        account: [{ required: true, message: '请输入账号', trigger: 'blur' }],
        password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
        phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }]
      }
    }
  },
  methods: {
    handleRegister() {
      this.$refs.formRef.validate(async (valid) => {
        if (!valid) return
        try {
          await register(this.form)
          ElMessage.success('注册成功')
          this.$router.push('/login')
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
  margin-bottom: 24px;
}
</style>
