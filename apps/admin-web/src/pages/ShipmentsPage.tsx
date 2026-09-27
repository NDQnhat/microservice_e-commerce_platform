import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '@/lib/api-client';
import { Shipment, ShipmentStatus, PageResponse } from '@/types';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { SkeletonTable } from '@/components/ui/SkeletonTable';
import { EmptyState } from '@/components/ui/EmptyState';
import { SlideOverDrawer } from '@/components/ui/SlideOverDrawer';
import { Modal } from '@/components/ui/Modal';
import { useToastStore } from '@/store/toast-store';
import {
  Truck,
  Search,
  PackageCheck,
  Clock,
  ShieldCheck,
  RefreshCw,
  Eye,
} from 'lucide-react';

const MOCK_SHIPMENTS: Shipment[] = [
  {
    id: 'shp-1001-a1b2',
    orderId: 'ord-1002-9931',
    carrierName: 'GIAO_HANG_NHANH',
    trackingCode: 'GHN-8849102-VN',
    status: 'PACKING',
    packedAt: '2026-09-27T07:25:00Z',
  },
  {
    id: 'shp-1002-c3d4',
    orderId: 'ord-1003-4411',
    carrierName: 'VIETTEL_POST',
    trackingCode: 'VTP-9921445-VN',
    status: 'SHIPPED',
    packedAt: '2026-09-26T15:00:00Z',
    shippedAt: '2026-09-26T16:00:00Z',
  },
  {
    id: 'shp-1003-e5f6',
    orderId: 'ord-1005-7788',
    carrierName: 'GIAO_HANG_TIET_KIEM',
    trackingCode: 'GHTK-3341092-VN',
    status: 'DELIVERED',
    packedAt: '2026-09-25T10:00:00Z',
    shippedAt: '2026-09-25T11:30:00Z',
    deliveredAt: '2026-09-26T14:15:00Z',
  },
];

