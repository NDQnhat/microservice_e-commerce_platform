import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '@/lib/api-client';
import { ExceptionRecord, ExceptionRecordStatus, PageResponse } from '@/types';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { SkeletonTable } from '@/components/ui/SkeletonTable';
import { EmptyState } from '@/components/ui/EmptyState';
import { SlideOverDrawer } from '@/components/ui/SlideOverDrawer';
import { JsonViewer } from '@/components/ui/JsonViewer';
import { Modal } from '@/components/ui/Modal';
import { useToastStore } from '@/store/toast-store';
import { useAuthStore } from '@/store/auth-store';
import {
  AlertTriangle,
  Search,
  Filter,
  CheckCircle,
  Eye,
  UserCheck,
  RefreshCw,
} from 'lucide-react';

const MOCK_EXCEPTIONS: ExceptionRecord[] = [
  {
    id: 'exc-7701-a1b2',
    exceptionType: 'PAYMENT_FAILED',
    sourceService: 'payment-service',
    referenceId: 'ord-8890-4412',
    referenceType: 'ORDER',
    errorCode: 'ERR_GATEWAY_TIMEOUT',
    errorMessage: 'Payment gateway timed out after 30000ms waiting for provider callback.',
    payload: JSON.stringify(
      {
        orderId: 'ord-8890-4412',
        gateway: 'STRIPE_EMULATOR',
        attemptedAmount: 1450000,
        currency: 'VND',
        retryCount: 3,
        lastAttempt: '2026-09-27T08:15:30Z',
      },
      null,
      2
    ),
    status: 'OPEN',
    createdAt: '2026-09-27T08:15:35Z',
    updatedAt: '2026-09-27T08:15:35Z',
  },
  {
    id: 'exc-7702-c3d4',
    exceptionType: 'STUCK_ORDER',
    sourceService: 'order-service',
    referenceId: 'ord-9921-5531',
    referenceType: 'ORDER',
    errorCode: 'ERR_SAGA_STUCK_PACKING',
    errorMessage: 'Order stuck in RESERVED state without payment transition event exceeding 60m threshold.',
    payload: JSON.stringify(
      {
        orderId: 'ord-9921-5531',
        stuckMinutes: 72,
        expectedState: 'PAID',
        reservationId: 'res-4410-99',
      },
      null,
      2
    ),
    status: 'INVESTIGATING',
    assignedTo: 'elena.rostova@ecommerce.internal',
    createdAt: '2026-09-27T07:30:10Z',
    updatedAt: '2026-09-27T07:45:00Z',
  },
  {
    id: 'exc-7703-e5f6',
    exceptionType: 'DUPLICATE_PAYMENT',
    sourceService: 'payment-service',
    referenceId: 'ord-6650-1122',
    referenceType: 'ORDER',
    errorCode: 'ERR_DUPLICATE_WEBHOOK',
    errorMessage: 'Second payment callback received for already confirmed transaction.',
    payload: JSON.stringify(
      {
        orderId: 'ord-6650-1122',
        firstTransactionId: 'txn-1002',
        duplicatePayloadId: 'txn-1002-dup',
        amount: 890000,
      },
      null,
      2
    ),
    status: 'OPEN',
    createdAt: '2026-09-27T09:00:20Z',
    updatedAt: '2026-09-27T09:00:20Z',
  },
  {
    id: 'exc-7704-g7h8',
    exceptionType: 'INVENTORY_DISCREPANCY',
    sourceService: 'inventory-service',
    referenceId: 'sku-nike-pegasus-40-blk-42',
    referenceType: 'SKU',
    errorCode: 'ERR_NEGATIVE_STOCK_PREVENTED',
    errorMessage: 'Reservation requested quantity exceeding atomic available stock.',
    payload: JSON.stringify(
      {
        skuId: 'sku-nike-pegasus-40-blk-42',
        requestedQty: 5,
        availableQty: 2,
      },
      null,
      2
    ),
    status: 'RESOLVED',
    assignedTo: 'david.bradley@ecommerce.internal',
    resolvedBy: 'david.bradley@ecommerce.internal',
    resolvedAt: '2026-09-27T09:20:00Z',
    resolutionAction: 'CORRECTED_INVENTORY_RESTOCK',
    resolutionNotes: 'Warehouse physically confirmed 20 units received; added restock log per BR-015.',
    createdAt: '2026-09-27T06:12:00Z',
    updatedAt: '2026-09-27T09:20:00Z',
  },
];

