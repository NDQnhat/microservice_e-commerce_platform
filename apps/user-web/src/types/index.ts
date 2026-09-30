export interface ApiError {
  type: string;
  title: string;
  status: number;
  detail: string;
  instance: string;
  timestamp: string;
  code?: string;
  correlationId?: string;
  invalidParams?: { name: string; reason: string }[];
}

export interface User {
  id: string;
  email: string;
  fullName: string;
  phone?: string;
  status: 'ACTIVE' | 'LOCKED' | 'SUSPENDED';
  roles: string[];
}

export interface AuthResponse {
  accessToken: string;
  refreshToken?: string;
  tokenType?: string;
  expiresIn?: number;
  user: User;
}

export interface CustomerAddress {
  id: string;
  customerId?: string;
  recipientName: string;
  phone: string;
  line1: string;
  line2?: string;
  ward: string;
  district: string;
  city: string;
  isDefault: boolean;
  createdAt?: string;
}

export interface Category {
  id: string;
  name: string;
  code: string;
  parentId?: string | null;
  displayOrder: number;
  isActive: boolean;
  imageUrl?: string;
  subcategories?: Category[];
}

export interface SkuInventory {
  quantityOnHand: number;
  quantityReserved: number;
  quantityAvailable: number;
}

export interface Sku {
  id: string;
  productId?: string;
  skuCode: string;
  barcode?: string;
  price: number;
  salePrice?: number;
  isActive: boolean;
  attributes: Record<string, string>; // e.g. { Color: 'Black', Size: 'M' }
  inventory?: SkuInventory;
}

export interface Product {
  id: string;
  name: string;
  slug: string;
  description: string;
  category: Category;
  skus: Sku[];
  status: 'DRAFT' | 'ACTIVE' | 'ARCHIVED';
  mediaUrls: string[];
  rating?: number;
  reviewCount?: number;
  isFeatured?: boolean;
  isBestSeller?: boolean;
  isNewArrival?: boolean;
  createdAt?: string;
}

export interface CartItem {
  id: string;
  skuId: string;
  skuCode: string;
  productId: string;
  productName: string;
  productImage?: string;
  quantity: number;
  unitPrice: number;
  originalPrice?: number;
  totalPrice: number;
  attributes?: Record<string, string>;
  maxAvailableStock: number;
}

export interface Cart {
  id: string;
  userId?: string;
  items: CartItem[];
  subtotal: number;
  currency: string;
}

export type OrderStatus =
  | 'RESERVED'
  | 'PAID'
  | 'PACKING'
  | 'SHIPPED'
  | 'COMPLETED'
  | 'PAYMENT_FAILED'
  | 'EXPIRED'
  | 'CANCELLED';

export interface OrderItem {
  id: string;
  orderId?: string;
  skuId: string;
  skuCodeSnapshot: string;
  productNameSnapshot: string;
  productImageSnapshot?: string;
  attributeSnapshot?: Record<string, string>;
  quantity: number;
  unitPriceSnapshot: number;
  lineTotal: number;
}

export interface OrderTimelineEvent {
  id: string;
  orderId: string;
  fromStatus: OrderStatus | null;
  toStatus: OrderStatus;
  actorId?: string;
  actorType: 'CUSTOMER' | 'BACK_OFFICE' | 'SYSTEM';
  note?: string;
  occurredAt: string;
}

export interface Order {
  id: string;
  orderNumber: string;
  customerId: string;
  status: OrderStatus;
  subtotalAmount: number;
  shippingFeeAmount: number;
  discountAmount: number;
  grandTotalAmount: number;
  currency: string;
  items: OrderItem[];
  shippingAddress: CustomerAddress; // Snapshotted address
  timeline?: OrderTimelineEvent[];
  paymentMethod?: 'MOCK_GATEWAY' | 'VNPAY' | 'COD' | 'BANK_TRANSFER';
  idempotencyKey?: string;
  createdAt: string;
  updatedAt?: string;
}

export interface PaymentTransaction {
  id: string;
  orderId: string;
  providerReference?: string;
  amount: number;
  status: 'INITIATED' | 'SUCCEEDED' | 'FAILED' | 'TIMEOUT';
  createdAt: string;
  attemptedAt?: string;
}

export interface PaymentCallbackPayload {
  order_id: string;
  provider_reference: string;
  result: 'SUCCESS' | 'FAILED';
  amount: number;
}

export interface PaginatedResult<T> {
  items: T[];
  page: number;
  size: number;
  total: number;
  totalPages: number;
}

export interface ProductFilterParams {
  categoryId?: string;
  search?: string;
  minPrice?: number;
  maxPrice?: number;
  color?: string;
  size?: string;
  sortBy?: 'price-asc' | 'price-desc' | 'newest' | 'rating';
  inStockOnly?: boolean;
  page?: number;
  pageSize?: number;
}

export interface BusinessConfiguration {
  free_shipping_threshold: number;
  standard_shipping_fee: number;
  reservation_timeout_minutes: number;
}

export interface OrderNotification {
  id: string;
  orderId: string;
  orderNumber: string;
  type: 'OrderCreated' | 'PaymentSucceeded' | 'OrderShipped' | 'OrderCancelled' | 'OrderExpired';
  title: string;
  message: string;
  timestamp: string;
  read: boolean;
}

