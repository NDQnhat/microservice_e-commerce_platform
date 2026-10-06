import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { User, CustomerAddress, AuthResponse } from '@/types';
import { apiClient } from '@/lib/api-client';
import { MOCK_USER_ADDRESSES, MOCK_CUSTOMER_ID } from '@/lib/mock-data';

interface UserState {
  user: User | null;
  accessToken: string | null;
  isAuthenticated: boolean;
  addresses: CustomerAddress[];
  isHydrated: boolean;
  setUser: (user: User, token: string) => void;
  setAddresses: (addresses: CustomerAddress[]) => void;
  loadAddresses: () => Promise<void>;
  addAddress: (addr: Omit<CustomerAddress, 'id'>) => Promise<CustomerAddress>;
  updateAddress: (id: string, addr: Partial<CustomerAddress>) => Promise<CustomerAddress>;
  deleteAddress: (id: string) => Promise<void>;
  setDefaultAddress: (id: string) => Promise<void>;
  login: (email: string, pass: string) => Promise<AuthResponse>;
  register: (fullName: string, email: string, pass: string) => Promise<User>;
  logout: () => void;
  setHydrated: () => void;
}

export const useUserStore = create<UserState>()(
  persist(
    (set, get) => ({
      user: null,
      accessToken: null,
      isAuthenticated: false,
      addresses: [],
      isHydrated: false,

      setHydrated: () => set({ isHydrated: true }),

      setUser: (user, token) => {
        if (typeof window !== 'undefined') {
          localStorage.setItem('access_token', token);
        }
        set({ user, accessToken: token, isAuthenticated: true });
      },

      setAddresses: (addresses) => set({ addresses }),

      loadAddresses: async () => {
        const { user } = get();
        if (!user) return;
        try {
          const res = await apiClient<CustomerAddress[]>(`/api/v1/customers/${user.id}/addresses`);
          set({ addresses: res });
        } catch {
          // Fallback in-memory
        }
      },

      addAddress: async (addr) => {
        const { user, addresses } = get();
        const userId = user?.id || MOCK_CUSTOMER_ID;
        const res = await apiClient<CustomerAddress>(`/api/v1/customers/${userId}/addresses`, {
          method: 'POST',
          body: JSON.stringify(addr),
        });

        let updatedList = [...addresses];
        if (res.isDefault) {
          updatedList = updatedList.map((a) => ({ ...a, isDefault: false }));
        }
        updatedList.push(res);
        set({ addresses: updatedList });
        return res;
      },

      updateAddress: async (id, addr) => {
        const { user, addresses } = get();
        const userId = user?.id || MOCK_CUSTOMER_ID;
        const res = await apiClient<CustomerAddress>(`/api/v1/customers/${userId}/addresses/${id}`, {
          method: 'PUT',
          body: JSON.stringify(addr),
        });

        let updatedList = addresses.map((a) => (a.id === id ? res : a));
        if (res.isDefault) {
          updatedList = updatedList.map((a) => (a.id === id ? a : { ...a, isDefault: false }));
        }
        set({ addresses: updatedList });
        return res;
      },

      deleteAddress: async (id) => {
        const { user, addresses } = get();
        const userId = user?.id || MOCK_CUSTOMER_ID;
        await apiClient(`/api/v1/customers/${userId}/addresses/${id}`, {
          method: 'DELETE',
        });
        set({ addresses: addresses.filter((a) => a.id !== id) });
      },

      setDefaultAddress: async (id) => {
        const { addresses } = get();
        const target = addresses.find((a) => a.id === id);
        if (!target) return;
        await get().updateAddress(id, { ...target, isDefault: true });
      },

      login: async (email, password) => {
        const res = await apiClient<AuthResponse>('/api/v1/auth/login', {
          method: 'POST',
          body: JSON.stringify({ email, password }),
        });
        get().setUser(res.user, res.accessToken);
        await get().loadAddresses();
        return res;
      },

      register: async (fullName, email, password) => {
        const res = await apiClient<User>('/api/v1/customers/register', {
          method: 'POST',
          body: JSON.stringify({ full_name: fullName, email, password }),
        });
        return res;
      },

      logout: () => {
        if (typeof window !== 'undefined') {
          localStorage.removeItem('access_token');
        }
        set({
          user: null,
          accessToken: null,
          isAuthenticated: false,
          addresses: [],
        });
      },
    }),
    {
      name: 'user-store-storage',
      onRehydrateStorage: () => (state) => {
        state?.setHydrated();
      },
    }
  )
);