export const ExceptionsPage: React.FC = () => {
  const queryClient = useQueryClient();
  const { user } = useAuthStore();
  const { showSuccess, showError } = useToastStore();

  const [statusFilter, setStatusFilter] = useState<ExceptionRecordStatus | ''>('OPEN');
  const [typeFilter, setTypeFilter] = useState<string>('');
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedRecord, setSelectedRecord] = useState<ExceptionRecord | null>(null);

  // Modal states
  const [isResolveModalOpen, setIsResolveModalOpen] = useState(false);
  const [isIgnoreModalOpen, setIsIgnoreModalOpen] = useState(false);
  const [resolutionAction, setResolutionAction] = useState('MANUAL_CORRECTION');
  const [resolutionNotes, setResolutionNotes] = useState('');
  const [ignoreReason, setIgnoreReason] = useState('');

  const { data, isLoading, isFetching, refetch } = useQuery<PageResponse<ExceptionRecord>>({
    queryKey: ['exceptions', statusFilter, typeFilter],
    queryFn: async () => {
      try {
        const params = new URLSearchParams();
        if (statusFilter) params.append('status', statusFilter);
        if (typeFilter) params.append('exceptionType', typeFilter);
        const res = await apiClient<PageResponse<ExceptionRecord>>(`/api/v1/backoffice/exceptions?${params.toString()}`);
        return res;
      } catch {
        // Filter mock exceptions for offline testing
        let filtered = [...MOCK_EXCEPTIONS];
        if (statusFilter) {
          filtered = filtered.filter((e) => e.status === statusFilter);
        }
        if (typeFilter) {
          filtered = filtered.filter((e) => e.exceptionType === typeFilter);
        }
        return {
          content: filtered,
          totalElements: filtered.length,
        };
      }
    },
  });

  // Assign mutation
  const assignMutation = useMutation({
    mutationFn: async (id: string) => {
      return apiClient(`/api/v1/backoffice/exceptions/${id}/assign`, {
        method: 'POST',
      });
    },
    onSuccess: (_, id) => {
      queryClient.invalidateQueries({ queryKey: ['exceptions'] });
      showSuccess('Exception Assigned', `Status shifted to INVESTIGATING for #${id}`);
      if (selectedRecord && selectedRecord.id === id) {
        setSelectedRecord({
          ...selectedRecord,
          status: 'INVESTIGATING',
          assignedTo: user?.email || 'CURRENT_OPERATOR',
        });
      }
    },
    onError: (err) => showError(err, 'Failed to assign exception'),
  });

  // Resolve mutation (BR-017, BR-018)
  const resolveMutation = useMutation({
    mutationFn: async ({ id, action, notes }: { id: string; action: string; notes: string }) => {
      return apiClient(`/api/v1/backoffice/exceptions/${id}/resolve`, {
        method: 'POST',
        body: JSON.stringify({ action, notes }),
      });
    },
    onSuccess: (_, { id, action, notes }) => {
      queryClient.invalidateQueries({ queryKey: ['exceptions'] });
      showSuccess('Exception Resolved', `Discrepancy #${id} marked as RESOLVED (BR-018 fulfilled)`);
      setIsResolveModalOpen(false);
      setResolutionNotes('');
      if (selectedRecord && selectedRecord.id === id) {
        setSelectedRecord({
          ...selectedRecord,
          status: 'RESOLVED',
          resolvedBy: user?.email || 'CURRENT_OPERATOR',
          resolvedAt: new Date().toISOString(),
          resolutionAction: action,
          resolutionNotes: notes,
        });
      }
    },
    onError: (err) => showError(err, 'Failed to resolve exception'),
  });

  // Ignore mutation (BR-018)
  const ignoreMutation = useMutation({
    mutationFn: async ({ id, notes }: { id: string; notes: string }) => {
      return apiClient(`/api/v1/backoffice/exceptions/${id}/ignore`, {
        method: 'POST',
        body: JSON.stringify({ notes }),
      });
    },
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: ['exceptions'] });
      showSuccess('Exception Ignored', `Record #${id} marked as IGNORED with logged justification`);
      setIsIgnoreModalOpen(false);
      setIgnoreReason('');
      if (selectedRecord && selectedRecord.id === id) {
        setSelectedRecord({
          ...selectedRecord,
          status: 'IGNORED',
        });
      }
    },
    onError: (err) => showError(err, 'Failed to ignore exception'),
  });

  const records = (data?.content || []).filter((r) => {
    if (!searchQuery.trim()) return true;
    const q = searchQuery.toLowerCase();
    return (
      r.id.toLowerCase().includes(q) ||
      r.referenceId.toLowerCase().includes(q) ||
      r.errorMessage.toLowerCase().includes(q) ||
      r.sourceService.toLowerCase().includes(q)
    );
  });

  return (
    <div className="space-y-6">
      {/* Title & Filter Bar */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-white">Exception Management Board</h1>
          <p className="text-sm text-slate-400">
            Triage, investigate, and safely resolve asynchronous processing discrepancies (API-EXC-001, BR-017, BR-018)
          </p>
        </div>
        <button
          onClick={() => refetch()}
          disabled={isFetching}
          className="flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs font-semibold bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700/80 transition w-fit"
        >
          <RefreshCw className={`h-3.5 w-3.5 ${isFetching ? 'animate-spin' : ''}`} />
          <span>Refresh Records</span>
        </button>
      </div>

      {/* Filter and Search Bar */}
      <div className="p-4 rounded-xl border border-slate-800 bg-slate-900/90 backdrop-blur flex flex-col md:flex-row items-center justify-between gap-4">
        {/* Search */}
        <div className="relative w-full md:w-80">
          <Search className="absolute left-3 top-2.5 h-4 w-4 text-slate-500" />
          <input
            type="text"
            placeholder="Search by ID, Order #, or error..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full bg-slate-950 border border-slate-700/80 rounded-lg pl-9 pr-3 py-2 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-indigo-500"
          />
        </div>

        {/* Status Facets */}
        <div className="flex items-center gap-1.5 flex-wrap w-full md:w-auto">
          {(['', 'OPEN', 'INVESTIGATING', 'RESOLVED', 'IGNORED'] as const).map((st) => (
            <button
              key={st}
              onClick={() => setStatusFilter(st)}
              className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition ${
                statusFilter === st
                  ? 'bg-indigo-600 text-white shadow-sm shadow-indigo-500/20'
                  : 'bg-slate-800/80 text-slate-400 hover:text-white hover:bg-slate-800'
              }`}
            >
              {st || 'ALL'}
            </button>
          ))}
        </div>

        {/* Type Filter */}
        <div className="flex items-center gap-2 w-full md:w-auto">
          <Filter className="h-4 w-4 text-slate-500 shrink-0" />
          <select
            value={typeFilter}
            onChange={(e) => setTypeFilter(e.target.value)}
            className="bg-slate-950 border border-slate-700/80 rounded-lg px-3 py-1.5 text-xs text-slate-300 focus:outline-none focus:border-indigo-500"
          >
            <option value="">All Exception Types</option>
            <option value="PAYMENT_FAILED">PAYMENT_FAILED</option>
            <option value="STUCK_ORDER">STUCK_ORDER</option>
            <option value="NOTIFICATION_FAILED">NOTIFICATION_FAILED</option>
            <option value="DUPLICATE_PAYMENT">DUPLICATE_PAYMENT</option>
            <option value="INVENTORY_DISCREPANCY">INVENTORY_DISCREPANCY</option>
          </select>
        </div>
      </div>

      {/* Main Table */}
      {isLoading ? (
        <SkeletonTable rows={5} cols={6} />
      ) : records.length === 0 ? (
        <EmptyState
          icon={AlertTriangle}
          title="No Exceptions Found"
          description={
            statusFilter
              ? `No exception records currently matching status "${statusFilter}".`
              : 'All systems are operating nominally with zero pending discrepancies.'
          }
          actionLabel="Clear Filters"
          onAction={() => {
            setStatusFilter('');
            setTypeFilter('');
            setSearchQuery('');
          }}
        />
      ) : (
        <div className="rounded-xl border border-slate-800 bg-slate-950 overflow-hidden shadow-xl">
          <table className="w-full text-left text-sm">
            <thead className="sticky top-0 bg-slate-900/95 backdrop-blur z-10 border-b border-slate-800 text-xs font-semibold uppercase tracking-wider text-slate-400">
              <tr>
                <th className="py-3 px-4">Exception Type</th>
                <th className="py-3 px-4">Source Service</th>
                <th className="py-3 px-4">Reference</th>
                <th className="py-3 px-4">Error Summary</th>
                <th className="py-3 px-4">Status</th>
                <th className="py-3 px-4 text-right">Quick Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/80">
              {records.map((rec) => (
                <tr
                  key={rec.id}
                  onClick={() => setSelectedRecord(rec)}
                  className="hover:bg-slate-900/50 cursor-pointer transition group"
                >
                  <td className="py-3 px-4">
                    <span className="font-mono text-xs font-bold text-rose-400">
                      {rec.exceptionType}
                    </span>
                  </td>
                  <td className="py-3 px-4 text-xs font-mono text-slate-300">
                    {rec.sourceService}
                  </td>
                  <td className="py-3 px-4">
                    <div className="font-mono text-xs text-slate-300">
                      <span className="text-slate-500">{rec.referenceType}: </span>
                      {rec.referenceId}
                    </div>
                  </td>
                  <td className="py-3 px-4 max-w-sm truncate text-xs text-slate-400">
                    {rec.errorMessage}
                  </td>
                  <td className="py-3 px-4">
                    <StatusBadge status={rec.status} />
                  </td>
                  <td
                    className="py-3 px-4 text-right space-x-2"
                    onClick={(e) => e.stopPropagation()}
                  >
                    {rec.status === 'OPEN' && (
                      <button
                        onClick={() => assignMutation.mutate(rec.id)}
                        disabled={assignMutation.isPending}
                        className="px-2.5 py-1 text-xs font-semibold bg-slate-800 hover:bg-slate-700 text-slate-200 rounded border border-slate-700/80 transition"
                      >
                        Investigate
                      </button>
                    )}
                    {(rec.status === 'OPEN' || rec.status === 'INVESTIGATING') && (
                      <button
                        onClick={() => {
                          setSelectedRecord(rec);
                          setIsResolveModalOpen(true);
                        }}
                        className="px-2.5 py-1 text-xs font-semibold bg-indigo-600 hover:bg-indigo-500 text-white rounded transition shadow-xs"
                      >
                        Resolve
                      </button>
                    )}
                    <button
                      onClick={() => setSelectedRecord(rec)}
                      className="p-1 text-slate-400 hover:text-white rounded transition"
                      title="Inspect Record"
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

      {/* Slide-over Drawer for Details */}
      <SlideOverDrawer
        isOpen={!!selectedRecord}
        onClose={() => setSelectedRecord(null)}
        title={selectedRecord?.exceptionType || 'Exception Record'}
        subtitle={`Service: ${selectedRecord?.sourceService} • Created: ${selectedRecord?.createdAt}`}
        idToCopy={selectedRecord?.id}
        badge={selectedRecord ? <StatusBadge status={selectedRecord.status} /> : null}
        footerActions={
          selectedRecord && (
            <>
              {selectedRecord.status === 'OPEN' && (
                <button
                  onClick={() => assignMutation.mutate(selectedRecord.id)}
                  disabled={assignMutation.isPending}
                  className="px-4 py-2 text-xs font-semibold bg-slate-800 hover:bg-slate-700 text-white rounded-lg border border-slate-700 transition"
                >
                  Assign to Me (Investigate)
                </button>
              )}
              {(selectedRecord.status === 'OPEN' || selectedRecord.status === 'INVESTIGATING') && (
                <>
                  <button
                    onClick={() => setIsIgnoreModalOpen(true)}
                    className="px-4 py-2 text-xs font-semibold text-rose-400 hover:bg-rose-500/10 rounded-lg transition"
                  >
                    Mark as Ignored
                  </button>
                  <button
                    onClick={() => setIsResolveModalOpen(true)}
                    className="px-4 py-2 text-xs font-semibold bg-emerald-600 hover:bg-emerald-500 text-white rounded-lg shadow-sm transition"
                  >
                    Resolve Discrepancy
                  </button>
                </>
              )}
            </>
          )
        }
      >
        {selectedRecord && (
          <div className="space-y-6">
            {/* Meta Attributes */}
            <div className="grid grid-cols-2 gap-4 p-4 rounded-xl bg-slate-950 border border-slate-800 text-xs">
              <div>
                <span className="text-slate-500 block">Reference Type</span>
                <span className="font-mono text-slate-200 font-semibold">{selectedRecord.referenceType}</span>
              </div>
              <div>
                <span className="text-slate-500 block">Reference ID</span>
                <span className="font-mono text-indigo-400 font-semibold">{selectedRecord.referenceId}</span>
              </div>
              <div>
                <span className="text-slate-500 block">Assigned Operator</span>
                <span className="text-slate-200">{selectedRecord.assignedTo || 'Unassigned'}</span>
              </div>
              <div>
                <span className="text-slate-500 block">Error Code</span>
                <span className="font-mono text-rose-400 font-semibold">{selectedRecord.errorCode || 'UNKNOWN'}</span>
              </div>
            </div>

            {/* Error Message Box */}
            <div className="space-y-2">
              <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
                Full Technical Diagnostic Message
              </label>
              <div className="p-3.5 rounded-lg bg-slate-950 border border-slate-800 text-xs font-mono text-rose-300 leading-relaxed">
                {selectedRecord.errorMessage}
              </div>
            </div>

            {/* Raw JSON Payload Viewer */}
            <div className="space-y-2">
              <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
                Contextual Snapshot Payload (JSON)
              </label>
              <JsonViewer
                data={selectedRecord.payload || { info: 'No technical payload attached' }}
                title="Exception Event Envelope Payload"
              />
            </div>

            {/* Resolution History if resolved */}
            {selectedRecord.status === 'RESOLVED' && (
              <div className="p-4 rounded-xl border border-emerald-500/20 bg-emerald-500/5 space-y-2 text-xs">
                <div className="flex items-center gap-1.5 text-emerald-400 font-bold">
                  <CheckCircle className="h-4 w-4" />
                  <span>Resolution Audit Record (BR-018)</span>
                </div>
                <p className="text-slate-300">
                  <strong className="text-slate-400">Action:</strong> {selectedRecord.resolutionAction}
                </p>
                <p className="text-slate-300">
                  <strong className="text-slate-400">Notes:</strong> {selectedRecord.resolutionNotes}
                </p>
                <p className="text-slate-400 text-[11px]">
                  Resolved by: {selectedRecord.resolvedBy} at {selectedRecord.resolvedAt}
                </p>
              </div>
            )}
          </div>
        )}
      </SlideOverDrawer>

      {/* Resolution Modal (BR-017, BR-018) */}
      <Modal
        isOpen={isResolveModalOpen}
        onClose={() => setIsResolveModalOpen(false)}
        title="Resolve System Exception (BR-018)"
        subtitle="Mandatory documentation of incident cause and resolution action"
        footerActions={
          <>
            <button
              onClick={() => setIsResolveModalOpen(false)}
              className="px-4 py-2 text-xs font-semibold text-slate-400 hover:text-white"
            >
              Cancel
            </button>
            <button
              disabled={!resolutionNotes.trim() || resolveMutation.isPending}
              onClick={() => {
                if (selectedRecord) {
                  resolveMutation.mutate({
                    id: selectedRecord.id,
                    action: resolutionAction,
                    notes: resolutionNotes,
                  });
                }
              }}
              className="px-4 py-2 text-xs font-semibold bg-emerald-600 hover:bg-emerald-500 disabled:opacity-50 text-white rounded-lg transition"
            >
              {resolveMutation.isPending ? 'Committing...' : 'Confirm Resolution'}
            </button>
          </>
        }
      >
        <div className="space-y-4">
          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-300 uppercase">
              Resolution Action Code
            </label>
            <input
              type="text"
              value={resolutionAction}
              onChange={(e) => setResolutionAction(e.target.value)}
              className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white focus:outline-none focus:border-indigo-500 font-mono"
            />
          </div>

          <div className="space-y-1.5">
            <div className="flex items-center justify-between">
              <label className="text-xs font-semibold text-slate-300 uppercase">
                Resolution Notes (Mandatory per BR-018)
              </label>
              <span className="text-[11px] text-rose-400">* Required</span>
            </div>
            <textarea
              rows={4}
              value={resolutionNotes}
              onChange={(e) => setResolutionNotes(e.target.value)}
              placeholder="Detail root cause analysis, corrective step taken, and operator audit justification..."
              className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white focus:outline-none focus:border-indigo-500 placeholder-slate-500"
            />
          </div>

          <div className="p-3 rounded-lg bg-indigo-500/10 border border-indigo-500/20 text-xs text-indigo-300 space-y-1">
            <p className="font-semibold flex items-center gap-1.5">
              <UserCheck className="h-3.5 w-3.5" />
              Operator Auto-Stamping (BR-017)
            </p>
            <p className="text-[11px] text-slate-400">
              Current operator identifier (<span className="text-white font-mono">{user?.email}</span>) will be permanently attached to this resolution.
            </p>
          </div>
        </div>
      </Modal>

      {/* Ignore Modal (BR-018) */}
      <Modal
        isOpen={isIgnoreModalOpen}
        onClose={() => setIsIgnoreModalOpen(false)}
        title="Ignore Exception Record"
        subtitle="Mandatory business justification for dismissing discrepancy without action"
        footerActions={
          <>
            <button
              onClick={() => setIsIgnoreModalOpen(false)}
              className="px-4 py-2 text-xs font-semibold text-slate-400 hover:text-white"
            >
              Cancel
            </button>
            <button
              disabled={!ignoreReason.trim() || ignoreMutation.isPending}
              onClick={() => {
                if (selectedRecord) {
                  ignoreMutation.mutate({
                    id: selectedRecord.id,
                    notes: ignoreReason,
                  });
                }
              }}
              className="px-4 py-2 text-xs font-semibold bg-rose-600 hover:bg-rose-500 disabled:opacity-50 text-white rounded-lg transition"
            >
              {ignoreMutation.isPending ? 'Marking...' : 'Confirm Ignore'}
            </button>
          </>
        }
      >
        <div className="space-y-4">
          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-300 uppercase">
              Justification Reason (Mandatory per BR-018)
            </label>
            <textarea
              rows={3}
              value={ignoreReason}
              onChange={(e) => setIgnoreReason(e.target.value)}
              placeholder="State reason why this discrepancy is considered benign or false positive..."
              className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white focus:outline-none focus:border-indigo-500 placeholder-slate-500"
            />
          </div>
        </div>
      </Modal>
    </div>
  );
};
