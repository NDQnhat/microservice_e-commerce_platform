import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { CartItem, Product, Sku, BusinessConfiguration, Cart } from '@/types';
import { apiClient, ApiClientError } from '@/lib/api-client';

export const DEFAULT_FREE_SHIPPING_THRESHOLD = 500000; // 500,000 VND
export const DEFAULT_STANDARD_SHIPPING_FEE = 30000; // 30,000 VND
export const FREE_SHIPPING_THRESHOLD = DEFAULT_FREE_SHIPPING_THRESHOLD; // Backwards compatibility for existing imports

export interface CartState {
  cartId: string;
  items: CartItem[];
  itemCount: number;
  subtotal: number;
  freeShippingThreshold: number;
  standardShippingFee: number;
  isDrawerOpen: boolean;
  isHydrated: boolean;

  setHydrated: () => void;
  openDrawer: () => void;
  closeDrawer: () => void;
  toggleDrawer: () => void;

  loadConfigurations: () => Promise<void>;
  loadCart: (customerId?: string) => Promise<void>;
  addItem: (
    product: Product,
    sku: Sku,
    quantity?: number,
    customerId?: string
  ) => Promise<{ success: boolean; message?: string }>;
  updateQuantity: (
    itemId: string,
    quantity: number,
    customerId?: string
  ) => Promise<{ success: boolean; message?: string }>;
  removeItem: (itemId: string, customerId?: string) => Promise<void>;
  clearCart: (customerId?: string) => Promise<void>;
}

