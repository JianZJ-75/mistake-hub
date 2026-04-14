export interface TagResp {
  id: number
  name: string
  type: string
  parentId: number
  accountId?: number | null
  createdTime: string
  children?: TagResp[]
}

export interface MistakeDetailResp {
  id: number
  title: string
  correctAnswer?: string
  titleImageUrl?: string
  answerImageUrl?: string
  reviewStage: number
  masteryLevel: number
  lastReviewTime?: string
  nextReviewTime?: string
  createdTime?: string
  updatedTime?: string
  tags?: TagResp[]
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

export interface ReviewTaskResp {
  mistakeId: number
  title: string
  titleImageUrl?: string
  correctAnswer?: string
  answerImageUrl?: string
  reviewStage: number
  masteryLevel: number
  overdueDays: number
  priority: number
  tags?: TagResp[]
  planStatus: string
  reviewPlanId: number
}

export interface ReviewSubmitResp {
  reviewStageBefore: number
  reviewStageAfter: number
  masteryBefore: number
  masteryAfter: number
  nextReviewTime: string
}

export interface ReviewProgressResp {
  totalToday: number
  completedToday: number
  skippedToday: number
  streakDays: number
}

export interface ReviewRecordResp {
  id: number
  isCorrect: number
  reviewStageBefore: number
  reviewStageAfter: number
  masteryBefore: number
  masteryAfter: number
  note?: string
  noteImageUrl?: string
  reviewTime: string
}

export interface StatsOverviewResp {
  totalMistakes: number
  masteredCount: number
  pendingReviewCount: number
  newThisWeek: number
}

export interface SubjectStatsResp {
  subject: string
  count: number
  avgMastery: number
}

export interface MasteryDistributionResp {
  notMastered: number
  learning: number
  mastered: number
}

export interface DailyCompletionResp {
  date: string
  totalPlanned: number
  completed: number
  completionRate: number | null
}

export interface StreakResp {
  currentStreak: number
  longestStreak: number
}

export interface MasteryTrendResp {
  date: string
  avgMastery: number
}

export interface AccountDetailResp {
  id: number
  code: string
  nickname: string
  avatarUrl?: string
  role: string
  dailyLimit: number
  createdTime: string
}
