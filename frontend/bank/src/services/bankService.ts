import { api } from './api';
import type { BankCreateRequest, BankResponse } from '../types/api';

export const bankService = {
  listBanks: async () => {
    const response = await api.get<BankResponse[]>('/banks');
    return response.data;
  },
  createBank: async (request: BankCreateRequest) => {
    const response = await api.post<BankResponse>('/banks', request);
    return response.data;
  },
  disableBank: async (id: number | string) => {
    const response = await api.patch<BankResponse>(`/banks/${id}/disable`);
    return response.data;
  },
};
