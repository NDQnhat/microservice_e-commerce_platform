import React from 'react';
import { LucideIcon } from 'lucide-react';

interface MetricCardProps {
  title: string;
  value: string | number;
  icon: LucideIcon;
  colorScheme?: 'indigo' | 'emerald' | 'amber' | 'rose' | 'cyan';
  delta?: {
    value: string;
    isPositive: boolean;
  };
  sparklineProgress?: number; // 0 to 100
  isLoading?: boolean;
}

export const MetricCard: React.FC<MetricCardProps> = ({
  title,
  value,
  icon: Icon,
  colorScheme = 'indigo',
  delta,
  sparklineProgress = 70,
  isLoading = false,
}) => {
  const colorStyles = {
    indigo: {
      bg: 'bg-indigo-500/10',
      text: 'text-indigo-400',
      border: 'border-indigo-500/20',
      bar: 'bg-indigo-500',
    },
    emerald: {
      bg: 'bg-emerald-500/10',
      text: 'text-emerald-400',
      border: 'border-emerald-500/20',
      bar: 'bg-emerald-500',
    },
    amber: {
      bg: 'bg-amber-500/10',
      text: 'text-amber-400',
      border: 'border-amber-500/20',
      bar: 'bg-amber-500',
    },
    rose: {
      bg: 'bg-rose-500/10',
      text: 'text-rose-400',
      border: 'border-rose-500/20',
      bar: 'bg-rose-500',
    },
    cyan: {
      bg: 'bg-cyan-500/10',
      text: 'text-cyan-400',
      border: 'border-cyan-500/20',
      bar: 'bg-cyan-500',
    },
  };

  const scheme = colorStyles[colorScheme];

  return (
    <div className="p-6 rounded-xl border border-slate-800/80 bg-slate-900/90 backdrop-blur-md shadow-sm hover:border-slate-700/80 transition-all flex flex-col justify-between">
      <div>
        <div className="flex items-center justify-between">
          <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">{title}</span>
          <div className={`p-2.5 rounded-xl ${scheme.bg} ${scheme.text} border ${scheme.border}`}>
            <Icon className="h-5 w-5" />
          </div>
        </div>
        <div className="mt-3 flex items-baseline gap-3">
          <p className="text-3xl font-extrabold text-white tracking-tight">
            {isLoading ? '...' : value}
          </p>
          {delta && (
            <span
              className={`text-xs font-semibold px-2 py-0.5 rounded-full ${
                delta.isPositive
                  ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'
                  : 'bg-rose-500/10 text-rose-400 border border-rose-500/20'
              }`}
            >
              {delta.value}
            </span>
          )}
        </div>
      </div>

      {/* Mini sparkline bar */}
      <div className="mt-5 w-full bg-slate-800/80 h-1.5 rounded-full overflow-hidden">
        <div
          className={`h-full rounded-full ${scheme.bar} transition-all duration-500`}
          style={{ width: `${Math.min(100, Math.max(0, sparklineProgress))}%` }}
        />
      </div>
    </div>
  );
};
