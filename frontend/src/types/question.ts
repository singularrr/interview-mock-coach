export type QuestionCategory =
  | 'SELF_INTRO'
  | 'PROFESSIONAL_BASE'
  | 'PROJECT_EXPERIENCE'
  | 'ADMISSION_MOTIVE'
  | 'RESEARCH_DIRECTION'
  | 'FOLLOW_UP'

export type DifficultyLevel = 'EASY' | 'MEDIUM' | 'HARD'

export interface InterviewQuestion {
  questionId: string
  category: QuestionCategory
  difficulty: DifficultyLevel
  content: string
  shouldFollowUp: boolean
  orderIndex: number
}
