import request from '../utils/request'
import { StatsOverviewResp, SubjectStatsResp, MasteryDistributionResp, DailyCompletionResp, StreakResp, MasteryTrendResp } from '../types'

export const statsOverview = (): Promise<StatsOverviewResp> =>
  request({ url: '/v1/stats/overview' })

export const statsMastery = (): Promise<MasteryDistributionResp> =>
  request({ url: '/v1/stats/mastery' })

export const statsStreak = (): Promise<StreakResp> =>
  request({ url: '/v1/stats/streak' })

export const statsSubject = (): Promise<SubjectStatsResp[]> =>
  request({ url: '/v1/stats/subject' })

export const statsDailyCompletion = (): Promise<DailyCompletionResp[]> =>
  request({ url: '/v1/stats/daily-completion' })

export const statsMasteryTrend = (): Promise<MasteryTrendResp[]> =>
  request({ url: '/v1/stats/mastery-trend' })
