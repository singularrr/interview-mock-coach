export interface CategoryCount {
  category: string
  count: number
}

export interface InterviewSummary {
  summaryId: string
  sessionId: string
  overallScore: number
  weakAreas: CategoryCount[]
  frequentQuestionCategories: CategoryCount[]
  summaryAdvice: string
  markdownContent: string
}
