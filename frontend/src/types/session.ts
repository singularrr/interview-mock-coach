import type { CandidateProfile } from './candidate'
import type { InterviewQuestion } from './question'

export type SessionStatus = 'IN_PROGRESS' | 'COMPLETED'

export interface InterviewSession {
  sessionId: string
  status: SessionStatus
  currentQuestionIndex: number
  totalQuestions: number
  candidateProfile: CandidateProfile
  currentQuestion: InterviewQuestion | null
  questions: InterviewQuestion[]
}
