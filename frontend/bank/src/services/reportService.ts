import { api } from './api';

export interface DailySummarySendResponse {
  status: string;
  recipientCount: number;
}

export const reportService = {
  sendDailySummaryNow: async () => {
    const response = await api.post<DailySummarySendResponse>('/reports/daily-summary/send-now');
    return response.data;
  },
};
