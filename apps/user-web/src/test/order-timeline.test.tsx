import React from 'react';
import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { OrderTimeline } from '@/components/order/OrderTimeline';
import { OrderTimelineEvent } from '@/types';

describe('OrderTimeline & State Machine Transitions', () => {
  const sampleTimeline: OrderTimelineEvent[] = [
    {
      id: 'tl-1',
      orderId: 'ord-123',
      fromStatus: null,
      toStatus: 'RESERVED',
      actorType: 'CUSTOMER',
      note: 'Khởi tạo đơn hàng & giữ chỗ',
      occurredAt: '2026-09-20T10:00:00Z',
    },
    {
      id: 'tl-2',
      orderId: 'ord-123',
      fromStatus: 'RESERVED',
      toStatus: 'PAID',
      actorType: 'SYSTEM',
      note: 'Xác nhận thanh toán thành công',
      occurredAt: '2026-09-20T10:02:00Z',
    },
  ];

  it('renders all sequential workflow steps from RESERVED to COMPLETED', () => {
    render(<OrderTimeline status="PAID" timeline={sampleTimeline} />);

    expect(screen.getByText('Đặt hàng & Giữ chỗ')).toBeInTheDocument();
    expect(screen.getByText('Đã thanh toán')).toBeInTheDocument();
    expect(screen.getByText('Đang đóng gói')).toBeInTheDocument();
    expect(screen.getByText('Đang vận chuyển')).toBeInTheDocument();
    expect(screen.getByText('Giao hàng thành công')).toBeInTheDocument();
  });

  it('renders detailed chronological timeline events and actor types', () => {
    render(<OrderTimeline status="PAID" timeline={sampleTimeline} />);

    expect(screen.getByText('Khách hàng')).toBeInTheDocument();
    expect(screen.getByText('Hệ thống tự động')).toBeInTheDocument();
    expect(screen.getByText('Khởi tạo đơn hàng & giữ chỗ')).toBeInTheDocument();
    expect(screen.getByText('Xác nhận thanh toán thành công')).toBeInTheDocument();
  });

  it('renders appropriate alert banner for terminal CANCELLED state per BR-001', () => {
    const cancelledTimeline: OrderTimelineEvent[] = [
      ...sampleTimeline,
      {
        id: 'tl-3',
        orderId: 'ord-123',
        fromStatus: 'PAID',
        toStatus: 'CANCELLED',
        actorType: 'CUSTOMER',
        note: 'Khách hàng hủy đơn hàng trước khi đóng gói',
        occurredAt: '2026-09-20T10:05:00Z',
      },
    ];

    render(<OrderTimeline status="CANCELLED" timeline={cancelledTimeline} />);

    expect(screen.getByText('Đơn hàng đã được hủy')).toBeInTheDocument();
    expect(screen.getByText(/Tồn kho đã được giải phóng tự động về kho hàng/)).toBeInTheDocument();
  });
});
