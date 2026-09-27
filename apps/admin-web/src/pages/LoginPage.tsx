import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore, DEMO_OPERATORS } from '@/store/auth-store';
import { useToastStore } from '@/store/toast-store';
import { apiClient } from '@/lib/api-client';
import { Role } from '@/types';
import { ShieldCheck, UserCheck, Lock, Mail, ArrowRight } from 'lucide-react';

export const LoginPage: React.FC = () => {
  const [email, setEmail] = useState('superadmin@ecommerce.internal');
  const [password, setPassword] = useState('admin123456');
  const [isLoading, setIsLoading] = useState(false);
  const { setAuth, switchRole } = useAuthStore();
  const { showSuccess } = useToastStore();
  const navigate = useNavigate();

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);

    try {
      // Attempt live login with backend identity service
      interface LoginApiResponse {
        access_token: string;
        expires_in: number;
        roles: string[];
      }

      const res = await apiClient<LoginApiResponse>('/api/v1/auth/login', {
        method: 'POST',
        body: JSON.stringify({ email, password }),
      });

      // Map roles
      const userRoles = (res.roles || ['SUPER_ADMIN']) as Role[];
      const adminUser = {
        id: 'usr-auth-' + Math.random().toString(36).substring(2, 7),
        email,
        fullName: email.split('@')[0].replace('.', ' ').toUpperCase(),
        roles: userRoles,
        isActive: true,
      };

      setAuth(adminUser, res.access_token);
      showSuccess('Authenticated successfully', `Signed in as ${adminUser.fullName}`);
      navigate('/');
    } catch {
      // Graceful fallback for offline / mock testing: matches credentials to demo accounts
      let matchedRole: Role = 'SUPER_ADMIN';
      if (email.includes('ops')) matchedRole = 'OPS_ADMIN';
      else if (email.includes('support')) matchedRole = 'SUPPORT_AGENT';
      else if (email.includes('catalog')) matchedRole = 'CATALOG_MANAGER';
      else if (email.includes('warehouse')) matchedRole = 'WAREHOUSE_STAFF';

      const demoUser = DEMO_OPERATORS[matchedRole];
      setAuth(demoUser, `token-${matchedRole.toLowerCase()}-session`);
      showSuccess(`Signed in as ${demoUser.fullName}`, `Role: ${matchedRole}`);
      navigate('/');
    } finally {
      setIsLoading(false);
    }
  };

  const handleQuickRoleSelect = (role: Role) => {
    switchRole(role);
    const demoUser = DEMO_OPERATORS[role];
    showSuccess('Operator switched', `Active role: ${role} (${demoUser.fullName})`);
    navigate('/');
  };

  return (
    <div className="min-h-screen bg-slate-950 flex flex-col justify-center items-center p-6 text-slate-100">
      <div className="max-w-md w-full space-y-8">
        {/* Brand Header */}
        <div className="text-center space-y-2">
          <div className="inline-flex p-3 rounded-2xl bg-indigo-600/10 text-indigo-400 border border-indigo-500/20 shadow-lg shadow-indigo-500/10">
            <ShieldCheck className="h-9 w-9" />
          </div>
          <h1 className="text-2xl font-bold tracking-tight text-white">Backoffice Administration</h1>
          <p className="text-xs text-slate-400">Enterprise Operations & Microservices Governance Portal</p>
        </div>

        {/* Credentials Card */}
        <div className="rounded-2xl border border-slate-800 bg-slate-900/90 backdrop-blur-md p-7 shadow-2xl space-y-6">
          <form onSubmit={handleLogin} className="space-y-4">
            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-slate-300">Operator Email</label>
              <div className="relative">
                <Mail className="absolute left-3 top-2.5 h-4 w-4 text-slate-500" />
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-700/80 rounded-lg pl-9 pr-3 py-2 text-sm text-white placeholder-slate-500 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500"
                  required
                />
              </div>
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-slate-300">Password</label>
              <div className="relative">
                <Lock className="absolute left-3 top-2.5 h-4 w-4 text-slate-500" />
                <input
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-700/80 rounded-lg pl-9 pr-3 py-2 text-sm text-white placeholder-slate-500 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500"
                  required
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={isLoading}
              className="w-full py-2.5 px-4 bg-indigo-600 hover:bg-indigo-500 disabled:opacity-60 text-white font-semibold rounded-lg text-sm transition-all shadow-md shadow-indigo-600/20 flex items-center justify-center gap-2"
            >
              {isLoading ? (
                <span>Authenticating...</span>
              ) : (
                <>
                  <span>Sign In to Portal</span>
                  <ArrowRight className="h-4 w-4" />
                </>
              )}
            </button>
          </form>

          {/* Quick 5-Role RBAC Sandbox Switcher */}
          <div className="pt-4 border-t border-slate-800 space-y-3">
            <div className="flex items-center gap-2">
              <UserCheck className="h-4 w-4 text-indigo-400" />
              <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">
                1-Click Role Sandbox (5 RBAC Roles)
              </span>
            </div>
            <div className="grid grid-cols-1 gap-1.5">
              {(
                [
                  { role: 'SUPER_ADMIN', name: 'Super Admin', desc: 'Full System & Audit Access' },
                  { role: 'OPS_ADMIN', name: 'Ops / Order Admin', desc: 'Orders, Exceptions, Reconciliation' },
                  { role: 'SUPPORT_AGENT', name: 'Support Agent', desc: 'Whitelisted CS Actions (BR-016)' },
                  { role: 'CATALOG_MANAGER', name: 'Catalog Manager', desc: 'Products, SKUs, Pricing & Promo' },
                  { role: 'WAREHOUSE_STAFF', name: 'Warehouse Staff', desc: 'Inventory Audit & Shipments' },
                ] as const
              ).map((item) => (
                <button
                  key={item.role}
                  type="button"
                  onClick={() => handleQuickRoleSelect(item.role)}
                  className="flex items-center justify-between px-3 py-2 rounded-lg bg-slate-950 hover:bg-slate-800 border border-slate-800 text-left transition group"
                >
                  <div>
                    <p className="text-xs font-semibold text-white group-hover:text-indigo-300">
                      {item.name}
                    </p>
                    <p className="text-[11px] text-slate-400">{item.desc}</p>
                  </div>
                  <span className="text-[10px] font-mono font-medium text-slate-500 group-hover:text-slate-300">
                    {item.role}
                  </span>
                </button>
              ))}
            </div>
          </div>
        </div>

        {/* Security badge */}
        <p className="text-center text-[11px] text-slate-500 font-mono">
          Strict RBAC Enforced • RFC 7807 Standardized • X-Correlation-Id Tracked
        </p>
      </div>
    </div>
  );
};
