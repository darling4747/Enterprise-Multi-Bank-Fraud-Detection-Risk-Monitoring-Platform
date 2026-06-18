import { authService } from '../services/authService';
import type { EnterpriseRoleType } from '../types/api';

export const hasAnyRole = (allowed: EnterpriseRoleType[]) => {
  const roles = authService.getStoredUser()?.roles ?? [];
  return roles.some((role) => allowed.includes(role));
};

export const canManageBanks = () => hasAnyRole(['PLATFORM_ADMIN', 'SUPER_ADMIN']);
export const canManageBranches = () => hasAnyRole(['PLATFORM_ADMIN', 'SUPER_ADMIN', 'BANK_ADMIN']);
export const canManageUsers = () => hasAnyRole(['PLATFORM_ADMIN', 'SUPER_ADMIN', 'BANK_ADMIN']);
export const canIssueBankAdmins = () => hasAnyRole(['PLATFORM_ADMIN', 'SUPER_ADMIN']);
export const canIssueEmployees = () => hasAnyRole(['BANK_ADMIN']);
export const canUseTransactionSimulator = () => hasAnyRole(['PLATFORM_ADMIN', 'SUPER_ADMIN', 'BANK_ADMIN']);
export const canWriteTransactions = () => hasAnyRole(['PLATFORM_ADMIN', 'SUPER_ADMIN', 'BRANCH_MANAGER', 'FRAUD_ANALYST']);
export const canDeleteTransactions = () => hasAnyRole(['PLATFORM_ADMIN', 'SUPER_ADMIN']);
export const canManageAlerts = () => hasAnyRole(['PLATFORM_ADMIN', 'SUPER_ADMIN', 'FRAUD_ANALYST']);
