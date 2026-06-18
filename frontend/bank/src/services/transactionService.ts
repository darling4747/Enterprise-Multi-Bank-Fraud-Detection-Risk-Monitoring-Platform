import { api } from './api';
import type { ExternalTransactionIngestRequest, RiskLevel, TransactionRequest, TransactionResponse, TransactionStatus, TransactionSummaryResponse } from '../types/api';

export interface TransactionSearchParams {
  customerId?: string;
  status?: TransactionStatus;
  riskLevel?: RiskLevel;
  bankId?: number;
  branchId?: number;
  fromDate?: string;
  toDate?: string;
}

export const transactionService = {
  getTransactions: async (params?: TransactionSearchParams) => {
    const response = await api.get<TransactionSummaryResponse[]>('/transactions', { params });
    return response.data;
  },
  getTransactionById: async (id: number | string) => {
    const response = await api.get<TransactionResponse>(`/transactions/${id}`);
    return response.data;
  },
  createTransaction: async (request: TransactionRequest) => {
    const response = await api.post<TransactionResponse>('/transactions', request);
    return response.data;
  },
  ingestTransaction: async (request: ExternalTransactionIngestRequest) => {
    const response = await api.post<TransactionResponse>('/transactions/ingest', request);
    return response.data;
  },
  updateTransaction: async (id: number | string, request: TransactionRequest) => {
    const response = await api.put<TransactionResponse>(`/transactions/${id}`, request);
    return response.data;
  },
  updateStatus: async (id: number | string, status: TransactionStatus) => {
    const response = await api.patch<TransactionResponse>(`/transactions/${id}/status`, null, { params: { status } });
    return response.data;
  },
  deleteTransaction: async (id: number | string) => {
    await api.delete(`/transactions/${id}`);
  },
};
