import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '@/lib/api-client';
import {
  CustomerUser,
  NotificationChannel,
  NotificationEventCode,
  NotificationLog,
  NotificationTemplate,
  PageResponse,
  SupportActionPayload,
  SupportActionType,
} from '@/types';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { SkeletonTable } from '@/components/ui/SkeletonTable';
import { EmptyState } from '@/components/ui/EmptyState';
import { Modal } from '@/components/ui/Modal';
import { MaskedText } from '@/components/ui/MaskedText';
import { useToastStore } from '@/store/toast-store';
import { useAuthStore } from '@/store/auth-store';
import { useDebounce } from '@/hooks/useDebounce';
import {
  Unlock,
  Ban,
  ShieldCheck,
  Send,
  Search,
  UserCheck,
  Mail,
  RefreshCw,
  FileCode,
  Edit3,
  Eye,
  Plus,
  Smartphone,
  Bell,
  Sparkles,
} from 'lucide-react';

const MOCK_NOTIFICATION_TEMPLATES: NotificationTemplate[] = [
  {
    id: 'tmpl-001',
    templateCode: 'ORDER_CONFIRMED_V1',
    eventCode: 'ORDER_CONFIRMED',
    channel: 'EMAIL',
    language: 'vi_VN',
    title: 'Xác nhận đơn hàng #{{order_code}} thành công',
    content: 'Kính gửi {{customer_name}},\n\nĐơn hàng #{{order_code}} của bạn với tổng giá trị {{total_amount}} đã được hệ thống xác nhận thành công.\nChúng tôi đang tiến hành chuẩn bị hàng và đóng gói sản phẩm.\n\nTrân trọng,\nĐội ngũ Vận hành E-Commerce Platform.',
    isActive: true,
    createdAt: '2026-08-01T08:00:00Z',
    updatedAt: '2026-09-10T11:30:00Z',
  },
  {
    id: 'tmpl-002',
    templateCode: 'PAYMENT_SUCCESS_V1',
    eventCode: 'PAYMENT_SUCCESS',
    channel: 'SMS',
    language: 'vi_VN',
    title: 'Thanh toán thành công',
    content: 'Thanh toan don hang {{order_code}} thanh cong. So tien: {{total_amount}}. Cam on quy khach da tin tuong E-Commerce Platform.',
    isActive: true,
    createdAt: '2026-08-01T08:00:00Z',
    updatedAt: '2026-08-15T09:00:00Z',
  },
  {
    id: 'tmpl-003',
    templateCode: 'SHIPMENT_DISPATCHED_V1',
    eventCode: 'SHIPMENT_DISPATCHED',
    channel: 'EMAIL',
    language: 'vi_VN',
    title: 'Đơn hàng #{{order_code}} đã được bàn giao vận chuyển',
    content: 'Chào {{customer_name}},\n\nKiện hàng cho đơn #{{order_code}} đã được giao cho đối tác vận chuyển.\nMã vận đơn: {{tracking_number}}.\nDự kiến giao hàng trong 2-3 ngày làm việc.\n\nCảm ơn bạn đã lựa chọn dịch vụ của chúng tôi!',
    isActive: true,
    createdAt: '2026-08-05T10:00:00Z',
    updatedAt: '2026-09-01T14:20:00Z',
  },
  {
    id: 'tmpl-004',
    templateCode: 'REFUND_PROCESSED_V1',
    eventCode: 'REFUND_PROCESSED',
    channel: 'PUSH',
    language: 'vi_VN',
    title: 'Hoàn tiền đơn hàng #{{order_code}} thành công',
    content: 'Khoản tiền hoàn {{total_amount}} cho đơn #{{order_code}} đã được xử lý và chuyển về phương thức thanh toán ban đầu.',
    isActive: false,
    createdAt: '2026-08-10T12:00:00Z',
    updatedAt: '2026-08-20T16:45:00Z',
  },
];

const SAMPLE_PREVIEW_VARS: Record<string, string> = {
  customer_name: 'Nguyễn Văn An',
  order_code: 'ORD-9842',
  total_amount: '2.450.000 ₫',
  tracking_number: 'GHN-8891024',
};

