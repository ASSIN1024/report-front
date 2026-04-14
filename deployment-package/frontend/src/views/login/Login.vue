<template>
  <div class="login-container">
    <div class="login-background">
      <div class="bg-shape shape-1"></div>
      <div class="bg-shape shape-2"></div>
      <div class="bg-shape shape-3"></div>
    </div>
    <el-card class="login-card">
      <div class="login-header">
        <div class="login-logo">
          <i class="el-icon-data-analysis"></i>
        </div>
        <h2 class="login-title">报表数据处理平台</h2>
        <p class="login-subtitle">Report Data Processing Platform</p>
      </div>
      <el-form :model="loginForm" :rules="rules" ref="loginForm" class="login-form">
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入用户名"
            prefix-icon="el-icon-user"
            clearable
          ></el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            prefix-icon="el-icon-lock"
            show-password
            @keyup.enter.native="handleLogin"
          ></el-input>
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            class="login-button"
            @click="handleLogin"
          >
            登 录
          </el-button>
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

          if (response.data && response.data.csrfToken) {
            setToken(response.data.csrfToken)
          }

          const user = response.data ? { userId: response.data.userId, username: response.data.username } : { username: this.loginForm.username }
          this.$store.commit('setUser', user)
          localStorage.setItem('user', JSON.stringify(user))

          this.$message.success('登录成功')
          this.$root.$emit('login-success')

          this.$router.push('/ftp')
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

<style scoped lang="scss">
@import '@/styles/variables.scss';

.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
  position: relative;
  overflow: hidden;
}

.login-background {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;

  .bg-shape {
    position: absolute;
    border-radius: 50%;
    opacity: 0.1;
  }

  .shape-1 {
    width: 400px;
    height: 400px;
    background: #409EFF;
    top: -100px;
    right: -100px;
    animation: float 6s ease-in-out infinite;
  }

  .shape-2 {
    width: 300px;
    height: 300px;
    background: #67C23A;
    bottom: -50px;
    left: -50px;
    animation: float 8s ease-in-out infinite reverse;
  }

  .shape-3 {
    width: 200px;
    height: 200px;
    background: #E6A23C;
    top: 50%;
    left: 50%;
    animation: float 10s ease-in-out infinite;
  }
}

@keyframes float {
  0%, 100% {
    transform: translateY(0) rotate(0deg);
  }
  50% {
    transform: translateY(-20px) rotate(5deg);
  }
}

.login-card {
  width: 420px;
  padding: 40px 35px;
  border-radius: $border-radius-xl;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  position: relative;
  z-index: 1;
}

.login-header {
  text-align: center;
  margin-bottom: 35px;
}

.login-logo {
  width: 70px;
  height: 70px;
  margin: 0 auto 20px;
  background: linear-gradient(135deg, #409EFF 0%, #337ecc 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 20px rgba(64, 158, 255, 0.3);

  i {
    font-size: 32px;
    color: #fff;
  }
}

.login-title {
  color: #303133;
  margin: 0 0 8px;
  font-size: 24px;
  font-weight: 600;
  letter-spacing: 2px;
}

.login-subtitle {
  color: #909399;
  margin: 0;
  font-size: 12px;
  letter-spacing: 1px;
}

.login-form {
  .el-form-item {
    margin-bottom: 22px;
  }

  .el-input {
    .el-input__inner {
      height: 44px;
      border-radius: $border-radius-base;
      border: 1px solid #dcdfe6;
      transition: all 0.3s ease;

      &:focus {
        border-color: #409EFF;
        box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.1);
      }
    }

    .el-input__prefix {
      i {
        line-height: 44px;
        color: #909399;
      }
    }
  }
}

.login-button {
  width: 100%;
  height: 44px;
  font-size: 16px;
  font-weight: 500;
  letter-spacing: 4px;
  border-radius: $border-radius-base;
  background: linear-gradient(135deg, #409EFF 0%, #337ecc 100%);
  border: none;
  box-shadow: 0 4px 15px rgba(64, 158, 255, 0.4);
  transition: all 0.3s ease;

  &:hover:not(:disabled) {
    transform: translateY(-2px);
    box-shadow: 0 6px 20px rgba(64, 158, 255, 0.5);
  }

  &:active:not(:disabled) {
    transform: translateY(0);
  }

  &:disabled {
    background: #a0cfff;
    box-shadow: none;
  }
}

@media (max-width: 480px) {
  .login-card {
    width: 90%;
    padding: 30px 25px;
  }

  .login-logo {
    width: 60px;
    height: 60px;

    i {
      font-size: 26px;
    }
  }

  .login-title {
    font-size: 20px;
  }
}
</style>