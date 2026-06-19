import { api } from './api';
import type {
  AuthResponse,
  ChangePasswordRequest,
  LoginRequest,
  MfaSetupResponse,
  MfaVerifyRequest,
  ProfileResponse,
  ProfileUpdateRequest,
  UserPreferencesResponse,
  UserPreferencesUpdateRequest,
  UserSessionResponse,
} from '../types/api';

export const SESSION_ACTIVITY_KEY = 'securebank_last_activity_at';

const persistSession = (session: AuthResponse) => {
  if (!session.token) {
    return session;
  }
  localStorage.setItem('auth_token', session.token);
  if (session.refreshToken) {
    localStorage.setItem('refresh_token', session.refreshToken);
  }
  localStorage.setItem('auth_user', JSON.stringify(session));
  return session;
};

const mergeWithStoredSession = (session: AuthResponse) => {
  const raw = localStorage.getItem('auth_user');
  if (!raw) {
    return session;
  }
  const stored = JSON.parse(raw) as AuthResponse;
  return {
    ...session,
    token: stored.token || session.token,
    refreshToken: stored.refreshToken || session.refreshToken,
    sessionId: stored.sessionId || session.sessionId,
  };
};

export const authService = {
  login: async (credentials: LoginRequest) => {
    localStorage.removeItem('auth_token');
    localStorage.removeItem('refresh_token');
    localStorage.removeItem('auth_user');
    localStorage.removeItem(SESSION_ACTIVITY_KEY);
    const response = await api.post<AuthResponse>('/auth/login', credentials);
    if (response.data.mfaRequired) {
      return response.data;
    }
    return persistSession(response.data);
  },
  logout: () => {
    localStorage.removeItem('auth_token');
    localStorage.removeItem('refresh_token');
    localStorage.removeItem('auth_user');
    localStorage.removeItem(SESSION_ACTIVITY_KEY);
  },
  refresh: async () => {
    const refreshToken = localStorage.getItem('refresh_token');
    if (!refreshToken) {
      throw new Error('Missing refresh token');
    }
    const response = await api.post<AuthResponse>('/auth/refresh', { refreshToken });
    return persistSession(response.data);
  },
  getCurrentUser: async () => {
    const response = await api.get<AuthResponse>('/auth/me');
    return persistSession(response.data);
  },
  startMfaSetup: async () => {
    const response = await api.post<MfaSetupResponse>('/auth/mfa/setup');
    return response.data;
  },
  verifyMfaSetup: async (request: MfaVerifyRequest) => {
    const response = await api.post<AuthResponse>('/auth/mfa/verify', request);
    return persistSession(response.data);
  },
  disableMfa: async (request: MfaVerifyRequest) => {
    const response = await api.post<AuthResponse>('/auth/mfa/disable', request);
    return persistSession(response.data);
  },
  changePassword: async (request: ChangePasswordRequest) => {
    const response = await api.post<AuthResponse>('/auth/change-password', request);
    return persistSession(mergeWithStoredSession(response.data));
  },
  getProfile: async () => {
    const response = await api.get<ProfileResponse>('/auth/profile');
    return response.data;
  },
  updateProfile: async (request: ProfileUpdateRequest) => {
    const response = await api.put<ProfileResponse>('/auth/profile', request);
    const stored = authService.getStoredUser();
    if (stored) {
      localStorage.setItem('auth_user', JSON.stringify({ ...stored, fullName: response.data.fullName, email: response.data.email }));
    }
    return response.data;
  },
  getPreferences: async () => {
    const response = await api.get<UserPreferencesResponse>('/auth/preferences');
    return response.data;
  },
  updatePreferences: async (request: UserPreferencesUpdateRequest) => {
    const response = await api.put<UserPreferencesResponse>('/auth/preferences', request);
    const stored = authService.getStoredUser();
    if (stored) {
      localStorage.setItem('auth_user', JSON.stringify({
        ...stored,
        mfaEnabled: response.data.mfaEnabled,
        sessionTimeoutMinutes: response.data.sessionTimeoutMinutes,
      }));
    }
    return response.data;
  },
  getSessions: async () => {
    const response = await api.get<UserSessionResponse[]>('/auth/sessions');
    return response.data;
  },
  logoutOtherSessions: async () => {
    await api.post('/auth/sessions/logout-others');
  },
  getStoredUser: (): AuthResponse | null => {
    const raw = localStorage.getItem('auth_user');
    return raw ? JSON.parse(raw) as AuthResponse : null;
  },
  isAuthenticated: () => Boolean(localStorage.getItem('auth_token')),
  token: () => localStorage.getItem('auth_token'),
  apiBaseUrl: () => import.meta.env.VITE_API_BASE_URL || '/api',
};
