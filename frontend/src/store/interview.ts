import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import type { CandidateProfile } from '@/types/candidate'
import type { InterviewQuestion } from '@/types/question'
import type { AnswerEvaluation } from '@/types/answer'
import type { InterviewSummary } from '@/types/summary'
import { evaluateAnswer, generateQuestions } from '@/api/interview'
import { generateSummary } from '@/api/summary'
import { useUiStore } from './ui'

export const useInterviewStore = defineStore('interview', () => {
  const uiStore = useUiStore()
  const profile = ref<CandidateProfile>({
    school: '',
    major: '',
    researchDirection: '',
    resumePoints: '',
  })
  const sessionId = ref('')
  const questions = ref<InterviewQuestion[]>([])
  const currentIndex = ref(0)
  const currentAnswer = ref('')
  const latestEvaluation = ref<AnswerEvaluation | null>(null)
  const summary = ref<InterviewSummary | null>(null)
  const loading = ref(false)
  const evaluating = ref(false)
  const summarizing = ref(false)

  const currentQuestion = computed(() => questions.value[currentIndex.value] ?? null)
  const progressText = computed(() => {
    if (!questions.value.length) {
      return '0 / 0'
    }
    return `${currentIndex.value + 1} / ${questions.value.length}`
  })

  function fillExampleProfile() {
    profile.value = {
      school: '浙江大学',
      major: '软件工程',
      researchDirection: '大模型与智能体',
      resumePoints: '本科阶段完成了推荐系统和知识图谱相关项目；有一段后端开发实习经历；参加过校级算法竞赛。',
    }
  }

  async function createInterview(questionCount = 8) {
    loading.value = true
    try {
      const response = await generateQuestions({
        candidateProfile: profile.value,
        questionCount,
      })
      sessionId.value = response.data.sessionId
      questions.value = response.data.questions ?? []
      currentIndex.value = 0
      currentAnswer.value = ''
      latestEvaluation.value = null
      summary.value = null
      uiStore.clearFollowUpText()
      return response.data
    } finally {
      loading.value = false
    }
  }

  async function submitCurrentAnswer() {
    if (!sessionId.value || !currentQuestion.value) {
      return
    }
    evaluating.value = true
    try {
      const response = await evaluateAnswer({
        sessionId: sessionId.value,
        questionId: currentQuestion.value.questionId,
        answerText: currentAnswer.value,
      })
      latestEvaluation.value = response.data.evaluation
      currentIndex.value = Math.min(response.data.currentQuestionIndex - 1, questions.value.length - 1)
      return response.data
    } finally {
      evaluating.value = false
    }
  }

  function nextQuestion() {
    if (currentIndex.value < questions.value.length - 1) {
      currentIndex.value += 1
      currentAnswer.value = ''
      latestEvaluation.value = null
    }
  }

  function skipQuestion() {
    if (currentIndex.value < questions.value.length - 1) {
      currentIndex.value += 1
      currentAnswer.value = ''
      latestEvaluation.value = null
    }
  }

  function askFollowUp() {
    if (!currentQuestion.value || !latestEvaluation.value?.followUpPoints?.length) {
      return ''
    }
    return latestEvaluation.value.followUpPoints[0]
  }

  async function submitSummary() {
    if (!sessionId.value) {
      return
    }
    summarizing.value = true
    try {
      const response = await generateSummary(sessionId.value)
      summary.value = response.data.summary
      return response.data.summary
    } finally {
      summarizing.value = false
    }
  }

  function applyLoadedSession(data: {
    sessionId: string
    currentQuestionIndex: number
    questions: InterviewQuestion[]
  }) {
    sessionId.value = data.sessionId
    questions.value = data.questions ?? []
    currentIndex.value = Math.max(0, (data.currentQuestionIndex ?? 1) - 1)
  }

  function resetInterview() {
    sessionId.value = ''
    questions.value = []
    currentIndex.value = 0
    currentAnswer.value = ''
    latestEvaluation.value = null
    summary.value = null
    uiStore.clearFollowUpText()
  }

  return {
    profile,
    sessionId,
    questions,
    currentIndex,
    currentQuestion,
    currentAnswer,
    latestEvaluation,
    summary,
    loading,
    evaluating,
    summarizing,
    progressText,
    fillExampleProfile,
    createInterview,
    submitCurrentAnswer,
    nextQuestion,
    skipQuestion,
    askFollowUp,
    submitSummary,
    applyLoadedSession,
    resetInterview,
  }
})
