<template>
  <el-card shadow="never">
    <template #header>
      <div class="card-title" style="margin-bottom: 0;">
        <strong>{{ title }}</strong>
      </div>
    </template>
    <slot />
  </el-card>
</template>

<script setup lang="ts">
defineProps<{
  title: string
}>()
</script>
