import { get, post } from './http'
import type { ApiResponse } from '@/types/api'
import type { CandidateProfile } from '@/types/candidate'
import type { InterviewSession } from '@/types/session'
import type { AnswerSubmission } from '@/types/answer'

export interface GenerateQuestionsRequest {
  candidateProfile: CandidateProfile
  questionCount?: number
}

export interface GenerateQuestionsResult extends InterviewSession {}

export interface EvaluateAnswerResult {
  sessionId: string
  questionId: string
  currentQuestionIndex: number
  totalQuestions: number
  evaluation: import('@/types/answer').AnswerEvaluation
}

export async function generateQuestions(request: GenerateQuestionsRequest) {
  return post<ApiResponse<GenerateQuestionsResult>, GenerateQuestionsRequest>('/api/interview/questions/generate', request)
}

export async function evaluateAnswer(request: AnswerSubmission) {
  return post<ApiResponse<EvaluateAnswerResult>, AnswerSubmission>('/api/interview/answer/evaluate', request)
}

export async function getSession(sessionId: string) {
  return get<ApiResponse<GenerateQuestionsResult>>(`/api/interview/sessions/${sessionId}`)
}
