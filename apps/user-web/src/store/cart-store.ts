import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { CartItem, Product, Sku } from '@/types';

export const FREE_SHIPPING_THRESHOLD = 500000; // 500,000 VND

interface CartState {
  items: CartItem[];
  itemCount: number;
  subtotal: number;
  isDrawerOpen: boolean;
  isHydrated: boolean;

  setHydrated: () => void;
  openDrawer: () => void;
  closeDrawer: () => void;
  toggleDrawer: () => void;

  addItem: (product: Product, sku: Sku, quantity?: number) => { success: boolean; message?: string };
  updateQuantity: (itemId: string, quantity: number) => { success: boolean; message?: string };
  removeItem: (itemId: string) => void;
  clearCart: () => void;
}

export const useCartStore = create<CartState>()(
  persist(
    (set, get) => ({
      items: [],
      itemCount: 0,
      subtotal: 0,
      isDrawerOpen: false,
      isHydrated: false,

      setHydrated: () => set({ isHydrated: true }),

      openDrawer: () => set({ isDrawerOpen: true }),
      closeDrawer: () => set({ isDrawerOpen: false }),
      toggleDrawer: () => set((state) => ({ isDrawerOpen: !state.isDrawerOpen })),

      addItem: (product, sku, quantity = 1) => {
        const { items } = get();
        const maxStock = sku.inventory?.quantityAvailable ?? 10;

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

        set({
          items: updatedItems,
          itemCount: newCount,
          subtotal: newSubtotal,
          isDrawerOpen: true, // Mini Cart Slide-over Drawer opens instantly
        });

        return { success: true };
      },

      updateQuantity: (itemId, quantity) => {
        const { items } = get();
        const target = items.find((i) => i.id === itemId);
        if (!target) return { success: false, message: 'Sản phẩm không có trong giỏ.' };

        if (quantity <= 0) {
          get().removeItem(itemId);
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

        set({
          items: updatedItems,
          itemCount: newCount,
          subtotal: newSubtotal,
        });

        return { success: true };
      },

      removeItem: (itemId) => {
        const { items } = get();
        const updatedItems = items.filter((i) => i.id !== itemId);
        const newCount = updatedItems.reduce((sum, i) => sum + i.quantity, 0);
        const newSubtotal = updatedItems.reduce((sum, i) => sum + i.totalPrice, 0);

        set({
          items: updatedItems,
          itemCount: newCount,
          subtotal: newSubtotal,
        });
      },

      clearCart: () => {
        set({
          items: [],
          itemCount: 0,
          subtotal: 0,
        });
      },
    }),
    {
      name: 'cart-store-storage',
      onRehydrateStorage: () => (state) => {
        state?.setHydrated();
      },
    }
  )
);
