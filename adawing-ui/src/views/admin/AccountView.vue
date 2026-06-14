<script setup>
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth.js'
import { listMcpKeys, generateMcpKey, revokeMcpKey } from '@/api/mcpKeys.js'
import { formatDate } from '@/utils/formatDate.js'

const auth = useAuthStore()
const passwordForm = ref({ oldPassword: '', newPassword: '', confirm: '' })
const changing = ref(false)
const passwordMessage = ref('')

const keys = ref([])
const loadingKeys = ref(false)
const showGenerate = ref(false)
const keyForm = ref({ name: '', description: '' })
const generating = ref(false)
const plainKey = ref('')
const copied = ref(false)

async function changePassword() {
  passwordMessage.value = ''
  if (!passwordForm.value.oldPassword || !passwordForm.value.newPassword) {
    passwordMessage.value = 'Please fill in all fields'
    return
  }
  if (passwordForm.value.newPassword !== passwordForm.value.confirm) {
    passwordMessage.value = 'New passwords do not match'
    return
  }
  changing.value = true
  try {
    await auth.changePassword(passwordForm.value.oldPassword, passwordForm.value.newPassword)
    passwordMessage.value = 'Password updated'
    passwordForm.value = { oldPassword: '', newPassword: '', confirm: '' }
  } catch (e) {
    passwordMessage.value = e.message || 'Update failed'
  } finally {
    changing.value = false
  }
}

async function loadKeys() {
  loadingKeys.value = true
  try {
    const res = await listMcpKeys()
    keys.value = res.data || res || []
  } finally {
    loadingKeys.value = false
  }
}

async function generate() {
  if (!keyForm.value.name) return
  generating.value = true
  try {
    const res = await generateMcpKey(keyForm.value)
    plainKey.value = res.data || res
    copied.value = false
    keyForm.value = { name: '', description: '' }
    await loadKeys()
  } catch (e) {
    alert(e.message)
  } finally {
    generating.value = false
  }
}

async function copyPlainKey() {
  try {
    await navigator.clipboard.writeText(plainKey.value)
    copied.value = true
    setTimeout(() => { copied.value = false }, 1500)
  } catch {
    alert('Copy failed')
  }
}

async function revoke(id) {
  if (!confirm('Revoke this key?')) return
  await revokeMcpKey(id)
  await loadKeys()
}

onMounted(loadKeys)
</script>

<template>
  <div class="admin-account">
    <div class="admin-header-ori">
      <h1>Account</h1>
    </div>

    <section class="account-section-ori">
      <h2>Change Password</h2>
      <div class="form-group-ori">
        <label>Current Password</label>
        <input v-model="passwordForm.oldPassword" type="password" class="input-ori" placeholder="Current password" />
      </div>
      <div class="form-group-ori">
        <label>New Password</label>
        <input v-model="passwordForm.newPassword" type="password" class="input-ori" placeholder="New password" />
      </div>
      <div class="form-group-ori">
        <label>Confirm New Password</label>
        <input v-model="passwordForm.confirm" type="password" class="input-ori" placeholder="Confirm new password" />
      </div>
      <div class="account-actions">
        <button class="btn-ori btn-ori-primary" :disabled="changing" @click="changePassword">
          {{ changing ? 'Saving...' : 'Update Password' }}
        </button>
        <span v-if="passwordMessage" class="message-ori">{{ passwordMessage }}</span>
      </div>
    </section>

    <section class="account-section-ori">
      <div class="section-header-ori">
        <h2>MCP Keys</h2>
        <button class="btn-ori" @click="showGenerate = true">Generate</button>
      </div>

      <div v-if="showGenerate" class="key-form-ori">
        <input v-model="keyForm.name" class="input-ori" placeholder="Key name" />
        <input v-model="keyForm.description" class="input-ori" placeholder="Description" />
        <div class="key-form-actions">
          <button class="btn-ori btn-ori-primary" :disabled="generating" @click="generate">
            {{ generating ? 'Creating...' : 'Create' }}
          </button>
          <button class="btn-ori" @click="showGenerate = false; plainKey = ''">Cancel</button>
        </div>
      </div>

      <div v-if="plainKey" class="warning-ori">
        <div class="warning-head">
          <strong>Copy now — this key will not be shown again</strong>
          <button class="btn-ori btn-sm-ori" @click="copyPlainKey">{{ copied ? 'Copied' : 'Copy' }}</button>
        </div>
        <code>{{ plainKey }}</code>
      </div>

      <div v-if="loadingKeys" class="loading-ori">Loading...</div>
      <table v-else class="table-ori">
        <thead>
          <tr>
            <th>Name</th>
            <th>Description</th>
            <th>Last Used</th>
            <th>Created</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="key in keys" :key="key.id">
            <td>{{ key.name }}</td>
            <td>{{ key.description }}</td>
            <td>{{ formatDate(key.lastUsedAt) }}</td>
            <td>{{ formatDate(key.createTime) }}</td>
            <td class="actions">
              <button @click="revoke(key.id)">Revoke</button>
            </td>
          </tr>
          <tr v-if="keys.length === 0">
            <td colspan="5" class="empty-ori" style="padding: 40px 0;">No keys yet.</td>
          </tr>
        </tbody>
      </table>
    </section>
  </div>
</template>

<style scoped>
.account-section-ori {
  margin-bottom: 48px;
}

.account-section-ori h2 {
  font-size: 1.1rem;
  margin-bottom: 24px;
  color: var(--ink);
}

.account-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 8px;
}

.message-ori {
  color: var(--accent);
  font-size: 13px;
  letter-spacing: 0.05em;
}

.section-header-ori {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 32px;
}

.section-header-ori h2 {
  margin-bottom: 0;
}

.key-form-ori {
  padding: 20px;
  border: 1px solid var(--line);
  margin-bottom: 24px;
}

.key-form-ori input {
  margin-bottom: 16px;
}

.key-form-actions {
  display: flex;
  gap: 16px;
}

.warning-ori {
  padding: 16px 20px;
  background: var(--accent-faint);
  border: 1px solid var(--accent);
  border-radius: 0;
  margin-bottom: 24px;
  font-size: 13px;
  color: var(--ink);
}

.warning-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.warning-ori strong {
  color: var(--accent);
}

.warning-ori code {
  font-family: 'JetBrains Mono', monospace;
  word-break: break-all;
  font-size: 12px;
  color: var(--ink-light);
}
</style>
