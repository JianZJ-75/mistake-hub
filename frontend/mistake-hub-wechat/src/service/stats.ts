import request from '../utils/request'
import { StatsOverviewResp, SubjectStatsResp, MasteryDistributionResp, DailyCompletionResp, StreakResp, MasteryTrendResp, ForgettingCurveResp, MemoryHealthResp } from '../types'

export const statsOverview = (): Promise<StatsOverviewResp> =>
  request({ url: '/v1/stats/overview' })

export const statsMastery = (): Promise<MasteryDistributionResp> =>
  request({ url: '/v1/stats/mastery' })

export const statsStreak = (): Promise<StreakResp> =>
  request({ url: '/v1/stats/streak' })

export const statsSubject = (): Promise<SubjectStatsResp[]> =>
  request({ url: '/v1/stats/subject' })

export const statsDailyCompletion = (days = 30): Promise<DailyCompletionResp[]> =>
  request({ url: `/v1/stats/daily-completion?days=${days}` })

export const statsMasteryTrend = (days = 30): Promise<MasteryTrendResp[]> =>
  request({ url: `/v1/stats/mastery-trend?days=${days}` })

export const statsForgettingCurve = (mistakeId: number): Promise<ForgettingCurveResp> =>
  request({ url: '/v1/stats/forgetting-curve', data: { mistakeId } })

export const statsMemoryHealth = (): Promise<MemoryHealthResp> =>
  request({ url: '/v1/stats/memory-health' })
