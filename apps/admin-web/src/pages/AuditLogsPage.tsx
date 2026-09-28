import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { apiClient } from '@/lib/api-client';
import { AuditLog, PageResponse } from '@/types';
import { SkeletonTable } from '@/components/ui/SkeletonTable';
import { EmptyState } from '@/components/ui/EmptyState';
import { SlideOverDrawer } from '@/components/ui/SlideOverDrawer';
import { JsonViewer } from '@/components/ui/JsonViewer';
import { useDebounce } from '@/hooks/useDebounce';
import {
  ShieldAlert,
  Search,
  Filter,
  Eye,
  ShieldCheck,
  RefreshCw,
  Lock,
  Download,
  Calendar,
} from 'lucide-react';

const MOCK_AUDIT_LOGS: AuditLog[] = [
  {
    id: 'aud-8801-a1b2',
    actorId: 'usr-admin-001',
    actorRole: 'SUPER_ADMIN',
    actionType: 'CONFIG_UPDATE',
    entityType: 'BUSINESS_CONFIGURATION',
    entityId: 'shipping.free.minimum.amount',
    beforeValue: JSON.stringify({ amount: 300000, version: 2 }),
    afterValue: JSON.stringify({ amount: 500000, version: 3 }),
    reason: 'Updated free shipping threshold to align with fuel surcharge indexation.',
    createdAt: '2026-09-20T08:00:00Z',
  },
  {
    id: 'aud-8802-c3d4',
    actorId: 'usr-ops-004',
    actorRole: 'OPS_ADMIN',
    actionType: 'MANUAL_PAYMENT_RECONCILE',
    entityType: 'PAYMENT_TRANSACTION',
    entityId: 'txn-9903-ef56',
    beforeValue: JSON.stringify({ status: 'FAILED' }),
    afterValue: JSON.stringify({
      status: 'SUCCESS',
      evidence: 'BANK-STATEMENT-REF-20260927-9941',
    }),
    reason: 'Gateway webhook lost in network partition; confirmed against Vietcombank corporate statement.',
    createdAt: '2026-09-27T06:30:00Z',
  },
  {
    id: 'aud-8803-e5f6',
    actorId: 'usr-warehouse-003',
    actorRole: 'WAREHOUSE_STAFF',
    actionType: 'INVENTORY_ADJUSTMENT',
    entityType: 'INVENTORY_BALANCE',
    entityId: 'sku-nike-pegasus-40-blk-42',
    beforeValue: JSON.stringify({ available: -4, total: -4 }),
    afterValue: JSON.stringify({ available: 6, total: 6, delta: 10 }),
    reason: 'Inbound container shipment C-409 checked into warehouse bay 3',
    createdAt: '2026-09-26T14:30:00Z',
  },
  {
    id: 'aud-8804-g7h8',
    actorId: 'usr-support-005',
    actorRole: 'SUPPORT_AGENT',
    actionType: 'ACCOUNT_UNLOCK',
    entityType: 'USER_ACCOUNT',
    entityId: 'usr-cust-9901',
    beforeValue: JSON.stringify({ isLocked: true }),
    afterValue: JSON.stringify({ isLocked: false }),
    reason: 'Customer verified phone OTP via verified telephone support call.',
    createdAt: '2026-09-27T08:00:00Z',
  },
];

