export type Role =
  | 'SUPER_ADMIN'
  | 'OPS_ADMIN'
  | 'ORDER_OPS_ADMIN'
  | 'ORDER_OPERATOR'
  | 'SUPPORT_AGENT'
  | 'CUSTOMER_SUPPORT'
  | 'CATALOG_MANAGER'
  | 'WAREHOUSE_STAFF'
  | 'FINANCIAL_AUDITOR';

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

export interface ApiError {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  code?: string;
  correlationId?: string;
  timestamp?: string;
  invalidParams?: { name: string; reason: string }[];
  invalid_params?: { name: string; reason: string }[];
}

export interface ToastMessage {
  id: string;
  type: 'success' | 'error' | 'warning' | 'info';
  title: string;
  detail?: string;
  code?: string;
  correlationId?: string;
  durationMs?: number;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages?: number;
  size?: number;
  number?: number;
}

// ==========================================
// Exception Management Types (API-EXC-001, API-DASH-001)
// ==========================================
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
  activeOrdersCount?: number;
  lowStockCount?: number;
  systemHealth?: string;
}

// ==========================================
// Order Types (API-ORD-001..007)
// ==========================================
export type OrderStatus =
  | 'CREATED'
  | 'RESERVED'
  | 'PAID'
  | 'PACKING'
  | 'SHIPPED'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'PAYMENT_FAILED'
  | 'EXPIRED';

export interface OrderItem {
  id: string;
  orderId?: string;
  skuId: string;
  skuCode?: string;
  productName?: string;
  quantity: number;
  unitPrice: number;
  subtotalAmount: number;
}

export interface OrderTimelineEvent {
  id: string;
  orderId: string;
  fromStatus: string;
  toStatus: string;
  reason?: string;
  actorId?: string;
  actorRole?: string;
  createdAt: string;
}

export interface Order {
  id: string;
  customerId: string;
  status: OrderStatus;
  idempotencyKey?: string;
  shippingRecipientName?: string;
  shippingPhone?: string;
  shippingLine1?: string;
  shippingLine2?: string;
  shippingWard?: string;
  shippingDistrict?: string;
  shippingCity?: string;
  subtotalAmount: number;
  shippingFeeAmount: number;
  discountAmount: number;
  grandTotalAmount: number;
  currency: string;
  placedAt: string;
  items?: OrderItem[];
  timeline?: OrderTimelineEvent[];
}

// ==========================================
// Catalog & Pricing Types (API-CAT, API-PRC)
// ==========================================
export interface Category {
  id: string;
  name: string;
  slug: string;
  parentId?: string | null;
  isActive: boolean;
  sortOrder: number;
  createdAt: string;
}

export interface Sku {
  id: string;
  productId: string;
  skuCode: string;
  barcode?: string;
  attributes: Record<string, string>;
  basePrice?: number;
  status: string;
  createdAt: string;
}

export interface Product {
  id: string;
  name: string;
  slug: string;
  description?: string;
  categoryId?: string;
  categoryName?: string;
  status: string;
  skus?: Sku[];
  createdAt: string;
  updatedAt: string;
}

export interface PriceRecord {
  id: string;
  skuId: string;
  amount: number;
  currency: string;
  effectiveFrom: string;
  effectiveTo?: string;
  isPromotion: boolean;
  promotionName?: string;
}

// ==========================================
// Inventory Types (API-INV-001..004)
// ==========================================
export type AdjustmentReasonCode =
  | 'RESTOCK'
  | 'DAMAGED'
  | 'CORRECTION'
  | 'CYCLE_COUNT'
  | 'RETURN_RESTOCK'
  | 'DAMAGED_GOODS'
  | 'INVENTORY_AUDIT_DISCREPANCY'
  | 'RESTOCK_IMPORT'
  | 'EXPIRED_DISPOSAL';

export type OrderCancellationReasonCode =
  | 'CUSTOMER_REQUEST'
  | 'OUT_OF_STOCK'
  | 'PAYMENT_TIMEOUT'
  | 'SUSPECTED_FRAUD'
  | 'OPERATOR_OVERRIDE';

export interface SkuInventory {
  skuId: string;
  availableQuantity: number;
  reservedQuantity: number;
  totalQuantity: number;
  updatedAt?: string;
}

export interface InventoryAdjustmentLog {
  id: string;
  skuId: string;
  delta: number;
  previousQuantity: number;
  newQuantity: number;
  reasonCode: AdjustmentReasonCode;
  note?: string;
  actorId?: string;
  createdAt: string;
}

