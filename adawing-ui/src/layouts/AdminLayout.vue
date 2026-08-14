<script setup>
import AdminSidebar from '@/components/AdminSidebar.vue'
import ThemeToggle from '@/components/ThemeToggle.vue'
import ThemePanel from '@/components/ThemePanel.vue'
</script>

<template>
  <div class="admin-ori">
    <AdminSidebar />
    <main class="admin-main-ori">
      <ThemeToggle />
      <ThemePanel />
      <RouterView />
    </main>
  </div>
</template>
