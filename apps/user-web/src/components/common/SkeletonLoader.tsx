import React from 'react';
import { cn } from '@/lib/utils';

export function Skeleton({ className, ...props }: React.HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={cn('animate-pulse rounded-md bg-zinc-200/80', className)}
      {...props}
    />
  );
}

export function ProductCardSkeleton() {
  return (
    <div className="rounded-2xl border border-zinc-200/80 bg-white p-3 space-y-3 shadow-xs">
      <Skeleton className="aspect-square w-full rounded-xl" />
      <div className="space-y-2 px-1">
        <Skeleton className="h-3 w-1/3" />
        <Skeleton className="h-4 w-4/5" />
        <div className="flex items-center justify-between pt-2">
          <Skeleton className="h-5 w-1/3" />
          <Skeleton className="h-4 w-1/4" />
        </div>
      </div>
    </div>
  );
}

export function ProductGridSkeleton({ count = 8 }: { count?: number }) {
  return (
    <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4 sm:gap-6">
      {Array.from({ length: count }).map((_, i) => (
        <ProductCardSkeleton key={i} />
      ))}
    </div>
  );
}

export function OrderCardSkeleton() {
  return (
    <div className="rounded-2xl border border-zinc-200/80 bg-white p-5 space-y-4 shadow-xs">
      <div className="flex justify-between items-center border-b border-zinc-100 pb-3">
        <Skeleton className="h-4 w-40" />
        <Skeleton className="h-6 w-20 rounded-full" />
      </div>
      <div className="space-y-3">
        <div className="flex gap-4">
          <Skeleton className="w-16 h-16 rounded-xl shrink-0" />
          <div className="flex-1 space-y-2">
            <Skeleton className="h-4 w-3/4" />
            <Skeleton className="h-3 w-1/2" />
          </div>
        </div>
      </div>
      <div className="flex justify-between items-center pt-2">
        <Skeleton className="h-4 w-28" />
        <Skeleton className="h-8 w-24 rounded-full" />
      </div>
    </div>
  );
}
