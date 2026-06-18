import { api } from './api';
import type { BranchCreateRequest, BranchResponse } from '../types/api';

export const branchService = {
  listBranches: async (bankId?: number) => {
    const response = await api.get<BranchResponse[]>('/branches', { params: { bankId } });
    return response.data;
  },
  createBranch: async (request: BranchCreateRequest) => {
    const response = await api.post<BranchResponse>('/branches', request);
    return response.data;
  },
  disableBranch: async (id: number | string) => {
    const response = await api.patch<BranchResponse>(`/branches/${id}/disable`);
    return response.data;
  },
};
