import { api } from './api';
import type { AlertResponse, AlertStatus, FraudCaseResponse, FraudCaseStatus, FraudCaseUpdateRequest } from '../types/api';

export const fraudService = {
  getAlerts: async (status?: AlertStatus) => {
    const response = await api.get<AlertResponse[]>('/alerts', { params: { status } });
    return response.data;
  },
  updateAlertStatus: async (id: number | string, status: AlertStatus, assignedTo?: string) => {
    const response = await api.patch<AlertResponse>(`/alerts/${id}/status`, null, { params: { status, assignedTo } });
    return response.data;
  },
  getFraudCases: async () => {
    const response = await api.get<FraudCaseResponse[]>('/fraud/cases');
    return response.data;
  },
  getFraudCase: async (id: number | string) => {
    const response = await api.get<FraudCaseResponse>(`/fraud/cases/${id}`);
    return response.data;
  },
  updateFraudCaseStatus: async (id: number | string, status: FraudCaseStatus) => {
    const response = await api.patch<FraudCaseResponse>(`/fraud/cases/${id}/status`, null, { params: { status } });
    return response.data;
  },
  submitInvestigation: async (id: number | string, request: FraudCaseUpdateRequest) => {
    const response = await api.patch<FraudCaseResponse>(`/fraud/cases/${id}/investigation`, request);
    return response.data;
  },
};
