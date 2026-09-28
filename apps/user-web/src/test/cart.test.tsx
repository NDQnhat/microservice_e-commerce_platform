import { describe, it, expect, beforeEach } from 'vitest';
import { useCartStore, FREE_SHIPPING_THRESHOLD } from '@/store/cart-store';
import { MOCK_PRODUCTS } from '@/lib/mock-data';

describe('Cart Store & BR-004 Stock Limit Enforcement', () => {
  beforeEach(() => {
    useCartStore.getState().clearCart();
  });

  it('initializes with empty items and zero subtotal', () => {
    const state = useCartStore.getState();
    expect(state.items).toEqual([]);
    expect(state.itemCount).toBe(0);
    expect(state.subtotal).toBe(0);
  });

  it('adds item to cart and updates count and subtotal', () => {
    const product = MOCK_PRODUCTS[0];
    const sku = product.skus[0]; // Price: 850k, Sale: 680k
    const effectivePrice = sku.salePrice || sku.price;

    const res = useCartStore.getState().addItem(product, sku, 2);
    expect(res.success).toBe(true);

    const state = useCartStore.getState();
    expect(state.itemCount).toBe(2);
    expect(state.items.length).toBe(1);
    expect(state.items[0].skuId).toBe(sku.id);
    expect(state.subtotal).toBe(effectivePrice * 2);
    expect(state.isDrawerOpen).toBe(true);
  });

  it('enforces BR-004 by rejecting adding quantity exceeding available stock', () => {
    const product = MOCK_PRODUCTS[0];
    // Find sku with low stock (2 available)
    const lowStockSku = product.skus.find(
      (s) => (s.inventory?.quantityAvailable ?? 0) === 2
    )!;

    // Try adding 3 when only 2 available
    const res = useCartStore.getState().addItem(product, lowStockSku, 3);
    expect(res.success).toBe(false);
    expect(res.message).toContain('Không thể thêm quá số lượng khả dụng');

    const state = useCartStore.getState();
    expect(state.itemCount).toBe(0);
  });

  it('enforces BR-004 when updating quantity in cart', () => {
    const product = MOCK_PRODUCTS[0];
    const lowStockSku = product.skus.find(
      (s) => (s.inventory?.quantityAvailable ?? 0) === 2
    )!;

    useCartStore.getState().addItem(product, lowStockSku, 1);
    const itemId = useCartStore.getState().items[0].id;

    // Updating to 3 should be rejected because max is 2
    const updateRes = useCartStore.getState().updateQuantity(itemId, 3);
    expect(updateRes.success).toBe(false);
    expect(updateRes.message).toContain('Chỉ còn 2 sản phẩm khả dụng trong kho');

    // Quantity should remain 1
    expect(useCartStore.getState().items[0].quantity).toBe(1);
  });

  it('removes item when quantity is reduced to 0', () => {
    const product = MOCK_PRODUCTS[0];
    const sku = product.skus[0];

    useCartStore.getState().addItem(product, sku, 1);
    const itemId = useCartStore.getState().items[0].id;

    useCartStore.getState().updateQuantity(itemId, 0);
    expect(useCartStore.getState().items.length).toBe(0);
    expect(useCartStore.getState().itemCount).toBe(0);
  });

  it('correctly calculates free shipping eligibility threshold', () => {
    const state = useCartStore.getState();
    expect(FREE_SHIPPING_THRESHOLD).toBe(500000);
    expect(state.subtotal >= FREE_SHIPPING_THRESHOLD).toBe(false);

    // Add item with subtotal >= 500k
    const product = MOCK_PRODUCTS[0];
    const sku = product.skus[0]; // 680k
    useCartStore.getState().addItem(product, sku, 1);

    expect(useCartStore.getState().subtotal >= FREE_SHIPPING_THRESHOLD).toBe(true);
  });
});
