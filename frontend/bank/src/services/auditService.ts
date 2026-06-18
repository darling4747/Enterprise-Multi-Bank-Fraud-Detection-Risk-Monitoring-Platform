import { api } from './api';
import type { AuditLogResponse } from '../types/api';

export const auditService = {
  listAuditLogs: async (bankId?: number) => {
    const response = await api.get<AuditLogResponse[]>('/audit-logs', { params: { bankId } });
    return response.data;
  },
};
