<template>
  <el-card shadow="never">
    <div class="stack">
      <div class="action-row" style="justify-content: space-between;">
        <div class="action-row">
          <el-tag type="info">{{ categoryLabel }}</el-tag>
          <el-tag :type="difficultyType">{{ difficultyLabel }}</el-tag>
          <el-tag v-if="question.shouldFollowUp" type="warning">建议追问</el-tag>
        </div>
        <div class="muted-text">第 {{ question.orderIndex }} 题</div>
      </div>
      <div style="font-size: 18px; line-height: 1.7;">
        {{ displayContent }}
      </div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { DifficultyLevel, InterviewQuestion, QuestionCategory } from '@/types/question'

const props = defineProps<{
  question: InterviewQuestion
  followUpText?: string
}>()

const categoryMap: Record<QuestionCategory, string> = {
  SELF_INTRO: '自我介绍类',
  PROFESSIONAL_BASE: '专业基础类',
  PROJECT_EXPERIENCE: '项目经历类',
  ADMISSION_MOTIVE: '报考动机类',
  RESEARCH_DIRECTION: '研究方向类',
  FOLLOW_UP: '追问类',
}

const difficultyMap: Record<DifficultyLevel, string> = {
  EASY: '简单',
  MEDIUM: '中等',
  HARD: '困难',
}

const displayContent = computed(() => props.followUpText?.trim() || props.question.content)
const categoryLabel = computed(() => categoryMap[props.question.category])
const difficultyLabel = computed(() => difficultyMap[props.question.difficulty])
const difficultyType = computed(() => {
  if (props.question.difficulty === 'EASY') return 'success'
  if (props.question.difficulty === 'MEDIUM') return 'warning'
  return 'danger'
})
</script>
