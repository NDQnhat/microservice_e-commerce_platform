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
      if (user.roles.includes('SUPER_ADMIN')) return true;
      if (role === 'OPS_ADMIN' && user.roles.includes('ORDER_OPS_ADMIN')) return true;
      return user.roles.includes(role);
    },
    hasAnyRole: (roles: Role[]) => {
      const { user } = get();
      if (!user) return false;
      if (user.roles.includes('SUPER_ADMIN')) return true;
      return roles.some((r) => {
        if (r === 'OPS_ADMIN' && user.roles.includes('ORDER_OPS_ADMIN')) return true;
        return user.roles.includes(r);
      });
    },
  };
});
