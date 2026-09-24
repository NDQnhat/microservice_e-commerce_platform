export type Role =
  | 'SUPER_ADMIN'
  | 'OPS_ADMIN'
  | 'SUPPORT_AGENT'
  | 'CATALOG_MANAGER'
  | 'WAREHOUSE_STAFF';

export interface AdminUser {
  id: string;
  email: string;
  fullName: string;
  roles: Role[];
  isActive: boolean;
}

export interface AuthState {
  user: AdminUser | null;
  accessToken: string | null;
  isAuthenticated: boolean;
}

export type ExceptionType =
  | 'PAYMENT_FAILED'
  | 'STUCK_ORDER'
  | 'NOTIFICATION_FAILED'
  | 'DUPLICATE_PAYMENT'
  | 'LATE_OR_STALE_PAYMENT_CALLBACK'
  | 'INVENTORY_DISCREPANCY'
  | 'UNKNOWN_PROCESSING_ERROR';

export type ExceptionRecordStatus = 'OPEN' | 'INVESTIGATING' | 'RESOLVED' | 'IGNORED';

export interface ExceptionRecord {
  id: string;
  exceptionType: ExceptionType;
  sourceService: string;
  referenceId: string;
  referenceType: string;
  errorCode: string;
  errorMessage: string;
  payload?: string;
  status: ExceptionRecordStatus;
  assignedTo?: string;
  resolvedBy?: string;
  resolvedAt?: string;
  resolutionAction?: string;
  resolutionNotes?: string;
  createdAt: string;
  updatedAt: string;
}

export interface DashboardSummary {
  totalOpenExceptions: number;
  totalInvestigatingExceptions: number;
  totalResolvedExceptions: number;
  openExceptionsByType: Record<string, number>;
}

export interface BusinessConfiguration {
  id: string;
  configKey: string;
  configValue: string;
  description?: string;
  version: number;
  isActive: boolean;
  effectiveFrom: string;
  effectiveTo?: string;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
}

export interface ApiError {
  type: string;
  title: string;
  status: number;
  detail: string;
  instance: string;
  timestamp: string;
  invalidParams?: { name: string; reason: string }[];
}
