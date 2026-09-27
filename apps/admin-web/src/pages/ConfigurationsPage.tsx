import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '@/lib/api-client';
import { BusinessConfiguration } from '@/types';
import { SkeletonTable } from '@/components/ui/SkeletonTable';
import { EmptyState } from '@/components/ui/EmptyState';
import { SlideOverDrawer } from '@/components/ui/SlideOverDrawer';
import { Modal } from '@/components/ui/Modal';
import { useToastStore } from '@/store/toast-store';
import {
  Sliders,
  Search,
  Edit3,
  ShieldCheck,
  RefreshCw,
  Plus,
  Eye,
} from 'lucide-react';

const MOCK_CONFIGS: BusinessConfiguration[] = [
  {
    id: 'cfg-001',
    configKey: 'order.reservation.ttl.minutes',
    configValue: '15',
    description: 'Inventory reservation time-to-live before automatic expiration release (BR-004)',
    version: 1,
    isActive: true,
    effectiveFrom: '2026-09-01T00:00:00Z',
    createdBy: 'superadmin@ecommerce.internal',
    createdAt: '2026-09-01T00:00:00Z',
    updatedAt: '2026-09-01T00:00:00Z',
  },
  {
    id: 'cfg-002',
    configKey: 'payment.timeout.scan.minutes',
    configValue: '15',
    description: 'Cutoff threshold for scanning and transitioning hung payment transactions',
    version: 2,
    isActive: true,
    effectiveFrom: '2026-09-15T10:00:00Z',
    createdBy: 'superadmin@ecommerce.internal',
    createdAt: '2026-09-15T10:00:00Z',
    updatedAt: '2026-09-15T10:00:00Z',
  },
  {
    id: 'cfg-003',
    configKey: 'order.stuck.sla.minutes',
    configValue: '60',
    description: 'SLA threshold for flagging stuck unprogressed orders in telemetry',
    version: 1,
    isActive: true,
    effectiveFrom: '2026-09-01T00:00:00Z',
    createdBy: 'superadmin@ecommerce.internal',
    createdAt: '2026-09-01T00:00:00Z',
    updatedAt: '2026-09-01T00:00:00Z',
  },
  {
    id: 'cfg-004',
    configKey: 'shipping.free.minimum.amount',
    configValue: '500000',
    description: 'Cart grand total qualifying for 100% free nationwide express shipping (VND)',
    version: 3,
    isActive: true,
    effectiveFrom: '2026-09-20T08:00:00Z',
    createdBy: 'superadmin@ecommerce.internal',
    createdAt: '2026-09-20T08:00:00Z',
    updatedAt: '2026-09-20T08:00:00Z',
  },
];

