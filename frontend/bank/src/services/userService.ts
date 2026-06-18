import { api } from './api';
import type { EnterpriseRoleType, TemporaryCredentialResponse, UserCreateRequest, UserResponse } from '../types/api';

export const userService = {
  listUsers: async () => {
    const response = await api.get<UserResponse[]>('/users');
    return response.data;
  },
  createUser: async (request: UserCreateRequest) => {
    const response = await api.post<UserResponse>('/users', request);
    return response.data;
  },
  createBankAdmin: async (request: UserCreateRequest) => {
    const response = await api.post<TemporaryCredentialResponse>('/users/bank-admins', request);
    return response.data;
  },
  createEmployee: async (request: UserCreateRequest) => {
    const response = await api.post<TemporaryCredentialResponse>('/users/employees', request);
    return response.data;
  },
  updateRoles: async (id: number | string, roles: EnterpriseRoleType[]) => {
    const response = await api.put<UserResponse>(`/users/${id}/roles`, { roles });
    return response.data;
  },
  setEnabled: async (id: number | string, enabled: boolean) => {
    const response = await api.patch<UserResponse>(`/users/${id}/status`, null, { params: { enabled } });
    return response.data;
  },
  deleteUser: async (id: number | string) => {
    await api.delete(`/users/${id}`);
  },
  resetPassword: async (id: number | string) => {
    const response = await api.post<TemporaryCredentialResponse>(`/users/${id}/reset-password`);
    return response.data;
  },
  unlockUser: async (id: number | string) => {
    const response = await api.post<UserResponse>(`/users/${id}/unlock`);
    return response.data;
  },
};
