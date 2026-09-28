import {
  ApiError,
  User,
  AuthResponse,
  Category,
  Product,
  CustomerAddress,
  Order,
  Cart,
  PaymentCallbackPayload,
  ProductFilterParams,
  PaginatedResult,
} from '@/types';
import {
  MOCK_CATEGORIES,
  MOCK_PRODUCTS,
  MOCK_USER_ADDRESSES,
  MOCK_ORDERS,
} from './mock-data';
import { generateUUID } from './utils';

export class ApiClientError extends Error {
  constructor(public readonly problem: ApiError) {
    super(problem.detail || problem.title || 'API Error');
    this.name = 'ApiClientError';
  }
}

// In-memory runtime state for mock fallback
let liveAddresses: CustomerAddress[] = [...MOCK_USER_ADDRESSES];
let liveOrders: Order[] = [...MOCK_ORDERS];
let liveCart: Cart = {
  id: 'cart-demo-001',
  userId: 'cust-demo-001',
  items: [],
  subtotal: 0,
  currency: 'VND',
};
const processedPaymentSuccessOrders = new Set<string>(['ord-2026-001', 'ord-2026-002']);
const processedIdempotencyKeys = new Map<string, Order>();

function generateCorrelationId(): string {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    return crypto.randomUUID();
  }
  return 'corr-' + Math.random().toString(36).substring(2, 15);
}

export interface ApiClientOptions extends RequestInit {
  idempotencyKey?: string;
}

export async function apiClient<T>(
  endpoint: string,
  options: ApiClientOptions = {}
): Promise<T> {
  const correlationId = generateCorrelationId();
  const token = typeof window !== 'undefined' ? localStorage.getItem('access_token') : null;

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    'X-Correlation-Id': correlationId,
    ...(options.headers as Record<string, string>),
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  if (options.idempotencyKey) {
    headers['Idempotency-Key'] = options.idempotencyKey;
  }

  // Attempt real network call to API Gateway
  const baseUrl = process.env.NEXT_PUBLIC_API_URL || '';
  const fullUrl = endpoint.startsWith('http') ? endpoint : `${baseUrl}${endpoint}`;

  try {
    const response = await fetch(fullUrl, {
      ...options,
      headers,
    });

    if (response.ok) {
      if (response.status === 204) {
        return {} as T;
      }
      return (await response.json()) as T;
    }

    // Try parsing RFC 7807 problem details
    let errorProblem: ApiError;
    try {
      errorProblem = await response.json();
    } catch {
      errorProblem = {
        type: 'about:blank',
        title: response.statusText,
        status: response.status,
        detail: `HTTP error ${response.status}`,
        instance: endpoint,
        timestamp: new Date().toISOString(),
        correlationId,
      };
    }
    throw new ApiClientError(errorProblem);
  } catch (err: unknown) {
    // If it's already an ApiClientError from an actual 4xx/5xx response, rethrow
    if (err instanceof ApiClientError) {
      throw err;
    }

    // Otherwise, Network Error or Gateway not running -> Fallback to High-Fidelity Mock Handler
    return handleMockRequest<T>(endpoint, options, correlationId);
  }
}

