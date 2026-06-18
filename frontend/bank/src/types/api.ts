export type RoleType = 'SUPER_ADMIN' | 'BRANCH_MANAGER' | 'FRAUD_ANALYST' | 'RISK_OFFICER' | 'AUDITOR';
export type EnterpriseRoleType = 'PLATFORM_ADMIN' | 'BANK_ADMIN' | RoleType;
export type UserStatus = 'ACTIVE' | 'INACTIVE' | 'LOCKED';
export type PasswordStatus = 'PERMANENT' | 'TEMPORARY' | 'EXPIRED';
export type TransactionStatus = 'PENDING' | 'APPROVED' | 'DECLINED' | 'REVIEW' | 'BLOCKED' | 'COMPLETED';
export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type FraudDecision = 'APPROVE' | 'REVIEW' | 'BLOCK';
export type AlertStatus = 'OPEN' | 'IN_REVIEW' | 'RESOLVED' | 'FALSE_POSITIVE';
export type FraudCaseStatus = 'OPEN' | 'UNDER_REVIEW' | 'ESCALATED' | 'CONFIRMED_FRAUD' | 'FALSE_POSITIVE' | 'CLOSED';
export type CasePriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type AuditStatus = 'SUCCESS' | 'FAILURE';
export type AccountType = 'INDIVIDUAL' | 'SMALL_BUSINESS' | 'BUSINESS' | 'CORPORATE' | 'GOVERNMENT';
export type CustomerType = 'RETAIL' | 'PREMIUM' | 'HIGH_NET_WORTH' | 'SME' | 'ENTERPRISE' | 'GOVERNMENT' | 'NEW_CUSTOMER';
export type DailyTransactionPattern = 'NORMAL' | 'UNUSUAL';
export type CustomerStatus = 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';
export type AccountStatus = 'ACTIVE' | 'FROZEN' | 'CLOSED';
export type TransactionChannel = 'MOBILE_BANKING' | 'INTERNET_BANKING' | 'UPI' | 'ATM' | 'BRANCH_BANKING' | 'CORPORATE_PORTAL' | 'CORE_BANKING';
export type SecurityIncidentType = 'IMPOSSIBLE_TRAVEL' | 'MULTIPLE_MFA_FAILURES' | 'ACCOUNT_TAKEOVER_ATTEMPT' | 'PRIVILEGE_ESCALATION' | 'MULTIPLE_FAILED_LOGINS' | 'NEW_DEVICE_LOGIN' | 'ADMIN_PRIVILEGE_CHANGE';
export type SecurityIncidentStatus = 'OPEN' | 'UNDER_REVIEW' | 'RESOLVED' | 'FALSE_POSITIVE';
export type NotificationType = 'NEW_FRAUD_ALERT' | 'PASSWORD_EXPIRY' | 'ACCOUNT_LOCKED' | 'DAILY_REPORT_READY' | 'SECURITY_INCIDENT' | 'CASE_ASSIGNED';

export interface AuthResponse {
  token?: string;
  tokenType: 'Bearer';
  refreshToken?: string;
  sessionId?: number;
  expiresAt?: string;
  userId: number;
  username: string;
  fullName: string;
  email: string;
  roles: EnterpriseRoleType[];
  bankId?: number;
  mustChangePassword: boolean;
  mfaEnabled: boolean;
  mfaRequired: boolean;
  sessionTimeoutMinutes: number;
}

