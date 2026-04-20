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
  stranger: number
  beginner: number
  basic: number
  proficient: number
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
  avgMastery: number | null
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

export interface ForgettingCurveResp {
  mistakeId: number
  createdTime: string
  currentRetention: number
  currentStage: number
  nextReviewTime: string
  segments: CurveSegment[]
  prediction: CurvePrediction
}

export interface CurveSegment {
  startTime: string
  stability: number
  stageAfter: number
  isCorrect: boolean | null
  endTime: string
}

export interface CurvePrediction {
  fromTime: string
  fromRetention: number
  stability: number
  daysToShow: number
}

export interface MemoryHealthResp {
  overallRetention: number
  totalActive: number
  dangerCount: number
  warningCount: number
  safeCount: number
  distribution: RetentionBucket[]
  upcoming: UpcomingReview[]
}

export interface RetentionBucket {
  range: string
  count: number
}

export interface UpcomingReview {
  daysFromNow: number
  count: number
}
