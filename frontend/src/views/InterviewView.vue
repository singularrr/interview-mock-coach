<template>
  <div class="page-shell">
    <div class="page-container">
      <PageHeader
        title="模拟问答"
        subtitle="一题一答，提交后查看评分与点评。当前按钮中的“下一题 / 跳过 / 重新追问”由前端控制，适合演示。"
      >
        <template #extra>
          <div class="action-row">
            <el-button @click="backHome">返回首页</el-button>
            <el-button type="success" plain :loading="summarizing" @click="finishInterview">生成总结</el-button>
          </div>
        </template>
      </PageHeader>

      <div v-if="currentQuestion" class="two-col">
        <div class="stack">
          <QuestionCard :question="currentQuestion" :follow-up-text="followUpText" />
          <el-card shadow="never">
            <template #header><strong>答题区</strong></template>
            <el-input
              v-model="currentAnswer"
              type="textarea"
              :rows="10"
              resize="none"
              placeholder="在这里输入你的回答"
            />
            <div class="action-row section-gap">
              <el-button type="primary" :loading="evaluating" @click="submitAnswerHandler">提交回答</el-button>
              <el-button @click="nextHandler">下一题</el-button>
              <el-button @click="skipHandler">跳过</el-button>
              <el-button @click="followUpHandler">重新追问</el-button>
            </div>
            <div class="section-gap muted-text">
              当前进度：{{ progressText }}
            </div>
          </el-card>
        </div>

        <div class="stack">
          <EvaluationCard :evaluation="latestEvaluation" />
          <el-card shadow="never">
            <template #header><strong>会话信息</strong></template>
            <div class="stack">
              <div><strong>学校：</strong>{{ profile.school }}</div>
              <div><strong>专业：</strong>{{ profile.major }}</div>
              <div><strong>研究方向：</strong>{{ profile.researchDirection }}</div>
              <div><strong>当前题目：</strong>{{ progressText }}</div>
            </div>
          </el-card>
        </div>
      </div>

      <el-empty v-else description="请先返回首页生成模拟面试" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import QuestionCard from '@/components/QuestionCard.vue'
import EvaluationCard from '@/components/EvaluationCard.vue'
import { useInterviewStore } from '@/store/interview'
import { useUiStore } from '@/store/ui'

const router = useRouter()
const interviewStore = useInterviewStore()
const uiStore = useUiStore()

const currentQuestion = computed(() => interviewStore.currentQuestion)
const currentAnswer = computed({
  get: () => interviewStore.currentAnswer,
  set: (value: string) => {
    interviewStore.currentAnswer = value
  },
})
const latestEvaluation = computed(() => interviewStore.latestEvaluation)
const progressText = computed(() => interviewStore.progressText)
const followUpText = computed(() => uiStore.followUpText)
const evaluating = computed(() => interviewStore.evaluating)
const summarizing = computed(() => interviewStore.summarizing)
const profile = computed(() => interviewStore.profile)

function nextHandler() {
  interviewStore.nextQuestion()
  uiStore.clearFollowUpText()
  interviewStore.currentAnswer = ''
}

function skipHandler() {
  interviewStore.skipQuestion()
  uiStore.clearFollowUpText()
  interviewStore.currentAnswer = ''
}

function followUpHandler() {
  const followUp = interviewStore.askFollowUp()
  if (!followUp) {
    ElMessage.info('当前题目还没有追问点')
    return
  }
  uiStore.setFollowUpText(followUp)
  ElMessage.success('已切换为追问展示')
}

async function submitAnswerHandler() {
  if (!interviewStore.currentAnswer.trim()) {
    ElMessage.warning('请先输入回答内容')
    return
  }
  try {
    await interviewStore.submitCurrentAnswer()
    ElMessage.success('回答已提交')
  } catch {
    ElMessage.error('提交回答失败')
  }
}

async function finishInterview() {
  try {
    await interviewStore.submitSummary()
    await router.push('/summary')
  } catch {
    ElMessage.error('生成总结失败')
  }
}

function backHome() {
  router.push('/')
}
</script>
