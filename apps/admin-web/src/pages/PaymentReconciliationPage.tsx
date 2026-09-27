import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '@/lib/api-client';
import { PaymentTransaction, PageResponse, PaymentTransactionStatus, ResolutionType } from '@/types';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { SkeletonTable } from '@/components/ui/SkeletonTable';
import { EmptyState } from '@/components/ui/EmptyState';
import { SlideOverDrawer } from '@/components/ui/SlideOverDrawer';
import { Modal } from '@/components/ui/Modal';
import { useToastStore } from '@/store/toast-store';
import { useAuthStore } from '@/store/auth-store';
import {
  CreditCard,
  Search,
  CheckCircle,
  Clock,
  ShieldCheck,
  RefreshCw,
  FileCheck2,
  Eye,
  AlertTriangle,
  Lock,
} from 'lucide-react';

const MOCK_ANOMALIES: PaymentTransaction[] = [
  {
    id: 'txn-9901-ab12',
    orderId: 'ord-8890-4412',
    providerReference: 'PAY-STRIPE-EMU-TIMEOUT-001',
    amount: 1450000,
    status: 'FAILED',
    attemptedAt: '2026-09-27T08:15:00Z',
    evidenceReference: undefined,
    reconciliationReason: undefined,
  },
  {
    id: 'txn-9902-cd34',
    orderId: 'ord-6650-1122',
    providerReference: 'PAY-VNPAY-DUPLICATE-002',
    amount: 890000,
    status: 'PENDING',
    attemptedAt: '2026-09-27T08:45:00Z',
  },
  {
    id: 'txn-9903-ef56',
    orderId: 'ord-3320-7711',
    providerReference: 'PAY-MOMO-RECONCILED-003',
    amount: 2150000,
    status: 'SUCCESS',
    attemptedAt: '2026-09-27T06:00:00Z',
    confirmedAt: '2026-09-27T06:30:00Z',
    evidenceReference: 'BANK-STATEMENT-REF-20260927-9941',
    reconciliationReason: 'Gateway webhook lost in network partition; confirmed against Vietcombank corporate statement.',
  },
];

