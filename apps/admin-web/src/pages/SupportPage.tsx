import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '@/lib/api-client';
import { CustomerUser, NotificationLog, PageResponse } from '@/types';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { SkeletonTable } from '@/components/ui/SkeletonTable';
import { EmptyState } from '@/components/ui/EmptyState';
import { Modal } from '@/components/ui/Modal';
import { useToastStore } from '@/store/toast-store';
import {
  Unlock,
  Ban,
  ShieldCheck,
  Send,
  Search,
  UserCheck,
  Mail,
  RefreshCw,
} from 'lucide-react';

const MOCK_LOCKED_USERS: CustomerUser[] = [
  {
    id: 'usr-cust-9901',
    email: 'customer.locked@example.com',
    fullName: 'Nguyen Thi Huong',
    phone: '0901234567',
    roles: ['CUSTOMER'],
    isActive: true,
    isLocked: true,
    createdAt: '2026-08-15T09:00:00Z',
  },
  {
    id: 'usr-cust-9905',
    email: 'trader.failed@example.com',
    fullName: 'Hoang Van Bach',
    phone: '0988776655',
    roles: ['CUSTOMER'],
    isActive: true,
    isLocked: true,
    createdAt: '2026-09-02T14:30:00Z',
  },
];

const MOCK_NOTIFICATION_LOGS: NotificationLog[] = [
  {
    id: 'notif-log-5001',
    orderId: 'ord-1001-8842',
    recipient: 'customer.locked@example.com',
    channel: 'EMAIL',
    templateCode: 'ORDER_CONFIRMED_V1',
    status: 'FAILED',
    retryCount: 3,
    sentAt: undefined,
    createdAt: '2026-09-27T08:35:00Z',
  },
  {
    id: 'notif-log-5002',
    orderId: 'ord-1002-9931',
    recipient: 'tran.mai@example.com',
    channel: 'SMS',
    templateCode: 'SHIPMENT_DISPATCHED_V1',
    status: 'FAILED',
    retryCount: 2,
    sentAt: undefined,
    createdAt: '2026-09-27T07:30:00Z',
  },
];

