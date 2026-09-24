import React, { useState } from 'react';
import { useAuthStore } from '@/store/auth-store';
import { ShieldCheck } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

export const LoginPage: React.FC = () => {
  const [email, setEmail] = useState('superadmin@ecommerce.internal');
  const [password, setPassword] = useState('admin123456');
  const { setAuth } = useAuthStore();
  const navigate = useNavigate();

  const handleLogin = (e: React.FormEvent) => {
    e.preventDefault();
    // Mock login for foundation scaffolding
    setAuth(
      {
        id: 'usr-admin-1',
        email,
        fullName: 'Lead Operations Super Admin',
        roles: ['SUPER_ADMIN'],
        isActive: true,
      },
      'mock-admin-token'
    );
    navigate('/');
  };

  return (
    <div className="min-h-screen bg-slate-950 flex items-center justify-center p-4">
      <div className="max-w-md w-full border border-slate-800 rounded-2xl bg-slate-900 p-8 space-y-6 shadow-2xl">
        <div className="text-center space-y-2">
          <div className="inline-flex p-3 rounded-xl bg-indigo-600/20 text-indigo-400">
            <ShieldCheck className="h-8 w-8" />
          </div>
          <h2 className="text-2xl font-bold text-white">Backoffice Portal</h2>
          <p className="text-xs text-slate-400">Authenticate with operator credentials</p>
        </div>

        <form onSubmit={handleLogin} className="space-y-4">
          <div className="space-y-1">
            <label className="text-xs font-semibold text-slate-300">Email Address</label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-indigo-500"
              required
            />
          </div>
          <div className="space-y-1">
            <label className="text-xs font-semibold text-slate-300">Password</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-indigo-500"
              required
            />
          </div>
          <button
            type="submit"
            className="w-full py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white font-semibold rounded-lg text-sm transition"
          >
            Sign In to Backoffice
          </button>
        </form>
      </div>
    </div>
  );
};