// ==========================================
// Payment Types (API-PAY-002, 003)
// ==========================================
export type PaymentTransactionStatus =
  | 'PENDING'
  | 'SUCCESS'
  | 'FAILED'
  | 'CANCELLED'
  | 'REFUNDED';

export type ResolutionType = 'ADJUST_LEDGER_CONFIRM' | 'FORCE_REFUND';

export interface PaymentTransaction {
  id: string;
  orderId: string;
  providerReference: string;
  amount: number;
  status: PaymentTransactionStatus;
  attemptedAt: string;
  confirmedAt?: string;
  evidenceReference?: string;
  reconciliationReason?: string;
}

// ==========================================
// Fulfillment Types (API-FUL-001..003)
// ==========================================
export type CarrierCode = 'VNPOST' | 'GHN' | 'GHTK' | 'VIETTELPOST';

export type ShipmentStatus =
  | 'CREATED'
  | 'PACKING'
  | 'READY_FOR_PICKUP'
  | 'HANDED_OVER'
  | 'IN_TRANSIT'
  | 'SHIPPED'
  | 'DELIVERED'
  | 'DELIVERY_FAILED'
  | 'RETURNED';

export interface Shipment {
  id: string;
  orderId: string;
  carrierName?: string;
  trackingCode?: string;
  status: ShipmentStatus;
  packedAt?: string;
  shippedAt?: string;
  deliveredAt?: string;
}

// ==========================================
// Dynamic Configuration Types (API-CFG-001)
// ==========================================
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

// ==========================================
// Audit & Compliance Types (API-AUDIT-001)
// ==========================================
export interface AuditLog {
  id: string;
  actorId: string;
  actorRole: string;
  actionType: string;
  entityType: string;
  entityId: string;
  beforeValue?: string;
  afterValue?: string;
  reason?: string;
  createdAt: string;
}

// ==========================================
// Customer Support & Notification Types (API-SUP-001, API-NOT)
// ==========================================
export interface NotificationLog {
  id: string;
  orderId?: string;
  recipient: string;
  channel: string;
  templateCode: string;
  status: string;
  retryCount: number;
  payload?: string;
  sentAt?: string;
  createdAt: string;
}

export interface CustomerUser {
  id: string;
  email: string;
  fullName: string;
  phone?: string;
  roles: string[];
  isActive: boolean;
  isLocked: boolean;
  createdAt: string;
}

export type SupportActionType = 'UNLOCK_ACCOUNT' | 'RESEND_NOTIFICATION' | 'INITIATE_CANCEL';

export interface SupportActionPayload {
  action_type: SupportActionType;
  reason: string;
  target_id?: string;
}

// ==========================================
// RBAC Management Types (FR-033, BR-018, API-RBAC-001)
// ==========================================
export type RbacPermission =
  | 'catalog:read'
  | 'catalog:write'
  | 'order:read'
  | 'order:update'
  | 'order:cancel'
  | 'inventory:read'
  | 'inventory:adjust'
  | 'payment:reconcile'
  | 'shipment:dispatch'
  | 'support:action'
  | 'system:config'
  | 'audit:read';

export interface CustomRoleDefinition {
  code: string;
  name: string;
  description: string;
  permissions: RbacPermission[];
  isSystem: boolean;
  createdAt: string;
}

export interface StaffAccount {
  id: string;
  fullName: string;
  email: string;
  roles: Role[];
  isActive: boolean;
  createdAt: string;
}

export type NotificationEventCode =
  | 'ORDER_CONFIRMED'
  | 'PAYMENT_SUCCESS'
  | 'SHIPMENT_DISPATCHED'
  | 'REFUND_PROCESSED';

export type NotificationChannel = 'EMAIL' | 'SMS' | 'PUSH';

export interface NotificationTemplate {
  id: string;
  templateCode: string;
  eventCode: NotificationEventCode;
  channel: NotificationChannel;
  language: string;
  title: string;
  content: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface MasterAttribute {
  id: string;
  code: string;
  name: string;
  values: string[];
}

export interface SkuMedia {
  id: string;
  url: string;
  isPrimary: boolean;
  displayOrder: number;
  fileName: string;
}

export interface ConfigVersionHistory {
  id: string;
  configKey: string;
  version: number;
  configValue: string;
  effectiveFrom: string;
  effectiveTo?: string;
  changedBy: string;
  reason: string;
  createdAt: string;
}

