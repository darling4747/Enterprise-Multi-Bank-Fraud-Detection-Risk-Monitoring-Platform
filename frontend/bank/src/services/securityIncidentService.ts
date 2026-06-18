import { api } from './api';
import type { SecurityAlertCreateRequest, SecurityAlertResponse, SecurityIncidentStatus } from '../types/api';

export const securityIncidentService = {
  list: async () => {
    const response = await api.get<SecurityAlertResponse[]>('/security-incidents');
    return response.data;
  },
  create: async (request: SecurityAlertCreateRequest) => {
    const response = await api.post<SecurityAlertResponse>('/security-incidents', request);
    return response.data;
  },
  updateStatus: async (id: number | string, status: SecurityIncidentStatus) => {
    const response = await api.patch<SecurityAlertResponse>(`/security-incidents/${id}/status`, null, { params: { status } });
    return response.data;
  },
};