export interface LoginRequest {
  username: string;
  password: string;
  mfaCode?: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export interface MfaSetupResponse {
  secret: string;
  otpAuthUri: string;
  qrCodeUrl: string;
}

export interface MfaVerifyRequest {
  code: string;
}

export interface ProfileResponse {
  fullName: string;
  email: string;
  profilePhotoDataUrl?: string;
}

export interface ProfileUpdateRequest {
  fullName: string;
  email: string;
  profilePhotoDataUrl?: string;
}

export interface UserPreferencesResponse {
  criticalAlertEmails: boolean;
  dailySummaryReport: boolean;
  sessionTimeoutMinutes: number;
  mfaEnabled: boolean;
}

export interface UserPreferencesUpdateRequest {
  criticalAlertEmails: boolean;
  dailySummaryReport: boolean;
  sessionTimeoutMinutes: number;
}

export interface UserSessionResponse {
  id: number;
  deviceId?: string;
  browser?: string;
  operatingSystem?: string;
  ipAddress?: string;
  userAgent?: string;
  loginTime: string;
  lastSeenAt: string;
  expiresAt: string;
  current: boolean;
}

export interface UserBaseRequest {
  username: string;
  email: string;
  password: string;
  fullName: string;
  roles?: EnterpriseRoleType[];
}

export interface UserCreateRequest extends UserBaseRequest {
  bankId?: number;
  branchId?: number;
  employeeId?: string;
  roles: EnterpriseRoleType[];
  enabled: boolean;
}

export interface UserResponse {
  id: number;
  username: string;
  email: string;
  fullName: string;
  bankId?: number;
  bankCode?: string;
  branchId?: number;
  branchCode?: string;
  employeeId?: string;
  enabled: boolean;
  status: UserStatus;
  roles: EnterpriseRoleType[];
  createdByUserId?: number;
  createdAt: string;
  passwordStatus: PasswordStatus;
  mustChangePassword: boolean;
  temporaryPasswordExpiresAt?: string;
  visibleTemporaryPassword?: string;
  lastLoginAt?: string;
  failedLoginAttempts: number;
  accountLockedUntil?: string;
}

export interface TemporaryCredentialResponse {
  user: UserResponse;
  temporaryPassword: string;
  expiresAt: string;
}

export interface BankResponse {
  id: number;
  code: string;
  name: string;
  headOffice?: string;
  headOfficeCity?: string;
  headOfficeState?: string;
  headOfficeCountry?: string;
  swiftCode?: string;
  licenseNumber?: string;
  contactEmail?: string;
  contactPhone?: string;
  status: 'ACTIVE' | 'DISABLED';
  createdAt: string;
  updatedAt: string;
}

export interface BankCreateRequest {
  code: string;
  name: string;
  headOffice?: string;
  headOfficeCity?: string;
  headOfficeState?: string;
  headOfficeCountry?: string;
  swiftCode?: string;
  licenseNumber?: string;
  contactEmail?: string;
  contactPhone?: string;
}

export interface BranchResponse {
  id: number;
  bankId: number;
  bankCode: string;
  code: string;
  name: string;
  ifscCode?: string;
  city?: string;
  state?: string;
  address?: string;
  managerName?: string;
  status: 'ACTIVE' | 'DISABLED';
  createdAt: string;
}

export interface BranchCreateRequest {
  bankId?: number;
  code: string;
  name: string;
  ifscCode?: string;
  city?: string;
  state?: string;
  address?: string;
  managerName?: string;
}

export interface CustomerCreateRequest {
  customerId: string;
  bankId?: number;
  branchId?: number;
  customerType: CustomerType;
  fullName: string;
  email?: string;
  phone?: string;
}

export interface CustomerResponse extends CustomerCreateRequest {
  id: number;
  bankId: number;
  bankCode: string;
  branchCode?: string;
  status: CustomerStatus;
  createdAt: string;
  updatedAt: string;
}

export interface AccountCreateRequest {
  customerId: string;
  accountNumber: string;
  accountType: AccountType;
  balance: number;
  currency?: string;
  branchId?: number;
}

export interface AccountResponse {
  id: number;
  customerId: string;
  customerName: string;
  bankId: number;
  bankCode: string;
  branchId?: number;
  branchCode?: string;
  accountNumber: string;
  accountType: AccountType;
  balance: number;
  currency: string;
  status: AccountStatus;
  createdAt: string;
  updatedAt: string;
}

export interface BeneficiaryCreateRequest {
  accountNumber: string;
  beneficiaryAccount: string;
  trustScore?: number;
}

export interface BeneficiaryResponse {
  id: number;
  accountId: number;
  accountNumber: string;
  beneficiaryAccount: string;
  trustScore: number;
  usageCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface SecurityAlertCreateRequest {
  eventType: SecurityIncidentType;
  severity: RiskLevel;
  description: string;
  userId?: number;
  bankId?: number;
  branchId?: number;
}

export interface SecurityAlertResponse {
  id: number;
  eventType: SecurityIncidentType;
  severity: RiskLevel;
  description: string;
  status: SecurityIncidentStatus;
  userId?: number;
  username?: string;
  bankId?: number;
  bankCode?: string;
  branchId?: number;
  branchCode?: string;
  createdAt: string;
  resolvedAt?: string;
}

export interface NotificationResponse {
  id: number;
  type: NotificationType;
  message: string;
  read: boolean;
  createdAt: string;
  readAt?: string;
}

export interface AuditLogResponse {
  id: number;
  eventType: string;
  performedByUserId?: number;
  targetUserId?: number;
  bankId?: number;
  ipAddress?: string;
  userAgent?: string;
  description: string;
  timestamp: string;
  status: AuditStatus;
}

export interface TransactionRequest {
  customerId: string;
  sourceAccount: string;
  destinationAccount: string;
  amount: number;
  currency: string;
  channel: TransactionChannel | string;
  merchantCategory?: string;
  country?: string;
  ipAddress?: string;
  deviceId?: string;
  accountType?: AccountType;
  customerType?: CustomerType;
  beneficiaryTrusted?: boolean;
  knownDevice?: boolean;
  knownLocation?: boolean;
  transactionHour?: number;
  dailyTransactionPattern?: DailyTransactionPattern;
  type?: string;
  step?: number;
  oldbalanceOrg?: number;
  newbalanceOrig?: number;
  oldbalanceDest?: number;
  newbalanceDest?: number;
  bankId?: number;
  branchId?: number;
}

export interface ExternalTransactionIngestRequest {
  bankId: number;
  branchId: number;
  customerId?: string;
  senderAccountNumber: string;
  receiverAccountNumber: string;
  amount: number;
  currency?: string;
  transactionType: string;
  channel: string;
  deviceId?: string;
  location?: string;
  ipAddress?: string;
  accountType?: AccountType;
  customerType?: CustomerType;
  beneficiaryTrusted?: boolean;
  knownDevice?: boolean;
  knownLocation?: boolean;
  transactionHour?: number;
  dailyTransactionPattern?: DailyTransactionPattern;
  step?: number;
  oldbalanceOrg?: number;
  newbalanceOrig?: number;
  oldbalanceDest?: number;
  newbalanceDest?: number;
}

export interface TransactionSummaryResponse {
  id: number;
  reference: string;
  customerId: string;
  bankId?: number;
  branchId?: number;
  amount: number;
  currency: string;
  status: TransactionStatus;
  riskLevel: RiskLevel;
  riskScore: number;
  createdAt: string;
}

export interface TransactionResponse extends TransactionSummaryResponse {
  sourceAccount: string;
  destinationAccount: string;
  channel: string;
  merchantCategory?: string;
  country?: string;
  ipAddress?: string;
  deviceId?: string;
  accountType: AccountType;
  customerType: CustomerType;
  beneficiaryTrusted: boolean;
  knownDevice: boolean;
  knownLocation: boolean;
  transactionHour?: number;
  dailyTransactionPattern: DailyTransactionPattern;
  type?: string;
  step?: number;
  oldbalanceOrg?: number;
  newbalanceOrig?: number;
  oldbalanceDest?: number;
  newbalanceDest?: number;
  bankCode?: string;
  branchCode?: string;
  fraudDecision: FraudDecision;
  riskSummary?: string;
  processedAt?: string;
}

export interface AlertResponse {
  id: number;
  fraudCaseId: number;
  transactionId: number;
  transactionReference: string;
  title: string;
  message: string;
  severity: RiskLevel;
  status: AlertStatus;
  assignedTo?: string;
  createdAt: string;
  resolvedAt?: string;
}

export interface FraudCaseResponse {
  id: number;
  transactionId: number;
  transactionReference: string;
  riskScore: number;
  riskLevel: RiskLevel;
  decision: FraudDecision;
  status: FraudCaseStatus;
  priority: CasePriority;
  reason: string;
  investigationNotes?: string;
  reviewedBy?: string;
  reviewedAt?: string;
  assignedToUserId?: number;
  assignedToUsername?: string;
  assignedByUserId?: number;
  assignedByUsername?: string;
  assignedAt?: string;
  createdAt: string;
  closedAt?: string;
}

export interface FraudCaseUpdateRequest {
  status: FraudCaseStatus;
  investigationNotes?: string;
  assignedToUserId?: number;
  priority?: CasePriority;
}

export interface DashboardStatsResponse {
  totalTransactions: number;
  pendingTransactions: number;
  reviewTransactions: number;
  blockedTransactions: number;
  openAlerts: number;
  activeFraudCases: number;
  highRiskTransactions: number;
  criticalRiskTransactions: number;
  last24hVolume: number;
}

export interface DashboardChartResponse {
  riskDistribution: Record<RiskLevel, number>;
  transactionStatusDistribution: Record<TransactionStatus, number>;
  alertStatusDistribution: Record<AlertStatus, number>;
}
