import { api } from './api';

export interface MlHealthResponse {
  status: string;
  model_loaded: boolean;
  model_path: string;
  model_version: string;
  model_updated_at?: string;
  reload_interval_seconds: number;
}

export interface MlLogEntry {
  timestamp: string;
  eventType: string;
  status: string;
  method?: string;
  path?: string;
  statusCode?: number;
  durationMs?: number;
  fraudProbability?: number;
  mlRiskScore?: number;
  modelVersion?: string;
  message?: string;
}

export interface MlFeatureImportanceEntry {
  feature: string;
  importance: number;
  normalizedImportance?: number;
}

export interface MlFeatureImportanceResponse {
  model_version: string;
  feature_importance: MlFeatureImportanceEntry[];
  source: string;
}

export const mlMonitorService = {
  health: async () => {
    const response = await api.get<MlHealthResponse>('/ml/health');
    return response.data;
  },
  logs: async () => {
    const response = await api.get<{ count: number; logs: MlLogEntry[] }>('/ml/logs');
    return response.data;
  },
  featureImportance: async () => {
    const response = await api.get<MlFeatureImportanceResponse>('/ml/feature-importance');
    return response.data;
  },
};
