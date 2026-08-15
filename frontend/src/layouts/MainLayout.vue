<script setup lang="ts">
import SideNav from '@/components/SideNav.vue';
import TopNav from '@/components/TopNav.vue';
</script>

<template>
  <div class="layout">
    <SideNav />
    <div class="layout-main">
      <TopNav />
      <main class="layout-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<style scoped>
.layout { min-height: 100vh; }
.layout-main { margin-left: var(--sidebar-w); display: flex; flex-direction: column; min-height: 100vh; }
.layout-content { flex: 1; }
</style>
