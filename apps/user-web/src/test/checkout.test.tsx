import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import CheckoutPage from '@/app/checkout/page';
import { useCartStore } from '@/store/cart-store';
import { useUserStore } from '@/store/user-store';
import { MOCK_PRODUCTS, MOCK_USER_ADDRESSES } from '@/lib/mock-data';
import * as apiClientModule from '@/lib/api-client';

// Mock Next.js router
vi.mock('next/navigation', () => ({
  useRouter: () => ({
    push: vi.fn(),
  }),
  useSearchParams: () => new URLSearchParams(),
}));

describe('Checkout Flow & Idempotency Key Invariants', () => {
  beforeEach(() => {
    useCartStore.getState().clearCart();
    useUserStore.setState({
      user: {
        id: 'cust-demo-001',
        email: 'customer@ecommerce.local',
        fullName: 'Nguyễn Văn An',
        status: 'ACTIVE',
        roles: ['CUSTOMER'],
      },
      isAuthenticated: true,
      addresses: [...MOCK_USER_ADDRESSES],
    });
  });

  it('renders address selection and displays BR-017 Address Snapshot notice', () => {
    // Add item to cart so checkout is not empty
    useCartStore.getState().addItem(MOCK_PRODUCTS[0], MOCK_PRODUCTS[0].skus[0], 1);

    render(<CheckoutPage />);

    expect(screen.getByText('1. Địa chỉ giao hàng')).toBeInTheDocument();
    expect(screen.getByText(/Bảo đảm Bất biến BR-017/)).toBeInTheDocument();
    expect(screen.getByText('Nguyễn Văn An')).toBeInTheDocument();
  });

  it('sends Idempotency-Key header on order creation to prevent double charge per BR-010', async () => {
    useCartStore.getState().addItem(MOCK_PRODUCTS[0], MOCK_PRODUCTS[0].skus[0], 1);

    const apiClientSpy = vi.spyOn(apiClientModule, 'apiClient');

    render(<CheckoutPage />);

    const submitBtn = screen.getByText('Xác nhận Đặt hàng');
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(apiClientSpy).toHaveBeenCalled();
    });

    const callArgs = apiClientSpy.mock.calls[0];
    const endpoint = callArgs[0];
    const options = callArgs[1] as any;

    expect(endpoint).toContain('/api/v1/customers/cust-demo-001/orders');
    expect(options.method).toBe('POST');
    // Header Idempotency-Key must exist and be a non-empty string
    expect(options.headers['Idempotency-Key']).toBeDefined();
    expect(typeof options.headers['Idempotency-Key']).toBe('string');
    expect(options.headers['Idempotency-Key'].length).toBeGreaterThan(10);
  });

  it('displays reservation contention modal on HTTP 422 INSUFFICIENT_STOCK error', async () => {
    useCartStore.getState().addItem(MOCK_PRODUCTS[0], MOCK_PRODUCTS[0].skus[0], 1);

    vi.spyOn(apiClientModule, 'apiClient').mockRejectedValueOnce(
      new apiClientModule.ApiClientError({
        type: 'https://api.ecommerce.local/errors/insufficient-stock',
        title: 'Inventory Reservation Contention',
        status: 422,
        code: 'INSUFFICIENT_STOCK',
        detail: 'Sản phẩm đã hết hàng do khách hàng khác vừa giữ chỗ.',
        instance: '/api/v1/customers/cust-demo-001/orders',
        timestamp: new Date().toISOString(),
      })
    );

    render(<CheckoutPage />);

    const submitBtn = screen.getByText('Xác nhận Đặt hàng');
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByText('Lỗi giữ chỗ tồn kho (Contention)')).toBeInTheDocument();
      expect(screen.getByText(/Sản phẩm đã hết hàng do khách hàng khác vừa giữ chỗ/)).toBeInTheDocument();
    });
  });
});
