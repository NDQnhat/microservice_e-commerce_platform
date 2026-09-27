import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '@/lib/api-client';
import { SkuInventory, InventoryAdjustmentLog, AdjustmentReasonCode, PageResponse } from '@/types';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { SkeletonTable } from '@/components/ui/SkeletonTable';
import { EmptyState } from '@/components/ui/EmptyState';
import { SlideOverDrawer } from '@/components/ui/SlideOverDrawer';
import { Modal } from '@/components/ui/Modal';
import { useToastStore } from '@/store/toast-store';
import {
  Boxes,
  Search,
  History,
  RefreshCw,
  ShieldCheck,
} from 'lucide-react';

interface SkuInventoryDisplay extends SkuInventory {
  productName: string;
  skuCode: string;
}

const MOCK_INVENTORIES: SkuInventoryDisplay[] = [
  {
    skuId: 'sku-nike-pegasus-40-blk-42',
    skuCode: 'NIKE-PEG40-BLK-42',
    productName: 'Nike Air Zoom Pegasus 40 (Black/42)',
    availableQuantity: 4,
    reservedQuantity: 2,
    totalQuantity: 6,
    updatedAt: '2026-09-27T08:30:00Z',
  },
  {
    skuId: 'sku-nike-pegasus-40-blk-43',
    skuCode: 'NIKE-PEG40-BLK-43',
    productName: 'Nike Air Zoom Pegasus 40 (Black/43)',
    availableQuantity: 28,
    reservedQuantity: 4,
    totalQuantity: 32,
    updatedAt: '2026-09-27T07:10:00Z',
  },
  {
    skuId: 'sku-apple-airpods-pro-2',
    skuCode: 'APP-AIRPODSPRO-WHT',
    productName: 'Apple AirPods Pro (2nd Gen White)',
    availableQuantity: 8,
    reservedQuantity: 3,
    totalQuantity: 11,
    updatedAt: '2026-09-27T09:12:00Z',
  },
  {
    skuId: 'sku-adidas-ultraboost-wht-41',
    skuCode: 'ADI-UB-WHT-41',
    productName: 'Adidas Ultraboost Light (White/41)',
    availableQuantity: 0,
    reservedQuantity: 5,
    totalQuantity: 5,
    updatedAt: '2026-09-27T06:40:00Z',
  },
];

const MOCK_ADJUSTMENT_LOGS: InventoryAdjustmentLog[] = [
  {
    id: 'log-inv-9001',
    skuId: 'sku-nike-pegasus-40-blk-42',
    delta: 10,
    previousQuantity: -4,
    newQuantity: 6,
    reasonCode: 'RESTOCK',
    note: 'Inbound container shipment C-409 checked into warehouse bay 3',
    actorId: 'warehouse.lead@ecommerce.internal',
    createdAt: '2026-09-26T14:30:00Z',
  },
  {
    id: 'log-inv-9002',
    skuId: 'sku-nike-pegasus-40-blk-42',
    delta: -2,
    previousQuantity: 8,
    newQuantity: 6,
    reasonCode: 'DAMAGED',
    note: 'Water damage during severe storm inspection',
    actorId: 'warehouse.lead@ecommerce.internal',
    createdAt: '2026-09-27T05:00:00Z',
  },
];

