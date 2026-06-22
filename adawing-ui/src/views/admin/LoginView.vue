<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth.js'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const username = ref('')
const password = ref('')
const loading = ref(false)
const error = ref('')

async function login() {
  if (!username.value || !password.value) {
    error.value = 'Please enter username and password'
    return
  }
  loading.value = true
  error.value = ''
  try {
    await auth.login(username.value, password.value)
    const redirect = route.query.redirect || '/yusal/admin'
    router.replace(redirect)
  } catch (e) {
    error.value = e.message || 'Sign in failed'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-ori">
    <div class="login-card-ori">
      <h1>AdaWing</h1>
      <div v-if="error" class="error">{{ error }}</div>
      <input v-model="username" class="input-ori" placeholder="Username" @keyup.enter="login" />
      <input v-model="password" class="input-ori" type="password" placeholder="Password" @keyup.enter="login" />
      <button class="btn-ori" :disabled="loading" @click="login">
        {{ loading ? 'Signing in...' : 'Sign in' }}
      </button>
    </div>
  </div>
</template>
