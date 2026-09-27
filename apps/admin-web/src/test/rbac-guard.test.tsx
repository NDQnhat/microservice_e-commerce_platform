import { render, screen } from '@testing-library/react';
import { describe, it, expect, beforeEach } from 'vitest';
import { RbacGuard } from '../components/ui/RbacGuard';
import { useAuthStore, DEMO_OPERATORS } from '../store/auth-store';

describe('RBAC Guard & Authorization (NFR-RBAC-001)', () => {
  beforeEach(() => {
    useAuthStore.getState().logout();
  });

  it('allows SUPER_ADMIN to access any restricted section', () => {
    useAuthStore.getState().setAuth(DEMO_OPERATORS.SUPER_ADMIN, 'token-super');

    render(
      <RbacGuard roles={['WAREHOUSE_STAFF']} showAccessDenied>
        <div>Protected Warehouse Content</div>
      </RbacGuard>
    );

    expect(screen.getByText('Protected Warehouse Content')).toBeInTheDocument();
  });

  it('blocks SUPPORT_AGENT from accessing WAREHOUSE_STAFF module with RBAC-403', () => {
    useAuthStore.getState().setAuth(DEMO_OPERATORS.SUPPORT_AGENT, 'token-support');

    render(
      <RbacGuard roles={['WAREHOUSE_STAFF']} showAccessDenied>
        <div>Restricted Warehouse Content</div>
      </RbacGuard>
    );

    expect(screen.queryByText('Restricted Warehouse Content')).not.toBeInTheDocument();
    expect(screen.getByText('Access Restricted (RBAC-403)')).toBeInTheDocument();
    expect(screen.getByText(/Required role\(s\):/)).toBeInTheDocument();
  });

  it('allows SUPPORT_AGENT to access customer support action module', () => {
    useAuthStore.getState().setAuth(DEMO_OPERATORS.SUPPORT_AGENT, 'token-support');

    render(
      <RbacGuard roles={['SUPPORT_AGENT']} showAccessDenied>
        <div>Whitelisted Support Actions Console</div>
      </RbacGuard>
    );

    expect(screen.getByText('Whitelisted Support Actions Console')).toBeInTheDocument();
    expect(screen.queryByText('Access Restricted (RBAC-403)')).not.toBeInTheDocument();
  });

  it('allows switching active role through sandbox store', () => {
    useAuthStore.getState().switchRole('CATALOG_MANAGER');
    expect(useAuthStore.getState().user?.roles).toContain('CATALOG_MANAGER');
    expect(useAuthStore.getState().hasRole('CATALOG_MANAGER')).toBe(true);
    expect(useAuthStore.getState().hasRole('WAREHOUSE_STAFF')).toBe(false);
  });
});
