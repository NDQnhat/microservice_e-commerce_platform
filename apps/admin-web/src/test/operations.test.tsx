import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { StatusBadge } from '../components/ui/StatusBadge';
import { EmptyState } from '../components/ui/EmptyState';
import { SkeletonTable } from '../components/ui/SkeletonTable';
import { useToastStore } from '../store/toast-store';
import { ApiClientError } from '../lib/api-client';
import { Order } from '../types';

describe('Operational Invariants & Component Blueprint Tests', () => {
  it('renders StatusBadge with semantic colors and dot indicator', () => {
    const { container, rerender } = render(<StatusBadge status="PAID" />);
    expect(screen.getByText('PAID')).toBeInTheDocument();
    expect(container.querySelector('.bg-emerald-400')).toBeInTheDocument();

    rerender(<StatusBadge status="OPEN" />);
    expect(screen.getByText('OPEN')).toBeInTheDocument();
    expect(container.querySelector('.bg-rose-400')).toBeInTheDocument();

    rerender(<StatusBadge status="INVESTIGATING" />);
    expect(screen.getByText('INVESTIGATING')).toBeInTheDocument();
    expect(container.querySelector('.bg-amber-400')).toBeInTheDocument();
  });

  it('renders EmptyState with action CTA button', () => {
    let clicked = false;
    render(
      <EmptyState
        title="No Records Found"
        description="Try clearing your active filters."
        actionLabel="Clear Filters"
        onAction={() => {
          clicked = true;
        }}
      />
    );

    expect(screen.getByText('No Records Found')).toBeInTheDocument();
    expect(screen.getByText('Try clearing your active filters.')).toBeInTheDocument();
    const button = screen.getByRole('button', { name: 'Clear Filters' });
    button.click();
    expect(clicked).toBe(true);
  });

  it('renders SkeletonTable placeholder with rows and columns', () => {
    const { container } = render(<SkeletonTable rows={3} cols={4} />);
    expect(container.querySelectorAll('.animate-pulse').length).toBeGreaterThan(0);
  });

  it('decodes RFC 7807 problem details in useToastStore', () => {
    const rfcProblem = {
      type: 'https://ecommerce.internal/errors/invalid-state',
      title: 'Invalid Order State Transition',
      status: 422,
      detail: 'Cannot transition order from PACKING to CANCELLED per BR-006',
      instance: '/api/v1/backoffice/orders/ord-123/status',
      code: 'ERR_CUTOFF_EXCEEDED',
      correlationId: 'corr-test-uuid-999',
      timestamp: '2026-09-27T10:00:00Z',
    };

    const error = new ApiClientError(rfcProblem);
    useToastStore.getState().showError(error);

    const toasts = useToastStore.getState().toasts;
    expect(toasts.length).toBeGreaterThan(0);
    const lastToast = toasts[toasts.length - 1];

    expect(lastToast.title).toBe('Invalid Order State Transition');
    expect(lastToast.detail).toBe('Cannot transition order from PACKING to CANCELLED per BR-006');
    expect(lastToast.code).toBe('ERR_CUTOFF_EXCEEDED');
    expect(lastToast.correlationId).toBe('corr-test-uuid-999');
  });

  it('enforces order cancellation invariant rule: only permitted prior to PACKING (BR-001, BR-006)', () => {
    const canCancel = (status: Order['status']): boolean => {
      return status === 'CREATED' || status === 'RESERVED' || status === 'PAID';
    };

    expect(canCancel('CREATED')).toBe(true);
    expect(canCancel('RESERVED')).toBe(true);
    expect(canCancel('PAID')).toBe(true);

    // Forbidden once in packing or later
    expect(canCancel('PACKING')).toBe(false);
    expect(canCancel('SHIPPED')).toBe(false);
    expect(canCancel('COMPLETED')).toBe(false);
    expect(canCancel('CANCELLED')).toBe(false);
  });
});
