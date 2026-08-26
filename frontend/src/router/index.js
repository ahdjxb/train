import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue')
  },
  {
    path: '/admin/login',
    name: 'AdminLogin',
    component: () => import('../views/admin/AdminLogin.vue')
  },
  {
    path: '/admin',
    component: () => import('../components/AdminLayout.vue'),
    children: [
      { path: 'dashboard', name: 'AdminDashboard', component: () => import('../views/admin/TrainManage.vue') },
      { path: 'orders', name: 'AdminOrders', component: () => import('../views/admin/AllOrders.vue') },
      { path: 'stats', name: 'AdminStats', component: () => import('../views/admin/TicketStats.vue') },
      { path: 'users', name: 'AdminUsers', component: () => import('../views/admin/UserManage.vue') }
    ]
  },
  {
    path: '/',
    component: () => import('../components/BasicLayout.vue'),
    children: [
      { path: 'search', name: 'TicketSearch', component: () => import('../views/TicketSearch.vue') },
      { path: 'my-orders', name: 'MyOrders', component: () => import('../views/MyOrders.vue') },
      { path: 'profile', name: 'Profile', component: () => import('../views/Profile.vue') },
      { path: 'order/confirm', name: 'OrderConfirm', component: () => import('../views/OrderConfirm.vue') }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const userId = localStorage.getItem('userId')
  const role = localStorage.getItem('role')
  if (to.path.startsWith('/admin') && to.path !== '/admin/login') {
    if (!userId || role !== 'ADMIN') {
      next('/admin/login')
      return
    }
  } else if (to.path !== '/login' && to.path !== '/register' && to.path !== '/admin/login' && !to.path.startsWith('/admin')) {
    if (!userId) {
      next('/login')
      return
    }
  }
  next()
})

export default router
