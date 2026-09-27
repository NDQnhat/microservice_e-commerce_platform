import React from 'react';
import { Lock } from 'lucide-react';

export const TERMINAL_STATUSES = [
  'DELIVERED',
  'COMPLETED',
  'CANCELLED',
  'REFUNDED',
  'RESOLVED',
  'IGNORED',
] as const;

export type TerminalStatus = typeof TERMINAL_STATUSES[number];

export function isTerminalStatus(status: string): boolean {
  return TERMINAL_STATUSES.includes(status.toUpperCase() as TerminalStatus);
}

interface TerminalBadgeProps {
  status: string;
  label?: string;
  className?: string;
  tooltip?: string;
}

export const TerminalBadge: React.FC<TerminalBadgeProps> = ({
  status,
  label,
  className = '',
  tooltip = 'Trạng thái kết thúc - Không được phép chỉnh sửa',
}) => {
  const displayStatus = label || status;
  const isTerminal = isTerminalStatus(status);

  // Status-specific color schemes
  const getBadgeStyle = () => {
    switch (status.toUpperCase()) {
      case 'CANCELLED':
      case 'IGNORED':
        return 'bg-rose-500/10 text-rose-400 border-rose-500/20';
      case 'DELIVERED':
      case 'COMPLETED':
      case 'RESOLVED':
        return 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20';
      case 'REFUNDED':
        return 'bg-purple-500/10 text-purple-400 border-purple-500/20';
      default:
        return 'bg-slate-800 text-slate-400 border-slate-700';
    }
  };

  return (
    <span
      title={isTerminal ? tooltip : undefined}
      className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold font-mono border transition ${getBadgeStyle()} ${className}`}
    >
      {isTerminal && <Lock className="h-3 w-3 shrink-0 opacity-80" />}
      <span>{displayStatus}</span>
    </span>
  );
};
