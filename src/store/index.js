import Vue from 'vue'
import Vuex from 'vuex'

Vue.use(Vuex)

// 从localStorage恢复用户信息
const savedUser = localStorage.getItem('user')
const initialUser = savedUser ? JSON.parse(savedUser) : null

export default new Vuex.Store({
  state: {
    user: initialUser
  },
  mutations: {
    setUser(state, user) {
      state.user = user
      // 持久化到localStorage
      if (user) {
        localStorage.setItem('user', JSON.stringify(user))
      } else {
        localStorage.removeItem('user')
      }
    }
  },
  actions: {
    setUser({ commit }, user) {
      commit('setUser', user)
    }
  },
  modules: {}
})