export const ConfigurationsPage: React.FC = () => {
  const queryClient = useQueryClient();
  const { showSuccess, showError } = useToastStore();

  const [searchQuery, setSearchQuery] = useState('');
  const [selectedConfig, setSelectedConfig] = useState<BusinessConfiguration | null>(null);

  // Edit / Create Modals
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [editKey, setEditKey] = useState('');
  const [editValue, setEditValue] = useState('');
  const [editDesc, setEditDesc] = useState('');
  const [editReason, setEditReason] = useState('');
  const [targetVersion, setTargetVersion] = useState(1);

  // Fetch Configurations (API-CFG-001)
  const { data: configs = MOCK_CONFIGS, isLoading, refetch, isFetching } = useQuery<BusinessConfiguration[]>({
    queryKey: ['configurations'],
    queryFn: async () => {
      try {
        return await apiClient<BusinessConfiguration[]>('/api/v1/backoffice/configurations');
      } catch {
        return MOCK_CONFIGS;
      }
    },
  });

  // Version Bump Mutation (BR-019)
  const updateMutation = useMutation({
    mutationFn: async (payload: {
      configKey: string;
      configValue: string;
      description?: string;
      reason: string;
    }) => {
      return apiClient('/api/v1/configurations', {
        method: 'POST',
        body: JSON.stringify(payload),
      });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['configurations'] });
      showSuccess(
        'Configuration Version Bumped (BR-019)',
        `Key "${editKey}" incremented to v${targetVersion}. Previous versions preserved in immutable audit log.`
      );
      setIsEditModalOpen(false);
      setEditReason('');
    },
    onError: (err) => showError(err, 'Failed to update configuration (Reason mandatory per BR-019)'),
  });

  const handleOpenEdit = (cfg: BusinessConfiguration) => {
    setSelectedConfig(cfg);
    setEditKey(cfg.configKey);
    setEditValue(cfg.configValue);
    setEditDesc(cfg.description || '');
    setTargetVersion(cfg.version + 1);
    setEditReason('');
    setIsEditModalOpen(true);
  };

  const handleOpenCreate = () => {
    setSelectedConfig(null);
    setEditKey('');
    setEditValue('');
    setEditDesc('');
    setTargetVersion(1);
    setEditReason('Initial configuration creation');
    setIsEditModalOpen(true);
  };

  const filteredConfigs = configs.filter((c) => {
    if (!searchQuery.trim()) return true;
    const q = searchQuery.toLowerCase();
    return c.configKey.toLowerCase().includes(q) || (c.description && c.description.toLowerCase().includes(q));
  });

  return (
    <div className="space-y-6">
      {/* Title & Top Controls */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-white">Dynamic Business Configuration</h1>
          <p className="text-sm text-slate-400">
            Runtime system parameters with strict immutable version bumps and audit reasons (API-CFG-001, FR-037, BR-019)
          </p>
        </div>
        <div className="flex items-center gap-3">
          <button
            onClick={handleOpenCreate}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold bg-indigo-600 hover:bg-indigo-500 text-white transition shadow-sm"
          >
            <Plus className="h-4 w-4" />
            <span>Create Parameter</span>
          </button>
          <button
            onClick={() => refetch()}
            disabled={isFetching}
            className="flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs font-semibold bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 transition"
          >
            <RefreshCw className={`h-3.5 w-3.5 ${isFetching ? 'animate-spin' : ''}`} />
            <span>Sync Configs</span>
          </button>
        </div>
      </div>

      {/* Governance Notice */}
      <div className="p-4 rounded-xl border border-indigo-500/20 bg-indigo-500/5 flex items-start gap-3">
        <ShieldCheck className="h-5 w-5 text-indigo-400 shrink-0 mt-0.5" />
        <div className="text-xs space-y-1">
          <p className="font-bold text-indigo-300">Immutable Versioning Invariant (BR-019)</p>
          <p className="text-slate-400 leading-relaxed">
            In-place updates and destructive overwrites are disabled. Changing a configuration terminates the current version window and creates a new immutable version record (vN + 1) with mandatory operator audit justification.
          </p>
        </div>
      </div>

      {/* Search Bar */}
      <div className="p-4 rounded-xl border border-slate-800 bg-slate-900/90 backdrop-blur flex items-center justify-between">
        <div className="relative w-full md:w-80">
          <Search className="absolute left-3 top-2.5 h-4 w-4 text-slate-500" />
          <input
            type="text"
            placeholder="Search configuration keys or purpose..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full bg-slate-950 border border-slate-700/80 rounded-lg pl-9 pr-3 py-2 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-indigo-500"
          />
        </div>
        <span className="text-xs font-mono text-slate-400">
          Total Parameters: <strong className="text-white">{filteredConfigs.length}</strong>
        </span>
      </div>

      {/* Table */}
      {isLoading ? (
        <SkeletonTable rows={4} cols={5} />
      ) : filteredConfigs.length === 0 ? (
        <EmptyState
          icon={Sliders}
          title="No Configurations Found"
          description="No configuration matches the search criteria."
          actionLabel="Add Parameter"
          onAction={handleOpenCreate}
        />
      ) : (
        <div className="rounded-xl border border-slate-800 bg-slate-950 overflow-hidden shadow-xl">
          <table className="w-full text-left text-sm">
            <thead className="sticky top-0 bg-slate-900/95 backdrop-blur z-10 border-b border-slate-800 text-xs font-semibold uppercase tracking-wider text-slate-400">
              <tr>
                <th className="py-3 px-4">Configuration Key</th>
                <th className="py-3 px-4">Active Value</th>
                <th className="py-3 px-4">Description</th>
                <th className="py-3 px-4">Version</th>
                <th className="py-3 px-4">Effective Since</th>
                <th className="py-3 px-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/80">
              {filteredConfigs.map((cfg) => (
                <tr
                  key={cfg.id}
                  onClick={() => setSelectedConfig(cfg)}
                  className="hover:bg-slate-900/50 cursor-pointer transition group"
                >
                  <td className="py-3 px-4">
                    <span className="font-mono text-xs font-bold text-indigo-400">{cfg.configKey}</span>
                  </td>
                  <td className="py-3 px-4">
                    <span className="font-mono text-xs text-white bg-slate-900 px-2.5 py-1 rounded border border-slate-800">
                      {cfg.configValue}
                    </span>
                  </td>
                  <td className="py-3 px-4 text-xs text-slate-400 max-w-sm truncate">
                    {cfg.description || '-'}
                  </td>
                  <td className="py-3 px-4">
                    <span className="font-mono text-xs font-bold px-2 py-0.5 rounded bg-indigo-500/10 text-indigo-300 border border-indigo-500/20">
                      v{cfg.version}
                    </span>
                  </td>
                  <td className="py-3 px-4 text-xs text-slate-400">
                    {new Date(cfg.effectiveFrom).toLocaleDateString()}
                  </td>
                  <td
                    className="py-3 px-4 text-right space-x-2"
                    onClick={(e) => e.stopPropagation()}
                  >
                    <button
                      onClick={() => handleOpenEdit(cfg)}
                      className="px-2.5 py-1 text-xs font-semibold bg-slate-800 hover:bg-slate-700 text-white rounded border border-slate-700 transition inline-flex items-center gap-1.5"
                    >
                      <Edit3 className="h-3 w-3" />
                      <span>Edit (v{cfg.version + 1})</span>
                    </button>
                    <button
                      onClick={() => setSelectedConfig(cfg)}
                      className="p-1 text-slate-400 hover:text-white rounded transition"
                      title="Inspect Details"
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

      {/* Slide-over Drawer for Configuration Inspection */}
      <SlideOverDrawer
        isOpen={!!selectedConfig && !isEditModalOpen}
        onClose={() => setSelectedConfig(null)}
        title={selectedConfig?.configKey || 'Configuration'}
        subtitle={`Active Version: v${selectedConfig?.version} • Created by: ${selectedConfig?.createdBy}`}
        idToCopy={selectedConfig?.id}
        badge={
          selectedConfig && (
            <span className="font-mono text-xs px-2 py-0.5 rounded bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 font-bold">
              ACTIVE
            </span>
          )
        }
        footerActions={
          selectedConfig && (
            <button
              onClick={() => handleOpenEdit(selectedConfig)}
              className="px-4 py-2 text-xs font-semibold bg-indigo-600 hover:bg-indigo-500 text-white rounded-lg shadow-sm transition flex items-center gap-1.5"
            >
              <Edit3 className="h-4 w-4" />
              <span>Create Incremented Version (v{selectedConfig.version + 1})</span>
            </button>
          )
        }
      >
        {selectedConfig && (
          <div className="space-y-6">
            <div className="space-y-2">
              <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
                Current Operational Value
              </label>
              <div className="p-4 rounded-xl bg-slate-950 border border-slate-800 font-mono text-sm text-emerald-400 font-bold">
                {selectedConfig.configValue}
              </div>
            </div>

            <div className="space-y-2">
              <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
                Description & Purpose
              </label>
              <p className="text-xs text-slate-300 p-4 rounded-xl bg-slate-950 border border-slate-800 leading-relaxed">
                {selectedConfig.description || 'No description provided.'}
              </p>
            </div>

            <div className="p-4 rounded-xl bg-slate-950 border border-slate-800 space-y-2 text-xs font-mono">
              <div className="flex justify-between">
                <span className="text-slate-500">Effective From:</span>
                <span className="text-slate-300">{new Date(selectedConfig.effectiveFrom).toLocaleString()}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-500">Effective To:</span>
                <span className="text-slate-300">{selectedConfig.effectiveTo || 'CURRENT (Indefinite)'}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-500">Author:</span>
                <span className="text-slate-300">{selectedConfig.createdBy}</span>
              </div>
            </div>
          </div>
        )}
      </SlideOverDrawer>

      {/* Edit / Version Bump Modal (BR-019) */}
      <Modal
        isOpen={isEditModalOpen}
        onClose={() => setIsEditModalOpen(false)}
        title={selectedConfig ? `Bump Configuration to v${targetVersion}` : 'Create New Configuration'}
        subtitle="Mandatory business reason for configuration modification (BR-019)"
        footerActions={
          <>
            <button
              onClick={() => setIsEditModalOpen(false)}
              className="px-4 py-2 text-xs font-semibold text-slate-400 hover:text-white"
            >
              Cancel
            </button>
            <button
              disabled={updateMutation.isPending || !editValue.trim() || !editReason.trim()}
              onClick={() => {
                updateMutation.mutate({
                  configKey: editKey,
                  configValue: editValue,
                  description: editDesc,
                  reason: editReason,
                });
              }}
              className="px-4 py-2 text-xs font-semibold bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 text-white rounded-lg transition"
            >
              {updateMutation.isPending ? 'Committing...' : `Commit Version ${targetVersion}`}
            </button>
          </>
        }
      >
        <div className="space-y-4">
          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-300 uppercase">Configuration Key</label>
            <input
              type="text"
              value={editKey}
              onChange={(e) => setEditKey(e.target.value)}
              disabled={!!selectedConfig}
              className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white font-mono focus:outline-none focus:border-indigo-500 disabled:opacity-60"
              required
            />
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-300 uppercase">New Config Value</label>
            <textarea
              rows={2}
              value={editValue}
              onChange={(e) => setEditValue(e.target.value)}
              className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white font-mono focus:outline-none focus:border-indigo-500"
              required
            />
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-300 uppercase">Description</label>
            <input
              type="text"
              value={editDesc}
              onChange={(e) => setEditDesc(e.target.value)}
              className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white focus:outline-none focus:border-indigo-500"
            />
          </div>

          <div className="space-y-1.5">
            <div className="flex items-center justify-between">
              <label className="text-xs font-semibold text-slate-300 uppercase">
                Reason for Change (Mandatory per BR-019)
              </label>
              <span className="text-[11px] text-rose-400">* Required</span>
            </div>
            <input
              type="text"
              value={editReason}
              onChange={(e) => setEditReason(e.target.value)}
              placeholder="e.g. Peak load adjustment ahead of Black Friday campaign"
              className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white focus:outline-none focus:border-indigo-500"
              required
            />
          </div>
        </div>
      </Modal>
    </div>
  );
};
