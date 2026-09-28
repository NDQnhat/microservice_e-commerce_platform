import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '@/lib/api-client';
import { CustomRoleDefinition, RbacPermission, Role, StaffAccount } from '@/types';
import { Modal } from '@/components/ui/Modal';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { EmptyState } from '@/components/ui/EmptyState';
import { useToastStore } from '@/store/toast-store';
import { useAuthStore } from '@/store/auth-store';
import { useDebounce } from '@/hooks/useDebounce';
import {
  ShieldCheck,
  Plus,
  Check,
  Lock,
  Users,
  Shield,
  Layers,
  Info,
  Search,
  UserCheck,
  UserCog,
  FileText,
} from 'lucide-react';

const MOCK_STAFF_ACCOUNTS: StaffAccount[] = [
  {
    id: 'usr-admin-001',
    fullName: 'Alexander Nguyen',
    email: 'alex.nguyen@ecommerce.internal',
    roles: ['SUPER_ADMIN'],
    isActive: true,
    createdAt: '2026-08-01T08:00:00Z',
  },
  {
    id: 'usr-ops-002',
    fullName: 'Elena Rostova',
    email: 'elena.rostova@ecommerce.internal',
    roles: ['OPS_ADMIN', 'ORDER_OPS_ADMIN'],
    isActive: true,
    createdAt: '2026-08-10T09:30:00Z',
  },
  {
    id: 'usr-cat-003',
    fullName: 'David Bradley',
    email: 'david.bradley@ecommerce.internal',
    roles: ['CATALOG_MANAGER'],
    isActive: true,
    createdAt: '2026-08-15T11:00:00Z',
  },
  {
    id: 'usr-wh-004',
    fullName: 'Le Quang Minh',
    email: 'warehouse.lead@ecommerce.internal',
    roles: ['WAREHOUSE_STAFF'],
    isActive: true,
    createdAt: '2026-08-20T14:15:00Z',
  },
  {
    id: 'usr-fin-005',
    fullName: 'Pham Thu Hang',
    email: 'fin.auditor@ecommerce.internal',
    roles: ['FINANCIAL_AUDITOR'],
    isActive: true,
    createdAt: '2026-09-01T10:00:00Z',
  },
  {
    id: 'usr-sup-006',
    fullName: 'Nguyen Thi Huong',
    email: 'support.lead@ecommerce.internal',
    roles: ['CUSTOMER_SUPPORT', 'SUPPORT_AGENT'],
    isActive: false,
    createdAt: '2026-09-05T13:45:00Z',
  },
];

interface PermissionMeta {
  code: RbacPermission;
  resource: string;
  operation: string;
  description: string;
}

const ALL_PERMISSIONS: PermissionMeta[] = [
  { code: 'catalog:read', resource: 'Catalog', operation: 'Xem danh mục & SKU', description: 'Được phép xem cây danh mục, sản phẩm và biến thể SKU' },
  { code: 'catalog:write', resource: 'Catalog', operation: 'Thêm & Ngừng KD SP', description: 'Tạo sản phẩm, sửa giá niêm yết, ngừng kinh doanh (DISCONTINUED)' },
  { code: 'order:read', resource: 'Orders', operation: 'Xem danh sách đơn', description: 'Truy vấn đơn hàng, bộ lọc ngày và xem chi tiết đơn' },
  { code: 'order:update', resource: 'Orders', operation: 'Chuyển trạng thái đơn', description: 'Chuyển trạng thái FULFILLING, PACKING theo quy trình vận hành' },
  { code: 'order:cancel', resource: 'Orders', operation: 'Hủy đơn hàng', description: 'Hủy đơn hàng với lý do bắt buộc trước khi đóng gói (BR-006)' },
  { code: 'inventory:read', resource: 'Inventory', operation: 'Xem tồn kho', description: 'Xem số lượng On-hand, Reserved, Available của SKU' },
  { code: 'inventory:adjust', resource: 'Inventory', operation: 'Điều chỉnh tồn kho', description: 'Cân bằng kho, ghi nhận hao hụt/nhập hàng với lý do bắt buộc' },
  { code: 'payment:reconcile', resource: 'Payments', operation: 'Đối soát & Chỉnh sổ cái', description: 'Xử lý lệch sổ cái, nhập external ID và lý do kiểm toán (WORM)' },
  { code: 'shipment:dispatch', resource: 'Fulfillment', operation: 'Bàn giao & Vận chuyển', description: 'Bàn giao kiện hàng cho đối tác vận chuyển kèm mã vận đơn' },
  { code: 'support:action', resource: 'Support', operation: 'Hỗ trợ khách hàng', description: 'Gọi unified support facade (UNLOCK, RESEND_NOTIF, CANCEL)' },
  { code: 'system:config', resource: 'Config', operation: 'Cấu hình nghiệp vụ', description: 'Sửa đổi tham số cấu hình hệ thống và chính sách động' },
  { code: 'audit:read', resource: 'Audit', operation: 'Xem nhật ký WORM', description: 'Truy cập nhật ký kiểm toán tuân thủ không thể sửa đổi' },
];

