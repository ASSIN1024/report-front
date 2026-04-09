<template>
  <div class="login-container">
    <el-card class="login-card">
      <h2 class="login-title">报表数据处理平台</h2>
      <el-form :model="loginForm" :rules="rules" ref="loginForm">
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="用户名"
            prefix-icon="el-icon-user"
          ></el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="密码"
            prefix-icon="el-icon-lock"
            @keyup.enter.native="handleLogin"
          ></el-input>
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            class="login-button"
            @click="handleLogin"
          >登录</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script>
import { login } from '@/api/auth'
import { setToken } from '@/utils/auth'

export default {
  name: 'Login',
  data() {
    return {
      loginForm: {
        username: '',
        password: ''
      },
      rules: {
        username: [
          { required: true, message: '请输入用户名', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' }
        ]
      },
      loading: false
    }
  },
  methods: {
    handleLogin() {
      this.$refs.loginForm.validate(async valid => {
        if (!valid) {
          return
        }

        this.loading = true
        try {
          const response = await login(this.loginForm.username, this.loginForm.password)

          // 保存token
          if (response.data && response.data.token) {
            setToken(response.data.token)
          }

          // 保存用户信息到store和localStorage
          const user = response.data && response.data.user ? response.data.user : { username: this.loginForm.username }
          this.$store.commit('setUser', user)
          localStorage.setItem('user', JSON.stringify(user))

          this.$message.success('登录成功')

          // 跳转到首页
          this.$router.push('/')
        } catch (error) {
          console.error('登录失败:', error)
        } finally {
          this.loading = false
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
  width: 400px;
  padding: 20px;
}

.login-title {
  text-align: center;
  color: #303133;
  margin-bottom: 30px;
  font-size: 24px;
  font-weight: 600;
}

.login-button {
  width: 100%;
}
</style>