export const AuditLogsPage: React.FC = () => {
  const [searchQuery, setSearchQuery] = useState('');
  const debouncedSearch = useDebounce(searchQuery, 300);
  const [actionTypeFilter, setActionTypeFilter] = useState('');
  const [fromDate, setFromDate] = useState('');
  const [toDate, setToDate] = useState('');
  const [selectedLog, setSelectedLog] = useState<AuditLog | null>(null);

  // Fetch Audit Logs (API-AUDIT-001)
  const { data, isLoading, refetch, isFetching } = useQuery<PageResponse<AuditLog>>({
    queryKey: ['audit-logs', actionTypeFilter],
    queryFn: async () => {
      try {
        const url = actionTypeFilter
          ? `/api/v1/backoffice/audit-log?actionType=${actionTypeFilter}`
          : '/api/v1/backoffice/audit-log';
        return await apiClient<PageResponse<AuditLog>>(url);
      } catch {
        let filtered = [...MOCK_AUDIT_LOGS];
        if (actionTypeFilter) {
          filtered = filtered.filter((l) => l.actionType === actionTypeFilter);
        }
        return { content: filtered, totalElements: filtered.length };
      }
    },
  });

  const logs = (data?.content || []).filter((l) => {
    // Search query filter
    if (debouncedSearch.trim()) {
      const q = debouncedSearch.toLowerCase();
      const matchesSearch =
        l.id.toLowerCase().includes(q) ||
        l.entityId.toLowerCase().includes(q) ||
        l.actorId.toLowerCase().includes(q) ||
        (l.reason && l.reason.toLowerCase().includes(q));
      if (!matchesSearch) return false;
    }

    // Date range filter
    if (fromDate) {
      const logTime = new Date(l.createdAt).getTime();
      const start = new Date(fromDate).setHours(0, 0, 0, 0);
      if (logTime < start) return false;
    }

    if (toDate) {
      const logTime = new Date(l.createdAt).getTime();
      const end = new Date(toDate).setHours(23, 59, 59, 999);
      if (logTime > end) return false;
    }

    return true;
  });

  // Export CSV Handler (STT 10)
  const handleExportCsv = () => {
    const headers = [
      'Log ID',
      'Action Type',
      'Actor ID',
      'Actor Role',
      'Entity Type',
      'Entity ID',
      'Before Value',
      'After Value',
      'Justification Reason',
      'Logged At',
    ];

    const escapeCsv = (val: string | undefined | null) => {
      if (val === undefined || val === null) return '""';
      const str = String(val).replace(/"/g, '""');
      return `"${str}"`;
    };

    const rows = logs.map((log) => [
      escapeCsv(log.id),
      escapeCsv(log.actionType),
      escapeCsv(log.actorId),
      escapeCsv(log.actorRole),
      escapeCsv(log.entityType),
      escapeCsv(log.entityId),
      escapeCsv(log.beforeValue),
      escapeCsv(log.afterValue),
      escapeCsv(log.reason),
      escapeCsv(log.createdAt),
    ]);

    const csvContent = '\uFEFF' + [headers.join(','), ...rows.map((r) => r.join(','))].join('\r\n');
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    const today = new Date().toISOString().split('T')[0];
    link.setAttribute('download', `audit-logs-export-${today}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  };

  return (
    <div className="space-y-6">
      {/* Title & Top Controls */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-white">Immutable Compliance & Audit Trail</h1>
          <p className="text-sm text-slate-400">
            Append-only enterprise ledger capturing all privileged actions and state mutations (API-AUDIT-001, FR-034, NFR-AUDIT-002)
          </p>
        </div>
        <div className="flex items-center gap-3">
          <div className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 text-xs font-semibold">
            <Lock className="h-3.5 w-3.5" />
            <span>Strict Read-Only Ledger (NFR-AUDIT-002)</span>
          </div>
          <button
            onClick={handleExportCsv}
            disabled={logs.length === 0}
            className="flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs font-semibold bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 text-white shadow-xs transition"
          >
            <Download className="h-3.5 w-3.5" />
            <span>Xuất báo cáo kiểm toán (CSV)</span>
          </button>
          <button
            onClick={() => refetch()}
            disabled={isFetching}
            className="flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs font-semibold bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 transition"
          >
            <RefreshCw className={`h-3.5 w-3.5 ${isFetching ? 'animate-spin' : ''}`} />
            <span>Sync Ledger</span>
          </button>
        </div>
      </div>

      {/* Compliance Security Banner */}
      <div className="p-4 rounded-xl border border-indigo-500/20 bg-indigo-500/5 flex items-start gap-3">
        <ShieldCheck className="h-5 w-5 text-indigo-400 shrink-0 mt-0.5" />
        <div className="text-xs space-y-1">
          <p className="font-bold text-indigo-300">Regulatory Compliance Invariant (NFR-AUDIT-002)</p>
          <p className="text-slate-400 leading-relaxed">
            Audit log records are cryptographically timestamped and write-once. This interface offers zero edit, update, or delete controls by architectural design.
          </p>
        </div>
      </div>

      {/* Search & Filters */}
      <div className="p-4 rounded-xl border border-slate-800 bg-slate-900/90 backdrop-blur flex flex-col md:flex-row items-center justify-between gap-4">
        <div className="relative w-full md:w-72">
          <Search className="absolute left-3 top-2.5 h-4 w-4 text-slate-500" />
          <input
            type="text"
            placeholder="Search by Entity ID, Actor, or reason..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full bg-slate-950 border border-slate-700/80 rounded-lg pl-9 pr-3 py-2 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-indigo-500"
          />
        </div>

        <div className="flex flex-wrap items-center gap-2 w-full md:w-auto">
          {/* Date Range Filters (STT 10) */}
          <div className="flex items-center gap-1.5 bg-slate-950 border border-slate-700/80 rounded-lg px-2.5 py-1 text-xs">
            <Calendar className="h-3.5 w-3.5 text-slate-400 shrink-0" />
            <span className="text-slate-500 text-[11px] whitespace-nowrap">Từ:</span>
            <input
              type="date"
              value={fromDate}
              onChange={(e) => setFromDate(e.target.value)}
              className="bg-transparent text-slate-300 focus:outline-none text-xs"
            />
          </div>
          <div className="flex items-center gap-1.5 bg-slate-950 border border-slate-700/80 rounded-lg px-2.5 py-1 text-xs">
            <span className="text-slate-500 text-[11px] whitespace-nowrap">Đến:</span>
            <input
              type="date"
              value={toDate}
              onChange={(e) => setToDate(e.target.value)}
              className="bg-transparent text-slate-300 focus:outline-none text-xs"
            />
          </div>
          {(fromDate || toDate) && (
            <button
              onClick={() => {
                setFromDate('');
                setToDate('');
              }}
              className="text-xs text-rose-400 hover:text-rose-300 px-1 font-semibold"
              title="Xóa khoảng thời gian"
            >
              ✕ Xóa ngày
            </button>
          )}

          <div className="flex items-center gap-2">
            <Filter className="h-4 w-4 text-slate-500 shrink-0" />
            <select
              value={actionTypeFilter}
              onChange={(e) => setActionTypeFilter(e.target.value)}
              className="bg-slate-950 border border-slate-700/80 rounded-lg px-3 py-1.5 text-xs text-slate-300 focus:outline-none focus:border-indigo-500"
            >
              <option value="">All Action Types</option>
              <option value="CONFIG_UPDATE">CONFIG_UPDATE</option>
              <option value="MANUAL_PAYMENT_RECONCILE">MANUAL_PAYMENT_RECONCILE</option>
              <option value="INVENTORY_ADJUSTMENT">INVENTORY_ADJUSTMENT</option>
              <option value="ACCOUNT_UNLOCK">ACCOUNT_UNLOCK</option>
            </select>
          </div>
        </div>
      </div>

      {/* Table */}
      {isLoading ? (
        <SkeletonTable rows={4} cols={5} />
      ) : logs.length === 0 ? (
        <EmptyState
          icon={ShieldAlert}
          title="No Audit Records Matching Filter"
          description="Try selecting a different action filter."
          actionLabel="Clear Filter"
          onAction={() => {
            setActionTypeFilter('');
            setSearchQuery('');
          }}
        />
      ) : (
        <div className="rounded-xl border border-slate-800 bg-slate-950 overflow-hidden shadow-xl">
          <table className="w-full text-left text-sm">
            <thead className="sticky top-0 bg-slate-900/95 backdrop-blur z-10 border-b border-slate-800 text-xs font-semibold uppercase tracking-wider text-slate-400">
              <tr>
                <th className="py-3 px-4">Action Type</th>
                <th className="py-3 px-4">Actor & Role</th>
                <th className="py-3 px-4">Target Entity</th>
                <th className="py-3 px-4">Justification Reason</th>
                <th className="py-3 px-4">Logged Timestamp</th>
                <th className="py-3 px-4 text-right">View Diff</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/80">
              {logs.map((log) => (
                <tr
                  key={log.id}
                  onClick={() => setSelectedLog(log)}
                  className="hover:bg-slate-900/50 cursor-pointer transition group"
                >
                  <td className="py-3 px-4">
                    <span className="font-mono text-xs font-bold text-indigo-400">
                      {log.actionType}
                    </span>
                  </td>
                  <td className="py-3 px-4">
                    <p className="text-xs text-white font-medium">{log.actorId}</p>
                    <span className="font-mono text-[10px] text-indigo-300 bg-indigo-500/10 px-1.5 py-0.5 rounded">
                      {log.actorRole}
                    </span>
                  </td>
                  <td className="py-3 px-4">
                    <p className="font-mono text-xs text-slate-300 font-semibold">{log.entityId}</p>
                    <p className="text-[10px] text-slate-500">{log.entityType}</p>
                  </td>
                  <td className="py-3 px-4 text-xs text-slate-400 max-w-sm truncate">
                    {log.reason || '-'}
                  </td>
                  <td className="py-3 px-4 text-xs text-slate-400 font-mono">
                    {new Date(log.createdAt).toLocaleString()}
                  </td>
                  <td
                    className="py-3 px-4 text-right space-x-2"
                    onClick={(e) => e.stopPropagation()}
                  >
                    <button
                      onClick={() => setSelectedLog(log)}
                      className="p-1.5 text-slate-400 hover:text-white rounded hover:bg-slate-800 transition"
                      title="Inspect Diff"
                    >
                      <Eye className="h-4 w-4" />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Slide-over Drawer for Immutable Audit Log Inspection */}
      <SlideOverDrawer
        isOpen={!!selectedLog}
        onClose={() => setSelectedLog(null)}
        title={`Audit Entry #${selectedLog?.id}`}
        subtitle={`Action: ${selectedLog?.actionType} • Logged: ${selectedLog ? new Date(selectedLog.createdAt).toLocaleString() : ''}`}
        idToCopy={selectedLog?.id}
        badge={
          <span className="font-mono text-xs px-2 py-0.5 rounded bg-indigo-500/10 text-indigo-400 border border-indigo-500/20 font-bold">
            IMMUTABLE
          </span>
        }
      >
        {selectedLog && (
          <div className="space-y-6">
            <div className="p-4 rounded-xl bg-slate-950 border border-slate-800 space-y-2 text-xs">
              <div className="flex justify-between">
                <span className="text-slate-400">Actor Identifier:</span>
                <span className="font-mono text-white font-bold">{selectedLog.actorId}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">Actor Role:</span>
                <span className="font-mono text-indigo-300">{selectedLog.actorRole}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">Entity Type:</span>
                <span className="font-mono text-slate-300">{selectedLog.entityType}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">Entity ID:</span>
                <span className="font-mono text-indigo-400">{selectedLog.entityId}</span>
              </div>
            </div>

            {/* Justification Box */}
            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
                Audit Justification & Intent
              </label>
              <div className="p-3.5 rounded-xl bg-slate-950 border border-slate-800 text-xs text-slate-200 leading-relaxed">
                {selectedLog.reason || 'No justification note provided.'}
              </div>
            </div>

            {/* Before vs After Values Diff */}
            <div className="space-y-3">
              <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
                State Mutation Delta (Before vs After)
              </label>
              <div className="space-y-3">
                <div className="space-y-1">
                  <span className="text-[11px] font-mono text-rose-400 font-bold">Before Mutation</span>
                  <JsonViewer data={selectedLog.beforeValue || '{}'} title="Previous State" />
                </div>
                <div className="space-y-1">
                  <span className="text-[11px] font-mono text-emerald-400 font-bold">After Mutation</span>
                  <JsonViewer data={selectedLog.afterValue || '{}'} title="Committed State" />
                </div>
              </div>
            </div>
          </div>
        )}
      </SlideOverDrawer>
    </div>
  );
};