// ---------------------------------------------------------
// High-Fidelity Mock Backend Handler matching SRS Invariants
// ---------------------------------------------------------
function handleMockRequest<T>(
  endpoint: string,
  options: ApiClientOptions,
  correlationId: string
): Promise<T> {
  const method = (options.method || 'GET').toUpperCase();
  const body = options.body ? JSON.parse(options.body as string) : null;

  return new Promise((resolve, reject) => {
    // Artificial small delay for micro-interactions
    setTimeout(() => {
      try {
        // --- 1. AUTHENTICATION & IAM ---
        // POST /api/v1/auth/login
        if (endpoint.includes('/api/v1/auth/login') && method === 'POST') {
          if (!body?.email || !body?.password) {
            return reject(
              new ApiClientError({
                type: 'https://api.ecommerce.local/errors/validation',
                title: 'Validation Error',
                status: 400,
                code: 'VALIDATION_ERROR',
                detail: 'Email và mật khẩu không được để trống.',
                instance: endpoint,
                timestamp: new Date().toISOString(),
                correlationId,
              })
            );
          }
          if (body.password === 'wrongpassword') {
            return reject(
              new ApiClientError({
                type: 'https://api.ecommerce.local/errors/auth',
                title: 'Authentication Failed',
                status: 401,
                code: 'AUTHENTICATION_FAILED',
                detail: 'Email hoặc mật khẩu không chính xác.',
                instance: endpoint,
                timestamp: new Date().toISOString(),
                correlationId,
              })
            );
          }

          const authRes: AuthResponse = {
            accessToken: 'mock-jwt-token-' + generateUUID(),
            tokenType: 'Bearer',
            expiresIn: 86400,
            user: {
              id: 'cust-demo-001',
              email: body.email,
              fullName: body.email.includes('admin') ? 'Quản trị viên' : 'Nguyễn Văn An',
              phone: '0987654321',
              status: 'ACTIVE',
              roles: ['CUSTOMER'],
            },
          };
          return resolve(authRes as T);
        }

        // POST /api/v1/customers/register
        if (endpoint.includes('/api/v1/customers/register') && method === 'POST') {
          if (!body?.email || !body?.password || !body?.full_name) {
            return reject(
              new ApiClientError({
                type: 'https://api.ecommerce.local/errors/validation',
                title: 'Validation Error',
                status: 400,
                code: 'VALIDATION_ERROR',
                detail: 'Vui lòng điền đầy đủ họ tên, email và mật khẩu.',
                instance: endpoint,
                timestamp: new Date().toISOString(),
                correlationId,
              })
            );
          }
          const newUser: User = {
            id: 'cust-' + generateUUID().slice(0, 8),
            email: body.email,
            fullName: body.full_name,
            status: 'ACTIVE',
            roles: ['CUSTOMER'],
          };
          return resolve(newUser as T);
        }

        // --- 2. ADDRESSES (FR-022, API-IAM-003, BR-017) ---
        // GET /api/v1/customers/:id/addresses
        if (endpoint.match(/\/api\/v1\/customers\/[^/]+\/addresses$/) && method === 'GET') {
          return resolve(liveAddresses as T);
        }

        // POST /api/v1/customers/:id/addresses
        if (endpoint.match(/\/api\/v1\/customers\/[^/]+\/addresses$/) && method === 'POST') {
          const newAddr: CustomerAddress = {
            id: 'addr-' + generateUUID().slice(0, 8),
            customerId: 'cust-demo-001',
            recipientName: body.recipient_name || body.recipientName,
            phone: body.phone,
            line1: body.line1 || body.addressLine1,
            line2: body.line2 || body.addressLine2,
            ward: body.ward,
            district: body.district,
            city: body.city,
            isDefault: Boolean(body.is_default ?? body.isDefault),
            createdAt: new Date().toISOString(),
          };

          if (newAddr.isDefault) {
            liveAddresses = liveAddresses.map((a) => ({ ...a, isDefault: false }));
          }
          liveAddresses.push(newAddr);
          return resolve(newAddr as T);
        }

        // PUT /api/v1/customers/:id/addresses/:addrId
        if (endpoint.match(/\/api\/v1\/customers\/[^/]+\/addresses\/[^/]+$/) && method === 'PUT') {
          const addrId = endpoint.split('/').pop();
          const isDef = Boolean(body.is_default ?? body.isDefault);
          if (isDef) {
            liveAddresses = liveAddresses.map((a) => ({ ...a, isDefault: false }));
          }
          liveAddresses = liveAddresses.map((a) =>
            a.id === addrId
              ? {
                  ...a,
                  recipientName: body.recipient_name || body.recipientName || a.recipientName,
                  phone: body.phone || a.phone,
                  line1: body.line1 || body.addressLine1 || a.line1,
                  line2: body.line2 || body.addressLine2 || a.line2,
                  ward: body.ward || a.ward,
                  district: body.district || a.district,
                  city: body.city || a.city,
                  isDefault: isDef,
                }
              : a
          );
          const updated = liveAddresses.find((a) => a.id === addrId);
          return resolve(updated as T);
        }

        // DELETE /api/v1/customers/:id/addresses/:addrId
        if (endpoint.match(/\/api\/v1\/customers\/[^/]+\/addresses\/[^/]+$/) && method === 'DELETE') {
          const addrId = endpoint.split('/').pop();
          liveAddresses = liveAddresses.filter((a) => a.id !== addrId);
          return resolve({} as T);
        }

        // --- 3. CATEGORIES & PRODUCTS (FR-003, FR-004, FR-005) ---
        // GET /api/v1/categories
        if (endpoint.includes('/api/v1/categories') && method === 'GET' && !endpoint.includes('/products')) {
          return resolve(MOCK_CATEGORIES as T);
        }

        // GET /api/v1/products/search or /api/v1/products
        if (endpoint.includes('/api/v1/products') && method === 'GET') {
          // Single product detail: /api/v1/products/:id
          const singleMatch = endpoint.match(/\/api\/v1\/products\/([^/?]+)$/);
          if (singleMatch && singleMatch[1] !== 'search') {
            const prodId = singleMatch[1];
            const found = MOCK_PRODUCTS.find((p) => p.id === prodId || p.slug === prodId);
            if (!found) {
              return reject(
                new ApiClientError({
                  type: 'https://api.ecommerce.local/errors/not-found',
                  title: 'Product Not Found',
                  status: 404,
                  code: 'NOT_FOUND',
                  detail: `Không tìm thấy sản phẩm có mã '${prodId}'.`,
                  instance: endpoint,
                  timestamp: new Date().toISOString(),
                  correlationId,
                })
              );
            }
            return resolve(found as T);
          }

          // Search / Catalog filter
          let filtered = [...MOCK_PRODUCTS];
          const urlObj = new URL('http://dummy.com' + endpoint);
          const q = urlObj.searchParams.get('q') || urlObj.searchParams.get('search');
          const cat = urlObj.searchParams.get('categoryId');
          const minPrice = Number(urlObj.searchParams.get('minPrice')) || 0;
          const maxPrice = Number(urlObj.searchParams.get('maxPrice')) || Infinity;
          const sortBy = urlObj.searchParams.get('sortBy');

          if (q) {
            const queryLower = q.toLowerCase();
            filtered = filtered.filter(
              (p) =>
                p.name.toLowerCase().includes(queryLower) ||
                p.description.toLowerCase().includes(queryLower)
            );
          }
          if (cat) {
            filtered = filtered.filter(
              (p) =>
                p.category.id === cat ||
                p.category.parentId === cat ||
                p.category.code.toLowerCase() === cat.toLowerCase()
            );
          }
          if (minPrice > 0 || maxPrice < Infinity) {
            filtered = filtered.filter((p) => {
              const effectivePrice = p.skus[0]?.salePrice || p.skus[0]?.price || 0;
              return effectivePrice >= minPrice && effectivePrice <= maxPrice;
            });
          }
          if (sortBy === 'price-asc') {
            filtered.sort(
              (a, b) =>
                (a.skus[0]?.salePrice || a.skus[0]?.price || 0) -
                (b.skus[0]?.salePrice || b.skus[0]?.price || 0)
            );
          } else if (sortBy === 'price-desc') {
            filtered.sort(
              (a, b) =>
                (b.skus[0]?.salePrice || b.skus[0]?.price || 0) -
                (a.skus[0]?.salePrice || a.skus[0]?.price || 0)
            );
          }

          const result: PaginatedResult<Product> = {
            items: filtered,
            page: 1,
            size: filtered.length,
            total: filtered.length,
            totalPages: 1,
          };
          return resolve(result as T);
        }

        // --- 4. CART (API-CART-001, 002, 003, BR-004) ---
        // GET /api/v1/customers/:id/cart
        if (endpoint.includes('/cart') && method === 'GET') {
          return resolve(liveCart as T);
        }

        // POST /api/v1/customers/:id/cart/items
        if (endpoint.includes('/cart/items') && method === 'POST') {
          const skuId = body?.sku_id || body?.skuId;
          const quantity = Number(body?.quantity) || 1;

          // Find SKU across products
          let targetSku = null;
          let targetProd = null;
          for (const prod of MOCK_PRODUCTS) {
            const foundSku = prod.skus.find((s) => s.id === skuId);
            if (foundSku) {
              targetSku = foundSku;
              targetProd = prod;
              break;
            }
          }

          if (!targetSku || !targetProd) {
            return reject(
              new ApiClientError({
                type: 'https://api.ecommerce.local/errors/not-found',
                title: 'SKU Not Found',
                status: 404,
                code: 'NOT_FOUND',
                detail: `Không tìm thấy thông tin biến thể SKU: ${skuId}`,
                instance: endpoint,
                timestamp: new Date().toISOString(),
                correlationId,
              })
            );
          }

          const maxAvailable = targetSku.inventory?.quantityAvailable ?? 10;
          const existingItem = liveCart.items.find((i) => i.skuId === skuId);
          const newQty = (existingItem?.quantity || 0) + quantity;

          // BR-004 check
          if (newQty > maxAvailable) {
            return reject(
              new ApiClientError({
                type: 'https://api.ecommerce.local/errors/insufficient-stock',
                title: 'Business Rule Violation - Exceeds Available Stock',
                status: 422,
                code: 'INSUFFICIENT_STOCK',
                detail: `Số lượng yêu cầu (${newQty}) vượt quá số lượng hàng khả dụng (${maxAvailable}) của sản phẩm ${targetProd.name}.`,
                instance: endpoint,
                timestamp: new Date().toISOString(),
                correlationId,
              })
            );
          }

          const unitPrice = targetSku.salePrice || targetSku.price;
          if (existingItem) {
            existingItem.quantity = newQty;
            existingItem.totalPrice = newQty * unitPrice;
          } else {
            liveCart.items.push({
              id: 'cart-item-' + generateUUID().slice(0, 8),
              skuId: targetSku.id,
              skuCode: targetSku.skuCode,
              productId: targetProd.id,
              productName: targetProd.name,
              productImage: targetProd.mediaUrls[0],
              quantity,
              unitPrice,
              originalPrice: targetSku.salePrice ? targetSku.price : undefined,
              totalPrice: quantity * unitPrice,
              attributes: targetSku.attributes,
              maxAvailableStock: maxAvailable,
            });
          }

          liveCart.subtotal = liveCart.items.reduce((acc, i) => acc + i.totalPrice, 0);
          return resolve(liveCart as T);
        }

        // --- 5. CHECKOUT & ORDERS (API-ORD-001, FR-009, BR-009, BR-010, BR-017) ---
        // POST /api/v1/customers/:id/orders
        if (endpoint.match(/\/api\/v1\/customers\/[^/]+\/orders$/) && method === 'POST') {
          const idempotencyKey = options.idempotencyKey || (options.headers as Record<string, string>)?.[
            'Idempotency-Key'
          ];

          // Check for Idempotency replay (BR-010)
          if (idempotencyKey && processedIdempotencyKeys.has(idempotencyKey)) {
            const priorOrder = processedIdempotencyKeys.get(idempotencyKey)!;
            return resolve(priorOrder as T);
          }

          const addressId = body?.address_id || body?.addressId;
          const address = liveAddresses.find((a) => a.id === addressId) || liveAddresses[0];

          if (!address) {
            return reject(
              new ApiClientError({
                type: 'https://api.ecommerce.local/errors/validation',
                title: 'Address Required',
                status: 400,
                code: 'VALIDATION_ERROR',
                detail: 'Vui lòng chọn địa chỉ giao hàng hợp lệ.',
                instance: endpoint,
                timestamp: new Date().toISOString(),
                correlationId,
              })
            );
          }

          // Check if items in order are available
          const orderItems = body?.items || liveCart.items;
          if (!orderItems || orderItems.length === 0) {
            return reject(
              new ApiClientError({
                type: 'https://api.ecommerce.local/errors/validation',
                title: 'Empty Cart',
                status: 400,
                code: 'VALIDATION_ERROR',
                detail: 'Giỏ hàng của bạn đang trống, không thể thanh toán.',
                instance: endpoint,
                timestamp: new Date().toISOString(),
                correlationId,
              })
            );
          }

          // Simulate inventory contention for testing edge cases if quantity requested is impossible
          for (const item of orderItems) {
            if (item.quantity > 50) {
              return reject(
                new ApiClientError({
                  type: 'https://api.ecommerce.local/errors/insufficient-stock',
                  title: 'Inventory Reservation Contention',
                  status: 422,
                  code: 'INSUFFICIENT_STOCK',
                  detail: `Sản phẩm '${item.productName || item.skuCodeSnapshot}' không đủ tồn kho khả dụng để thực hiện đặt hàng.`,
                  instance: endpoint,
                  timestamp: new Date().toISOString(),
                  correlationId,
                })
              );
            }
          }

          const subtotal = orderItems.reduce(
            (sum: number, i: any) => sum + (i.totalPrice || (i.unitPriceSnapshot || i.unitPrice) * i.quantity),
            0
          );
          const shippingFee = subtotal >= 500000 ? 0 : 30000;
          const grandTotal = subtotal + shippingFee;

          const newOrder: Order = {
            id: 'ord-' + generateUUID().slice(0, 8),
            orderNumber: 'ORD-' + new Date().toISOString().slice(0, 10).replace(/-/g, '') + '-' + Math.floor(1000 + Math.random() * 9000),
            customerId: 'cust-demo-001',
            status: 'RESERVED',
            subtotalAmount: subtotal,
            shippingFeeAmount: shippingFee,
            discountAmount: 0,
            grandTotalAmount: grandTotal,
            currency: 'VND',
            // BR-017: Deep snapshot of shipping address
            shippingAddress: { ...address },
            // BR-013 & FR-028: Snapshot of order items
            items: orderItems.map((i: any) => ({
              id: 'oi-' + generateUUID().slice(0, 8),
              skuId: i.skuId,
              skuCodeSnapshot: i.skuCode || i.skuCodeSnapshot,
              productNameSnapshot: i.productName || i.productNameSnapshot,
              productImageSnapshot: i.productImage || i.productImageSnapshot,
              attributeSnapshot: i.attributes || i.attributeSnapshot,
              quantity: i.quantity,
              unitPriceSnapshot: i.unitPrice || i.unitPriceSnapshot,
              lineTotal: (i.unitPrice || i.unitPriceSnapshot) * i.quantity,
            })),
            paymentMethod: body?.payment_method || 'MOCK_GATEWAY',
            idempotencyKey,
            createdAt: new Date().toISOString(),
            timeline: [
              {
                id: 'tl-' + generateUUID().slice(0, 8),
                orderId: 'new',
                fromStatus: null,
                toStatus: 'RESERVED',
                actorType: 'CUSTOMER',
                actorId: 'cust-demo-001',
                note: 'Khởi tạo đơn hàng & Giữ chỗ tồn kho thành công (Atomic Reservation)',
                occurredAt: new Date().toISOString(),
              },
            ],
          };

          if (idempotencyKey) {
            processedIdempotencyKeys.set(idempotencyKey, newOrder);
          }

          liveOrders.unshift(newOrder);
          // Clear cart on successful order creation
          liveCart.items = [];
          liveCart.subtotal = 0;

          return resolve(newOrder as T);
        }

        // GET /api/v1/customers/:id/orders
        if (endpoint.match(/\/api\/v1\/customers\/[^/]+\/orders$/) && method === 'GET') {
          return resolve(liveOrders as T);
        }

        // GET /api/v1/customers/:id/orders/:orderId
        if (endpoint.match(/\/api\/v1\/customers\/[^/]+\/orders\/[^/]+$/) && method === 'GET') {
          const orderId = endpoint.split('/').pop();
          const order = liveOrders.find((o) => o.id === orderId || o.orderNumber === orderId);
          if (!order) {
            return reject(
              new ApiClientError({
                type: 'https://api.ecommerce.local/errors/not-found',
                title: 'Order Not Found',
                status: 404,
                code: 'NOT_FOUND',
                detail: `Không tìm thấy đơn hàng: ${orderId}`,
                instance: endpoint,
                timestamp: new Date().toISOString(),
                correlationId,
              })
            );
          }
          return resolve(order as T);
        }

        // POST /api/v1/customers/:id/orders/:orderId/cancel (FR-012, BR-001, BR-006, BR-007)
        if (endpoint.includes('/cancel') && method === 'POST') {
          const parts = endpoint.split('/');
          const orderId = parts[parts.indexOf('orders') + 1];
          const order = liveOrders.find((o) => o.id === orderId || o.orderNumber === orderId);

          if (!order) {
            return reject(
              new ApiClientError({
                type: 'https://api.ecommerce.local/errors/not-found',
                title: 'Order Not Found',
                status: 404,
                code: 'NOT_FOUND',
                detail: `Không tìm thấy đơn hàng để hủy.`,
                instance: endpoint,
                timestamp: new Date().toISOString(),
                correlationId,
              })
            );
          }

          if (order.status === 'CANCELLED') {
            return reject(
              new ApiClientError({
                type: 'https://api.ecommerce.local/errors/business-rule',
                title: 'Business Rule Violation',
                status: 422,
                code: 'BUSINESS_RULE_VIOLATION',
                detail: 'Đơn hàng đã ở trạng thái ĐÃ HỦY (CANCELLED), không thể hủy lại (BR-001).',
                instance: endpoint,
                timestamp: new Date().toISOString(),
                correlationId,
              })
            );
          }

          // Cutoff check: BR-006 & BR-007: Only allowed prior to PACKING
          if (order.status !== 'RESERVED' && order.status !== 'PAID') {
            return reject(
              new ApiClientError({
                type: 'https://api.ecommerce.local/errors/invalid-state',
                title: 'Invalid State Cutoff',
                status: 409,
                code: 'INVALID_STATE',
                detail: `Không thể hủy đơn hàng khi trạng thái đã chuyển sang '${order.status}'. Đơn hàng đang được kho đóng gói và vận chuyển (BR-006).`,
                instance: endpoint,
                timestamp: new Date().toISOString(),
                correlationId,
              })
            );
          }

          // Perform valid cancellation
          const prevStatus = order.status;
          order.status = 'CANCELLED';
          order.timeline?.push({
            id: 'tl-' + generateUUID().slice(0, 8),
            orderId: order.id,
            fromStatus: prevStatus,
            toStatus: 'CANCELLED',
            actorType: 'CUSTOMER',
            note: body?.reason || 'Khách hàng yêu cầu hủy đơn hàng trước khi đóng gói',
            occurredAt: new Date().toISOString(),
          });

          return resolve(order as T);
        }

        // --- 6. PAYMENT PROCESSING & CALLBACK (API-PAY-001, FR-010, BR-002) ---
        // POST /api/v1/payments/callback
        if (endpoint.includes('/api/v1/payments/callback') && method === 'POST') {
          const payload = body as PaymentCallbackPayload;
          const order = liveOrders.find((o) => o.id === payload.order_id || o.orderNumber === payload.order_id);

          if (!order) {
            return reject(
              new ApiClientError({
                type: 'https://api.ecommerce.local/errors/not-found',
                title: 'Order Not Found',
                status: 404,
                code: 'NOT_FOUND',
                detail: `Không tìm thấy đơn hàng liên kết với mã thanh toán: ${payload.order_id}`,
                instance: endpoint,
                timestamp: new Date().toISOString(),
                correlationId,
              })
            );
          }

          // BR-002: At most one SUCCEEDED payment transaction
          if (payload.result === 'SUCCESS' && (order.status === 'PAID' || processedPaymentSuccessOrders.has(order.id))) {
            return reject(
              new ApiClientError({
                type: 'https://api.ecommerce.local/errors/duplicate-payment',
                title: 'Duplicate Payment Callback Detected',
                status: 422,
                code: 'DUPLICATE_PAYMENT',
                detail: `Đơn hàng '${order.orderNumber}' đã được thanh toán thành công trước đó (BR-002). Giao dịch trùng lặp bị từ chối để chống thu phí kép.`,
                instance: endpoint,
                timestamp: new Date().toISOString(),
                correlationId,
              })
            );
          }

          if (payload.result === 'SUCCESS') {
            order.status = 'PAID';
            processedPaymentSuccessOrders.add(order.id);
            order.timeline?.push({
              id: 'tl-' + generateUUID().slice(0, 8),
              orderId: order.id,
              fromStatus: 'RESERVED',
              toStatus: 'PAID',
              actorType: 'SYSTEM',
              note: `Cổng thanh toán xác nhận giao dịch thành công (Mã GD: ${payload.provider_reference || generateUUID().slice(0, 8)})`,
              occurredAt: new Date().toISOString(),
            });
            return resolve({ message: 'Payment processed successfully', status: 'PAID', order } as T);
          } else {
            order.status = 'PAYMENT_FAILED';
            order.timeline?.push({
              id: 'tl-' + generateUUID().slice(0, 8),
              orderId: order.id,
              fromStatus: 'RESERVED',
              toStatus: 'PAYMENT_FAILED',
              actorType: 'SYSTEM',
              note: 'Giao dịch thanh toán thất bại hoặc người dùng hủy giao dịch',
              occurredAt: new Date().toISOString(),
            });
            return resolve({ message: 'Payment marked as failed', status: 'PAYMENT_FAILED', order } as T);
          }
        }

        // Default: If unrecognized endpoint, resolve empty or mock data
        return resolve({} as T);
      } catch (mockErr: any) {
        return reject(
          new ApiClientError({
            type: 'https://api.ecommerce.local/errors/internal',
            title: 'Mock Error',
            status: 500,
            detail: mockErr.message || 'Internal Mock Error',
            instance: endpoint,
            timestamp: new Date().toISOString(),
            correlationId,
          })
        );
      }
    }, 150);
  });
}
