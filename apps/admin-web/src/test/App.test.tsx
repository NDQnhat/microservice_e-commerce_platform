import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { App } from '../App';

describe('Admin Web Portal Integration', () => {
  it('renders enterprise backoffice layout and operational navigation', () => {
    render(<App />);

    // Brand and platform title
    expect(screen.getByText('Backoffice Ops')).toBeInTheDocument();

    // Operational navigation links
    expect(screen.getByText('Operational Dashboard')).toBeInTheDocument();
    expect(screen.getByText('Exceptions Board')).toBeInTheDocument();
    expect(screen.getByText('Order Operations')).toBeInTheDocument();
    expect(screen.getByText('Catalog & Pricing')).toBeInTheDocument();
    expect(screen.getByText('Inventory & Audit')).toBeInTheDocument();
    expect(screen.getByText('Payment Reconciliation')).toBeInTheDocument();
    expect(screen.getByText('Fulfillment & Shipments')).toBeInTheDocument();
    expect(screen.getByText('Customer Support')).toBeInTheDocument();
    expect(screen.getByText('Dynamic Configuration')).toBeInTheDocument();
    expect(screen.getByText('Compliance Audit Logs')).toBeInTheDocument();
    expect(screen.getByText('Quản lý vai trò & quyền')).toBeInTheDocument();

    // System Discovery and Gateway Status
    expect(screen.getByText('EUREKA:8761')).toBeInTheDocument();
    expect(screen.getByText('NETTY:8080')).toBeInTheDocument();
  });

  it('renders operational telemetry overview metrics', async () => {
    render(<App />);

    expect(screen.getByText('System Operational Overview')).toBeInTheDocument();
    expect(screen.getByText('Open Exceptions')).toBeInTheDocument();
    expect(screen.getByText('Under Investigation')).toBeInTheDocument();
    expect(screen.getByText('Resolved Today')).toBeInTheDocument();
    expect(screen.getByText('Active Cluster SLA')).toBeInTheDocument();
  });
});
