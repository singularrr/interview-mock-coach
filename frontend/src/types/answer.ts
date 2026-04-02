export interface AnswerEvaluation {
  evaluationId: string
  questionId: string
  score: number
  strengths: string[]
  weaknesses: string[]
  suggestions: string[]
  followUpPoints: string[]
  shouldFollowUp: boolean
  answerText: string
}

export interface AnswerSubmission {
  sessionId: string
  questionId: string
  answerText: string
}
