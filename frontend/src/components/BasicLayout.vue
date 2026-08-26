<template>
  <div class="layout-container">
    <!-- 左侧导航栏 -->
    <el-aside width="200px" class="sidebar">
      <div class="logo">
        <el-icon size="24"><Tickets /></el-icon>
        <span>火车售票</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        class="side-menu"
        background-color="#001529"
        text-color="rgba(255,255,255,0.65)"
        active-text-color="#fff"
      >
        <el-menu-item index="/search">
          <el-icon><Search /></el-icon>
          <span>车票查询</span>
        </el-menu-item>
        <el-menu-item index="/my-orders">
          <el-icon><List /></el-icon>
          <span>我的订单</span>
        </el-menu-item>
        <el-menu-item index="/profile">
          <el-icon><User /></el-icon>
          <span>个人中心</span>
        </el-menu-item>
      </el-menu>
      <div class="sidebar-footer">
        <div class="user-info">
          <el-avatar size="small" style="background:#409eff">{{ userInitial }}</el-avatar>
          <span class="username">{{ username }}</span>
        </div>
        <el-button text size="small" @click="logout" style="color:rgba(255,255,255,0.65)">
          <el-icon><SwitchButton /></el-icon> 退出
        </el-button>
      </div>
    </el-aside>

    <!-- 主内容区 -->
    <el-main class="main-content">
      <router-view />
    </el-main>
  </div>
</template>

<script>
import { Search, List, User, SwitchButton, Tickets } from '@element-plus/icons-vue'

export default {
  name: 'BasicLayout',
  components: { Search, List, User, SwitchButton, Tickets },
  data() {
    return {
      username: ''
    }
  },
  computed: {
    activeMenu() {
      return this.$route.path
    },
    userInitial() {
      return this.username ? this.username.charAt(0).toUpperCase() : 'U'
    }
  },
  mounted() {
    this.username = localStorage.getItem('username') || '用户'
    this.userId = localStorage.getItem('userId')
    if (!this.userId) {
      this.$router.push('/login')
    }
    // 监听用户名更新事件
    window.addEventListener('username-updated', this.onUsernameUpdated)
  },
  beforeUnmount() {
    window.removeEventListener('username-updated', this.onUsernameUpdated)
  },
  methods: {
    onUsernameUpdated(e) {
      this.username = e.detail || this.username
    },
    logout() {
      localStorage.clear()
      this.$router.push('/login')
    }
  }
}
</script>

<style scoped>
.layout-container {
  display: flex;
  height: 100vh;
  overflow: hidden;
}
.sidebar {
  background: #001529;
  display: flex;
  flex-direction: column;
  height: 100vh;
  position: fixed;
  left: 0;
  top: 0;
  z-index: 100;
}
.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #fff;
  font-size: 18px;
  font-weight: bold;
  border-bottom: 1px solid rgba(255,255,255,0.1);
}
.side-menu {
  flex: 1;
  border-right: none;
}
.sidebar-footer {
  padding: 12px 16px;
  border-top: 1px solid rgba(255,255,255,0.1);
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: center;
}
.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
}
.username {
  color: rgba(255,255,255,0.85);
  font-size: 14px;
}
.main-content {
  margin-left: 200px;
  padding: 0;
  background: #f0f2f5;
  height: 100vh;
  overflow-y: auto;
  width: calc(100% - 200px);
}
</style>