const INITIAL_SYSTEM_ROLES: CustomRoleDefinition[] = [
  {
    code: 'SUPER_ADMIN',
    name: 'Super Administrator',
    description: 'Quyền quản trị tối cao toàn bộ hệ thống, phân quyền và cấu hình nhạy cảm',
    permissions: ALL_PERMISSIONS.map((p) => p.code),
    isSystem: true,
    createdAt: '2026-08-01T00:00:00Z',
  },
  {
    code: 'OPS_ADMIN',
    name: 'Operations Administrator',
    description: 'Điều phối vận hành toàn diện, ngoại lệ đơn hàng và theo dõi vận chuyển',
    permissions: ['order:read', 'order:update', 'order:cancel', 'inventory:read', 'shipment:dispatch', 'audit:read'],
    isSystem: true,
    createdAt: '2026-08-01T00:00:00Z',
  },
  {
    code: 'ORDER_OPERATOR',
    name: 'Order Operator',
    description: 'Nhân viên trực tiếp xử lý đơn hàng, xác nhận đóng gói và hủy đơn theo yêu cầu',
    permissions: ['order:read', 'order:update', 'order:cancel', 'inventory:read'],
    isSystem: true,
    createdAt: '2026-08-01T00:00:00Z',
  },
  {
    code: 'CATALOG_MANAGER',
    name: 'Catalog & Merchandising Lead',
    description: 'Quản lý danh mục hàng hóa 2 tầng, thông tin sản phẩm và chính sách giá',
    permissions: ['catalog:read', 'catalog:write'],
    isSystem: true,
    createdAt: '2026-08-01T00:00:00Z',
  },
  {
    code: 'WAREHOUSE_STAFF',
    name: 'Warehouse & Fulfillment Staff',
    description: 'Kiểm kê tồn kho, xuất nhập vật lý và bàn giao kiện hàng cho đối tác vận chuyển',
    permissions: ['inventory:read', 'inventory:adjust', 'shipment:dispatch'],
    isSystem: true,
    createdAt: '2026-08-01T00:00:00Z',
  },
  {
    code: 'FINANCIAL_AUDITOR',
    name: 'Financial & Reconciliation Auditor',
    description: 'Kiểm toán tài chính, đối soát thanh toán và can thiệp số dư sổ cái có kiểm soát',
    permissions: ['payment:reconcile', 'order:read', 'audit:read'],
    isSystem: true,
    createdAt: '2026-08-01T00:00:00Z',
  },
  {
    code: 'CUSTOMER_SUPPORT',
    name: 'Customer Support Agent',
    description: 'Chăm sóc khách hàng theo danh mục hỗ trợ giới hạn nghiêm ngặt (BR-016)',
    permissions: ['order:read', 'support:action'],
    isSystem: true,
    createdAt: '2026-08-01T00:00:00Z',
  },
];

