<script setup>
import AdminSidebar from '@/components/AdminSidebar.vue'
import ThemeToggle from '@/components/ThemeToggle.vue'
</script>

<template>
  <div class="admin-ori">
    <AdminSidebar />
    <main class="admin-main-ori">
      <ThemeToggle />
      <RouterView />
    </main>
  </div>
</template>
