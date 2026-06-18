export interface User {
  id: string;
  name: string;
  email: string;
  role: 'ANALYST' | 'ADMIN' | 'RISK_OFFICER';
}

export interface Transaction {
  id: string;
  accountId: string;
  amount: number;
  currency: string;
  timestamp: string;
  status: 'PENDING' | 'COMPLETED' | 'FAILED' | 'BLOCKED';
  merchantDetails: string;
  location: string;
  riskScore: number;
}

export interface Alert {
  id: string;
  transactionId: string;
  timestamp: string;
  severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  status: 'NEW' | 'INVESTIGATING' | 'RESOLVED' | 'FALSE_POSITIVE';
  description: string;
}

export interface DashboardStats {
  totalTransactions: number;
  fraudAlerts: number;
  blockedTransactions: number;
  totalVolume: number;
}

export interface FraudTrend {
  date: string;
  alerts: number;
  blocked: number;
}
