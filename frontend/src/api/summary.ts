import { post } from './http'
import type { ApiResponse } from '@/types/api'
import type { InterviewSummary } from '@/types/summary'

export async function generateSummary(sessionId: string) {
  return post<ApiResponse<{ sessionId: string; summary: InterviewSummary }>, { sessionId: string }>(
    '/api/interview/summary/generate',
    { sessionId },
  )
}
