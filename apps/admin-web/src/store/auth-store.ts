import { create } from 'zustand';
import { AdminUser, Role } from '@/types';

export const DEMO_OPERATORS: Record<Role, AdminUser> = {
  SUPER_ADMIN: {
    id: 'usr-admin-001',
    email: 'superadmin@ecommerce.internal',
    fullName: 'Alexander Vance (Super Admin)',
    roles: ['SUPER_ADMIN'],
    isActive: true,
  },
  OPS_ADMIN: {
    id: 'usr-ops-004',
    email: 'ops.admin@ecommerce.internal',
    fullName: 'Elena Rostova (Operations Admin)',
    roles: ['OPS_ADMIN'],
    isActive: true,
  },
  ORDER_OPS_ADMIN: {
    id: 'usr-order-004',
    email: 'order.ops@ecommerce.internal',
    fullName: 'Marcus Kane (Order Operations)',
    roles: ['ORDER_OPS_ADMIN', 'OPS_ADMIN'],
    isActive: true,
  },
  SUPPORT_AGENT: {
    id: 'usr-support-005',
    email: 'support.lead@ecommerce.internal',
    fullName: 'Sarah Chen (Senior Support Specialist)',
    roles: ['SUPPORT_AGENT'],
    isActive: true,
  },
  CATALOG_MANAGER: {
    id: 'usr-catalog-002',
    email: 'catalog.mgr@ecommerce.internal',
    fullName: 'David Bradley (Head of Merchandising)',
    roles: ['CATALOG_MANAGER'],
    isActive: true,
  },
  WAREHOUSE_STAFF: {
    id: 'usr-warehouse-003',
    email: 'warehouse.lead@ecommerce.internal',
    fullName: 'Mateo Silva (Fulfillment Lead)',
    roles: ['WAREHOUSE_STAFF'],
    isActive: true,
  },
  FINANCIAL_AUDITOR: {
    id: 'usr-finance-006',
    email: 'finance.auditor@ecommerce.internal',
    fullName: 'Jonathan Pierce (Lead Financial Auditor)',
    roles: ['FINANCIAL_AUDITOR'],
    isActive: true,
  },
  ORDER_OPERATOR: {
    id: 'usr-order-005',
    email: 'order.operator@ecommerce.internal',
    fullName: 'Marcus Kane (Order Operator)',
    roles: ['ORDER_OPERATOR', 'ORDER_OPS_ADMIN', 'OPS_ADMIN'],
    isActive: true,
  },
  CUSTOMER_SUPPORT: {
    id: 'usr-support-006',
    email: 'customer.support@ecommerce.internal',
    fullName: 'Sarah Chen (Customer Support Specialist)',
    roles: ['CUSTOMER_SUPPORT', 'SUPPORT_AGENT'],
    isActive: true,
  },
};

interface AuthStore {
  user: AdminUser | null;
  accessToken: string | null;
  isAuthenticated: boolean;
  setAuth: (user: AdminUser, token: string) => void;
  switchRole: (role: Role) => void;
  logout: () => void;
  hasRole: (role: Role) => boolean;
  hasAnyRole: (roles: Role[]) => boolean;
}

export const useAuthStore = create<AuthStore>((set, get) => {
  const initialToken = typeof window !== 'undefined' ? localStorage.getItem('admin_access_token') : null;
  const initialUserJson = typeof window !== 'undefined' ? localStorage.getItem('admin_user') : null;
  let initialUser: AdminUser | null = null;
  if (initialUserJson) {
    try {
      initialUser = JSON.parse(initialUserJson);
    } catch {
      initialUser = null;
    }
  }

  // Default to Super Admin if not logged in to make immediate testing seamless
  if (!initialUser && !initialToken) {
    initialUser = DEMO_OPERATORS.SUPER_ADMIN;
  }

  const roleMatches = (userRoles: Role[], targetRole: Role): boolean => {
    if (userRoles.includes('SUPER_ADMIN')) return true;
    if (userRoles.includes(targetRole)) return true;
    if (targetRole === 'OPS_ADMIN' && userRoles.includes('ORDER_OPS_ADMIN')) return true;
    if ((targetRole === 'ORDER_OPERATOR' || targetRole === 'ORDER_OPS_ADMIN') && (userRoles.includes('ORDER_OPERATOR') || userRoles.includes('ORDER_OPS_ADMIN'))) return true;
    if ((targetRole === 'CUSTOMER_SUPPORT' || targetRole === 'SUPPORT_AGENT') && (userRoles.includes('CUSTOMER_SUPPORT') || userRoles.includes('SUPPORT_AGENT'))) return true;
    return false;
  };

  return {
    user: initialUser,
    accessToken: initialToken || 'mock-admin-session-token',
    isAuthenticated: true,
    setAuth: (user, token) => {
      if (typeof window !== 'undefined') {
        localStorage.setItem('admin_access_token', token);
        localStorage.setItem('admin_user', JSON.stringify(user));
      }
      set({ user, accessToken: token, isAuthenticated: true });
    },
    switchRole: (role: Role) => {
      const demoUser = DEMO_OPERATORS[role] || DEMO_OPERATORS.SUPER_ADMIN;
      if (typeof window !== 'undefined') {
        localStorage.setItem('admin_access_token', `demo-token-${role.toLowerCase()}`);
        localStorage.setItem('admin_user', JSON.stringify(demoUser));
      }
      set({ user: demoUser, accessToken: `demo-token-${role.toLowerCase()}`, isAuthenticated: true });
    },
    logout: () => {
      if (typeof window !== 'undefined') {
        localStorage.removeItem('admin_access_token');
        localStorage.removeItem('admin_user');
      }
      set({ user: null, accessToken: null, isAuthenticated: false });
    },
    hasRole: (role: Role) => {
      const { user } = get();
      if (!user) return false;
      return roleMatches(user.roles, role);
    },
    hasAnyRole: (roles: Role[]) => {
      const { user } = get();
      if (!user) return false;
      return roles.some((r) => roleMatches(user.roles, r));
    },
  };
});
