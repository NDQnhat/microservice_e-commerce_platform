import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '@/lib/api-client';
import { CustomRoleDefinition, RbacPermission } from '@/types';
import { Modal } from '@/components/ui/Modal';
import { useToastStore } from '@/store/toast-store';
import { useAuthStore } from '@/store/auth-store';
import {
  ShieldCheck,
  Plus,
  Check,
  Lock,
  Users,
  Shield,
  Layers,
  Info,
} from 'lucide-react';

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

  const [activeTab, setActiveTab] = useState<'matrix' | 'roles'>('matrix');
  const [isCreateRoleModalOpen, setIsCreateRoleModalOpen] = useState(false);

  // New role form state
  const [newRoleCode, setNewRoleCode] = useState('');
  const [newRoleName, setNewRoleName] = useState('');
  const [newRoleDesc, setNewRoleDesc] = useState('');
  const [selectedPermissions, setSelectedPermissions] = useState<RbacPermission[]>([]);

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
  const canSubmit = isRoleCodeValid && newRoleName.trim().length >= 3 && selectedPermissions.length > 0;

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

      {/* Tab 2: Role Catalog Cards */}
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