export const SupportPage: React.FC = () => {
  const queryClient = useQueryClient();
  const { showSuccess, showError } = useToastStore();

  const [activeTab, setActiveTab] = useState<'unlock' | 'notifications' | 'cancellation'>('unlock');
  const [searchQuery, setSearchQuery] = useState('');

  // Modals
  const [isUnlockModalOpen, setIsUnlockModalOpen] = useState(false);
  const [selectedUser, setSelectedUser] = useState<CustomerUser | null>(null);
  const [unlockReason, setUnlockReason] = useState('Customer confirmed identity via telephone support ticket');

  const [isCancelModalOpen, setIsCancelModalOpen] = useState(false);
  const [cancelOrderId, setCancelOrderId] = useState('');
  const [cancelReason, setCancelReason] = useState('Customer requested order cancellation prior to packaging');

  // Query Locked Users
  const { data: users = MOCK_LOCKED_USERS, isLoading: isLoadingUsers, refetch: refetchUsers, isFetching } = useQuery<CustomerUser[]>({
    queryKey: ['locked-users'],
    queryFn: async () => {
      try {
        const res = await apiClient<CustomerUser[]>('/api/v1/backoffice/users');
        return res.filter((u) => u.isLocked);
      } catch {
        return MOCK_LOCKED_USERS;
      }
    },
  });

  // Query Failed Notification Logs
  const { data: notifData, isLoading: isLoadingNotifs, refetch: refetchNotifs } = useQuery<PageResponse<NotificationLog>>({
    queryKey: ['failed-notifications'],
    queryFn: async () => {
      try {
        return await apiClient<PageResponse<NotificationLog>>('/api/v1/backoffice/notifications?status=FAILED');
      } catch {
        return { content: MOCK_NOTIFICATION_LOGS, totalElements: MOCK_NOTIFICATION_LOGS.length };
      }
    },
  });

  // Whitelisted Action 1: UNLOCK_ACCOUNT (FR-041)
  const unlockMutation = useMutation({
    mutationFn: async (userId: string) => {
      return apiClient(`/api/v1/backoffice/users/${userId}/unlock`, {
        method: 'POST',
      });
    },
    onSuccess: (_, userId) => {
      queryClient.invalidateQueries({ queryKey: ['locked-users'] });
      showSuccess('Account Unlocked (FR-041)', `Account #${userId} unlocked. Customer may now sign in.`);
      setIsUnlockModalOpen(false);
    },
    onError: (err) => showError(err, 'Failed to unlock customer account'),
  });

  // Whitelisted Action 2: RESEND_NOTIFICATION (FR-041)
  const resendNotifMutation = useMutation({
    mutationFn: async (logId: string) => {
      return apiClient(`/api/v1/backoffice/notifications/${logId}/retry`, {
        method: 'POST',
      });
    },
    onSuccess: (_, logId) => {
      queryClient.invalidateQueries({ queryKey: ['failed-notifications'] });
      showSuccess('Notification Resent (FR-041)', `Dispatched retry event for notification log #${logId}`);
    },
    onError: (err) => showError(err, 'Failed to resend notification'),
  });

  // Whitelisted Action 3: INITIATE_CANCEL (FR-041, BR-006)
  const initiateCancelMutation = useMutation({
    mutationFn: async ({ orderId, reason }: { orderId: string; reason: string }) => {
      return apiClient(`/api/v1/orders/${orderId}/cancel`, {
        method: 'POST',
        body: JSON.stringify({ reason }),
      });
    },
    onSuccess: (_, { orderId }) => {
      showSuccess('Cancellation Initiated (FR-041)', `Order #${orderId} cancelled per customer request`);
      setIsCancelModalOpen(false);
      setCancelOrderId('');
    },
    onError: (err) => showError(err, 'Cancellation rejected (Order may already be PACKING or later per BR-006)'),
  });

  return (
    <div className="space-y-6">
      {/* Title & Top Controls */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-white">Customer Support Assisted Operations</h1>
          <p className="text-sm text-slate-400">
            Whitelisted customer assistance console adhering to strict support boundaries (API-SUP-001, FR-041, BR-016)
          </p>
        </div>
        <button
          onClick={() => {
            refetchUsers();
            refetchNotifs();
          }}
          disabled={isFetching}
          className="flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs font-semibold bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 transition w-fit"
        >
          <RefreshCw className={`h-3.5 w-3.5 ${isFetching ? 'animate-spin' : ''}`} />
          <span>Sync Support Data</span>
        </button>
      </div>

      {/* Strict Support Guardrail Notice (BR-016) */}
      <div className="p-4 rounded-xl border border-indigo-500/20 bg-indigo-500/5 flex items-start gap-3">
        <ShieldCheck className="h-5 w-5 text-indigo-400 shrink-0 mt-0.5" />
        <div className="text-xs space-y-1">
          <p className="font-bold text-indigo-300">Customer Support Invariant & Whitelist (BR-016, FR-041)</p>
          <p className="text-slate-400 leading-relaxed">
            The Customer Support role is strictly limited to 3 whitelisted actions:
            <strong className="text-white font-mono"> RESEND_NOTIFICATION</strong>,
            <strong className="text-white font-mono"> UNLOCK_ACCOUNT</strong>, and
            <strong className="text-white font-mono"> INITIATE_CANCEL</strong> (subject to the pre-packing window).
            Direct alteration of order prices, refunds without ledger validation, or manual inventory balance edits are strictly blocked.
          </p>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex items-center gap-2 border-b border-slate-800 pb-2">
        <button
          onClick={() => setActiveTab('unlock')}
          className={`flex items-center gap-2 px-4 py-2 text-xs font-bold rounded-lg transition ${
            activeTab === 'unlock'
              ? 'bg-indigo-600 text-white shadow-sm shadow-indigo-500/20'
              : 'text-slate-400 hover:text-white hover:bg-slate-800'
          }`}
        >
          <Unlock className="h-4 w-4" />
          <span>Unlock Accounts ({users.length})</span>
        </button>
        <button
          onClick={() => setActiveTab('notifications')}
          className={`flex items-center gap-2 px-4 py-2 text-xs font-bold rounded-lg transition ${
            activeTab === 'notifications'
              ? 'bg-indigo-600 text-white shadow-sm shadow-indigo-500/20'
              : 'text-slate-400 hover:text-white hover:bg-slate-800'
          }`}
        >
          <Mail className="h-4 w-4" />
          <span>Resend Notifications ({notifData?.totalElements || 0})</span>
        </button>
        <button
          onClick={() => setActiveTab('cancellation')}
          className={`flex items-center gap-2 px-4 py-2 text-xs font-bold rounded-lg transition ${
            activeTab === 'cancellation'
              ? 'bg-indigo-600 text-white shadow-sm shadow-indigo-500/20'
              : 'text-slate-400 hover:text-white hover:bg-slate-800'
          }`}
        >
          <Ban className="h-4 w-4" />
          <span>Order Cancellation Assistance</span>
        </button>
      </div>

      {/* Tab 1: Unlock Accounts */}
      {activeTab === 'unlock' && (
        <div className="space-y-4">
          <div className="relative w-full md:w-80">
            <Search className="absolute left-3 top-2.5 h-4 w-4 text-slate-500" />
            <input
              type="text"
              placeholder="Search locked accounts by email or ID..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full bg-slate-950 border border-slate-700/80 rounded-lg pl-9 pr-3 py-2 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-indigo-500"
            />
          </div>

          {isLoadingUsers ? (
            <SkeletonTable rows={3} cols={5} />
          ) : users.length === 0 ? (
            <EmptyState
              icon={UserCheck}
              title="No Locked Accounts"
              description="All customer accounts in the identity service are active and in good standing."
            />
          ) : (
            <div className="rounded-xl border border-slate-800 bg-slate-950 overflow-hidden shadow-xl">
              <table className="w-full text-left text-sm">
                <thead className="sticky top-0 bg-slate-900/95 backdrop-blur z-10 border-b border-slate-800 text-xs font-semibold uppercase tracking-wider text-slate-400">
                  <tr>
                    <th className="py-3 px-4">User Identifier</th>
                    <th className="py-3 px-4">Customer Name</th>
                    <th className="py-3 px-4">Email & Phone</th>
                    <th className="py-3 px-4">Account Status</th>
                    <th className="py-3 px-4 text-right">Support Action</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800/80">
                  {users.map((cust) => (
                    <tr key={cust.id} className="hover:bg-slate-900/50 transition">
                      <td className="py-3 px-4">
                        <span className="font-mono text-xs font-bold text-indigo-400">{cust.id}</span>
                      </td>
                      <td className="py-3 px-4 font-semibold text-white text-xs">{cust.fullName}</td>
                      <td className="py-3 px-4 text-xs">
                        <p className="text-slate-300">{cust.email}</p>
                        <p className="font-mono text-slate-500 text-[11px]">{cust.phone || 'No phone'}</p>
                      </td>
                      <td className="py-3 px-4">
                        <StatusBadge status="LOCKED" variant="rose" />
                      </td>
                      <td className="py-3 px-4 text-right">
                        <button
                          onClick={() => {
                            setSelectedUser(cust);
                            setIsUnlockModalOpen(true);
                          }}
                          className="px-3 py-1.5 text-xs font-semibold bg-emerald-600 hover:bg-emerald-500 text-white rounded-lg transition shadow-xs flex items-center gap-1.5 ml-auto"
                        >
                          <Unlock className="h-3.5 w-3.5" />
                          <span>Unlock Account</span>
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {/* Tab 2: Failed Notifications Resend */}
      {activeTab === 'notifications' && (
        <div className="space-y-4">
          {isLoadingNotifs ? (
            <SkeletonTable rows={3} cols={5} />
          ) : (notifData?.content || []).length === 0 ? (
            <EmptyState
              icon={Mail}
              title="No Failed Notifications"
              description="All outgoing email and SMS notifications have successfully delivered."
            />
          ) : (
            <div className="rounded-xl border border-slate-800 bg-slate-950 overflow-hidden shadow-xl">
              <table className="w-full text-left text-sm">
                <thead className="sticky top-0 bg-slate-900/95 backdrop-blur z-10 border-b border-slate-800 text-xs font-semibold uppercase tracking-wider text-slate-400">
                  <tr>
                    <th className="py-3 px-4">Log ID</th>
                    <th className="py-3 px-4">Channel & Template</th>
                    <th className="py-3 px-4">Recipient</th>
                    <th className="py-3 px-4">Status & Retries</th>
                    <th className="py-3 px-4 text-right">Resend Action</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800/80">
                  {(notifData?.content || []).map((log) => (
                    <tr key={log.id} className="hover:bg-slate-900/50 transition">
                      <td className="py-3 px-4">
                        <span className="font-mono text-xs font-bold text-indigo-400">{log.id}</span>
                        {log.orderId && (
                          <p className="font-mono text-[11px] text-slate-500">Order: {log.orderId}</p>
                        )}
                      </td>
                      <td className="py-3 px-4">
                        <span className="font-bold text-white text-xs">{log.channel}</span>
                        <p className="font-mono text-[11px] text-slate-400">{log.templateCode}</p>
                      </td>
                      <td className="py-3 px-4 font-mono text-xs text-slate-300">{log.recipient}</td>
                      <td className="py-3 px-4">
                        <div className="flex items-center gap-2">
                          <StatusBadge status={log.status} variant="rose" />
                          <span className="text-[11px] font-mono text-slate-500">({log.retryCount} tries)</span>
                        </div>
                      </td>
                      <td className="py-3 px-4 text-right">
                        <button
                          onClick={() => resendNotifMutation.mutate(log.id)}
                          disabled={resendNotifMutation.isPending}
                          className="px-3 py-1.5 text-xs font-semibold bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 text-white rounded-lg transition shadow-xs flex items-center gap-1.5 ml-auto"
                        >
                          <Send className="h-3.5 w-3.5" />
                          <span>Resend (FR-041)</span>
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {/* Tab 3: Order Cancellation Assistance */}
      {activeTab === 'cancellation' && (
        <div className="rounded-xl border border-slate-800 bg-slate-950 p-6 space-y-6">
          <div>
            <h3 className="text-base font-bold text-white">Assisted Customer Order Cancellation</h3>
            <p className="text-xs text-slate-400 leading-relaxed">
              Subject to invariant BR-006: Orders can only be cancelled while in state
              <strong className="text-white font-mono"> RESERVED</strong> or
              <strong className="text-white font-mono"> PAID</strong>. Once an order enters
              <strong className="text-rose-400 font-mono"> PACKING</strong> or later, the cancellation window closes.
            </p>
          </div>

          <div className="max-w-md space-y-4">
            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-slate-300 uppercase">Target Order ID</label>
              <input
                type="text"
                placeholder="e.g. ord-1001-8842"
                value={cancelOrderId}
                onChange={(e) => setCancelOrderId(e.target.value)}
                className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white font-mono focus:outline-none focus:border-indigo-500"
              />
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-slate-300 uppercase">Customer Reason</label>
              <input
                type="text"
                value={cancelReason}
                onChange={(e) => setCancelReason(e.target.value)}
                className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white focus:outline-none focus:border-indigo-500"
              />
            </div>

            <button
              disabled={!cancelOrderId.trim()}
              onClick={() => setIsCancelModalOpen(true)}
              className="px-4 py-2 text-xs font-semibold bg-rose-600 hover:bg-rose-500 disabled:opacity-50 text-white rounded-lg shadow-sm transition flex items-center gap-1.5"
            >
              <Ban className="h-4 w-4" />
              <span>Initiate Cancellation Check (BR-006)</span>
            </button>
          </div>
        </div>
      )}

      {/* Modal: Unlock Account Confirmation */}
      <Modal
        isOpen={isUnlockModalOpen}
        onClose={() => setIsUnlockModalOpen(false)}
        title="Unlock Customer Account (FR-041)"
        subtitle="Restores login access for authenticated user"
        footerActions={
          <>
            <button
              onClick={() => setIsUnlockModalOpen(false)}
              className="px-4 py-2 text-xs font-semibold text-slate-400 hover:text-white"
            >
              Cancel
            </button>
            <button
              disabled={unlockMutation.isPending}
              onClick={() => {
                if (selectedUser) {
                  unlockMutation.mutate(selectedUser.id);
                }
              }}
              className="px-4 py-2 text-xs font-semibold bg-emerald-600 hover:bg-emerald-500 text-white rounded-lg transition"
            >
              {unlockMutation.isPending ? 'Unlocking...' : 'Confirm Account Unlock'}
            </button>
          </>
        }
      >
        <div className="space-y-4">
          <div className="p-3 rounded-lg bg-emerald-500/10 border border-emerald-500/20 text-xs text-emerald-300">
            Unlocking Account: <strong className="text-white">{selectedUser?.fullName}</strong> ({selectedUser?.email})
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-300 uppercase">Support Note / Verification</label>
            <input
              type="text"
              value={unlockReason}
              onChange={(e) => setUnlockReason(e.target.value)}
              className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white focus:outline-none focus:border-indigo-500"
            />
          </div>
        </div>
      </Modal>

      {/* Modal: Cancel Order Confirmation */}
      <Modal
        isOpen={isCancelModalOpen}
        onClose={() => setIsCancelModalOpen(false)}
        title="Execute Customer Support Order Cancellation"
        subtitle="Verifies cancellation cutoff prior to PACKING (BR-006)"
        footerActions={
          <>
            <button
              onClick={() => setIsCancelModalOpen(false)}
              className="px-4 py-2 text-xs font-semibold text-slate-400 hover:text-white"
            >
              Cancel
            </button>
            <button
              disabled={initiateCancelMutation.isPending}
              onClick={() => {
                initiateCancelMutation.mutate({
                  orderId: cancelOrderId,
                  reason: cancelReason,
                });
              }}
              className="px-4 py-2 text-xs font-semibold bg-rose-600 hover:bg-rose-500 text-white rounded-lg transition"
            >
              {initiateCancelMutation.isPending ? 'Verifying & Cancelling...' : 'Execute Cancellation'}
            </button>
          </>
        }
      >
        <div className="space-y-4">
          <div className="p-3 rounded-lg bg-rose-500/10 border border-rose-500/20 text-xs text-rose-300">
            Order Identifier: <strong className="text-white font-mono">{cancelOrderId}</strong>
          </div>
          <p className="text-xs text-slate-400 leading-relaxed">
            The order service will inspect current status. If the order has already progressed to PACKING or later, the cancellation will be strictly rejected per BR-006.
          </p>
        </div>
      </Modal>
    </div>
  );
};