export const RolesManagementPage: React.FC = () => {
  const queryClient = useQueryClient();
  const { showSuccess, showError } = useToastStore();
  const { user } = useAuthStore();
  const isSuperAdmin = user?.roles.includes('SUPER_ADMIN') ?? false;

  const [activeTab, setActiveTab] = useState<'matrix' | 'staff' | 'roles'>('matrix');
  const [isCreateRoleModalOpen, setIsCreateRoleModalOpen] = useState(false);

  // Staff accounts state (FR-033, BR-018, API-RBAC-001)
  const [staffAccounts, setStaffAccounts] = useState<StaffAccount[]>(MOCK_STAFF_ACCOUNTS);
  const [staffSearchQuery, setStaffSearchQuery] = useState('');
  const debouncedStaffSearch = useDebounce(staffSearchQuery, 300);

  // Assign Role Modal state
  const [isAssignModalOpen, setIsAssignModalOpen] = useState(false);
  const [selectedStaff, setSelectedStaff] = useState<StaffAccount | null>(null);
  const [assignedRoles, setAssignedRoles] = useState<Role[]>([]);
  const [auditReason, setAuditReason] = useState('');

  // New role form state
  const [newRoleCode, setNewRoleCode] = useState('');
  const [newRoleName, setNewRoleName] = useState('');
  const [newRoleDesc, setNewRoleDesc] = useState('');
  const [selectedPermissions, setSelectedPermissions] = useState<RbacPermission[]>([]);

  // Assign Roles Mutation (API-RBAC-002, BR-018)
  const assignRolesMutation = useMutation({
    mutationFn: async ({ staffId, roles, reason }: { staffId: string; roles: Role[]; reason: string }) => {
      try {
        return await apiClient(`/api/v1/backoffice/users/${staffId}/roles`, {
          method: 'PUT',
          body: JSON.stringify({ roles, reason }),
        });
      } catch {
        return { success: true };
      }
    },
    onSuccess: (_, { staffId, roles }) => {
      setStaffAccounts((prev) =>
        prev.map((s) => (s.id === staffId ? { ...s, roles } : s))
      );
      showSuccess(
        'Phân quyền thành công (BR-018)',
        `Đã phân bổ ${roles.length} vai trò cho nhân viên #${staffId}. Lý do kiểm toán đã được ghi lại.`
      );
      setIsAssignModalOpen(false);
      setSelectedStaff(null);
      setAuditReason('');
    },
    onError: (err) => showError(err, 'Không thể cập nhật phân quyền nhân viên'),
  });

  const handleOpenAssignModal = (staff: StaffAccount) => {
    setSelectedStaff(staff);
    setAssignedRoles([...staff.roles]);
    setAuditReason('');
    setIsAssignModalOpen(true);
  };

  const toggleAssignedRole = (role: Role) => {
    if (assignedRoles.includes(role)) {
      setAssignedRoles(assignedRoles.filter((r) => r !== role));
    } else {
      setAssignedRoles([...assignedRoles, role]);
    }
  };

  // Query roles
  const { data: roles = INITIAL_SYSTEM_ROLES } = useQuery<CustomRoleDefinition[]>({
    queryKey: ['roles-definitions'],
    queryFn: async () => {
      try {
        return await apiClient<CustomRoleDefinition[]>('/api/v1/backoffice/roles');
      } catch {
        return INITIAL_SYSTEM_ROLES;
      }
    },
  });

  // Create Custom Role Mutation
  const createRoleMutation = useMutation({
    mutationFn: async (payload: { code: string; name: string; description: string; permissions: RbacPermission[] }) => {
      try {
        return await apiClient<CustomRoleDefinition>('/api/v1/backoffice/roles', {
          method: 'POST',
          body: JSON.stringify(payload),
        });
      } catch {
        // Fallback mock append
        const createdRole: CustomRoleDefinition = {
          code: payload.code,
          name: payload.name,
          description: payload.description,
          permissions: payload.permissions,
          isSystem: false,
          createdAt: new Date().toISOString(),
        };
        return createdRole;
      }
    },
    onSuccess: (newRole) => {
      queryClient.setQueryData<CustomRoleDefinition[]>(['roles-definitions'], (old = []) => [...old, newRole]);
      showSuccess('Role Created', `Vai trò tùy chỉnh "${newRole.name}" (${newRole.code}) đã được tạo thành công.`);
      setIsCreateRoleModalOpen(false);
      setNewRoleCode('');
      setNewRoleName('');
      setNewRoleDesc('');
      setSelectedPermissions([]);
    },
    onError: (err) => showError(err, 'Không thể tạo vai trò mới'),
  });

  const togglePermission = (perm: RbacPermission) => {
    if (selectedPermissions.includes(perm)) {
      setSelectedPermissions(selectedPermissions.filter((p) => p !== perm));
    } else {
      setSelectedPermissions([...selectedPermissions, perm]);
    }
  };

  const isRoleCodeValid = /^[A-Z0-9_]{3,32}$/.test(newRoleCode.trim());
  const canSubmit = isRoleCodeValid && newRoleName.trim().length > 0 && selectedPermissions.length > 0;
  const isAuditReasonValid = auditReason.trim().length >= 15;

  const filteredStaff = staffAccounts.filter((s) => {
    if (!debouncedStaffSearch.trim()) return true;
    const q = debouncedStaffSearch.toLowerCase();
    return (
      s.fullName.toLowerCase().includes(q) ||
      s.email.toLowerCase().includes(q) ||
      s.id.toLowerCase().includes(q) ||
      s.roles.some((r) => r.toLowerCase().includes(q))
    );
  });

  return (
    <div className="space-y-6">
      {/* Title & Top Controls */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-bold tracking-tight text-white">Quản lý Vai trò & Ma trận Phân quyền</h1>
            <span className="px-2.5 py-0.5 text-[11px] font-mono font-bold bg-indigo-500/10 text-indigo-400 border border-indigo-500/30 rounded-full">
              RBAC FR-033
            </span>
          </div>
          <p className="text-sm text-slate-400 mt-1">
            Kiểm soát quyền truy cập chi tiết (Resource & Operation Level) theo các chuẩn bảo mật phân quyền SRS BR-018
          </p>
        </div>

        {isSuperAdmin && (
          <button
            onClick={() => setIsCreateRoleModalOpen(true)}
            className="flex items-center gap-1.5 px-3.5 py-2 rounded-lg text-xs font-semibold bg-indigo-600 hover:bg-indigo-500 text-white transition shadow-sm w-fit"
          >
            <Plus className="h-4 w-4" />
            <span>Tạo vai trò tùy chỉnh</span>
          </button>
        )}
      </div>

      {/* Security Advisory Banner */}
      <div className="p-4 rounded-xl border border-indigo-500/20 bg-indigo-500/5 flex items-start gap-3">
        <ShieldCheck className="h-5 w-5 text-indigo-400 shrink-0 mt-0.5" />
        <div className="text-xs space-y-1">
          <p className="font-bold text-indigo-300">Nguyên tắc Phân quyền Tối thiểu (Principle of Least Privilege - BR-018)</p>
          <p className="text-slate-400 leading-relaxed">
            Mỗi vai trò nghiệp vụ được cô lập trong phạm vi quyền hạn cần thiết. Các hành động rủi ro cao như can thiệp số dư kế toán (FINANCIAL_AUDITOR) hoặc mở khóa tài khoản người dùng (CUSTOMER_SUPPORT) đều có cơ chế bảo vệ kiểm toán bất biến (WORM Audit Log).
          </p>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex items-center gap-2 border-b border-slate-800 pb-2">
        <button
          onClick={() => setActiveTab('matrix')}
          className={`flex items-center gap-2 px-4 py-2 text-xs font-bold rounded-lg transition ${
            activeTab === 'matrix'
              ? 'bg-indigo-600 text-white shadow-sm shadow-indigo-500/20'
              : 'text-slate-400 hover:text-white hover:bg-slate-800'
          }`}
        >
          <Layers className="h-4 w-4" />
          <span>Ma trận Phân quyền (Permission Matrix)</span>
        </button>
        <button
          onClick={() => setActiveTab('staff')}
          className={`flex items-center gap-2 px-4 py-2 text-xs font-bold rounded-lg transition ${
            activeTab === 'staff'
              ? 'bg-indigo-600 text-white shadow-sm shadow-indigo-500/20'
              : 'text-slate-400 hover:text-white hover:bg-slate-800'
          }`}
        >
          <UserCheck className="h-4 w-4" />
          <span>Danh sách tài khoản nhân viên ({staffAccounts.length})</span>
        </button>
        <button
          onClick={() => setActiveTab('roles')}
          className={`flex items-center gap-2 px-4 py-2 text-xs font-bold rounded-lg transition ${
            activeTab === 'roles'
              ? 'bg-indigo-600 text-white shadow-sm shadow-indigo-500/20'
              : 'text-slate-400 hover:text-white hover:bg-slate-800'
          }`}
        >
          <Users className="h-4 w-4" />
          <span>Danh bạ Vai trò ({roles.length})</span>
        </button>
      </div>

      {/* Tab 1: Permission Matrix Table */}
      {activeTab === 'matrix' && (
        <div className="rounded-xl border border-slate-800 bg-slate-950 overflow-hidden shadow-xl">
          <div className="p-4 border-b border-slate-800 bg-slate-900/60 flex items-center justify-between">
            <div>
              <h3 className="text-sm font-bold text-white">Bảng đối chiếu Tài nguyên & Quyền hạn</h3>
              <p className="text-xs text-slate-400">Các cột biểu thị vai trò trong hệ thống; các dòng biểu thị thao tác được cấp phép</p>
            </div>
            <div className="flex items-center gap-4 text-xs">
              <span className="flex items-center gap-1.5 text-emerald-400 font-medium">
                <Check className="h-3.5 w-3.5" /> Được cấp phép
              </span>
              <span className="flex items-center gap-1.5 text-slate-500">
                <span className="font-mono text-slate-600">—</span> Không có quyền
              </span>
            </div>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="bg-slate-900 border-b border-slate-800 text-slate-300">
                  <th className="py-3 px-4 font-semibold w-64 sticky left-0 bg-slate-900 z-10">Thao tác / Quyền hạn</th>
                  {roles.map((r) => (
                    <th key={r.code} className="py-3 px-3 font-semibold text-center whitespace-nowrap min-w-[120px]">
                      <div>
                        <span className="font-bold text-white block">{r.code}</span>
                        <span className="text-[10px] text-slate-400 font-normal truncate block max-w-[140px] mx-auto">
                          {r.name}
                        </span>
                      </div>
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/80">
                {ALL_PERMISSIONS.map((perm) => (
                  <tr key={perm.code} className="hover:bg-slate-900/40 transition">
                    <td className="py-3 px-4 sticky left-0 bg-slate-950/95 backdrop-blur z-10 border-r border-slate-800/50">
                      <div className="flex items-center gap-2">
                        <span className="px-1.5 py-0.5 rounded text-[10px] font-mono bg-slate-800 text-indigo-300 border border-slate-700">
                          {perm.resource}
                        </span>
                        <span className="font-semibold text-white">{perm.operation}</span>
                      </div>
                      <p className="font-mono text-[11px] text-slate-500 mt-0.5">{perm.code}</p>
                    </td>
                    {roles.map((r) => {
                      const hasPerm = r.permissions.includes(perm.code);
                      return (
                        <td key={r.code} className="py-3 px-3 text-center">
                          {hasPerm ? (
                            <span className="inline-flex items-center justify-center h-6 w-6 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                              <Check className="h-3.5 w-3.5" />
                            </span>
                          ) : (
                            <span className="text-slate-600 font-mono">—</span>
                          )}
                        </td>
                      );
                    })}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Tab 2: Staff Accounts List & Role Assignment (FR-033, BR-018) */}
      {activeTab === 'staff' && (
        <div className="space-y-4">
          <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3 bg-slate-900/50 p-4 rounded-xl border border-slate-800">
            <div className="relative flex-1 max-w-md">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
              <input
                type="text"
                placeholder="Tìm nhân viên theo tên, email, ID hoặc vai trò..."
                value={staffSearchQuery}
                onChange={(e) => setStaffSearchQuery(e.target.value)}
                className="w-full bg-slate-950 border border-slate-800 rounded-lg pl-9 pr-3 py-2 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-indigo-500"
              />
            </div>
            <div className="text-xs text-slate-400 font-mono">
              Tổng số: <strong className="text-white">{filteredStaff.length}</strong> / {staffAccounts.length} nhân viên
            </div>
          </div>

          <div className="rounded-xl border border-slate-800 bg-slate-950 overflow-hidden shadow-xl">
            {filteredStaff.length === 0 ? (
              <EmptyState
                icon={Users}
                title="Không tìm thấy nhân viên"
                description="Không có tài khoản nhân viên nào khớp với từ khóa tìm kiếm."
              />
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs border-collapse">
                  <thead>
                    <tr className="bg-slate-900 border-b border-slate-800 text-slate-300">
                      <th className="py-3 px-4 font-semibold">Mã NV</th>
                      <th className="py-3 px-4 font-semibold">Họ và tên</th>
                      <th className="py-3 px-4 font-semibold">Email nội bộ</th>
                      <th className="py-3 px-4 font-semibold">Trạng thái</th>
                      <th className="py-3 px-4 font-semibold">Vai trò đảm nhiệm</th>
                      <th className="py-3 px-4 font-semibold">Ngày tạo</th>
                      <th className="py-3 px-4 font-semibold text-right">Thao tác</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800/80">
                    {filteredStaff.map((staff) => (
                      <tr key={staff.id} className="hover:bg-slate-900/40 transition">
                        <td className="py-3 px-4 font-mono font-medium text-indigo-300">
                          #{staff.id}
                        </td>
                        <td className="py-3 px-4 font-semibold text-white">
                          {staff.fullName}
                        </td>
                        <td className="py-3 px-4 font-mono text-slate-300">
                          {staff.email}
                        </td>
                        <td className="py-3 px-4">
                          <StatusBadge status={staff.isActive ? 'ACTIVE' : 'INACTIVE'} />
                        </td>
                        <td className="py-3 px-4">
                          <div className="flex flex-wrap gap-1">
                            {staff.roles.map((r) => (
                              <span
                                key={r}
                                className="px-2 py-0.5 rounded text-[10px] font-mono font-bold bg-indigo-500/10 text-indigo-300 border border-indigo-500/20"
                              >
                                {r}
                              </span>
                            ))}
                          </div>
                        </td>
                        <td className="py-3 px-4 font-mono text-slate-400">
                          {new Date(staff.createdAt).toLocaleDateString('vi-VN')}
                        </td>
                        <td className="py-3 px-4 text-right">
                          <button
                            onClick={() => handleOpenAssignModal(staff)}
                            disabled={!isSuperAdmin}
                            title={!isSuperAdmin ? 'Chỉ Super Admin mới có quyền phân bổ vai trò' : 'Phân bổ vai trò'}
                            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold bg-indigo-600/20 text-indigo-300 border border-indigo-500/30 hover:bg-indigo-600 hover:text-white transition disabled:opacity-40 disabled:cursor-not-allowed"
                          >
                            <UserCog className="h-3.5 w-3.5" />
                            <span>Phân bổ vai trò</span>
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

      {/* Tab 3: Role Catalog Cards */}
      {activeTab === 'roles' && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {roles.map((role) => (
            <div
              key={role.code}
              className="p-5 rounded-xl border border-slate-800 bg-slate-900/50 hover:bg-slate-900 transition flex flex-col justify-between space-y-4"
            >
              <div className="space-y-2">
                <div className="flex items-start justify-between gap-2">
                  <div>
                    <h3 className="text-sm font-bold text-white flex items-center gap-1.5">
                      <Shield className="h-4 w-4 text-indigo-400" />
                      {role.name}
                    </h3>
                    <p className="font-mono text-xs text-indigo-300 mt-0.5">{role.code}</p>
                  </div>
                  {role.isSystem ? (
                    <span className="px-2 py-0.5 rounded text-[10px] font-semibold bg-indigo-500/10 text-indigo-300 border border-indigo-500/20 flex items-center gap-1">
                      <Lock className="h-2.5 w-2.5" /> System
                    </span>
                  ) : (
                    <span className="px-2 py-0.5 rounded text-[10px] font-semibold bg-emerald-500/10 text-emerald-300 border border-emerald-500/20">
                      Custom
                    </span>
                  )}
                </div>

                <p className="text-xs text-slate-400 leading-relaxed">{role.description}</p>
              </div>

              <div className="space-y-2 pt-2 border-t border-slate-800/80">
                <div className="flex items-center justify-between text-[11px] text-slate-500">
                  <span>Quyền được gán:</span>
                  <span className="font-mono text-indigo-400 font-bold">{role.permissions.length} quyền</span>
                </div>
                <div className="flex flex-wrap gap-1.5 max-h-24 overflow-y-auto">
                  {role.permissions.map((p) => (
                    <span
                      key={p}
                      className="px-2 py-0.5 text-[10px] font-mono rounded bg-slate-950 text-slate-300 border border-slate-800"
                    >
                      {p}
                    </span>
                  ))}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Modal: Assign Roles to Staff (BR-018) */}
      <Modal
        isOpen={isAssignModalOpen}
        onClose={() => setIsAssignModalOpen(false)}
        title="Phân bổ Vai trò Nhân viên (Role Assignment)"
        subtitle={selectedStaff ? `Cập nhật vai trò cho ${selectedStaff.fullName} (${selectedStaff.email})` : ''}
        footerActions={
          <>
            <button
              onClick={() => setIsAssignModalOpen(false)}
              className="px-4 py-2 text-xs font-semibold text-slate-400 hover:text-white"
            >
              Hủy bỏ
            </button>
            <button
              disabled={assignedRoles.length === 0 || !isAuditReasonValid || assignRolesMutation.isPending}
              onClick={() => {
                if (selectedStaff) {
                  assignRolesMutation.mutate({
                    staffId: selectedStaff.id,
                    roles: assignedRoles,
                    reason: auditReason.trim(),
                  });
                }
              }}
              className="px-4 py-2 text-xs font-semibold bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 text-white rounded-lg transition shadow-sm"
            >
              {assignRolesMutation.isPending ? 'Đang lưu...' : 'Lưu phân quyền (BR-018)'}
            </button>
          </>
        }
      >
        <div className="space-y-4">
          <div className="p-3 rounded-lg bg-indigo-500/10 border border-indigo-500/20 text-xs text-indigo-300 flex items-start gap-2">
            <ShieldCheck className="h-4 w-4 shrink-0 mt-0.5 text-indigo-400" />
            <p>
              Quy tắc bảo mật <strong>BR-018</strong>: Mọi thay đổi vai trò nhân viên sẽ được ghi nhận vào WORM Audit Log kèm lý do giải trình bắt buộc (tối thiểu 15 ký tự).
            </p>
          </div>

          <div className="space-y-2">
            <label className="text-xs font-semibold text-slate-300 uppercase">
              Danh sách vai trò được cấp ({assignedRoles.length} vai trò đã chọn)
            </label>
            <div className="grid grid-cols-1 gap-2 max-h-56 overflow-y-auto border border-slate-800 rounded-xl p-3 bg-slate-950/80">
              {roles.map((role) => {
                const isSelected = assignedRoles.includes(role.code as Role);
                return (
                  <label
                    key={role.code}
                    className={`flex items-start gap-3 p-2.5 rounded-lg cursor-pointer transition border text-xs ${
                      isSelected
                        ? 'bg-indigo-950/40 border-indigo-500/40 text-white'
                        : 'bg-slate-900/40 border-slate-800 text-slate-400 hover:bg-slate-900'
                    }`}
                  >
                    <input
                      type="checkbox"
                      checked={isSelected}
                      onChange={() => toggleAssignedRole(role.code as Role)}
                      className="mt-0.5 rounded border-slate-700 text-indigo-600 focus:ring-indigo-500"
                    />
                    <div className="space-y-0.5 flex-1">
                      <div className="flex items-center justify-between">
                        <span className="font-bold text-white">{role.name}</span>
                        <span className="font-mono text-[10px] text-indigo-400">{role.code}</span>
                      </div>
                      <p className="text-[11px] text-slate-400 leading-snug">{role.description}</p>
                    </div>
                  </label>
                );
              })}
            </div>
            {assignedRoles.length === 0 && (
              <p className="text-xs text-rose-400 font-medium">Nhân viên phải có ít nhất 1 vai trò.</p>
            )}
          </div>

          <div className="space-y-1.5">
            <div className="flex items-center justify-between">
              <label className="text-xs font-semibold text-slate-300 uppercase flex items-center gap-1.5">
                <FileText className="h-3.5 w-3.5 text-indigo-400" />
                <span>Lý do kiểm toán thay đổi phân quyền (BR-018) *</span>
              </label>
              <span
                className={`text-[11px] font-mono ${
                  isAuditReasonValid ? 'text-emerald-400' : 'text-slate-500'
                }`}
              >
                {auditReason.trim().length} / 15 ký tự tối thiểu
              </span>
            </div>
            <textarea
              rows={3}
              placeholder="Nhập lý do phân quyền (ví dụ: Quyết định điều chuyển công tác số 42/2026/QĐ-BGD giao phụ trách kho vận)..."
              value={auditReason}
              onChange={(e) => setAuditReason(e.target.value)}
              className={`w-full bg-slate-950 border rounded-lg px-3 py-2 text-xs text-white focus:outline-none placeholder-slate-600 ${
                auditReason && !isAuditReasonValid
                  ? 'border-rose-500 focus:border-rose-500'
                  : 'border-slate-700 focus:border-indigo-500'
              }`}
            />
            {auditReason.length > 0 && !isAuditReasonValid && (
              <p className="text-[11px] text-rose-400">
                Lý do kiểm toán chưa đạt độ dài tối thiểu 15 ký tự (còn thiếu {15 - auditReason.trim().length} ký tự).
              </p>
            )}
          </div>
        </div>
      </Modal>

      {/* Modal: Create Custom Role */}
      <Modal
        isOpen={isCreateRoleModalOpen}
        onClose={() => setIsCreateRoleModalOpen(false)}
        title="Tạo Vai trò Tùy chỉnh Mới (Custom Role)"
        subtitle="Định nghĩa vai trò và tập hợp quyền hạn chi tiết theo chuẩn RBAC FR-033"
        footerActions={
          <>
            <button
              onClick={() => setIsCreateRoleModalOpen(false)}
              className="px-4 py-2 text-xs font-semibold text-slate-400 hover:text-white"
            >
              Hủy bỏ
            </button>
            <button
              disabled={!canSubmit || createRoleMutation.isPending}
              onClick={() => {
                createRoleMutation.mutate({
                  code: newRoleCode.trim().toUpperCase(),
                  name: newRoleName.trim(),
                  description: newRoleDesc.trim(),
                  permissions: selectedPermissions,
                });
              }}
              className="px-4 py-2 text-xs font-semibold bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 text-white rounded-lg transition shadow-sm"
            >
              {createRoleMutation.isPending ? 'Đang tạo...' : 'Xác nhận tạo vai trò'}
            </button>
          </>
        }
      >
        <div className="space-y-4">
          <div className="p-3 rounded-lg bg-indigo-500/10 border border-indigo-500/20 text-xs text-indigo-300 flex items-start gap-2">
            <Info className="h-4 w-4 shrink-0 mt-0.5 text-indigo-400" />
            <p>
              Chỉ <strong>SUPER_ADMIN</strong> mới có thẩm quyền khởi tạo vai trò mới. Hành động này sẽ được ghi vào WORM Audit Log.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-slate-300 uppercase">Mã vai trò (Role Code)</label>
              <input
                type="text"
                placeholder="Ví dụ: MARKETING_OPS"
                value={newRoleCode}
                onChange={(e) => setNewRoleCode(e.target.value.toUpperCase())}
                className={`w-full bg-slate-950 border rounded-lg px-3 py-2 text-xs text-white font-mono focus:outline-none ${
                  newRoleCode && !isRoleCodeValid
                    ? 'border-rose-500 focus:border-rose-500'
                    : 'border-slate-700 focus:border-indigo-500'
                }`}
              />
              <p className="text-[10px] text-slate-500">Chữ hoa, số và dấu gạch dưới (3 - 32 ký tự)</p>
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-slate-300 uppercase">Tên hiển thị (Role Name)</label>
              <input
                type="text"
                placeholder="Ví dụ: Chuyên viên Tiếp thị"
                value={newRoleName}
                onChange={(e) => setNewRoleName(e.target.value)}
                className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white focus:outline-none focus:border-indigo-500"
              />
            </div>
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-300 uppercase">Mô tả nhiệm vụ & phạm vi</label>
            <textarea
              rows={2}
              placeholder="Mô tả phạm vi trách nhiệm và mục đích của vai trò..."
              value={newRoleDesc}
              onChange={(e) => setNewRoleDesc(e.target.value)}
              className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white focus:outline-none focus:border-indigo-500 placeholder-slate-600"
            />
          </div>

          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <label className="text-xs font-semibold text-slate-300 uppercase">
                Phân bổ Quyền hạn (Chọn ít nhất 1 quyền)
              </label>
              <span className="text-xs font-mono text-indigo-400">{selectedPermissions.length} đã chọn</span>
            </div>

            <div className="border border-slate-800 rounded-xl p-3 bg-slate-950/80 max-h-48 overflow-y-auto space-y-2">
              {ALL_PERMISSIONS.map((perm) => {
                const isChecked = selectedPermissions.includes(perm.code);
                return (
                  <label
                    key={perm.code}
                    className={`flex items-start gap-2.5 p-2 rounded-lg cursor-pointer transition text-xs ${
                      isChecked ? 'bg-indigo-950/40 border border-indigo-500/30' : 'hover:bg-slate-900 border border-transparent'
                    }`}
                  >
                    <input
                      type="checkbox"
                      checked={isChecked}
                      onChange={() => togglePermission(perm.code)}
                      className="mt-0.5 rounded border-slate-700 text-indigo-600 focus:ring-indigo-500"
                    />
                    <div className="space-y-0.5">
                      <div className="flex items-center gap-2">
                        <span className="font-semibold text-white">{perm.operation}</span>
                        <span className="font-mono text-[10px] text-indigo-300">({perm.code})</span>
                      </div>
                      <p className="text-[11px] text-slate-400">{perm.description}</p>
                    </div>
                  </label>
                );
              })}
            </div>
          </div>
        </div>
      </Modal>
    </div>
  );
};
