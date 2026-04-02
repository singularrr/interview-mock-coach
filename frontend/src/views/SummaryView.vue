<template>
  <div class="page-shell">
    <div class="page-container">
      <PageHeader
        title="总结页"
        subtitle="展示总评分、薄弱项、高频题型和复试改进建议，并支持导出 Markdown。"
      >
        <template #extra>
          <div class="action-row">
            <el-button @click="backHome">返回首页</el-button>
            <el-button type="primary" plain :disabled="!summary" @click="exportMarkdownHandler">导出 Markdown</el-button>
          </div>
        </template>
      </PageHeader>

      <el-empty v-if="!summary" description="请先完成模拟面试并生成总结" />

      <div v-else class="stack">
        <el-row :gutter="16">
          <el-col :xs="24" :md="8">
            <SummaryCard title="总评分">
              <div style="font-size: 44px; font-weight: 700; color: var(--primary);">{{ summary.overallScore }}</div>
            </SummaryCard>
          </el-col>
          <el-col :xs="24" :md="8">
            <SummaryCard title="薄弱项">
              <el-space direction="vertical" alignment="start">
                <el-tag v-for="item in summary.weakAreas" :key="item.category" type="warning">
                  {{ formatCategory(item.category) }}：{{ item.count }}
                </el-tag>
              </el-space>
            </SummaryCard>
          </el-col>
          <el-col :xs="24" :md="8">
            <SummaryCard title="高频题型">
              <el-space direction="vertical" alignment="start">
                <el-tag v-for="item in summary.frequentQuestionCategories" :key="item.category" type="info">
                  {{ formatCategory(item.category) }}：{{ item.count }}
                </el-tag>
              </el-space>
            </SummaryCard>
          </el-col>
        </el-row>

        <SummaryCard title="改进建议">
          <p style="margin: 0; line-height: 1.8;">{{ summary.summaryAdvice }}</p>
        </SummaryCard>

        <SummaryCard title="Markdown 预览">
          <el-input :model-value="summary.markdownContent" type="textarea" :rows="14" readonly />
        </SummaryCard>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import SummaryCard from '@/components/SummaryCard.vue'
import { useInterviewStore } from '@/store/interview'

const router = useRouter()
const interviewStore = useInterviewStore()
const summary = computed(() => interviewStore.summary)

function backHome() {
  router.push('/')
}

function formatCategory(category: string) {
  const map: Record<string, string> = {
    SELF_INTRO: '自我介绍类',
    PROFESSIONAL_BASE: '专业基础类',
    PROJECT_EXPERIENCE: '项目经历类',
    ADMISSION_MOTIVE: '报考动机类',
    RESEARCH_DIRECTION: '研究方向类',
    FOLLOW_UP: '追问类',
  }
  return map[category] ?? category
}

function exportMarkdownHandler() {
  if (!summary.value) return
  const blob = new Blob([summary.value.markdownContent], { type: 'text/markdown;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `复试模拟总结-${summary.value.sessionId}.md`
  a.click()
  URL.revokeObjectURL(url)
  ElMessage.success('已导出 Markdown')
}
</script>
