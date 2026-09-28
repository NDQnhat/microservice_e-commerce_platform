import React from 'react';
import Link from 'next/link';
import { PackageOpen } from 'lucide-react';
import { cn } from '@/lib/utils';

interface EmptyStateProps {
  icon?: React.ReactNode;
  title: string;
  description: string;
  actionText?: string;
  actionHref?: string;
  onAction?: () => void;
  className?: string;
}

export function EmptyState({
  icon,
  title,
  description,
  actionText,
  actionHref,
  onAction,
  className,
}: EmptyStateProps) {
  return (
    <div
      className={cn(
        'flex flex-col items-center justify-center text-center p-8 sm:p-12 rounded-2xl border border-dashed border-zinc-300 bg-zinc-50/50 max-w-lg mx-auto',
        className
      )}
    >
      <div className="w-16 h-16 rounded-2xl bg-zinc-100 flex items-center justify-center text-zinc-400 mb-4 shadow-xs">
        {icon || <PackageOpen className="w-8 h-8 text-zinc-400" />}
      </div>
      <h3 className="text-base sm:text-lg font-bold text-zinc-900">{title}</h3>
      <p className="mt-1.5 text-xs sm:text-sm text-zinc-500 max-w-sm leading-relaxed">
        {description}
      </p>

      {(actionText && (actionHref || onAction)) && (
        <div className="mt-6">
          {actionHref ? (
            <Link
              href={actionHref}
              className="inline-flex items-center justify-center px-5 py-2.5 rounded-full bg-zinc-900 hover:bg-zinc-800 text-white text-xs sm:text-sm font-semibold transition shadow-xs"
            >
              {actionText}
            </Link>
          ) : (
            <button
              onClick={onAction}
              className="inline-flex items-center justify-center px-5 py-2.5 rounded-full bg-zinc-900 hover:bg-zinc-800 text-white text-xs sm:text-sm font-semibold transition shadow-xs"
            >
              {actionText}
            </button>
          )}
        </div>
      )}
    </div>
  );
}
