import React from 'react';
import { Link, Outlet, useLocation } from 'react-router-dom';
import { useAuthStore } from '@/store/auth-store';
import {
  LayoutDashboard,
  AlertTriangle,
  Sliders,
  Package,
  ShoppingCart,
  Boxes,
  LogOut,
  ShieldCheck,
} from 'lucide-react';

export const Layout: React.FC = () => {
  const { user, logout } = useAuthStore();
  const location = useLocation();

  const navigation = [
    { name: 'Dashboard', href: '/', icon: LayoutDashboard },
    { name: 'Exceptions Board', href: '/exceptions', icon: AlertTriangle },
    { name: 'Business Config', href: '/configurations', icon: Sliders },
    { name: 'Orders', href: '/orders', icon: ShoppingCart },
    { name: 'Catalog', href: '/catalog', icon: Package },
    { name: 'Inventory', href: '/inventory', icon: Boxes },
  ];

  return (
    <div className="flex h-screen bg-slate-900 text-slate-100">
      {/* Sidebar */}
      <aside className="w-64 border-r border-slate-800 bg-slate-950 flex flex-col">
        <div className="h-16 px-6 flex items-center gap-3 border-b border-slate-800">
          <ShieldCheck className="h-7 w-7 text-indigo-500" />
          <span className="font-bold text-lg text-white">Backoffice Ops</span>
        </div>
        <nav className="flex-1 px-4 py-6 space-y-1 overflow-y-auto">
          {navigation.map((item) => {
            const isActive = location.pathname === item.href;
            const Icon = item.icon;
            return (
              <Link
                key={item.name}
                to={item.href}
                className={`flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition ${
                  isActive
                    ? 'bg-indigo-600 text-white font-semibold'
                    : 'text-slate-400 hover:text-white hover:bg-slate-800'
                }`}
              >
                <Icon className="h-5 w-5" />
                {item.name}
              </Link>
            );
          })}
        </nav>
        <div className="p-4 border-t border-slate-800 flex items-center justify-between">
          <div>
            <p className="text-sm font-medium text-white">{user?.fullName || 'Operator'}</p>
            <p className="text-xs text-indigo-400">{user?.roles[0] || 'SUPER_ADMIN'}</p>
          </div>
          <button
            onClick={logout}
            title="Sign out"
            className="p-1.5 text-slate-400 hover:text-white rounded hover:bg-slate-800 transition"
          >
            <LogOut className="h-5 w-5" />
          </button>
        </div>
      </aside>

      {/* Main Content */}
      <main className="flex-1 flex flex-col overflow-hidden">
        <header className="h-16 border-b border-slate-800 bg-slate-950/60 backdrop-blur px-8 flex items-center justify-between">
          <h2 className="text-lg font-semibold text-white capitalize">
            {location.pathname === '/' ? 'Operational Dashboard' : location.pathname.replace('/', '').replace('-', ' ')}
          </h2>
          <div className="flex items-center gap-4 text-xs text-slate-400">
            <span>Cluster Status: <strong className="text-emerald-400">HEALTHY</strong></span>
            <span>Gateway: <strong className="text-emerald-400">CONNECTED</strong></span>
          </div>
        </header>
        <div className="flex-1 overflow-y-auto p-8">
          <Outlet />
        </div>
      </main>
    </div>
  );
};
