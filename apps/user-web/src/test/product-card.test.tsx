import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, beforeEach } from 'vitest';
import { ProductCard } from '@/components/product/ProductCard';
import { MOCK_PRODUCTS } from '@/lib/mock-data';
import { useCartStore } from '@/store/cart-store';

describe('ProductCard Component', () => {
  beforeEach(() => {
    useCartStore.getState().clearCart();
  });

  it('renders product information, category, and bold price', () => {
    const product = MOCK_PRODUCTS[0]; // Áo Hoodie Minimalist
    render(<ProductCard product={product} />);

    expect(screen.getByText(product.name)).toBeInTheDocument();
    expect(screen.getByText(product.category.name)).toBeInTheDocument();
    // Sale price should be rendered (680,000)
    expect(screen.getByText(/680\.000/)).toBeInTheDocument();
  });

  it('renders strike-through original price and discount percentage badge per BR-013', () => {
    const product = MOCK_PRODUCTS[0]; // 850k base, 680k sale = -20%
    render(<ProductCard product={product} />);

    // Original strike-through price
    expect(screen.getByText(/850\.000/)).toBeInTheDocument();
    // Discount badge: -20%
    expect(screen.getByText('-20%')).toBeInTheDocument();
  });

  it('triggers quick add to cart on click and updates cart state', () => {
    const product = MOCK_PRODUCTS[0];
    render(<ProductCard product={product} />);

    const quickAddButton = screen.getByText('Thêm nhanh');
    fireEvent.click(quickAddButton);

    const cartState = useCartStore.getState();
    expect(cartState.itemCount).toBe(1);
    expect(cartState.items[0].productId).toBe(product.id);
    expect(cartState.isDrawerOpen).toBe(true);
  });
});
