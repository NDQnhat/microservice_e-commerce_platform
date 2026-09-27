import React from 'react';

interface SkeletonCardProps {
  count?: number;
}

export const SkeletonCard: React.FC<SkeletonCardProps> = ({ count = 4 }) => {
  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
      {Array.from({ length: count }).map((_, i) => (
        <div
          key={`skeleton-card-${i}`}
          className="p-6 rounded-xl border border-slate-800 bg-slate-950 shadow-sm animate-pulse space-y-4"
        >
          <div className="flex items-center justify-between">
            <div className="h-3 w-28 bg-slate-800 rounded" />
            <div className="h-8 w-8 bg-slate-800 rounded-lg" />
          </div>
          <div className="h-8 w-20 bg-slate-800 rounded mt-3" />
          <div className="h-2 w-full bg-slate-800/60 rounded" />
        </div>
      ))}
    </div>
  );
};
