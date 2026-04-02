<template>
  <el-button type="info" plain @click="$emit('fill')">一键填充示例数据</el-button>
</template>

<script setup lang="ts">
defineEmits<{
  fill: []
}>()
</script>