export const ShipmentsPage: React.FC = () => {
  const queryClient = useQueryClient();
  const { showSuccess, showError } = useToastStore();

  const [activeTab, setActiveTab] = useState<'all' | 'stuck'>('all');
  const [statusFilter, setStatusFilter] = useState<ShipmentStatus | ''>('');
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedShipment, setSelectedShipment] = useState<Shipment | null>(null);

  // Update Shipment Modal (API-FUL-001, BR-011)
  const [isUpdateModalOpen, setIsUpdateModalOpen] = useState(false);
  const [carrierName, setCarrierName] = useState('GIAO_HANG_NHANH');
  const [trackingCode, setTrackingCode] = useState('');
  const [targetStatus, setTargetStatus] = useState<ShipmentStatus>('SHIPPED');

  // Initiate Shipment Modal
  const [isInitiateModalOpen, setIsInitiateModalOpen] = useState(false);
  const [initiateOrderId, setInitiateOrderId] = useState('');

  // Fetch Shipments
  const { data, isLoading, refetch, isFetching } = useQuery<PageResponse<Shipment>>({
    queryKey: ['shipments', statusFilter, activeTab],
    queryFn: async () => {
      try {
        if (activeTab === 'stuck') {
          const res = await apiClient<Shipment[]>('/api/v1/backoffice/fulfillment/stuck?thresholdMinutes=60');
          return { content: res, totalElements: res.length };
        }
        const url = statusFilter
          ? `/api/v1/backoffice/fulfillment?status=${statusFilter}`
          : '/api/v1/backoffice/fulfillment';
        return await apiClient<PageResponse<Shipment>>(url);
      } catch {
        let filtered = [...MOCK_SHIPMENTS];
        if (activeTab === 'stuck') {
          filtered = filtered.filter((s) => s.status === 'PACKING');
        } else if (statusFilter) {
          filtered = filtered.filter((s) => s.status === statusFilter);
        }
        return { content: filtered, totalElements: filtered.length };
      }
    },
  });

  // Update Shipment Transition Mutation (BR-011)
  const updateShipmentMutation = useMutation({
    mutationFn: async ({
      orderId,
      carrier_name,
      tracking_code,
      target_status,
    }: {
      orderId: string;
      carrier_name: string;
      tracking_code: string;
      target_status: ShipmentStatus;
    }) => {
      return apiClient<Shipment>(`/api/v1/backoffice/fulfillment/orders/${orderId}`, {
        method: 'POST',
        body: JSON.stringify({ carrier_name, tracking_code, target_status }),
      });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['shipments'] });
      showSuccess(
        'Shipment Dispatched (BR-011)',
        `Order #${selectedShipment?.orderId} transitioned to ${targetStatus} with carrier ${carrierName}`
      );
      setIsUpdateModalOpen(false);
      setTrackingCode('');
      if (selectedShipment) {
        setSelectedShipment({
          ...selectedShipment,
          carrierName,
          trackingCode: trackingCode || selectedShipment.trackingCode,
          status: targetStatus,
          shippedAt: targetStatus === 'SHIPPED' ? new Date().toISOString() : selectedShipment.shippedAt,
          deliveredAt: targetStatus === 'DELIVERED' ? new Date().toISOString() : selectedShipment.deliveredAt,
        });
      }
    },
    onError: (err) => showError(err, 'Failed to update shipment (Carrier and tracking code mandatory per BR-011)'),
  });

  // Initiate Shipment Mutation
  const initiateMutation = useMutation({
    mutationFn: async (orderId: string) => {
      return apiClient<Shipment>(`/api/v1/backoffice/fulfillment/orders/${orderId}/initiate`, {
        method: 'POST',
      });
    },
    onSuccess: (created) => {
      queryClient.invalidateQueries({ queryKey: ['shipments'] });
      showSuccess('Shipment Initiated', `Fulfillment packing pipeline created for Order #${created.orderId}`);
      setIsInitiateModalOpen(false);
      setInitiateOrderId('');
    },
    onError: (err) => showError(err, 'Failed to initiate shipment (Order must be in PACKING per BR-011)'),
  });

  const shipments = (data?.content || []).filter((s) => {
    if (!searchQuery.trim()) return true;
    const q = searchQuery.toLowerCase();
    return (
      s.id.toLowerCase().includes(q) ||
      s.orderId.toLowerCase().includes(q) ||
      (s.trackingCode && s.trackingCode.toLowerCase().includes(q))
    );
  });

  return (
    <div className="space-y-6">
      {/* Title & Top Controls */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-white">Fulfillment & Shipment Pipeline</h1>
          <p className="text-sm text-slate-400">
            Carrier coordination, tracking number validation, and packing SLA monitoring (API-FUL-001..003, BR-011)
          </p>
        </div>
        <div className="flex items-center gap-3">
          <button
            onClick={() => setIsInitiateModalOpen(true)}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold bg-indigo-600 hover:bg-indigo-500 text-white transition shadow-sm"
          >
            <PackageCheck className="h-4 w-4" />
            <span>Initiate Shipment</span>
          </button>
          <button
            onClick={() => refetch()}
            disabled={isFetching}
            className="flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs font-semibold bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 transition"
          >
            <RefreshCw className={`h-3.5 w-3.5 ${isFetching ? 'animate-spin' : ''}`} />
            <span>Sync Shipments</span>
          </button>
        </div>
      </div>

      {/* Tabs & Search */}
      <div className="flex flex-col gap-4">
        <div className="flex items-center gap-2 border-b border-slate-800 pb-2">
          <button
            onClick={() => setActiveTab('all')}
            className={`px-4 py-2 text-xs font-bold rounded-lg transition ${
              activeTab === 'all'
                ? 'bg-indigo-600 text-white shadow-sm shadow-indigo-500/20'
                : 'text-slate-400 hover:text-white hover:bg-slate-800'
            }`}
          >
            All Shipments
          </button>
          <button
            onClick={() => setActiveTab('stuck')}
            className={`flex items-center gap-2 px-4 py-2 text-xs font-bold rounded-lg transition ${
              activeTab === 'stuck'
                ? 'bg-rose-600 text-white shadow-sm shadow-rose-500/20'
                : 'text-slate-400 hover:text-white hover:bg-slate-800'
            }`}
          >
            <Clock className="h-3.5 w-3.5" />
            <span>Stuck Fulfillment SLA (&gt;60m)</span>
          </button>
        </div>

        <div className="p-4 rounded-xl border border-slate-800 bg-slate-900/90 backdrop-blur flex flex-col md:flex-row items-center justify-between gap-4">
          <div className="relative w-full md:w-80">
            <Search className="absolute left-3 top-2.5 h-4 w-4 text-slate-500" />
            <input
              type="text"
              placeholder="Search by Shipment ID, Order #, or Tracking..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full bg-slate-950 border border-slate-700/80 rounded-lg pl-9 pr-3 py-2 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-indigo-500"
            />
          </div>

          <div className="flex items-center gap-1.5 flex-wrap">
            {(['', 'PACKING', 'SHIPPED', 'DELIVERED', 'DELIVERY_FAILED', 'RETURNED'] as const).map((st) => (
              <button
                key={st}
                onClick={() => setStatusFilter(st)}
                className={`px-2.5 py-1 rounded-lg text-xs font-semibold transition ${
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
      </div>

      {/* Table */}
      {isLoading ? (
        <SkeletonTable rows={4} cols={5} />
      ) : shipments.length === 0 ? (
        <EmptyState
          icon={Truck}
          title="No Shipments Found"
          description="All packages have cleared the fulfillment pipeline."
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
                <th className="py-3 px-4">Shipment ID</th>
                <th className="py-3 px-4">Order Reference</th>
                <th className="py-3 px-4">Carrier & Tracking Code</th>
                <th className="py-3 px-4">Fulfillment Status</th>
                <th className="py-3 px-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/80">
              {shipments.map((shp) => (
                <tr
                  key={shp.id}
                  onClick={() => setSelectedShipment(shp)}
                  className="hover:bg-slate-900/50 cursor-pointer transition group"
                >
                  <td className="py-3 px-4">
                    <span className="font-mono text-xs font-bold text-indigo-400">{shp.id}</span>
                  </td>
                  <td className="py-3 px-4 font-mono text-xs text-slate-300">{shp.orderId}</td>
                  <td className="py-3 px-4">
                    <p className="text-xs font-bold text-white">{shp.carrierName || 'Unassigned Carrier'}</p>
                    <p className="font-mono text-[11px] text-slate-400">{shp.trackingCode || 'No Tracking Code'}</p>
                  </td>
                  <td className="py-3 px-4">
                    <StatusBadge status={shp.status} />
                  </td>
                  <td
                    className="py-3 px-4 text-right space-x-2"
                    onClick={(e) => e.stopPropagation()}
                  >
                    {shp.status !== 'DELIVERED' && (
                      <button
                        onClick={() => {
                          setSelectedShipment(shp);
                          setCarrierName(shp.carrierName || 'GIAO_HANG_NHANH');
                          setTrackingCode(shp.trackingCode || '');
                          setTargetStatus(shp.status === 'PACKING' ? 'SHIPPED' : 'DELIVERED');
                          setIsUpdateModalOpen(true);
                        }}
                        className="px-2.5 py-1 text-xs font-semibold bg-indigo-600 hover:bg-indigo-500 text-white rounded transition shadow-xs"
                      >
                        Update Courier (BR-011)
                      </button>
                    )}
                    <button
                      onClick={() => setSelectedShipment(shp)}
                      className="p-1 text-slate-400 hover:text-white rounded transition"
                      title="Inspect Shipment"
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

      {/* Slide-over Drawer for Shipment Details */}
      <SlideOverDrawer
        isOpen={!!selectedShipment}
        onClose={() => setSelectedShipment(null)}
        title={`Shipment Details #${selectedShipment?.id}`}
        subtitle={`Order: ${selectedShipment?.orderId}`}
        idToCopy={selectedShipment?.id}
        badge={selectedShipment ? <StatusBadge status={selectedShipment.status} /> : null}
        footerActions={
          selectedShipment && selectedShipment.status !== 'DELIVERED' ? (
            <button
              onClick={() => {
                setCarrierName(selectedShipment.carrierName || 'GIAO_HANG_NHANH');
                setTrackingCode(selectedShipment.trackingCode || '');
                setTargetStatus(selectedShipment.status === 'PACKING' ? 'SHIPPED' : 'DELIVERED');
                setIsUpdateModalOpen(true);
              }}
              className="px-4 py-2 text-xs font-semibold bg-indigo-600 hover:bg-indigo-500 text-white rounded-lg shadow-sm transition"
            >
              Update Courier Transition (BR-011)
            </button>
          ) : null
        }
      >
        {selectedShipment && (
          <div className="space-y-6">
            {/* Courier Tracking Summary */}
            <div className="p-4 rounded-xl bg-slate-950 border border-slate-800 space-y-3 text-xs">
              <div className="flex justify-between items-center">
                <span className="text-slate-400">Carrier Partner</span>
                <span className="font-bold text-white">{selectedShipment.carrierName || 'Unassigned'}</span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-slate-400">Tracking Code (BR-011)</span>
                <span className="font-mono font-bold text-indigo-400">
                  {selectedShipment.trackingCode || 'Pending Dispatch'}
                </span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-slate-400">Packing Timestamp</span>
                <span className="font-mono text-slate-300">
                  {selectedShipment.packedAt ? new Date(selectedShipment.packedAt).toLocaleString() : 'N/A'}
                </span>
              </div>
              {selectedShipment.shippedAt && (
                <div className="flex justify-between items-center">
                  <span className="text-slate-400">Shipped Timestamp</span>
                  <span className="font-mono text-indigo-300">
                    {new Date(selectedShipment.shippedAt).toLocaleString()}
                  </span>
                </div>
              )}
              {selectedShipment.deliveredAt && (
                <div className="flex justify-between items-center">
                  <span className="text-slate-400">Delivered Timestamp</span>
                  <span className="font-mono text-emerald-400">
                    {new Date(selectedShipment.deliveredAt).toLocaleString()}
                  </span>
                </div>
              )}
            </div>

            {/* Invariant Note */}
            <div className="p-4 rounded-xl border border-indigo-500/20 bg-indigo-500/5 text-xs text-indigo-300 space-y-1">
              <p className="font-bold flex items-center gap-1.5">
                <ShieldCheck className="h-4 w-4" />
                Dispatch Invariant Guard (BR-011)
              </p>
              <p className="text-[11px] text-slate-400 leading-relaxed">
                Before transitioning to SHIPPED, the backend strictly requires both a valid carrier identifier and an assigned tracking number. Transitioning to DELIVERED is terminal for shipment tracking.
              </p>
            </div>
          </div>
        )}
      </SlideOverDrawer>

      {/* Modal: Update Shipment Transition (BR-011) */}
      <Modal
        isOpen={isUpdateModalOpen}
        onClose={() => setIsUpdateModalOpen(false)}
        title="Update Shipment Status (BR-011 Guard)"
        subtitle="Mandatory carrier code and tracking number before SHIPPED"
        footerActions={
          <>
            <button
              onClick={() => setIsUpdateModalOpen(false)}
              className="px-4 py-2 text-xs font-semibold text-slate-400 hover:text-white"
            >
              Cancel
            </button>
            <button
              disabled={
                updateShipmentMutation.isPending ||
                (targetStatus === 'SHIPPED' && (!carrierName.trim() || !trackingCode.trim()))
              }
              onClick={() => {
                if (selectedShipment) {
                  updateShipmentMutation.mutate({
                    orderId: selectedShipment.orderId,
                    carrier_name: carrierName,
                    tracking_code: trackingCode,
                    target_status: targetStatus,
                  });
                }
              }}
              className="px-4 py-2 text-xs font-semibold bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 text-white rounded-lg transition"
            >
              {updateShipmentMutation.isPending ? 'Committing...' : `Set ${targetStatus}`}
            </button>
          </>
        }
      >
        <div className="space-y-4">
          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-300 uppercase">Target Status</label>
            <select
              value={targetStatus}
              onChange={(e) => setTargetStatus(e.target.value as ShipmentStatus)}
              className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white focus:outline-none focus:border-indigo-500 font-mono"
            >
              <option value="SHIPPED">SHIPPED - Handed over to logistics carrier</option>
              <option value="DELIVERED">DELIVERED - Confirmed delivered to customer</option>
              <option value="DELIVERY_FAILED">DELIVERY_FAILED - Delivery attempt unsuccessful</option>
              <option value="RETURNED">RETURNED - Package returned to warehouse</option>
            </select>
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-300 uppercase">Carrier Name</label>
            <select
              value={carrierName}
              onChange={(e) => setCarrierName(e.target.value)}
              className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white focus:outline-none focus:border-indigo-500"
            >
              <option value="GIAO_HANG_NHANH">Giao Hang Nhanh (GHN)</option>
              <option value="VIETTEL_POST">Viettel Post</option>
              <option value="GIAO_HANG_TIET_KIEM">Giao Hang Tiet Kiem (GHTK)</option>
              <option value="VNPOST">VNPost Express</option>
            </select>
          </div>

          <div className="space-y-1.5">
            <div className="flex items-center justify-between">
              <label className="text-xs font-semibold text-slate-300 uppercase">
                Tracking Number (Mandatory for SHIPPED per BR-011)
              </label>
              {targetStatus === 'SHIPPED' && <span className="text-[11px] text-rose-400">* Required</span>}
            </div>
            <input
              type="text"
              value={trackingCode}
              onChange={(e) => setTrackingCode(e.target.value)}
              placeholder="e.g. GHN-9988112-VN"
              className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white font-mono focus:outline-none focus:border-indigo-500"
              required={targetStatus === 'SHIPPED'}
            />
          </div>
        </div>
      </Modal>

      {/* Modal: Initiate Shipment */}
      <Modal
        isOpen={isInitiateModalOpen}
        onClose={() => setIsInitiateModalOpen(false)}
        title="Initiate Shipment for Order (BR-011)"
        subtitle="Order must be in PACKING state to create shipment record"
        footerActions={
          <>
            <button
              onClick={() => setIsInitiateModalOpen(false)}
              className="px-4 py-2 text-xs font-semibold text-slate-400 hover:text-white"
            >
              Cancel
            </button>
            <button
              disabled={initiateMutation.isPending || !initiateOrderId.trim()}
              onClick={() => initiateMutation.mutate(initiateOrderId)}
              className="px-4 py-2 text-xs font-semibold bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 text-white rounded-lg transition"
            >
              {initiateMutation.isPending ? 'Initiating...' : 'Create Shipment'}
            </button>
          </>
        }
      >
        <div className="space-y-4">
          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-300 uppercase">Target Order ID</label>
            <input
              type="text"
              value={initiateOrderId}
              onChange={(e) => setInitiateOrderId(e.target.value)}
              placeholder="e.g. ord-1002-9931"
              className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white font-mono focus:outline-none focus:border-indigo-500"
            />
          </div>
        </div>
      </Modal>
    </div>
  );
};
