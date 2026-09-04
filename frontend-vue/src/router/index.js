import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'
import EmployeePortal from '../views/EmployeePortal.vue'
import SreDashboard from '../views/SreDashboard.vue'

const routes = [
  { path: '/', name: 'Login', component: Login },
  { path: '/employee', name: 'EmployeePortal', component: EmployeePortal },
  { path: '/sre-console', name: 'SreDashboard', component: SreDashboard }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router