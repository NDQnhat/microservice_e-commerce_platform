import { create } from 'zustand';
import { Cart, CartItem } from '@/types';

interface CartState {
  cart: Cart | null;
  itemCount: number;
  setCart: (cart: Cart) => void;
  clearCart: () => void;
}

export const useCartStore = create<CartState>((set) => ({
  cart: null,
  itemCount: 0,
  setCart: (cart: Cart) => {
    const itemCount = cart.items.reduce((sum, item) => sum + item.quantity, 0);
    set({ cart, itemCount });
  },
  clearCart: () => set({ cart: null, itemCount: 0 }),
}));
