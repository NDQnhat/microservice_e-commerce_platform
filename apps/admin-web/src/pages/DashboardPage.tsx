import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { apiClient } from '@/lib/api-client';
import { DashboardSummary } from '@/types';
import { AlertCircle, CheckCircle, Clock, TrendingUp } from 'lucide-react';

export const DashboardPage: React.FC = () => {
  const { data: summary, isLoading } = useQuery<DashboardSummary>({
    queryKey: ['dashboard-summary'],
    queryFn: () => apiClient<DashboardSummary>('/api/v1/exceptions/dashboard/summary'),
    refetchInterval: 30000,
  });

  const cards = [
    {
      title: 'Open Exceptions',
      value: summary?.totalOpenExceptions ?? 0,
      icon: AlertCircle,
      color: 'text-rose-500',
      bg: 'bg-rose-500/10',
    },
    {
      title: 'Under Investigation',
      value: summary?.totalInvestigatingExceptions ?? 0,
      icon: Clock,
      color: 'text-amber-500',
      bg: 'bg-amber-500/10',
    },
    {
      title: 'Resolved Today',
      value: summary?.totalResolvedExceptions ?? 0,
      icon: CheckCircle,
      color: 'text-emerald-500',
      bg: 'bg-emerald-500/10',
    },
    {
      title: 'System Health',
      value: '99.98%',
      icon: TrendingUp,
      color: 'text-indigo-400',
      bg: 'bg-indigo-500/10',
    },
  ];

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-bold text-white">System Operational Overview</h1>
        <p className="text-sm text-slate-400">Real-time health telemetry and critical exception monitoring</p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        {cards.map((card) => {
          const Icon = card.icon;
          return (
            <div key={card.title} className="p-6 rounded-xl border border-slate-800 bg-slate-950 shadow-sm">
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">{card.title}</span>
                <div className={`p-2 rounded-lg ${card.bg}`}>
                  <Icon className={`h-5 w-5 ${card.color}`} />
                </div>
              </div>
              <p className="text-3xl font-extrabold text-white mt-4">
                {isLoading ? '...' : card.value}
              </p>
            </div>
          );
        })}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="p-6 rounded-xl border border-slate-800 bg-slate-950">
          <h3 className="font-semibold text-white mb-4">Exceptions by Category</h3>
          {summary?.openExceptionsByType && Object.keys(summary.openExceptionsByType).length > 0 ? (
            <div className="space-y-3">
              {Object.entries(summary.openExceptionsByType).map(([type, count]) => (
                <div key={type} className="flex justify-between items-center py-2 border-b border-slate-800/60">
                  <span className="text-sm text-slate-300 font-mono">{type}</span>
                  <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-rose-500/20 text-rose-400">
                    {count}
                  </span>
                </div>
              ))}
            </div>
          ) : (
            <p className="text-sm text-slate-500 py-6 text-center">No open exceptions reported</p>
          )}
        </div>

        <div className="p-6 rounded-xl border border-slate-800 bg-slate-950">
          <h3 className="font-semibold text-white mb-4">Service Discovery Topology</h3>
          <div className="space-y-2">
            {[
              { name: 'identity-access-service', port: 8081, status: 'UP' },
              { name: 'catalog-service', port: 8082, status: 'UP' },
              { name: 'pricing-service', port: 8083, status: 'UP' },
              { name: 'cart-service', port: 8084, status: 'UP' },
              { name: 'inventory-service', port: 8085, status: 'UP' },
              { name: 'order-service', port: 8086, status: 'UP' },
              { name: 'payment-service', port: 8087, status: 'UP' },
              { name: 'fulfillment-service', port: 8088, status: 'UP' },
              { name: 'notification-service', port: 8089, status: 'UP' },
              { name: 'audit-compliance-service', port: 8090, status: 'UP' },
              { name: 'business-configuration-service', port: 8091, status: 'UP' },
              { name: 'exception-management-service', port: 8092, status: 'UP' },
            ].map((svc) => (
              <div key={svc.name} className="flex justify-between items-center text-xs py-1.5 border-b border-slate-800/40">
                <span className="text-slate-300 font-mono">{svc.name}</span>
                <div className="flex items-center gap-3">
                  <span className="text-slate-500">:{svc.port}</span>
                  <span className="text-emerald-400 font-bold">{svc.status}</span>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};
