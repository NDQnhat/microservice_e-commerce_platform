import { create } from 'zustand';
import { AdminUser, Role } from '@/types';

interface AuthStore {
  user: AdminUser | null;
  accessToken: string | null;
  isAuthenticated: boolean;
  setAuth: (user: AdminUser, token: string) => void;
  logout: () => void;
  hasRole: (role: Role) => boolean;
  hasAnyRole: (roles: Role[]) => boolean;
}

export const useAuthStore = create<AuthStore>((set, get) => ({
  user: null,
  accessToken: typeof window !== 'undefined' ? localStorage.getItem('admin_access_token') : null,
  isAuthenticated: false,
  setAuth: (user, token) => {
    localStorage.setItem('admin_access_token', token);
    set({ user, accessToken: token, isAuthenticated: true });
  },
  logout: () => {
    localStorage.removeItem('admin_access_token');
    set({ user: null, accessToken: null, isAuthenticated: false });
  },
  hasRole: (role: Role) => {
    const { user } = get();
    if (!user) return false;
    return user.roles.includes('SUPER_ADMIN') || user.roles.includes(role);
  },
  hasAnyRole: (roles: Role[]) => {
    const { user } = get();
    if (!user) return false;
    if (user.roles.includes('SUPER_ADMIN')) return true;
    return roles.some((r) => user.roles.includes(r));
  },
}));
