<template>
  <el-card shadow="never">
    <template #header>
      <div class="card-title" style="margin-bottom: 0;">
        <strong>回答点评</strong>
        <el-tag type="success" v-if="evaluation">评分 {{ evaluation.score }}</el-tag>
      </div>
    </template>

    <el-empty v-if="!evaluation" description="提交回答后查看点评" />

    <div v-else class="stack">
      <div>
        <strong>优点</strong>
        <ul>
          <li v-for="item in evaluation.strengths" :key="item">{{ item }}</li>
        </ul>
      </div>
      <div>
        <strong>不足</strong>
        <ul>
          <li v-for="item in evaluation.weaknesses" :key="item">{{ item }}</li>
        </ul>
      </div>
      <div>
        <strong>改进建议</strong>
        <ul>
          <li v-for="item in evaluation.suggestions" :key="item">{{ item }}</li>
        </ul>
      </div>
      <div>
        <strong>容易被追问的点</strong>
        <ul>
          <li v-for="item in evaluation.followUpPoints" :key="item">{{ item }}</li>
        </ul>
      </div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import type { AnswerEvaluation } from '@/types/answer'

defineProps<{
  evaluation: AnswerEvaluation | null
}>()
</script>
