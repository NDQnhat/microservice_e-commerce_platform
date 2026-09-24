export interface ApiError {
  type: string;
  title: string;
  status: number;
  detail: string;
  instance: string;
  timestamp: string;
  invalidParams?: { name: string; reason: string }[];
}

export interface User {
  id: string;
  email: string;
  fullName: string;
  phone?: string;
  status: 'ACTIVE' | 'SUSPENDED';
  roles: string[];
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: User;
}

export interface Category {
  id: string;
  name: string;
  code: string;
  parentId?: string;
  displayOrder: number;
  isActive: boolean;
}

export interface Sku {
  id: string;
  skuCode: string;
  barcode?: string;
  price: number;
  isActive: boolean;
  attributes: Record<string, string>;
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
}

export interface CartItem {
  id: string;
  skuId: string;
  skuCode: string;
  productName: string;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
}

export interface Cart {
  id: string;
  userId?: string;
  sessionId?: string;
  items: CartItem[];
  subtotal: number;
  currency: string;
}

export type OrderStatus =
  | 'PENDING_PAYMENT'
  | 'PAYMENT_CONFIRMED'
  | 'PROCESSING'
  | 'PACKING'
  | 'SHIPPED'
  | 'DELIVERED'
  | 'CANCELLED'
  | 'REFUNDED';

export interface OrderItem {
  id: string;
  skuId: string;
  skuCode: string;
  productName: string;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
}

export interface Order {
  id: string;
  orderNumber: string;
  userId: string;
  status: OrderStatus;
  subtotal: number;
  discountAmount: number;
  shippingFee: number;
  grandTotal: number;
  currency: string;
  items: OrderItem[];
  shippingAddress: {
    recipientName: string;
    phone: string;
    addressLine1: string;
    addressLine2?: string;
    ward: string;
    district: string;
    city: string;
  };
  createdAt: string;
}
