import { api } from './api';
import type { CustomerCreateRequest, CustomerResponse, CustomerStatus } from '../types/api';

export const customerService = {
  list: async () => {
    const response = await api.get<CustomerResponse[]>('/customers');
    return response.data;
  },
  create: async (request: CustomerCreateRequest) => {
    const response = await api.post<CustomerResponse>('/customers', request);
    return response.data;
  },
  updateStatus: async (id: number | string, status: CustomerStatus) => {
    const response = await api.patch<CustomerResponse>(`/customers/${id}/status`, null, { params: { status } });
    return response.data;
  },
};
