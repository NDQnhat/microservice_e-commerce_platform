import React from 'react';
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { useAuthStore } from '@/store/auth-store';
import { NetworkBanner } from '@/components/ui/NetworkBanner';
import { ToastContainer } from '@/components/ui/ToastContainer';
import { Role } from '@/types';
import {
  LayoutDashboard,
  AlertTriangle,
  Sliders,
  Package,
  ShoppingCart,
  Boxes,
  Truck,
  CreditCard,
  ShieldAlert,
  Headset,
  LogOut,
  ShieldCheck,
  UserCheck,
  Users,
} from 'lucide-react';

interface NavItem {
  name: string;
  href: string;
  icon: React.ComponentType<{ className?: string }>;
  allowedRoles?: Role[];
}

export const Layout: React.FC = () => {
  const { user, logout, switchRole, hasAnyRole } = useAuthStore();
  const location = useLocation();
  const navigate = useNavigate();

  const navigation: NavItem[] = [
    {
      name: 'Operational Dashboard',
      href: '/',
      icon: LayoutDashboard,
    },
    {
      name: 'Exceptions Board',
      href: '/exceptions',
      icon: AlertTriangle,
      allowedRoles: ['SUPER_ADMIN', 'OPS_ADMIN', 'ORDER_OPS_ADMIN'],
    },
    {
      name: 'Order Operations',
      href: '/orders',
      icon: ShoppingCart,
      allowedRoles: ['SUPER_ADMIN', 'OPS_ADMIN', 'ORDER_OPS_ADMIN', 'ORDER_OPERATOR', 'SUPPORT_AGENT'],
    },
    {
      name: 'Catalog & Pricing',
      href: '/catalog',
      icon: Package,
      allowedRoles: ['SUPER_ADMIN', 'CATALOG_MANAGER'],
    },
    {
      name: 'Inventory & Audit',
      href: '/inventory',
      icon: Boxes,
      allowedRoles: ['SUPER_ADMIN', 'WAREHOUSE_STAFF', 'OPS_ADMIN'],
    },
    {
      name: 'Payment Reconciliation',
      href: '/reconciliation',
      icon: CreditCard,
      allowedRoles: ['SUPER_ADMIN', 'OPS_ADMIN', 'FINANCIAL_AUDITOR'],
    },
    {
      name: 'Fulfillment & Shipments',
      href: '/shipments',
      icon: Truck,
      allowedRoles: ['SUPER_ADMIN', 'WAREHOUSE_STAFF', 'OPS_ADMIN'],
    },
    {
      name: 'Customer Support',
      href: '/support',
      icon: Headset,
      allowedRoles: ['SUPER_ADMIN', 'SUPPORT_AGENT', 'CUSTOMER_SUPPORT'],
    },
    {
      name: 'Dynamic Configuration',
      href: '/configurations',
      icon: Sliders,
      allowedRoles: ['SUPER_ADMIN'],
    },
    {
      name: 'Compliance Audit Logs',
      href: '/audit-logs',
      icon: ShieldAlert,
      allowedRoles: ['SUPER_ADMIN'],
    },
    {
      name: 'Quản lý vai trò & quyền',
      href: '/roles',
      icon: Users,
      allowedRoles: ['SUPER_ADMIN'],
    },
  ];

  const handleRoleChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const role = e.target.value as Role;
    switchRole(role);
  };

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  // Filter items based on current operator's active roles
  const visibleNav = navigation.filter((item) => {
    if (!item.allowedRoles) return true;
    return hasAnyRole(item.allowedRoles);
  });

  return (
    <div className="flex flex-col h-screen bg-slate-950 text-slate-100 antialiased overflow-hidden font-sans">
      <NetworkBanner />
      <ToastContainer />

      <div className="flex flex-1 min-h-0 overflow-hidden">
        {/* Sidebar */}
        <aside className="w-64 border-r border-slate-800 bg-slate-950 flex flex-col shrink-0">
        {/* Brand Header */}
        <div className="h-16 px-6 flex items-center gap-3 border-b border-slate-800 bg-slate-950">
          <div className="p-1.5 rounded-lg bg-indigo-600/10 text-indigo-400 border border-indigo-500/20">
            <ShieldCheck className="h-6 w-6" />
          </div>
          <div>
            <span className="font-bold text-sm text-white tracking-tight block">Backoffice Ops</span>
            <span className="text-[10px] font-mono text-slate-500 block">v2.2 Enterprise</span>
          </div>
        </div>

        {/* Navigation Items */}
        <nav className="flex-1 px-3 py-4 space-y-1 overflow-y-auto">
          <div className="px-3 pb-2 text-[10px] font-bold uppercase tracking-wider text-slate-500">
            Operations Modules
          </div>
          {visibleNav.map((item) => {
            const isActive =
              item.href === '/'
                ? location.pathname === '/'
                : location.pathname.startsWith(item.href);
            const Icon = item.icon;
            return (
              <Link
                key={item.name}
                to={item.href}
                className={`flex items-center gap-3 px-3 py-2 rounded-lg text-xs font-semibold transition ${
                  isActive
                    ? 'bg-indigo-600 text-white shadow-sm shadow-indigo-500/20'
                    : 'text-slate-400 hover:text-white hover:bg-slate-900'
                }`}
              >
                <Icon className={`h-4 w-4 ${isActive ? 'text-white' : 'text-slate-400'}`} />
                <span className="truncate">{item.name}</span>
              </Link>
            );
          })}
        </nav>

        {/* Operator Profile & Role Switcher */}
        <div className="p-3 border-t border-slate-800 bg-slate-900/60 space-y-2.5">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-1.5 text-[11px] font-semibold text-slate-400">
              <UserCheck className="h-3.5 w-3.5 text-indigo-400" />
              <span>RBAC Role Sandbox</span>
            </div>
            <button
              onClick={handleLogout}
              title="Sign Out"
              className="p-1 text-slate-400 hover:text-rose-400 rounded hover:bg-slate-800 transition"
            >
              <LogOut className="h-4 w-4" />
            </button>
          </div>

          <select
            value={user?.roles[0] || 'SUPER_ADMIN'}
            onChange={handleRoleChange}
            className="w-full bg-slate-950 border border-slate-700/80 rounded-lg px-2.5 py-1.5 text-xs text-indigo-300 font-mono focus:outline-none focus:border-indigo-500"
          >
            <option value="SUPER_ADMIN">SUPER_ADMIN (Full Access)</option>
            <option value="OPS_ADMIN">OPS_ADMIN (Orders & Ops)</option>
            <option value="ORDER_OPERATOR">ORDER_OPERATOR (Order Flow)</option>
            <option value="SUPPORT_AGENT">SUPPORT_AGENT (Customer Care)</option>
            <option value="CUSTOMER_SUPPORT">CUSTOMER_SUPPORT (Assisted Ops)</option>
            <option value="CATALOG_MANAGER">CATALOG_MANAGER (Catalog)</option>
            <option value="WAREHOUSE_STAFF">WAREHOUSE_STAFF (Inventory)</option>
            <option value="FINANCIAL_AUDITOR">FINANCIAL_AUDITOR (Ledger Audit)</option>
          </select>

          <div className="px-1 text-[11px] text-slate-400 truncate">
            <p className="font-semibold text-white truncate">{user?.fullName || 'Active Operator'}</p>
            <p className="font-mono text-[10px] text-slate-500 truncate">{user?.email}</p>
          </div>
        </div>
      </aside>

      {/* Main Content Area */}
      <div className="flex-1 flex flex-col min-w-0 overflow-hidden">
        {/* Top App Bar */}
        <header className="h-16 border-b border-slate-800 bg-slate-950/80 backdrop-blur-md px-8 flex items-center justify-between shrink-0 z-10">
          <div className="flex items-center gap-3">
            <h2 className="text-base font-bold text-white tracking-tight">
              {location.pathname === '/'
                ? 'Operational Telemetry & Health'
                : visibleNav.find((n) => n.href === location.pathname)?.name ||
                  location.pathname.replace('/', '').replace(/-/g, ' ').toUpperCase()}
            </h2>
          </div>

          <div className="flex items-center gap-5 text-xs text-slate-400">
            <div className="flex items-center gap-2">
              <span className="text-slate-500">Service Discovery:</span>
              <span className="font-mono text-emerald-400 font-bold flex items-center gap-1">
                <span className="h-1.5 w-1.5 rounded-full bg-emerald-400 animate-pulse" />
                EUREKA:8761
              </span>
            </div>
            <div className="flex items-center gap-2">
              <span className="text-slate-500">Gateway:</span>
              <span className="font-mono text-emerald-400 font-bold">NETTY:8080</span>
            </div>
            <div className="h-4 w-px bg-slate-800" />
            <div className="font-mono text-[11px] text-slate-400">
              Role: <strong className="text-indigo-400">{user?.roles[0]}</strong>
            </div>
          </div>
        </header>

        {/* Page Content Body */}
        <main className="flex-1 overflow-y-auto p-8 bg-slate-950">
          <div className="max-w-7xl mx-auto space-y-8">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  </div>
);
};
