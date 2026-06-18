import { api } from './api';
import type { BeneficiaryCreateRequest, BeneficiaryResponse } from '../types/api';

export const beneficiaryService = {
  list: async () => {
    const response = await api.get<BeneficiaryResponse[]>('/beneficiaries');
    return response.data;
  },
  create: async (request: BeneficiaryCreateRequest) => {
    const response = await api.post<BeneficiaryResponse>('/beneficiaries', request);
    return response.data;
  },
  markUsed: async (id: number | string) => {
    const response = await api.patch<BeneficiaryResponse>(`/beneficiaries/${id}/mark-used`);
    return response.data;
  },
};