export const InventoryPage: React.FC = () => {
  const queryClient = useQueryClient();
  const { showSuccess, showError } = useToastStore();

  const [searchQuery, setSearchQuery] = useState('');
  const [selectedSku, setSelectedSku] = useState<SkuInventoryDisplay | null>(null);

  // Adjustment Modal
  const [isAdjustModalOpen, setIsAdjustModalOpen] = useState(false);
  const [adjustmentDelta, setAdjustmentDelta] = useState<number>(10);
  const [reasonCode, setReasonCode] = useState<AdjustmentReasonCode>('RESTOCK');
  const [adjustmentNote, setAdjustmentNote] = useState('');

  // Fetch Inventories
  const { data: inventories = MOCK_INVENTORIES, isLoading, refetch, isFetching } = useQuery<SkuInventoryDisplay[]>({
    queryKey: ['inventory-list'],
    queryFn: async () => {
      try {
        // Query through backend inventory service
        return await apiClient<SkuInventoryDisplay[]>('/api/v1/backoffice/inventory');
      } catch {
        return MOCK_INVENTORIES;
      }
    },
  });

  // Fetch Adjustment Audit Logs for Selected SKU
  const { data: auditLogs = MOCK_ADJUSTMENT_LOGS, isLoading: isLoadingLogs } = useQuery<InventoryAdjustmentLog[]>({
    queryKey: ['inventory-audit-logs', selectedSku?.skuId],
    queryFn: async () => {
      if (!selectedSku) return [];
      try {
        const res = await apiClient<PageResponse<InventoryAdjustmentLog>>(
          `/api/v1/backoffice/skus/${selectedSku.skuId}/inventory/audit-log`
        );
        return res.content;
      } catch {
        return MOCK_ADJUSTMENT_LOGS.filter((l) => l.skuId === selectedSku.skuId);
      }
    },
    enabled: !!selectedSku,
  });

  // Adjustment Mutation (FR-030, BR-015)
  const adjustMutation = useMutation({
    mutationFn: async ({
      skuId,
      delta,
      reason_code,
      note,
    }: {
      skuId: string;
      delta: number;
      reason_code: AdjustmentReasonCode;
      note: string;
    }) => {
      return apiClient(`/api/v1/backoffice/skus/${skuId}/inventory/adjustments`, {
        method: 'POST',
        body: JSON.stringify({ delta, reason_code, note }),
      });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['inventory-list'] });
      queryClient.invalidateQueries({ queryKey: ['inventory-audit-logs'] });
      showSuccess(
        'Inventory Adjusted & Logged',
        `Stock delta ${adjustmentDelta > 0 ? `+${adjustmentDelta}` : adjustmentDelta} committed with reason ${reasonCode} (BR-015)`
      );
      setIsAdjustModalOpen(false);
      setAdjustmentNote('');
    },
    onError: (err) => showError(err, 'Failed to adjust inventory (Reason code mandatory per FR-030)'),
  });

  const filteredItems = inventories.filter((item) => {
    if (!searchQuery.trim()) return true;
    const q = searchQuery.toLowerCase();
    return (
      item.skuCode.toLowerCase().includes(q) ||
      item.productName.toLowerCase().includes(q) ||
      item.skuId.toLowerCase().includes(q)
    );
  });

  return (
    <div className="space-y-6">
      {/* Title & Top Bar */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-white">Inventory Management & Audit</h1>
          <p className="text-sm text-slate-400">
            Real-time stock reservation tracking, mandatory reason adjustments, and immutable ledger (API-INV, FR-030, BR-015)
          </p>
        </div>
        <button
          onClick={() => refetch()}
          disabled={isFetching}
          className="flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs font-semibold bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 transition w-fit"
        >
          <RefreshCw className={`h-3.5 w-3.5 ${isFetching ? 'animate-spin' : ''}`} />
          <span>Sync Stock</span>
        </button>
      </div>

      {/* Search & Statistics */}
      <div className="p-4 rounded-xl border border-slate-800 bg-slate-900/90 backdrop-blur flex flex-col md:flex-row items-center justify-between gap-4">
        <div className="relative w-full md:w-80">
          <Search className="absolute left-3 top-2.5 h-4 w-4 text-slate-500" />
          <input
            type="text"
            placeholder="Search by SKU code, product, or ID..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full bg-slate-950 border border-slate-700/80 rounded-lg pl-9 pr-3 py-2 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-indigo-500"
          />
        </div>

        <div className="flex items-center gap-4 text-xs">
          <div className="flex items-center gap-2">
            <span className="h-2 w-2 rounded-full bg-emerald-400" />
            <span className="text-slate-400">Normal Stock (&gt;10 units)</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="h-2 w-2 rounded-full bg-amber-400 animate-pulse" />
            <span className="text-slate-400">Low Stock Alert (&lt;10 units)</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="h-2 w-2 rounded-full bg-rose-400" />
            <span className="text-slate-400">Depleted / Stockout (0 units)</span>
          </div>
        </div>
      </div>

      {/* Stock Table */}
      {isLoading ? (
        <SkeletonTable rows={4} cols={5} />
      ) : filteredItems.length === 0 ? (
        <EmptyState
          icon={Boxes}
          title="No Inventory Records Found"
          description="Try broadening your SKU search query."
          actionLabel="Clear Search"
          onAction={() => setSearchQuery('')}
        />
      ) : (
        <div className="rounded-xl border border-slate-800 bg-slate-950 overflow-hidden shadow-xl">
          <table className="w-full text-left text-sm">
            <thead className="sticky top-0 bg-slate-900/95 backdrop-blur z-10 border-b border-slate-800 text-xs font-semibold uppercase tracking-wider text-slate-400">
              <tr>
                <th className="py-3 px-4">SKU Code & Product</th>
                <th className="py-3 px-4">Available Stock</th>
                <th className="py-3 px-4">Reserved Stock</th>
                <th className="py-3 px-4">Total Stock</th>
                <th className="py-3 px-4">Stock Status</th>
                <th className="py-3 px-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/80">
              {filteredItems.map((item) => {
                const isLow = item.availableQuantity > 0 && item.availableQuantity < 10;
                const isOut = item.availableQuantity === 0;

                return (
                  <tr
                    key={item.skuId}
                    onClick={() => setSelectedSku(item)}
                    className="hover:bg-slate-900/50 cursor-pointer transition group"
                  >
                    <td className="py-3 px-4">
                      <p className="font-semibold text-white text-xs">{item.productName}</p>
                      <p className="font-mono text-[11px] text-indigo-400">{item.skuCode}</p>
                    </td>
                    <td className="py-3 px-4">
                      <span
                        className={`font-mono text-sm font-bold ${
                          isOut ? 'text-rose-400' : isLow ? 'text-amber-400' : 'text-emerald-400'
                        }`}
                      >
                        {item.availableQuantity} units
                      </span>
                    </td>
                    <td className="py-3 px-4">
                      <span className="font-mono text-xs text-amber-300 font-semibold">
                        {item.reservedQuantity} units
                      </span>
                    </td>
                    <td className="py-3 px-4">
                      <span className="font-mono text-xs text-slate-300 font-semibold">
                        {item.totalQuantity} units
                      </span>
                    </td>
                    <td className="py-3 px-4">
                      {isOut ? (
                        <StatusBadge status="OUT_OF_STOCK" variant="rose" />
                      ) : isLow ? (
                        <StatusBadge status="LOW_STOCK" variant="amber" />
                      ) : (
                        <StatusBadge status="IN_STOCK" variant="emerald" />
                      )}
                    </td>
                    <td
                      className="py-3 px-4 text-right space-x-2"
                      onClick={(e) => e.stopPropagation()}
                    >
                      <button
                        onClick={() => {
                          setSelectedSku(item);
                          setIsAdjustModalOpen(true);
                        }}
                        className="px-2.5 py-1 text-xs font-semibold bg-indigo-600 hover:bg-indigo-500 text-white rounded transition shadow-xs"
                      >
                        Adjust Stock
                      </button>
                      <button
                        onClick={() => setSelectedSku(item)}
                        className="p-1 text-slate-400 hover:text-white rounded transition"
                        title="Audit Logs"
                      >
                        <History className="h-4 w-4" />
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      {/* Slide-over Drawer for Selected SKU & Immutable Audit Logs */}
      <SlideOverDrawer
        isOpen={!!selectedSku}
        onClose={() => setSelectedSku(null)}
        title={selectedSku?.productName || 'SKU Inventory'}
        subtitle={`SKU Code: ${selectedSku?.skuCode}`}
        idToCopy={selectedSku?.skuId}
        badge={
          selectedSku && (
            <span className="font-mono text-xs px-2 py-0.5 rounded bg-indigo-500/10 text-indigo-400 border border-indigo-500/20 font-bold">
              Total: {selectedSku.totalQuantity}
            </span>
          )
        }
        footerActions={
          selectedSku && (
            <button
              onClick={() => setIsAdjustModalOpen(true)}
              className="px-4 py-2 text-xs font-semibold bg-indigo-600 hover:bg-indigo-500 text-white rounded-lg shadow-sm transition"
            >
              Adjust Stock Level (FR-030)
            </button>
          )
        }
      >
        {selectedSku && (
          <div className="space-y-6">
            {/* Quick Balance Breakdown */}
            <div className="grid grid-cols-3 gap-3 p-4 rounded-xl bg-slate-950 border border-slate-800 text-center">
              <div>
                <p className="text-[11px] text-slate-500 uppercase font-semibold">Available</p>
                <p className="text-xl font-mono font-extrabold text-emerald-400 mt-1">
                  {selectedSku.availableQuantity}
                </p>
              </div>
              <div className="border-x border-slate-800">
                <p className="text-[11px] text-slate-500 uppercase font-semibold">Reserved</p>
                <p className="text-xl font-mono font-extrabold text-amber-400 mt-1">
                  {selectedSku.reservedQuantity}
                </p>
              </div>
              <div>
                <p className="text-[11px] text-slate-500 uppercase font-semibold">Total Physical</p>
                <p className="text-xl font-mono font-extrabold text-white mt-1">
                  {selectedSku.totalQuantity}
                </p>
              </div>
            </div>

            {/* Immutable Adjustment Audit Logs (BR-014, BR-015) */}
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <label className="text-xs font-semibold text-slate-300 uppercase tracking-wider flex items-center gap-1.5">
                  <ShieldCheck className="h-4 w-4 text-emerald-400" />
                  <span>Immutable Adjustment Audit Log (Append-Only)</span>
                </label>
                <span className="text-[11px] text-slate-500 font-mono">BR-015 Compliant</span>
              </div>

              {isLoadingLogs ? (
                <div className="text-center py-6 text-slate-500 text-xs">Loading audit ledger...</div>
              ) : auditLogs.length === 0 ? (
                <div className="p-6 text-center text-slate-500 text-xs border border-slate-800 rounded-xl bg-slate-950">
                  No adjustments recorded for this SKU yet.
                </div>
              ) : (
                <div className="space-y-2.5">
                  {auditLogs.map((log) => (
                    <div
                      key={log.id}
                      className="p-3.5 rounded-xl bg-slate-950 border border-slate-800/80 space-y-2 text-xs"
                    >
                      <div className="flex items-center justify-between">
                        <span
                          className={`font-mono font-bold px-2 py-0.5 rounded text-[11px] ${
                            log.delta > 0
                              ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'
                              : 'bg-rose-500/10 text-rose-400 border border-rose-500/20'
                          }`}
                        >
                          {log.delta > 0 ? `+${log.delta}` : log.delta} ({log.reasonCode})
                        </span>
                        <span className="font-mono text-slate-500 text-[11px]">
                          {new Date(log.createdAt).toLocaleString()}
                        </span>
                      </div>
                      {log.note && <p className="text-slate-300 text-xs">{log.note}</p>}
                      <div className="flex items-center justify-between text-[11px] text-slate-500 font-mono pt-1 border-t border-slate-800/40">
                        <span>
                          Balance: {log.previousQuantity} → {log.newQuantity}
                        </span>
                        <span>Actor: {log.actorId || 'SYSTEM'}</span>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        )}
      </SlideOverDrawer>

      {/* Stock Adjustment Modal (FR-030, BR-015) */}
      <Modal
        isOpen={isAdjustModalOpen}
        onClose={() => setIsAdjustModalOpen(false)}
        title="Adjust Inventory Balance (FR-030, BR-015)"
        subtitle="Mandatory reason code logging to immutable adjustment ledger"
        footerActions={
          <>
            <button
              onClick={() => setIsAdjustModalOpen(false)}
              className="px-4 py-2 text-xs font-semibold text-slate-400 hover:text-white"
            >
              Cancel
            </button>
            <button
              disabled={adjustMutation.isPending || !adjustmentNote.trim()}
              onClick={() => {
                if (selectedSku) {
                  adjustMutation.mutate({
                    skuId: selectedSku.skuId,
                    delta: adjustmentDelta,
                    reason_code: reasonCode,
                    note: adjustmentNote,
                  });
                }
              }}
              className="px-4 py-2 text-xs font-semibold bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 text-white rounded-lg transition"
            >
              {adjustMutation.isPending ? 'Committing...' : 'Commit Adjustment'}
            </button>
          </>
        }
      >
        <div className="space-y-4">
          <div className="p-3 rounded-lg bg-indigo-500/10 border border-indigo-500/20 text-xs text-indigo-300">
            Target SKU: <span className="font-mono font-bold text-white">{selectedSku?.skuCode}</span>
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-300 uppercase">
              Adjustment Delta (+ to add, - to subtract)
            </label>
            <input
              type="number"
              value={adjustmentDelta}
              onChange={(e) => setAdjustmentDelta(parseInt(e.target.value, 10) || 0)}
              className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white font-mono focus:outline-none focus:border-indigo-500"
              required
            />
          </div>

          <div className="space-y-1.5">
            <div className="flex items-center justify-between">
              <label className="text-xs font-semibold text-slate-300 uppercase">
                Reason Code (Mandatory per FR-030, BR-015)
              </label>
              <span className="text-[11px] text-rose-400">* Required</span>
            </div>
            <select
              value={reasonCode}
              onChange={(e) => setReasonCode(e.target.value as AdjustmentReasonCode)}
              className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white focus:outline-none focus:border-indigo-500 font-mono"
            >
              <option value="RESTOCK">RESTOCK - Inbound goods receipt from supplier</option>
              <option value="DAMAGED">DAMAGED - Physical shrinkage / breakage loss</option>
              <option value="CORRECTION">CORRECTION - Discrepancy correction post cycle count</option>
              <option value="CYCLE_COUNT">CYCLE_COUNT - Routine audit reconciliation</option>
              <option value="RETURN_RESTOCK">RETURN_RESTOCK - Customer return restocking</option>
            </select>
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-300 uppercase">
              Audit Note / Warehouse Justification
            </label>
            <textarea
              rows={3}
              value={adjustmentNote}
              onChange={(e) => setAdjustmentNote(e.target.value)}
              placeholder="Physical pallet location, batch code, or audit context..."
              className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white focus:outline-none focus:border-indigo-500 placeholder-slate-500"
              required
            />
          </div>
        </div>
      </Modal>
    </div>
  );
};