export const useCartStore = create<CartState>()(
  persist(
    (set, get) => ({
      cartId: 'cart-demo-001',
      items: [],
      itemCount: 0,
      subtotal: 0,
      freeShippingThreshold: DEFAULT_FREE_SHIPPING_THRESHOLD,
      standardShippingFee: DEFAULT_STANDARD_SHIPPING_FEE,
      isDrawerOpen: false,
      isHydrated: false,

      setHydrated: () => set({ isHydrated: true }),

      openDrawer: () => set({ isDrawerOpen: true }),
      closeDrawer: () => set({ isDrawerOpen: false }),
      toggleDrawer: () => set((state) => ({ isDrawerOpen: !state.isDrawerOpen })),

      loadConfigurations: async () => {
        try {
          const config = await apiClient<BusinessConfiguration>('/api/v1/configurations');
          if (config && typeof config.free_shipping_threshold === 'number') {
            set({
              freeShippingThreshold: config.free_shipping_threshold,
              standardShippingFee: config.standard_shipping_fee ?? DEFAULT_STANDARD_SHIPPING_FEE,
            });
          }
        } catch {
          // Fallback to default constants if network unavailable
        }
      },

      loadCart: async (customerId = 'cust-demo-001') => {
        try {
          const res = await apiClient<Cart>(`/api/v1/customers/${customerId}/cart`);
          if (res && Array.isArray(res.items)) {
            set({
              cartId: res.id || get().cartId || 'cart-demo-001',
              items: res.items,
              itemCount: res.items.reduce((sum, i) => sum + i.quantity, 0),
              subtotal: res.subtotal ?? res.items.reduce((sum, i) => sum + i.totalPrice, 0),
            });
          }
        } catch {
          // Fallback to local storage state
        }
      },

      addItem: async (product, sku, quantity = 1, customerId = 'cust-demo-001') => {
        const { items } = get();
        const maxStock = sku.inventory?.quantityAvailable ?? 0;

        // BR-004: Validate stock
        const existingItem = items.find((i) => i.skuId === sku.id);
        const currentQty = existingItem ? existingItem.quantity : 0;
        const newQty = currentQty + quantity;

        if (maxStock <= 0) {
          return {
            success: false,
            message: `Sản phẩm '${product.name}' hiện đã hết hàng.`,
          };
        }

        if (newQty > maxStock) {
          return {
            success: false,
            message: `Không thể thêm quá số lượng khả dụng (${maxStock} sản phẩm).`,
          };
        }

        const unitPrice = sku.salePrice || sku.price;
        let updatedItems: CartItem[];

        if (existingItem) {
          updatedItems = items.map((item) =>
            item.skuId === sku.id
              ? {
                  ...item,
                  quantity: newQty,
                  totalPrice: newQty * unitPrice,
                  maxAvailableStock: maxStock,
                }
              : item
          );
        } else {
          const newItem: CartItem = {
            id: `item-${Date.now()}-${Math.random().toString(36).substring(2, 7)}`,
            skuId: sku.id,
            skuCode: sku.skuCode,
            productId: product.id,
            productName: product.name,
            productImage: product.mediaUrls[0],
            quantity,
            unitPrice,
            originalPrice: sku.salePrice ? sku.price : undefined,
            totalPrice: quantity * unitPrice,
            attributes: sku.attributes,
            maxAvailableStock: maxStock,
          };
          updatedItems = [...items, newItem];
        }

        const newCount = updatedItems.reduce((sum, i) => sum + i.quantity, 0);
        const newSubtotal = updatedItems.reduce((sum, i) => sum + i.totalPrice, 0);

        // Optimistic UI update
        set({
          items: updatedItems,
          itemCount: newCount,
          subtotal: newSubtotal,
          isDrawerOpen: true, // Mini Cart Slide-over Drawer opens instantly
        });

        // Sync with Backend Cart Service (API-CART-001)
        try {
          const backendCart = await apiClient<Cart>(`/api/v1/customers/${customerId}/cart/items`, {
            method: 'POST',
            body: JSON.stringify({
              sku_id: sku.id,
              quantity,
            }),
          });
          if (backendCart && Array.isArray(backendCart.items)) {
            set({
              cartId: backendCart.id || get().cartId,
              items: backendCart.items,
              itemCount: backendCart.items.reduce((s, i) => s + i.quantity, 0),
              subtotal: backendCart.subtotal,
            });
          }
        } catch (err: unknown) {
          if (err instanceof ApiClientError && err.problem.code === 'INSUFFICIENT_STOCK') {
            // Rollback optimistic update on server rejection
            set({
              items,
              itemCount: items.reduce((s, i) => s + i.quantity, 0),
              subtotal: items.reduce((s, i) => s + i.totalPrice, 0),
            });
            return {
              success: false,
              message: err.problem.detail || 'Sản phẩm không đủ tồn kho khả dụng.',
            };
          }
          // On network failure, retain optimistic state for offline resilience
        }

        return { success: true };
      },

      updateQuantity: async (itemId, quantity, customerId = 'cust-demo-001') => {
        const { items } = get();
        const target = items.find((i) => i.id === itemId);
        if (!target) return { success: false, message: 'Sản phẩm không có trong giỏ.' };

        if (quantity <= 0) {
          await get().removeItem(itemId, customerId);
          return { success: true };
        }

        // BR-004 check
        if (quantity > target.maxAvailableStock) {
          return {
            success: false,
            message: `Chỉ còn ${target.maxAvailableStock} sản phẩm khả dụng trong kho.`,
          };
        }

        const updatedItems = items.map((item) =>
          item.id === itemId
            ? {
                ...item,
                quantity,
                totalPrice: quantity * item.unitPrice,
              }
            : item
        );

        const newCount = updatedItems.reduce((sum, i) => sum + i.quantity, 0);
        const newSubtotal = updatedItems.reduce((sum, i) => sum + i.totalPrice, 0);

        // Optimistic UI update
        set({
          items: updatedItems,
          itemCount: newCount,
          subtotal: newSubtotal,
        });

        // Sync with Backend Cart Service (API-CART-002)
        try {
          const backendCart = await apiClient<Cart>(
            `/api/v1/customers/${customerId}/cart/items/${itemId}`,
            {
              method: 'PUT',
              body: JSON.stringify({ quantity }),
            }
          );
          if (backendCart && Array.isArray(backendCart.items)) {
            set({
              items: backendCart.items,
              itemCount: backendCart.items.reduce((s, i) => s + i.quantity, 0),
              subtotal: backendCart.subtotal,
            });
          }
        } catch (err: unknown) {
          if (err instanceof ApiClientError && err.problem.code === 'INSUFFICIENT_STOCK') {
            // Rollback optimistic update
            set({
              items,
              itemCount: items.reduce((s, i) => s + i.quantity, 0),
              subtotal: items.reduce((s, i) => s + i.totalPrice, 0),
            });
            return {
              success: false,
              message: err.problem.detail || 'Số lượng vượt quá tồn kho khả dụng.',
            };
          }
        }

        return { success: true };
      },

      removeItem: async (itemId, customerId = 'cust-demo-001') => {
        const { items } = get();
        const updatedItems = items.filter((i) => i.id !== itemId);
        const newCount = updatedItems.reduce((sum, i) => sum + i.quantity, 0);
        const newSubtotal = updatedItems.reduce((sum, i) => sum + i.totalPrice, 0);

        set({
          items: updatedItems,
          itemCount: newCount,
          subtotal: newSubtotal,
        });

        try {
          await apiClient(`/api/v1/customers/${customerId}/cart/items/${itemId}`, {
            method: 'DELETE',
          });
        } catch {
          // Local removal stands if offline
        }
      },

      clearCart: async (customerId = 'cust-demo-001') => {
        set({
          items: [],
          itemCount: 0,
          subtotal: 0,
        });

        try {
          await apiClient(`/api/v1/customers/${customerId}/cart`, {
            method: 'DELETE',
          });
        } catch {
          // Local clear stands
        }
      },
    }),
    {
      name: 'cart-store-storage',
      onRehydrateStorage: () => (state) => {
        state?.setHydrated();
        // Load latest business configurations on hydration
        state?.loadConfigurations();
      },
    }
  )
);
