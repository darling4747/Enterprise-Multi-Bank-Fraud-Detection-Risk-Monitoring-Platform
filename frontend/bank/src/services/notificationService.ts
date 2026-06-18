import { api } from './api';
import type { NotificationResponse } from '../types/api';

export const notificationService = {
  list: async () => {
    const response = await api.get<NotificationResponse[]>('/notifications');
    return response.data;
  },
  unreadCount: async () => {
    const response = await api.get<{ count: number }>('/notifications/unread-count');
    return response.data.count;
  },
  markRead: async (id: number | string) => {
    const response = await api.patch<NotificationResponse>(`/notifications/${id}/read`);
    return response.data;
  },
  markAllRead: async () => {
    const response = await api.patch<NotificationResponse[]>('/notifications/read-all');
    return response.data;
  },
};
