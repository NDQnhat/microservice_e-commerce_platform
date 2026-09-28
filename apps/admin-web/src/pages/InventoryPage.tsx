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
import { useDebounce } from '@/hooks/useDebounce';
import {
  Boxes,
  Search,
  History,
  RefreshCw,
  ShieldCheck,
  Download,
  Calendar,
  AlertTriangle,
  ArrowUpRight,
  ArrowDownRight,
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
    previousQuantity: 6,
    newQuantity: 16,
    reasonCode: 'RESTOCK_IMPORT',
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
    reasonCode: 'DAMAGED_GOODS',
    note: 'Water damage during severe storm inspection',
    actorId: 'warehouse.lead@ecommerce.internal',
    createdAt: '2026-09-27T05:00:00Z',
  },
  {
    id: 'log-inv-9003',
    skuId: 'sku-apple-airpods-pro-2',
    delta: -1,
    previousQuantity: 12,
    newQuantity: 11,
    reasonCode: 'INVENTORY_AUDIT_DISCREPANCY',
    note: 'Discrepancy identified during weekly random cycle count',
    actorId: 'warehouse.lead@ecommerce.internal',
    createdAt: '2026-09-27T08:00:00Z',
  },
];

export const InventoryPage: React.FC = () => {
  const queryClient = useQueryClient();
  const { showSuccess, showError } = useToastStore();

  const [activeTab, setActiveTab] = useState<'stock' | 'ledger'>('stock');
  const [searchQuery, setSearchQuery] = useState('');
  const debouncedSearch = useDebounce(searchQuery, 300);
  const [selectedSku, setSelectedSku] = useState<SkuInventoryDisplay | null>(null);

  // Adjustment Modal (FR-030, BR-004, BR-015)
  const [isAdjustModalOpen, setIsAdjustModalOpen] = useState(false);
  const [adjustmentType, setAdjustmentType] = useState<'INCREASE' | 'DECREASE'>('INCREASE');
  const [adjustmentAmount, setAdjustmentAmount] = useState<number>(5);
  const [reasonCode, setReasonCode] = useState<AdjustmentReasonCode>('RESTOCK_IMPORT');
  const [adjustmentNote, setAdjustmentNote] = useState('');

  // Ledger Filter & Export
  const [ledgerFromDate, setLedgerFromDate] = useState('');
  const [ledgerToDate, setLedgerToDate] = useState('');

  // Fetch Inventories
  const { data: inventories = MOCK_INVENTORIES, isLoading, refetch, isFetching } = useQuery<SkuInventoryDisplay[]>({
    queryKey: ['inventory-list'],
    queryFn: async () => {
      try {
        return await apiClient<SkuInventoryDisplay[]>('/api/v1/backoffice/inventory');
      } catch {
        return MOCK_INVENTORIES;
      }
    },
  });

  // Fetch All Adjustment Logs (for InventoryLedger tab)
  const { data: allLedgerLogs = MOCK_ADJUSTMENT_LOGS, isLoading: isLoadingAllLedger } = useQuery<InventoryAdjustmentLog[]>({
    queryKey: ['all-inventory-ledger'],
    queryFn: async () => {
      try {
        const res = await apiClient<PageResponse<InventoryAdjustmentLog>>('/api/v1/backoffice/inventory/ledger');
        return res.content;
      } catch {
        return MOCK_ADJUSTMENT_LOGS;
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

  // Inventory Invariant Guardrail Validation (BR-003, BR-009, BR-017: available = physical - reserved >= 0)
  const currentOnHand = selectedSku?.totalQuantity ?? 0;
  const currentReserved = selectedSku?.reservedQuantity ?? 0;
  const currentAvailable = selectedSku?.availableQuantity ?? 0;

  const signedDelta = adjustmentType === 'INCREASE' ? adjustmentAmount : -adjustmentAmount;
  const newPhysical = currentOnHand + signedDelta;
  const newAvailable = currentAvailable + signedDelta;

  const isInvariantBreached = newAvailable < 0 || newPhysical < currentReserved;

  let negativeStockError: string | null = null;
  if (isInvariantBreached) {
    negativeStockError = `Vi phạm bất biến BR-009: Tồn vật lý mới không được nhỏ hơn số lượng đang giữ chỗ (Reserved: ${currentReserved})!`;
  }

  const isAdjustmentValid =
    !isInvariantBreached &&
    newAvailable >= 0 &&
    Math.abs(signedDelta) > 0 &&
    adjustmentNote.trim().length >= 10;

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
        body: JSON.stringify({
          delta,
          reason_code,
          note,
          audit_note: note,
        }),
      });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['inventory-list'] });
      queryClient.invalidateQueries({ queryKey: ['inventory-audit-logs'] });
      queryClient.invalidateQueries({ queryKey: ['all-inventory-ledger'] });
      const signedDelta = adjustmentType === 'INCREASE' ? `+${adjustmentAmount}` : `-${adjustmentAmount}`;
      showSuccess(
        'Inventory Adjusted & Logged',
        `Stock delta ${signedDelta} committed with reason ${reasonCode} (BR-015)`
      );
      setIsAdjustModalOpen(false);
      setAdjustmentNote('');
    },
    onError: (err) => showError(err, 'Failed to adjust inventory (Reason code mandatory per FR-030)'),
  });

  // Export Ledger to CSV
  const handleExportLedgerCsv = () => {
    const targetLogs = activeTab === 'ledger' ? filteredLedgerLogs : (selectedSku ? auditLogs : allLedgerLogs);
    if (targetLogs.length === 0) {
      showError('Không có dữ liệu lịch sử biến động kho để xuất CSV');
      return;
    }

    const headers = ['Log ID', 'SKU ID', 'Delta', 'Previous Qty', 'New Qty', 'Reason Code', 'Audit Note', 'Actor', 'Created At'];
    const rows = targetLogs.map((l) => [
      `"${l.id}"`,
      `"${l.skuId}"`,
      l.delta,
      l.previousQuantity,
      l.newQuantity,
      l.reasonCode,
      `"${l.note || ''}"`,
      `"${l.actorId || ''}"`,
      `"${l.createdAt}"`,
    ]);

    const csvContent = '\uFEFF' + [headers.join(','), ...rows.map((r) => r.join(','))].join('\n');
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `inventory-ledger-${new Date().toISOString().slice(0, 10)}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
    showSuccess('Export CSV thành công', `Đã xuất ${targetLogs.length} bản ghi lịch sử kho sang CSV.`);
  };

  const filteredItems = inventories.filter((item) => {
    if (!debouncedSearch.trim()) return true;
    const q = debouncedSearch.toLowerCase();
    return (
      item.skuCode.toLowerCase().includes(q) ||
      item.productName.toLowerCase().includes(q) ||
      item.skuId.toLowerCase().includes(q)
    );
  });

  const filteredLedgerLogs = allLedgerLogs.filter((log) => {
    if (ledgerFromDate) {
      const logDate = log.createdAt.slice(0, 10);
      if (logDate < ledgerFromDate) return false;
    }
    if (ledgerToDate) {
      const logDate = log.createdAt.slice(0, 10);
      if (logDate > ledgerToDate) return false;
    }
    return true;
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

      {/* Tabs */}
      <div className="flex items-center gap-2 border-b border-slate-800 pb-2">
        <button
          onClick={() => setActiveTab('stock')}
          className={`flex items-center gap-2 px-4 py-2 text-xs font-bold rounded-lg transition ${
            activeTab === 'stock'
              ? 'bg-indigo-600 text-white shadow-sm shadow-indigo-500/20'
              : 'text-slate-400 hover:text-white hover:bg-slate-800'
          }`}
        >
          <Boxes className="h-4 w-4" />
          <span>Tồn kho SKU (Stock Balance)</span>
        </button>
        <button
          onClick={() => setActiveTab('ledger')}
          className={`flex items-center gap-2 px-4 py-2 text-xs font-bold rounded-lg transition ${
            activeTab === 'ledger'
              ? 'bg-indigo-600 text-white shadow-sm shadow-indigo-500/20'
              : 'text-slate-400 hover:text-white hover:bg-slate-800'
          }`}
        >
          <History className="h-4 w-4" />
          <span>Lịch sử biến động kho (Inventory Ledger)</span>
        </button>
      </div>

      {/* Tab 1: Stock Balance View */}
      {activeTab === 'stock' && (
        <>
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
                              setAdjustmentType('INCREASE');
                              setAdjustmentAmount(5);
                              setAdjustmentNote('');
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
        </>
      )}

      {/* Tab 2: Inventory Ledger View */}
      {activeTab === 'ledger' && (
        <div className="space-y-4">
          <div className="p-4 rounded-xl border border-slate-800 bg-slate-900/90 backdrop-blur flex flex-col md:flex-row items-center justify-between gap-4">
            <div className="flex items-center gap-2 bg-slate-950 border border-slate-800 rounded-lg px-3 py-1.5 text-xs">
              <Calendar className="h-4 w-4 text-slate-500" />
              <input
                type="date"
                value={ledgerFromDate}
                onChange={(e) => setLedgerFromDate(e.target.value)}
                className="bg-transparent text-slate-200 text-xs focus:outline-none"
                title="From Date"
              />
              <span className="text-slate-600">→</span>
              <input
                type="date"
                value={ledgerToDate}
                onChange={(e) => setLedgerToDate(e.target.value)}
                className="bg-transparent text-slate-200 text-xs focus:outline-none"
                title="To Date"
              />
              {(ledgerFromDate || ledgerToDate) && (
                <button
                  type="button"
                  onClick={() => {
                    setLedgerFromDate('');
                    setLedgerToDate('');
                  }}
                  className="text-[10px] text-slate-400 hover:text-white ml-2 px-1.5 py-0.5 rounded bg-slate-800"
                >
                  Clear Filter
                </button>
              )}
            </div>

            <button
              onClick={handleExportLedgerCsv}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold bg-emerald-600 hover:bg-emerald-500 text-white transition shadow-xs"
            >
              <Download className="h-3.5 w-3.5" />
              <span>Export CSV</span>
            </button>
          </div>

          {isLoadingAllLedger ? (
            <SkeletonTable rows={4} cols={6} />
          ) : filteredLedgerLogs.length === 0 ? (
            <EmptyState
              icon={History}
              title="No Ledger Records Found"
              description="No inventory adjustments match the selected date range."
            />
          ) : (
            <div className="rounded-xl border border-slate-800 bg-slate-950 overflow-hidden shadow-xl">
              <table className="w-full text-left text-sm">
                <thead className="sticky top-0 bg-slate-900/95 backdrop-blur z-10 border-b border-slate-800 text-xs font-semibold uppercase tracking-wider text-slate-400">
                  <tr>
                    <th className="py-3 px-4">Log ID</th>
                    <th className="py-3 px-4">SKU Identifier</th>
                    <th className="py-3 px-4">Delta</th>
                    <th className="py-3 px-4">Balance Movement</th>
                    <th className="py-3 px-4">Reason Code</th>
                    <th className="py-3 px-4">Audit Note</th>
                    <th className="py-3 px-4">Actor</th>
                    <th className="py-3 px-4">Timestamp</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800/80">
                  {filteredLedgerLogs.map((log) => (
                    <tr key={log.id} className="hover:bg-slate-900/50 transition text-xs">
                      <td className="py-3 px-4 font-mono font-bold text-indigo-400">{log.id}</td>
                      <td className="py-3 px-4 font-mono text-slate-300">{log.skuId}</td>
                      <td className="py-3 px-4 font-mono font-bold">
                        <span
                          className={`px-2 py-0.5 rounded text-[11px] ${
                            log.delta > 0
                              ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'
                              : 'bg-rose-500/10 text-rose-400 border border-rose-500/20'
                          }`}
                        >
                          {log.delta > 0 ? `+${log.delta}` : log.delta}
                        </span>
                      </td>
                      <td className="py-3 px-4 font-mono text-slate-400">
                        {log.previousQuantity} → {log.newQuantity}
                      </td>
                      <td className="py-3 px-4 font-mono text-indigo-300 font-semibold">{log.reasonCode}</td>
                      <td className="py-3 px-4 text-slate-300 max-w-xs truncate">{log.note || '-'}</td>
                      <td className="py-3 px-4 font-mono text-slate-400">{log.actorId || 'SYSTEM'}</td>
                      <td className="py-3 px-4 text-slate-400">{new Date(log.createdAt).toLocaleString()}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
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
              onClick={() => {
                setAdjustmentType('INCREASE');
                setAdjustmentAmount(5);
                setAdjustmentNote('');
                setIsAdjustModalOpen(true);
              }}
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
                <p className="text-[11px] text-slate-500 uppercase font-semibold">Tồn khả dụng</p>
                <p className="text-xl font-mono font-extrabold text-emerald-400 mt-1">
                  {selectedSku.availableQuantity}
                </p>
              </div>
              <div className="border-x border-slate-800">
                <p className="text-[11px] text-slate-500 uppercase font-semibold">Đang giữ chỗ</p>
                <p className="text-xl font-mono font-extrabold text-amber-400 mt-1">
                  {selectedSku.reservedQuantity}
                </p>
              </div>
              <div>
                <p className="text-[11px] text-slate-500 uppercase font-semibold">Tồn thực tế (On-hand)</p>
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
                <button
                  onClick={handleExportLedgerCsv}
                  className="flex items-center gap-1 text-[11px] text-emerald-400 hover:text-emerald-300 font-semibold"
                >
                  <Download className="h-3 w-3" />
                  <span>Export CSV</span>
                </button>
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

      {/* Stock Adjustment Modal (FR-030, BR-004, BR-015) */}
      <Modal
        isOpen={isAdjustModalOpen}
        onClose={() => setIsAdjustModalOpen(false)}
        title="Điều chỉnh tồn kho (FR-030, BR-004, BR-015)"
        subtitle="Kiểm soát ranh giới âm tồn kho & ghi nhận sổ cái kiểm toán bất biến"
        footerActions={
          <>
            <button
              onClick={() => setIsAdjustModalOpen(false)}
              className="px-4 py-2 text-xs font-semibold text-slate-400 hover:text-white"
            >
              Hủy
            </button>
            <button
              disabled={adjustMutation.isPending || !isAdjustmentValid}
              onClick={() => {
                if (selectedSku) {
                  const deltaSigned = adjustmentType === 'INCREASE' ? adjustmentAmount : -adjustmentAmount;
                  adjustMutation.mutate({
                    skuId: selectedSku.skuId,
                    delta: deltaSigned,
                    reason_code: reasonCode,
                    note: adjustmentNote,
                  });
                }
              }}
              className="px-4 py-2 text-xs font-semibold bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 disabled:cursor-not-allowed text-white rounded-lg transition"
            >
              {adjustMutation.isPending ? 'Đang ghi nhận...' : 'Xác nhận điều chỉnh'}
            </button>
          </>
        }
      >
        <div className="space-y-4">
          {/* Target SKU Stock Summary & Live Preview (BR-003, BR-009, BR-017) */}
          <div className="p-3.5 rounded-xl bg-slate-950 border border-slate-800 space-y-3 text-xs">
            <div className="flex justify-between items-center">
              <span className="text-slate-400">Target SKU:</span>
              <span className="font-mono font-bold text-indigo-400">{selectedSku?.skuCode}</span>
            </div>
            
            <div className="grid grid-cols-3 gap-2 pt-2 border-t border-slate-800/60 text-center">
              <div className="p-2 rounded bg-slate-900/50">
                <p className="text-[10px] text-slate-400 uppercase font-semibold">Tồn thực tế hiện tại</p>
                <p className="text-sm font-mono font-bold text-white mt-0.5">{currentOnHand}</p>
              </div>
              <div className="p-2 rounded bg-slate-900/50">
                <p className="text-[10px] text-slate-400 uppercase font-semibold">Đang giữ chỗ</p>
                <p className="text-sm font-mono font-bold text-amber-400 mt-0.5">{currentReserved}</p>
              </div>
              <div className="p-2 rounded bg-slate-900/50">
                <p className="text-[10px] text-slate-400 uppercase font-semibold">Tồn khả dụng hiện tại</p>
                <p className="text-sm font-mono font-bold text-emerald-400 mt-0.5">{currentAvailable}</p>
              </div>
            </div>

            {/* Live Preview Box */}
            <div className="p-2.5 rounded-lg bg-indigo-950/30 border border-indigo-500/20 text-xs space-y-1.5">
              <div className="flex items-center justify-between font-semibold text-indigo-300 text-[11px] uppercase tracking-wider">
                <span>Dự tính sau điều chỉnh (Live Preview)</span>
                <span className="font-mono">Delta: {signedDelta > 0 ? `+${signedDelta}` : signedDelta}</span>
              </div>
              <div className="grid grid-cols-2 gap-2 text-[11px] font-mono">
                <div className="flex justify-between items-center bg-slate-950/70 px-2 py-1 rounded border border-slate-800">
                  <span className="text-slate-400">Tồn vật lý mới:</span>
                  <span className={`font-bold ${newPhysical < currentReserved ? 'text-rose-400' : 'text-white'}`}>
                    {currentOnHand} → {newPhysical}
                  </span>
                </div>
                <div className="flex justify-between items-center bg-slate-950/70 px-2 py-1 rounded border border-slate-800">
                  <span className="text-slate-400">Tồn khả dụng mới:</span>
                  <span className={`font-bold ${newAvailable < 0 ? 'text-rose-400' : 'text-emerald-400'}`}>
                    {currentAvailable} → {newAvailable}
                  </span>
                </div>
              </div>
            </div>
          </div>

          {/* Negative Stock Violation Error Banner (BR-009 Guard) */}
          {negativeStockError && (
            <div className="p-3 rounded-lg bg-rose-500/10 border border-rose-500/20 text-xs text-rose-400 space-y-1">
              <div className="flex items-center gap-1.5 font-bold text-rose-300">
                <AlertTriangle className="h-4 w-4 shrink-0 text-rose-400" />
                <span>Vi phạm bất biến BR-009: Tồn vật lý mới không được nhỏ hơn số lượng đang giữ chỗ (Reserved: {currentReserved})!</span>
              </div>
              <p className="text-[11px] text-rose-400/90 leading-relaxed">
                Hệ thống yêu cầu available_quantity = physical_quantity - reserved_quantity &ge; 0.
                Dự tính sau điều chỉnh: Tồn khả dụng mới = {newAvailable} &lt; 0. Thao tác bị khóa.
              </p>
            </div>
          )}

          {/* Adjustment Type Selector */}
          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-300 uppercase">Hình thức điều chỉnh tồn kho</label>
            <div className="grid grid-cols-2 gap-2">
              <button
                type="button"
                onClick={() => setAdjustmentType('INCREASE')}
                className={`py-2 px-3 rounded-lg text-xs font-semibold flex items-center justify-center gap-1.5 transition ${
                  adjustmentType === 'INCREASE'
                    ? 'bg-emerald-600 text-white shadow-sm'
                    : 'bg-slate-900 text-slate-400 hover:text-white border border-slate-800'
                }`}
              >
                <ArrowUpRight className="h-4 w-4" />
                <span>Tăng tồn kho (INCREASE)</span>
              </button>
              <button
                type="button"
                onClick={() => setAdjustmentType('DECREASE')}
                className={`py-2 px-3 rounded-lg text-xs font-semibold flex items-center justify-center gap-1.5 transition ${
                  adjustmentType === 'DECREASE'
                    ? 'bg-rose-600 text-white shadow-sm'
                    : 'bg-slate-900 text-slate-400 hover:text-white border border-slate-800'
                }`}
              >
                <ArrowDownRight className="h-4 w-4" />
                <span>Giảm tồn kho (DECREASE)</span>
              </button>
            </div>
          </div>

          {/* Quantity Amount */}
          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-300 uppercase">
              Số lượng thay đổi ({adjustmentType === 'INCREASE' ? '+' : '-'})
            </label>
            <input
              type="number"
              min={1}
              value={adjustmentAmount}
              onChange={(e) => setAdjustmentAmount(Math.max(1, parseInt(e.target.value, 10) || 0))}
              className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white font-mono focus:outline-none focus:border-indigo-500"
              required
            />
          </div>

          {/* Reason Code */}
          <div className="space-y-1.5">
            <div className="flex items-center justify-between">
              <label className="text-xs font-semibold text-slate-300 uppercase">
                Mã lý do điều chỉnh (Reason Code - BR-015)
              </label>
              <span className="text-[11px] text-rose-400">* Bắt buộc</span>
            </div>
            <select
              value={reasonCode}
              onChange={(e) => setReasonCode(e.target.value as AdjustmentReasonCode)}
              className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white focus:outline-none focus:border-indigo-500 font-mono"
            >
              <option value="RESTOCK_IMPORT">RESTOCK_IMPORT - Nhập hàng bổ sung từ nhà cung cấp</option>
              <option value="DAMAGED_GOODS">DAMAGED_GOODS - Hàng hư hỏng / bể vỡ trong kho</option>
              <option value="INVENTORY_AUDIT_DISCREPANCY">INVENTORY_AUDIT_DISCREPANCY - Lệch tồn sau kiểm kê thực tế</option>
              <option value="RETURN_RESTOCK">RETURN_RESTOCK - Nhập lại kho từ đơn hoàn trả</option>
              <option value="EXPIRED_DISPOSAL">EXPIRED_DISPOSAL - Tiêu hủy hàng hết hạn / lỗi kỹ thuật</option>
            </select>
          </div>

          {/* Audit Note */}
          <div className="space-y-1.5">
            <div className="flex items-center justify-between">
              <label className="text-xs font-semibold text-slate-300 uppercase">
                Ghi chú kiểm toán kho (Audit Note - Tối thiểu 10 ký tự)
              </label>
              <span
                className={`text-[11px] font-mono ${
                  adjustmentNote.trim().length >= 10 ? 'text-emerald-400 font-semibold' : 'text-rose-400'
                }`}
              >
                {adjustmentNote.trim().length} / 10 ký tự tối thiểu
              </span>
            </div>
            <textarea
              rows={3}
              value={adjustmentNote}
              onChange={(e) => setAdjustmentNote(e.target.value)}
              placeholder="Nhập vị trí pallet, mã biên bản kiểm kê hoặc lý do giải trình kiểm toán..."
              className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white focus:outline-none focus:border-indigo-500 placeholder-slate-500"
              required
            />
          </div>
        </div>
      </Modal>
    </div>
  );
};
