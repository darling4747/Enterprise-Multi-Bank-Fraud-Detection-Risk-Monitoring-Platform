import { api } from './api';
import type { AccountCreateRequest, AccountResponse, AccountStatus } from '../types/api';

export const accountService = {
  list: async () => {
    const response = await api.get<AccountResponse[]>('/accounts');
    return response.data;
  },
  create: async (request: AccountCreateRequest) => {
    const response = await api.post<AccountResponse>('/accounts', request);
    return response.data;
  },
  updateStatus: async (id: number | string, status: AccountStatus) => {
    const response = await api.patch<AccountResponse>(`/accounts/${id}/status`, null, { params: { status } });
    return response.data;
  },
  approveBranchOperation: async (id: number | string) => {
    const response = await api.post<AccountResponse>(`/accounts/${id}/approve-branch-operation`);
    return response.data;
  },
};