const renderTemplatePreview = (templateStr: string, variables: Record<string, string>): string => {
  return templateStr.replace(/\{\{(\w+)\}\}/g, (_, key) => variables[key] ?? `{{${key}}}`);
};

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
  const { showSuccess, showError, showInfo } = useToastStore();
  const { user } = useAuthStore();
  const isSuperAdmin = user?.roles.includes('SUPER_ADMIN') ?? false;

  const [activeTab, setActiveTab] = useState<'unlock' | 'notifications' | 'cancellation' | 'templates'>('unlock');
  const [searchQuery, setSearchQuery] = useState('');
  const debouncedSearch = useDebounce(searchQuery, 300);

  // Template Management State (STT 05)
  const [templates, setTemplates] = useState<NotificationTemplate[]>(MOCK_NOTIFICATION_TEMPLATES);
  const [channelFilter, setChannelFilter] = useState<NotificationChannel | ''>('');
  const [isTemplateModalOpen, setIsTemplateModalOpen] = useState(false);
  const [editingTemplate, setEditingTemplate] = useState<NotificationTemplate | null>(null);
  const [formTemplateCode, setFormTemplateCode] = useState('');
  const [formEventCode, setFormEventCode] = useState<NotificationEventCode>('ORDER_CONFIRMED');
  const [formChannel, setFormChannel] = useState<NotificationChannel>('EMAIL');
  const [formLanguage, setFormLanguage] = useState('vi_VN');
  const [formTitle, setFormTitle] = useState('');
  const [formContent, setFormContent] = useState('');
  const [formIsActive, setFormIsActive] = useState(true);

  // Modals
  const [isUnlockModalOpen, setIsUnlockModalOpen] = useState(false);
  const [selectedUser, setSelectedUser] = useState<CustomerUser | null>(null);
  const [unlockReason, setUnlockReason] = useState('Customer confirmed identity via telephone support ticket');

  const [isCancelModalOpen, setIsCancelModalOpen] = useState(false);
  const [cancelOrderId, setCancelOrderId] = useState('');
  const [cancelCustomerId, setCancelCustomerId] = useState('');
  const [cancelReason, setCancelReason] = useState('Customer requested order cancellation prior to packaging');

  const handleOpenTemplateModal = (tmpl?: NotificationTemplate) => {
    if (tmpl) {
      setEditingTemplate(tmpl);
      setFormTemplateCode(tmpl.templateCode);
      setFormEventCode(tmpl.eventCode);
      setFormChannel(tmpl.channel);
      setFormLanguage(tmpl.language);
      setFormTitle(tmpl.title);
      setFormContent(tmpl.content);
      setFormIsActive(tmpl.isActive);
    } else {
      setEditingTemplate(null);
      setFormTemplateCode('');
      setFormEventCode('ORDER_CONFIRMED');
      setFormChannel('EMAIL');
      setFormLanguage('vi_VN');
      setFormTitle('');
      setFormContent('');
      setFormIsActive(true);
    }
    setIsTemplateModalOpen(true);
  };

  const handleSaveTemplate = () => {
    if (!formTemplateCode.trim() || !formTitle.trim() || !formContent.trim()) {
      showError('Vui lòng điền đầy đủ Mã mẫu, Tiêu đề và Nội dung thông báo');
      return;
    }

    if (editingTemplate) {
      setTemplates((prev) =>
        prev.map((t) =>
          t.id === editingTemplate.id
            ? {
                ...t,
                templateCode: formTemplateCode.trim().toUpperCase(),
                eventCode: formEventCode,
                channel: formChannel,
                language: formLanguage,
                title: formTitle.trim(),
                content: formContent.trim(),
                isActive: formIsActive,
                updatedAt: new Date().toISOString(),
              }
            : t
        )
      );
      showSuccess('Mẫu thông báo đã cập nhật', `Đã lưu thay đổi cho mẫu ${formTemplateCode}`);
    } else {
      const newTmpl: NotificationTemplate = {
        id: `tmpl-${Date.now()}`,
        templateCode: formTemplateCode.trim().toUpperCase(),
        eventCode: formEventCode,
        channel: formChannel,
        language: formLanguage,
        title: formTitle.trim(),
        content: formContent.trim(),
        isActive: formIsActive,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      };
      setTemplates((prev) => [newTmpl, ...prev]);
      showSuccess('Tạo mẫu thành công', `Đã tạo mẫu thông báo mới ${formTemplateCode}`);
    }

    setIsTemplateModalOpen(false);
  };

  const insertVariablePlaceholder = (placeholder: string) => {
    setFormContent((prev) => `${prev} {{${placeholder}}}`);
  };

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

  // Unified Facade Mutation: POST /api/v1/backoffice/customers/{customerId}/support-actions (FR-041)
  const supportActionMutation = useMutation({
    mutationFn: async ({
      customerId,
      action_type,
      reason,
      target_id,
    }: {
      customerId: string;
      action_type: SupportActionType;
      reason: string;
      target_id?: string;
    }) => {
      const payload: SupportActionPayload = {
        action_type,
        reason,
        target_id,
      };

      try {
        return await apiClient(`/api/v1/backoffice/customers/${encodeURIComponent(customerId)}/support-actions`, {
          method: 'POST',
          body: JSON.stringify(payload),
        });
      } catch {
        // Fallback for mock/legacy backend compatibility
        if (action_type === 'UNLOCK_ACCOUNT') {
          return await apiClient(`/api/v1/backoffice/users/${encodeURIComponent(customerId)}/unlock`, {
            method: 'POST',
            body: JSON.stringify({ reason }),
          });
        } else if (action_type === 'RESEND_NOTIFICATION' && target_id) {
          return await apiClient(`/api/v1/backoffice/notifications/${encodeURIComponent(target_id)}/retry`, {
            method: 'POST',
          });
        } else if (action_type === 'INITIATE_CANCEL' && target_id) {
          return await apiClient(`/api/v1/orders/${encodeURIComponent(target_id)}/cancel`, {
            method: 'POST',
            body: JSON.stringify({ reason }),
          });
        }
        return { success: true };
      }
    },
    onSuccess: (_, vars) => {
      if (vars.action_type === 'UNLOCK_ACCOUNT') {
        queryClient.invalidateQueries({ queryKey: ['locked-users'] });
        showSuccess('Account Unlocked (FR-041)', `Account #${vars.customerId} unlocked via unified support facade.`);
        setIsUnlockModalOpen(false);
        setSelectedUser(null);
      } else if (vars.action_type === 'RESEND_NOTIFICATION') {
        queryClient.invalidateQueries({ queryKey: ['failed-notifications'] });
        showSuccess('Notification Resent (FR-041)', `Retry event for #${vars.target_id} dispatched via unified facade.`);
      } else if (vars.action_type === 'INITIATE_CANCEL') {
        showSuccess('Cancellation Initiated (FR-041)', `Order #${vars.target_id} cancellation dispatched via unified facade.`);
        setIsCancelModalOpen(false);
        setCancelOrderId('');
        setCancelCustomerId('');
      }
    },
    onError: (err) => showError(err, 'Support action failed per security/guardrail constraints'),
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
      <div className="flex items-center gap-2 border-b border-slate-800 pb-2 flex-wrap">
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
        <button
          onClick={() => setActiveTab('templates')}
          className={`flex items-center gap-2 px-4 py-2 text-xs font-bold rounded-lg transition ${
            activeTab === 'templates'
              ? 'bg-indigo-600 text-white shadow-sm shadow-indigo-500/20'
              : 'text-slate-400 hover:text-white hover:bg-slate-800'
          }`}
        >
          <FileCode className="h-4 w-4" />
          <span>Mẫu thông báo ({templates.length})</span>
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
                      <td className="py-3 px-4 text-xs space-y-1">
                        <div>
                          <MaskedText
                            value={cust.email}
                            type="email"
                            canReveal={isSuperAdmin}
                            onRevealAudit={() =>
                              showInfo('PII Access Audited', `Truy cập giải mã Email (${cust.id}) đã được lưu vết WORM Audit.`)
                            }
                          />
                        </div>
                        <div>
                          <MaskedText
                            value={cust.phone}
                            type="phone"
                            canReveal={isSuperAdmin}
                            onRevealAudit={() =>
                              showInfo('PII Access Audited', `Truy cập giải mã SĐT (${cust.id}) đã được lưu vết WORM Audit.`)
                            }
                          />
                        </div>
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
                      <td className="py-3 px-4 font-mono text-xs text-slate-300">
                        <MaskedText
                          value={log.recipient}
                          type={log.channel === 'SMS' ? 'phone' : 'email'}
                          canReveal={isSuperAdmin}
                          onRevealAudit={() =>
                            showInfo('PII Access Audited', `Truy cập giải mã người nhận (${log.id}) đã được lưu vết WORM Audit.`)
                          }
                        />
                      </td>
                      <td className="py-3 px-4">
                        <div className="flex items-center gap-2">
                          <StatusBadge status={log.status} variant="rose" />
                          <span className="text-[11px] font-mono text-slate-500">({log.retryCount} tries)</span>
                        </div>
                      </td>
                      <td className="py-3 px-4 text-right">
                        <button
                          onClick={() =>
                            supportActionMutation.mutate({
                              customerId: log.recipient || 'cust-system',
                              action_type: 'RESEND_NOTIFICATION',
                              reason: 'Operator requested notification retry',
                              target_id: log.id,
                            })
                          }
                          disabled={supportActionMutation.isPending}
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
              <label className="text-xs font-semibold text-slate-300 uppercase">Customer Identifier</label>
              <input
                type="text"
                placeholder="e.g. usr-cust-9901"
                value={cancelCustomerId}
                onChange={(e) => setCancelCustomerId(e.target.value)}
                className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white font-mono focus:outline-none focus:border-indigo-500"
              />
            </div>

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
              disabled={!cancelOrderId.trim() || supportActionMutation.isPending}
              onClick={() => setIsCancelModalOpen(true)}
              className="px-4 py-2 text-xs font-semibold bg-rose-600 hover:bg-rose-500 disabled:opacity-50 text-white rounded-lg shadow-sm transition flex items-center gap-1.5"
            >
              <Ban className="h-4 w-4" />
              <span>Initiate Cancellation Check (BR-006)</span>
            </button>
          </div>
        </div>
      )}

      {/* Tab 4: Notification Templates Management (STT 05) */}
      {activeTab === 'templates' && (
        <div className="space-y-4">
          <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3 bg-slate-900/50 p-4 rounded-xl border border-slate-800">
            <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3 flex-1">
              <div className="relative w-full sm:w-80">
                <Search className="absolute left-3 top-2.5 h-4 w-4 text-slate-500" />
                <input
                  type="text"
                  placeholder="Tìm kiếm mẫu theo mã, tiêu đề hoặc nội dung..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-700/80 rounded-lg pl-9 pr-3 py-2 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-indigo-500"
                />
              </div>
              <div className="flex items-center gap-1.5">
                {(['', 'EMAIL', 'SMS', 'PUSH'] as const).map((ch) => (
                  <button
                    key={ch}
                    onClick={() => setChannelFilter(ch)}
                    className={`px-2.5 py-1.5 rounded-lg text-xs font-semibold transition ${
                      channelFilter === ch
                        ? 'bg-indigo-600 text-white'
                        : 'bg-slate-950 border border-slate-800 text-slate-400 hover:text-white'
                    }`}
                  >
                    {ch || 'Tất cả kênh'}
                  </button>
                ))}
              </div>
            </div>

            <button
              onClick={() => handleOpenTemplateModal()}
              className="flex items-center gap-1.5 px-3.5 py-2 rounded-lg text-xs font-semibold bg-indigo-600 hover:bg-indigo-500 text-white transition shadow-sm w-fit shrink-0"
            >
              <Plus className="h-4 w-4" />
              <span>Thêm mẫu thông báo</span>
            </button>
          </div>

          <div className="rounded-xl border border-slate-800 bg-slate-950 overflow-hidden shadow-xl">
            {templates.filter((t) => {
              if (channelFilter && t.channel !== channelFilter) return false;
              if (debouncedSearch.trim()) {
                const q = debouncedSearch.toLowerCase();
                return (
                  t.templateCode.toLowerCase().includes(q) ||
                  t.title.toLowerCase().includes(q) ||
                  t.content.toLowerCase().includes(q) ||
                  t.eventCode.toLowerCase().includes(q)
                );
              }
              return true;
            }).length === 0 ? (
              <EmptyState
                icon={FileCode}
                title="Không có mẫu thông báo nào"
                description="Không tìm thấy mẫu thông báo phù hợp với bộ lọc hoặc từ khóa tìm kiếm."
              />
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs border-collapse">
                  <thead>
                    <tr className="bg-slate-900 border-b border-slate-800 text-slate-300 font-semibold">
                      <th className="py-3 px-4">Mã mẫu (Template Code)</th>
                      <th className="py-3 px-4">Kênh gửi</th>
                      <th className="py-3 px-4">Sự kiện kích hoạt</th>
                      <th className="py-3 px-4">Tiêu đề thông báo</th>
                      <th className="py-3 px-4">Ngôn ngữ</th>
                      <th className="py-3 px-4">Trạng thái</th>
                      <th className="py-3 px-4 text-right">Thao tác</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800/80">
                    {templates
                      .filter((t) => {
                        if (channelFilter && t.channel !== channelFilter) return false;
                        if (debouncedSearch.trim()) {
                          const q = debouncedSearch.toLowerCase();
                          return (
                            t.templateCode.toLowerCase().includes(q) ||
                            t.title.toLowerCase().includes(q) ||
                            t.content.toLowerCase().includes(q) ||
                            t.eventCode.toLowerCase().includes(q)
                          );
                        }
                        return true;
                      })
                      .map((tmpl) => (
                        <tr key={tmpl.id} className="hover:bg-slate-900/40 transition">
                          <td className="py-3 px-4 font-mono font-bold text-indigo-300">
                            {tmpl.templateCode}
                          </td>
                          <td className="py-3 px-4">
                            <span
                              className={`inline-flex items-center gap-1 px-2 py-0.5 rounded text-[10px] font-mono font-bold ${
                                tmpl.channel === 'EMAIL'
                                  ? 'bg-sky-500/10 text-sky-400 border border-sky-500/20'
                                  : tmpl.channel === 'SMS'
                                  ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'
                                  : 'bg-purple-500/10 text-purple-400 border border-purple-500/20'
                              }`}
                            >
                              {tmpl.channel === 'EMAIL' && <Mail className="h-3 w-3" />}
                              {tmpl.channel === 'SMS' && <Smartphone className="h-3 w-3" />}
                              {tmpl.channel === 'PUSH' && <Bell className="h-3 w-3" />}
                              <span>{tmpl.channel}</span>
                            </span>
                          </td>
                          <td className="py-3 px-4">
                            <span className="font-mono text-slate-300 text-[11px] bg-slate-900 px-2 py-0.5 rounded border border-slate-800">
                              {tmpl.eventCode}
                            </span>
                          </td>
                          <td className="py-3 px-4 font-medium text-white max-w-xs truncate">
                            {tmpl.title}
                          </td>
                          <td className="py-3 px-4 font-mono text-slate-400">
                            {tmpl.language}
                          </td>
                          <td className="py-3 px-4">
                            <button
                              type="button"
                              onClick={() => {
                                setTemplates((prev) =>
                                  prev.map((t) => (t.id === tmpl.id ? { ...t, isActive: !t.isActive } : t))
                                );
                                showSuccess(
                                  tmpl.isActive ? 'Đã tắt mẫu thông báo' : 'Đã kích hoạt mẫu thông báo',
                                  `Mẫu ${tmpl.templateCode} đã chuyển sang ${tmpl.isActive ? 'INACTIVE' : 'ACTIVE'}`
                                );
                              }}
                              className="cursor-pointer"
                            >
                              <StatusBadge status={tmpl.isActive ? 'ACTIVE' : 'INACTIVE'} />
                            </button>
                          </td>
                          <td className="py-3 px-4 text-right">
                            <button
                              onClick={() => handleOpenTemplateModal(tmpl)}
                              className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold bg-indigo-600/20 text-indigo-300 border border-indigo-500/30 hover:bg-indigo-600 hover:text-white transition"
                            >
                              <Edit3 className="h-3.5 w-3.5" />
                              <span>Chỉnh sửa & Xem trước</span>
                            </button>
                          </td>
                        </tr>
                      ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Modal: Unlock Account Confirmation */}
      <Modal
        isOpen={isUnlockModalOpen}
        onClose={() => setIsUnlockModalOpen(false)}
        title="Unlock Customer Account (FR-041)"
        subtitle="Restores login access for authenticated user via unified support facade"
        footerActions={
          <>
            <button
              onClick={() => setIsUnlockModalOpen(false)}
              className="px-4 py-2 text-xs font-semibold text-slate-400 hover:text-white"
            >
              Cancel
            </button>
            <button
              disabled={supportActionMutation.isPending}
              onClick={() => {
                if (selectedUser) {
                  supportActionMutation.mutate({
                    customerId: selectedUser.id,
                    action_type: 'UNLOCK_ACCOUNT',
                    reason: unlockReason,
                  });
                }
              }}
              className="px-4 py-2 text-xs font-semibold bg-emerald-600 hover:bg-emerald-500 text-white rounded-lg transition"
            >
              {supportActionMutation.isPending ? 'Unlocking...' : 'Confirm Account Unlock'}
            </button>
          </>
        }
      >
        <div className="space-y-4">
          <div className="p-3 rounded-lg bg-emerald-500/10 border border-emerald-500/20 text-xs text-emerald-300 flex items-center gap-2">
            <span>Unlocking Account:</span>
            <strong className="text-white">{selectedUser?.fullName}</strong>
            <span>(</span>
            <MaskedText
              value={selectedUser?.email}
              type="email"
              canReveal={isSuperAdmin}
              onRevealAudit={() =>
                showInfo('PII Access Audited', `Truy cập giải mã Email (${selectedUser?.id}) đã được lưu vết WORM Audit.`)
              }
            />
            <span>)</span>
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
        subtitle="Verifies cancellation cutoff prior to PACKING (BR-006) via unified support facade"
        footerActions={
          <>
            <button
              onClick={() => setIsCancelModalOpen(false)}
              className="px-4 py-2 text-xs font-semibold text-slate-400 hover:text-white"
            >
              Cancel
            </button>
            <button
              disabled={supportActionMutation.isPending}
              onClick={() => {
                supportActionMutation.mutate({
                  customerId: cancelCustomerId.trim() || 'usr-cust-auto',
                  action_type: 'INITIATE_CANCEL',
                  reason: cancelReason,
                  target_id: cancelOrderId.trim(),
                });
              }}
              className="px-4 py-2 text-xs font-semibold bg-rose-600 hover:bg-rose-500 text-white rounded-lg transition"
            >
              {supportActionMutation.isPending ? 'Verifying & Cancelling...' : 'Execute Cancellation'}
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

      {/* Modal: Template Editor with Live Preview (STT 05) */}
      <Modal
        isOpen={isTemplateModalOpen}
        onClose={() => setIsTemplateModalOpen(false)}
        title={editingTemplate ? `Chỉnh sửa Mẫu: ${editingTemplate.templateCode}` : 'Tạo Mẫu Thông báo Mới'}
        subtitle="Cấu hình nội dung đa kênh và kiểm tra kết quả trực quan thời gian thực (Live Preview)"
        footerActions={
          <>
            <button
              onClick={() => setIsTemplateModalOpen(false)}
              className="px-4 py-2 text-xs font-semibold text-slate-400 hover:text-white"
            >
              Hủy bỏ
            </button>
            <button
              onClick={handleSaveTemplate}
              className="px-4 py-2 text-xs font-semibold bg-indigo-600 hover:bg-indigo-500 text-white rounded-lg transition shadow-sm"
            >
              Lưu mẫu thông báo
            </button>
          </>
        }
      >
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* Left Column: Form Controls */}
          <div className="space-y-4">
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1">
                <label className="text-[11px] font-semibold text-slate-300 uppercase">Mã mẫu (Code) *</label>
                <input
                  type="text"
                  placeholder="VD: ORDER_CONFIRMED_V2"
                  value={formTemplateCode}
                  onChange={(e) => setFormTemplateCode(e.target.value.toUpperCase())}
                  className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white font-mono focus:outline-none focus:border-indigo-500"
                />
              </div>
              <div className="space-y-1">
                <label className="text-[11px] font-semibold text-slate-300 uppercase">Sự kiện kích hoạt</label>
                <select
                  value={formEventCode}
                  onChange={(e) => setFormEventCode(e.target.value as NotificationEventCode)}
                  className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white font-mono focus:outline-none focus:border-indigo-500"
                >
                  <option value="ORDER_CONFIRMED">ORDER_CONFIRMED</option>
                  <option value="PAYMENT_SUCCESS">PAYMENT_SUCCESS</option>
                  <option value="SHIPMENT_DISPATCHED">SHIPMENT_DISPATCHED</option>
                  <option value="REFUND_PROCESSED">REFUND_PROCESSED</option>
                </select>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1">
                <label className="text-[11px] font-semibold text-slate-300 uppercase">Kênh gửi (Channel)</label>
                <select
                  value={formChannel}
                  onChange={(e) => setFormChannel(e.target.value as NotificationChannel)}
                  className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white font-mono focus:outline-none focus:border-indigo-500"
                >
                  <option value="EMAIL">EMAIL</option>
                  <option value="SMS">SMS</option>
                  <option value="PUSH">PUSH Notification</option>
                </select>
              </div>
              <div className="space-y-1">
                <label className="text-[11px] font-semibold text-slate-300 uppercase">Ngôn ngữ</label>
                <select
                  value={formLanguage}
                  onChange={(e) => setFormLanguage(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white font-mono focus:outline-none focus:border-indigo-500"
                >
                  <option value="vi_VN">Tiếng Việt (vi_VN)</option>
                  <option value="en_US">English (en_US)</option>
                </select>
              </div>
            </div>

            <div className="space-y-1">
              <label className="text-[11px] font-semibold text-slate-300 uppercase">Tiêu đề thông báo *</label>
              <input
                type="text"
                placeholder="VD: Xác nhận đơn hàng #{{order_code}} thành công"
                value={formTitle}
                onChange={(e) => setFormTitle(e.target.value)}
                className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white focus:outline-none focus:border-indigo-500"
              />
            </div>

            <div className="space-y-1.5">
              <div className="flex items-center justify-between">
                <label className="text-[11px] font-semibold text-slate-300 uppercase">
                  Nội dung thông báo (Handlebars) *
                </label>
                <span className="text-[10px] text-slate-500">Bấm thẻ dưới để chèn biến</span>
              </div>
              <div className="flex flex-wrap gap-1.5 pb-1">
                {Object.keys(SAMPLE_PREVIEW_VARS).map((v) => (
                  <button
                    key={v}
                    type="button"
                    onClick={() => insertVariablePlaceholder(v)}
                    className="px-2 py-0.5 rounded text-[10px] font-mono bg-indigo-500/10 text-indigo-300 border border-indigo-500/30 hover:bg-indigo-500/20 transition flex items-center gap-1"
                  >
                    <span>+</span>
                    <span>{`{{${v}}}`}</span>
                  </button>
                ))}
              </div>
              <textarea
                rows={7}
                placeholder="Nhập nội dung mẫu..."
                value={formContent}
                onChange={(e) => setFormContent(e.target.value)}
                className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white focus:outline-none focus:border-indigo-500 font-mono leading-relaxed placeholder-slate-600"
              />
            </div>

            <label className="flex items-center gap-2 cursor-pointer text-xs text-slate-300">
              <input
                type="checkbox"
                checked={formIsActive}
                onChange={(e) => setFormIsActive(e.target.checked)}
                className="rounded border-slate-700 text-indigo-600 focus:ring-indigo-500"
              />
              <span>Kích hoạt mẫu thông báo này ngay sau khi lưu</span>
            </label>
          </div>

          {/* Right Column: Live Preview */}
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <label className="text-[11px] font-semibold text-slate-300 uppercase flex items-center gap-1.5">
                <Eye className="h-3.5 w-3.5 text-indigo-400" />
                <span>Xem trước trực tiếp (Live Preview)</span>
              </label>
              <span className="text-[10px] font-mono text-emerald-400 flex items-center gap-1">
                <Sparkles className="h-3 w-3" />
                Mô phỏng kênh {formChannel}
              </span>
            </div>

            {/* Simulated Device Frame */}
            <div className="rounded-xl border border-slate-800 bg-slate-950 p-4 min-h-[300px] flex flex-col justify-between shadow-inner">
              {formChannel === 'EMAIL' && (
                <div className="space-y-3">
                  <div className="border-b border-slate-800/80 pb-2 text-xs space-y-1 text-slate-400">
                    <p><span className="text-slate-600 font-mono">From:</span> no-reply@ecommerce-platform.vn</p>
                    <p><span className="text-slate-600 font-mono">To:</span> {SAMPLE_PREVIEW_VARS.customer_name} &lt;customer@example.com&gt;</p>
                    <p className="font-semibold text-white pt-1">
                      <span className="text-slate-600 font-mono font-normal">Subject:</span>{' '}
                      {renderTemplatePreview(formTitle || '(Chưa có tiêu đề)', SAMPLE_PREVIEW_VARS)}
                    </p>
                  </div>
                  <div className="text-xs text-slate-200 whitespace-pre-wrap leading-relaxed bg-slate-900/60 p-3 rounded-lg border border-slate-800/60">
                    {renderTemplatePreview(formContent || '(Chưa có nội dung)', SAMPLE_PREVIEW_VARS)}
                  </div>
                </div>
              )}

              {formChannel === 'SMS' && (
                <div className="space-y-2 max-w-sm mx-auto">
                  <div className="text-[10px] text-center text-slate-500 font-mono">Tin nhắn SMS viễn thông</div>
                  <div className="p-3.5 rounded-2xl rounded-tl-sm bg-emerald-950/40 border border-emerald-500/30 text-emerald-200 text-xs font-mono whitespace-pre-wrap leading-relaxed shadow-sm">
                    {renderTemplatePreview(formContent || '(Chưa có nội dung)', SAMPLE_PREVIEW_VARS)}
                  </div>
                  <div className="text-[10px] text-right text-slate-600 font-mono">
                    Độ dài: {renderTemplatePreview(formContent, SAMPLE_PREVIEW_VARS).length} ký tự
                  </div>
                </div>
              )}

              {formChannel === 'PUSH' && (
                <div className="space-y-2 max-w-sm mx-auto">
                  <div className="text-[10px] text-center text-slate-500 font-mono">Thông báo ứng dụng di động</div>
                  <div className="p-3.5 rounded-xl bg-slate-900 border border-slate-700/80 shadow-lg space-y-1">
                    <div className="flex items-center gap-1.5 text-xs text-indigo-400 font-bold">
                      <Bell className="h-3.5 w-3.5" />
                      <span>E-Commerce App</span>
                      <span className="text-[10px] text-slate-500 font-normal ml-auto">Vừa xong</span>
                    </div>
                    <p className="text-xs font-bold text-white">
                      {renderTemplatePreview(formTitle || 'Tiêu đề thông báo', SAMPLE_PREVIEW_VARS)}
                    </p>
                    <p className="text-[11px] text-slate-300 leading-snug whitespace-pre-wrap">
                      {renderTemplatePreview(formContent || 'Nội dung thông báo...', SAMPLE_PREVIEW_VARS)}
                    </p>
                  </div>
                </div>
              )}

              <div className="pt-3 border-t border-slate-800/60 text-[10px] text-slate-500 font-mono flex items-center justify-between">
                <span>Dữ liệu biến giả lập:</span>
                <span className="text-slate-400">customer_name, order_code, total_amount, tracking_number</span>
              </div>
            </div>
          </div>
        </div>
      </Modal>
    </div>
  );
};
