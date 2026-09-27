import React from 'react';
import { useAuthStore } from '@/store/auth-store';
import { Role } from '@/types';
import { ShieldAlert } from 'lucide-react';

interface RbacGuardProps {
  roles: Role[];
  children: React.ReactNode;
  fallback?: React.ReactNode;
  showAccessDenied?: boolean;
}

export const RbacGuard: React.FC<RbacGuardProps> = ({
  roles,
  children,
  fallback = null,
  showAccessDenied = false,
}) => {
  const { hasAnyRole, user } = useAuthStore();
  const allowed = hasAnyRole(roles);

  if (allowed) {
    return <>{children}</>;
  }

  if (showAccessDenied) {
    return (
      <div className="flex flex-col items-center justify-center p-12 rounded-xl border border-rose-500/20 bg-rose-500/5 text-center my-8">
        <div className="p-3 rounded-2xl bg-rose-500/10 text-rose-400 mb-4 border border-rose-500/20">
          <ShieldAlert className="h-8 w-8" />
        </div>
        <h3 className="text-lg font-bold text-white mb-1">Access Restricted (RBAC-403)</h3>
        <p className="text-sm text-slate-400 max-w-md mb-4 leading-relaxed">
          Your active role (<strong className="text-indigo-400 font-mono">{user?.roles.join(', ') || 'NONE'}</strong>)
          does not have sufficient authorization to view or operate this module. Required role(s):{' '}
          <strong className="text-slate-300 font-mono">{roles.join(', ')}</strong>.
        </p>
      </div>
    );
  }

  return <>{fallback}</>;
};
