import React from 'react';

interface SkeletonTableProps {
  rows?: number;
  cols?: number;
}

export const SkeletonTable: React.FC<SkeletonTableProps> = ({ rows = 5, cols = 5 }) => {
  return (
    <div className="w-full border border-slate-800 rounded-xl overflow-hidden bg-slate-950 p-4 space-y-4 animate-pulse">
      {/* Table Header Skeleton */}
      <div className="flex gap-4 pb-3 border-b border-slate-800/80">
        {Array.from({ length: cols }).map((_, i) => (
          <div key={`header-${i}`} className="h-4 bg-slate-800/80 rounded flex-1" />
        ))}
      </div>

      {/* Table Rows Skeleton */}
      {Array.from({ length: rows }).map((_, r) => (
        <div key={`row-${r}`} className="flex gap-4 items-center py-2.5 border-b border-slate-800/40">
          {Array.from({ length: cols }).map((_, c) => (
            <div
              key={`cell-${r}-${c}`}
              className={`h-4 bg-slate-800/50 rounded flex-1 ${
                c === 0 ? 'w-24' : c === cols - 1 ? 'w-16 ml-auto' : ''
              }`}
            />
          ))}
        </div>
      ))}
    </div>
  );
};
