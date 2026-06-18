import { api } from './api';
import type { DashboardChartResponse, DashboardStatsResponse } from '../types/api';

export const dashboardService = {
  getStats: async () => {
    const response = await api.get<DashboardStatsResponse>('/dashboard/stats');
    return response.data;
  },
  getCharts: async () => {
    const response = await api.get<DashboardChartResponse>('/dashboard/charts');
    return response.data;
  }
};