export const PaymentReconciliationPage: React.FC = () => {
  const queryClient = useQueryClient();
  const { showSuccess, showError } = useToastStore();

  const { user, hasAnyRole } = useAuthStore();
  const isAuthorizedAuditor = hasAnyRole(['FINANCIAL_AUDITOR', 'SUPER_ADMIN']);

  const [statusFilter, setStatusFilter] = useState<PaymentTransactionStatus | ''>('');
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedTxn, setSelectedTxn] = useState<PaymentTransaction | null>(null);

  // Controlled Reconcile Modal (FR-038, BR-012)
  const [isReconcileModalOpen, setIsReconcileModalOpen] = useState(false);
  const [externalTxnId, setExternalTxnId] = useState('');
  const [resolutionType, setResolutionType] = useState<ResolutionType>('ADJUST_LEDGER_CONFIRM');
  const [auditJustification, setAuditJustification] = useState('');

  // Fetch Anomalies (API-PAY-002)
  const { data, isLoading, refetch, isFetching } = useQuery<PageResponse<PaymentTransaction>>({
    queryKey: ['payment-anomalies', statusFilter],
    queryFn: async () => {
      try {
        const url = statusFilter
          ? `/api/v1/backoffice/payments/anomalies?status=${statusFilter}`
          : '/api/v1/backoffice/payments/anomalies';
        return await apiClient<PageResponse<PaymentTransaction>>(url);
      } catch {
        let filtered = [...MOCK_ANOMALIES];
        if (statusFilter) {
          filtered = filtered.filter((t) => t.status === statusFilter);
        }
        return { content: filtered, totalElements: filtered.length };
      }
    },
  });

  // Manual Reconcile Mutation (API-PAY-003, FR-038, BR-012)
  const reconcileMutation = useMutation({
    mutationFn: async ({
      id,
      external_transaction_id,
      resolution_type,
      audit_justification,
    }: {
      id: string;
      external_transaction_id: string;
      resolution_type: ResolutionType;
      audit_justification: string;
    }) => {
      return apiClient<PaymentTransaction>(`/api/v1/backoffice/payments/${id}/reconcile`, {
        method: 'POST',
        body: JSON.stringify({
          external_transaction_id,
          resolution_type,
          audit_justification,
          evidence_reference: external_transaction_id,
          reason: audit_justification,
        }),
      });
    },
    onSuccess: (updated) => {
      queryClient.invalidateQueries({ queryKey: ['payment-anomalies'] });
      showSuccess(
        'Transaction Reconciled (BR-012)',
        `Transaction #${updated.id || selectedTxn?.id} reconciled with external ID "${externalTxnId}" (${resolutionType})`
      );
      setIsReconcileModalOpen(false);
      setExternalTxnId('');
      setAuditJustification('');
      if (selectedTxn) {
        setSelectedTxn({
          ...selectedTxn,
          status: resolutionType === 'FORCE_REFUND' ? 'REFUNDED' : 'SUCCESS',
          evidenceReference: externalTxnId,
          reconciliationReason: auditJustification,
          confirmedAt: new Date().toISOString(),
        });
      }
    },
    onError: (err) => showError(err, 'Manual reconciliation failed (Evidence reference required per BR-012)'),
  });

  const externalIdRegex = /^[a-zA-Z0-9_-]{6,64}$/;
  const isExternalIdValid = externalIdRegex.test(externalTxnId.trim());
  const isJustificationValid = auditJustification.trim().length >= 15;
  const canSubmitReconcile = isExternalIdValid && isJustificationValid && isAuthorizedAuditor && !reconcileMutation.isPending;

  // Trigger Timeout Check
  const timeoutCheckMutation = useMutation({
    mutationFn: async () => {
      return apiClient<{ processed: number; timeoutMinutes: number }>('/api/v1/backoffice/payments/timeout-check?timeoutMinutes=15', {
        method: 'POST',
      });
    },
    onSuccess: (res) => {
      queryClient.invalidateQueries({ queryKey: ['payment-anomalies'] });
      showSuccess(
        'Timeout Scan Executed',
        `Scanned pending transactions (>15m): ${res?.processed ?? 0} transitioned to FAILED/TIMEOUT`
      );
    },
    onError: (err) => showError(err, 'Failed to trigger timeout scan'),
  });

  const transactions = (data?.content || []).filter((t) => {
    if (!searchQuery.trim()) return true;
    const q = searchQuery.toLowerCase();
    return (
      t.id.toLowerCase().includes(q) ||
      t.orderId.toLowerCase().includes(q) ||
      t.providerReference.toLowerCase().includes(q)
    );
  });

  return (
    <div className="space-y-6">
      {/* Title & Top Bar */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-white">Payment Reconciliation & Anomaly Board</h1>
          <p className="text-sm text-slate-400">
            Audit gateway timeout anomalies and perform evidence-backed manual settlement (API-PAY-002, API-PAY-003, BR-012)
          </p>
        </div>
        <div className="flex items-center gap-3">
          <button
            onClick={() => timeoutCheckMutation.mutate()}
            disabled={timeoutCheckMutation.isPending}
            className="flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs font-semibold bg-rose-600 hover:bg-rose-500 text-white shadow-xs transition"
          >
            <Clock className="h-3.5 w-3.5" />
            <span>Trigger 15m Timeout Scan</span>
          </button>
          <button
            onClick={() => refetch()}
            disabled={isFetching}
            className="flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs font-semibold bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 transition"
          >
            <RefreshCw className={`h-3.5 w-3.5 ${isFetching ? 'animate-spin' : ''}`} />
            <span>Sync Anomalies</span>
          </button>
        </div>
      </div>

      {/* Governance Notice */}
      <div className="p-4 rounded-xl border border-indigo-500/20 bg-indigo-500/5 flex items-start gap-3">
        <ShieldCheck className="h-5 w-5 text-indigo-400 shrink-0 mt-0.5" />
        <div className="text-xs space-y-1">
          <p className="font-bold text-indigo-300">Financial Integrity Guardrail (BR-012)</p>
          <p className="text-slate-400 leading-relaxed">
            Free-form "Set PAID" actions without verification are strictly prohibited by system invariant BR-012.
            Every manual reconciliation requires an immutable external evidence reference (e.g., bank statement identifier, gateway settlement reference) and operator justification.
          </p>
        </div>
      </div>

      {/* Filters & Search */}
      <div className="p-4 rounded-xl border border-slate-800 bg-slate-900/90 backdrop-blur flex flex-col md:flex-row items-center justify-between gap-4">
        <div className="relative w-full md:w-80">
          <Search className="absolute left-3 top-2.5 h-4 w-4 text-slate-500" />
          <input
            type="text"
            placeholder="Search by Transaction ID, Order #, or Provider Ref..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full bg-slate-950 border border-slate-700/80 rounded-lg pl-9 pr-3 py-2 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-indigo-500"
          />
        </div>

        <div className="flex items-center gap-2">
          {(['', 'FAILED', 'PENDING', 'SUCCESS'] as const).map((st) => (
            <button
              key={st}
              onClick={() => setStatusFilter(st)}
              className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition ${
                statusFilter === st
                  ? 'bg-indigo-600 text-white'
                  : 'bg-slate-800/80 text-slate-400 hover:text-white'
              }`}
            >
              {st || 'ALL'}
            </button>
          ))}
        </div>
      </div>

      {/* Transactions Table */}
      {isLoading ? (
        <SkeletonTable rows={4} cols={5} />
      ) : transactions.length === 0 ? (
        <EmptyState
          icon={CreditCard}
          title="No Payment Anomalies Found"
          description="All payment gateway transactions are reconciled and settled cleanly."
          actionLabel="Clear Filter"
          onAction={() => {
            setStatusFilter('');
            setSearchQuery('');
          }}
        />
      ) : (
        <div className="rounded-xl border border-slate-800 bg-slate-950 overflow-hidden shadow-xl">
          <table className="w-full text-left text-sm">
            <thead className="sticky top-0 bg-slate-900/95 backdrop-blur z-10 border-b border-slate-800 text-xs font-semibold uppercase tracking-wider text-slate-400">
              <tr>
                <th className="py-3 px-4">Transaction ID</th>
                <th className="py-3 px-4">Order Reference</th>
                <th className="py-3 px-4">Gateway Reference</th>
                <th className="py-3 px-4">Amount</th>
                <th className="py-3 px-4">Status</th>
                <th className="py-3 px-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/80">
              {transactions.map((txn) => (
                <tr
                  key={txn.id}
                  onClick={() => setSelectedTxn(txn)}
                  className="hover:bg-slate-900/50 cursor-pointer transition group"
                >
                  <td className="py-3 px-4">
                    <span className="font-mono text-xs font-bold text-indigo-400">{txn.id}</span>
                  </td>
                  <td className="py-3 px-4 font-mono text-xs text-slate-300">{txn.orderId}</td>
                  <td className="py-3 px-4 font-mono text-xs text-slate-400 max-w-xs truncate">
                    {txn.providerReference}
                  </td>
                  <td className="py-3 px-4 font-mono text-xs font-bold text-emerald-400">
                    {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(txn.amount)}
                  </td>
                  <td className="py-3 px-4">
                    <StatusBadge status={txn.status} />
                  </td>
                  <td
                    className="py-3 px-4 text-right space-x-2"
                    onClick={(e) => e.stopPropagation()}
                  >
                    {txn.status !== 'SUCCESS' && (
                      <button
                        onClick={() => {
                          setSelectedTxn(txn);
                          setIsReconcileModalOpen(true);
                        }}
                        className="px-2.5 py-1 text-xs font-semibold bg-emerald-600 hover:bg-emerald-500 text-white rounded transition shadow-xs flex items-center gap-1 ml-auto"
                      >
                        <FileCheck2 className="h-3.5 w-3.5" />
                        <span>Reconcile (BR-012)</span>
                      </button>
                    )}
                    {txn.status === 'SUCCESS' && (
                      <button
                        onClick={() => setSelectedTxn(txn)}
                        className="p-1 text-slate-400 hover:text-white rounded transition"
                        title="View Reconciled Evidence"
                      >
                        <Eye className="h-4 w-4" />
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Slide-over Drawer for Payment Details & Evidence */}
      <SlideOverDrawer
        isOpen={!!selectedTxn}
        onClose={() => setSelectedTxn(null)}
        title={`Payment Transaction #${selectedTxn?.id}`}
        subtitle={`Order: ${selectedTxn?.orderId}`}
        idToCopy={selectedTxn?.id}
        badge={selectedTxn ? <StatusBadge status={selectedTxn.status} /> : null}
        footerActions={
          selectedTxn && selectedTxn.status !== 'SUCCESS' ? (
            <button
              onClick={() => setIsReconcileModalOpen(true)}
              className="px-4 py-2 text-xs font-semibold bg-emerald-600 hover:bg-emerald-500 text-white rounded-lg shadow-sm transition flex items-center gap-1.5"
            >
              <FileCheck2 className="h-4 w-4" />
              <span>Perform Controlled Reconciliation (BR-012)</span>
            </button>
          ) : null
        }
      >
        {selectedTxn && (
          <div className="space-y-6">
            <div className="p-4 rounded-xl bg-slate-950 border border-slate-800 space-y-3 text-xs">
              <div className="flex justify-between items-center">
                <span className="text-slate-400">Payment Amount</span>
                <span className="font-mono text-emerald-400 font-extrabold text-sm">
                  {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(selectedTxn.amount)}
                </span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-slate-400">Attempted Timestamp</span>
                <span className="font-mono text-slate-300">
                  {new Date(selectedTxn.attemptedAt).toLocaleString()}
                </span>
              </div>
              {selectedTxn.confirmedAt && (
                <div className="flex justify-between items-center">
                  <span className="text-slate-400">Confirmed Timestamp</span>
                  <span className="font-mono text-emerald-400">
                    {new Date(selectedTxn.confirmedAt).toLocaleString()}
                  </span>
                </div>
              )}
              <div className="pt-2 border-t border-slate-800 flex justify-between items-center">
                <span className="text-slate-400">Provider Transaction Code</span>
                <span className="font-mono text-slate-200">{selectedTxn.providerReference}</span>
              </div>
            </div>

            {/* Reconciliation Audit Trail */}
            {selectedTxn.evidenceReference && (
              <div className="p-4 rounded-xl border border-emerald-500/20 bg-emerald-500/5 space-y-3 text-xs">
                <div className="flex items-center gap-2 text-emerald-400 font-bold">
                  <CheckCircle className="h-4 w-4" />
                  <span>Manual Reconciliation Record (BR-012)</span>
                </div>
                <div>
                  <span className="text-slate-400 block text-[11px] uppercase font-semibold">Evidence Reference</span>
                  <p className="font-mono font-bold text-white mt-0.5">{selectedTxn.evidenceReference}</p>
                </div>
                <div>
                  <span className="text-slate-400 block text-[11px] uppercase font-semibold">Justification Reason</span>
                  <p className="text-slate-300 mt-0.5 leading-relaxed">{selectedTxn.reconciliationReason}</p>
                </div>
              </div>
            )}
          </div>
        )}
      </SlideOverDrawer>

      {/* Manual Reconcile Modal (FR-038, BR-012) */}
      <Modal
        isOpen={isReconcileModalOpen}
        onClose={() => setIsReconcileModalOpen(false)}
        title="Xác nhận đối soát thanh toán thủ công (FR-038, BR-012)"
        subtitle="Ràng buộc kiểm toán sổ cái tài chính & bằng chứng giao dịch WORM"
        footerActions={
          <>
            <button
              onClick={() => setIsReconcileModalOpen(false)}
              className="px-4 py-2 text-xs font-semibold text-slate-400 hover:text-white"
            >
              Hủy
            </button>
            <button
              disabled={!canSubmitReconcile}
              onClick={() => {
                if (selectedTxn) {
                  reconcileMutation.mutate({
                    id: selectedTxn.id,
                    external_transaction_id: externalTxnId.trim(),
                    resolution_type: resolutionType,
                    audit_justification: auditJustification.trim(),
                  });
                }
              }}
              className="px-4 py-2 text-xs font-semibold bg-emerald-600 hover:bg-emerald-500 disabled:opacity-50 disabled:cursor-not-allowed text-white rounded-lg transition shadow-sm"
            >
              {reconcileMutation.isPending ? 'Đang ghi nhận WORM...' : 'Xác nhận đối soát (Approve)'}
            </button>
          </>
        }
      >
        <div className="space-y-4">
          {/* Financial Risk Warning Banner */}
          <div className="p-3.5 rounded-xl bg-amber-500/10 border border-amber-500/30 text-xs text-amber-300 space-y-1">
            <p className="font-bold flex items-center gap-1.5 text-amber-200">
              <AlertTriangle className="h-4 w-4 shrink-0 text-amber-400" />
              Cảnh báo rủi ro tài chính & Kiểm toán bất biến (BR-012)
            </p>
            <p className="text-[11px] text-amber-300/90 leading-relaxed">
              Hành động này can thiệp trực tiếp vào số dư sổ cái kế toán và sẽ được ghi vào WORM Audit Log không thể hoàn tác.
            </p>
          </div>

          {/* RBAC Authorization Guard Notice */}
          {!isAuthorizedAuditor && (
            <div className="p-3 rounded-lg bg-rose-500/10 border border-rose-500/20 text-xs text-rose-300 flex items-center gap-2">
              <Lock className="h-4 w-4 shrink-0 text-rose-400" />
              <span>
                Quyền hạn bị từ chối: Chỉ <strong className="font-mono text-white">FINANCIAL_AUDITOR</strong> hoặc <strong className="font-mono text-white">SUPER_ADMIN</strong> mới có quyền đối soát thủ công (Role hiện tại: {user?.roles[0] || 'NONE'}).
              </span>
            </div>
          )}

          <div className="p-3 rounded-lg bg-slate-950 border border-slate-800 text-xs space-y-1">
            <div className="flex justify-between">
              <span className="text-slate-400">Transaction ID:</span>
              <span className="font-mono font-bold text-white">{selectedTxn?.id}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-400">Order Reference:</span>
              <span className="font-mono text-indigo-400">{selectedTxn?.orderId}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-400">Số tiền:</span>
              <span className="font-mono font-bold text-emerald-400">
                {selectedTxn ? new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(selectedTxn.amount) : 0}
              </span>
            </div>
          </div>

          {/* External Transaction ID (Regex 6-64 chars) */}
          <div className="space-y-1.5">
            <div className="flex items-center justify-between">
              <label className="text-xs font-semibold text-slate-300 uppercase">
                Mã giao dịch đối soát bên ngoài (external_transaction_id)
              </label>
              <span className="text-[11px] text-rose-400">* Bắt buộc (6-64 ký tự)</span>
            </div>
            <input
              type="text"
              value={externalTxnId}
              onChange={(e) => setExternalTxnId(e.target.value)}
              placeholder="e.g. VCB-TXN-20260927-88910 hoặc STRIPE-CH-9941"
              className={`w-full bg-slate-950 border rounded-lg px-3 py-2 text-xs text-white font-mono focus:outline-none ${
                externalTxnId.length > 0 && !isExternalIdValid
                  ? 'border-rose-500 focus:border-rose-500'
                  : 'border-slate-700 focus:border-indigo-500'
              }`}
              required
            />
            {externalTxnId.length > 0 && !isExternalIdValid && (
              <p className="text-[11px] text-rose-400">
                Độ dài yêu cầu từ 6 đến 64 ký tự chữ số/gạch nối (alphanumeric/hyphen/underscore).
              </p>
            )}
          </div>

          {/* Resolution Type */}
          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-300 uppercase">
              Hình thức xử lý đối soát (resolution_type)
            </label>
            <select
              value={resolutionType}
              onChange={(e) => setResolutionType(e.target.value as ResolutionType)}
              className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white font-mono focus:outline-none focus:border-indigo-500"
            >
              <option value="ADJUST_LEDGER_CONFIRM">
                ADJUST_LEDGER_CONFIRM - Xác nhận đã thu tiền thực tế (Xác nhận PAID)
              </option>
              <option value="FORCE_REFUND">
                FORCE_REFUND - Xác nhận hoàn tiền cho khách (Chuyển REFUNDED)
              </option>
            </select>
          </div>

          {/* Audit Justification (Min 15 chars) */}
          <div className="space-y-1.5">
            <div className="flex items-center justify-between">
              <label className="text-xs font-semibold text-slate-300 uppercase">
                Biên bản giải trình kiểm toán (audit_justification)
              </label>
              <span
                className={`text-[11px] font-mono ${
                  auditJustification.trim().length >= 15 ? 'text-emerald-400 font-semibold' : 'text-rose-400'
                }`}
              >
                {auditJustification.trim().length} / 15 ký tự tối thiểu
              </span>
            </div>
            <textarea
              rows={3}
              value={auditJustification}
              onChange={(e) => setAuditJustification(e.target.value)}
              placeholder="Giải trình chi tiết kết quả đối soát thực tế từ sao kê ngân hàng / cổng thanh toán (tối thiểu 15 ký tự)..."
              className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white focus:outline-none focus:border-indigo-500 placeholder-slate-500"
              required
            />
          </div>
        </div>
      </Modal>
    </div>
  );
};
