'use client';

import React, { useState } from 'react';
import { X, AlertTriangle, Loader2 } from 'lucide-react';
import { Order } from '@/types';
import { apiClient } from '@/lib/api-client';
import { useToastStore } from '@/store/toast-store';

interface OrderCancelModalProps {
  order: Order;
  isOpen: boolean;
  onClose: () => void;
  onSuccess: (updatedOrder: Order) => void;
}

const CANCEL_REASONS = [
  'Tôi muốn đổi địa chỉ giao hàng',
  'Tôi muốn thay đổi sản phẩm / kích cỡ / màu sắc',
  'Tìm thấy giá tốt hơn ở nơi khác',
  'Thay đổi ý định không muốn mua nữa',
  'Thời gian giao hàng dự kiến quá lâu',
  'Lý do khác',
];

export function OrderCancelModal({
  order,
  isOpen,
  onClose,
  onSuccess,
}: OrderCancelModalProps) {
  const [selectedReason, setSelectedReason] = useState(CANCEL_REASONS[0]);
  const [customReason, setCustomReason] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const { showSuccess, showError } = useToastStore();

  if (!isOpen) return null;

  const handleCancelOrder = async () => {
    setIsSubmitting(true);
    const reasonText = selectedReason === 'Lý do khác' ? customReason : selectedReason;

    try {
      const updated = await apiClient<Order>(
        `/api/v1/customers/${order.customerId}/orders/${order.id}/cancel`,
        {
          method: 'POST',
          body: JSON.stringify({ reason: reasonText }),
        }
      );
      showSuccess('Hủy đơn hàng thành công', 'Tồn kho đã được giải phóng và hoàn tất hủy đơn.');
      onSuccess(updated);
      onClose();
    } catch (err) {
      showError('Không thể hủy đơn hàng', err);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-black/50 backdrop-blur-xs transition-opacity"
        onClick={onClose}
        aria-hidden="true"
      />

      {/* Modal Dialog */}
      <div className="relative bg-white rounded-2xl max-w-lg w-full p-6 shadow-2xl border border-zinc-200 z-10 animate-in fade-in zoom-in-95">
        <div className="flex items-start justify-between pb-3 border-b border-zinc-100">
          <div className="flex items-center gap-2 text-rose-600">
            <AlertTriangle className="w-5 h-5 shrink-0" />
            <h3 className="text-base font-bold text-zinc-900">Xác nhận Hủy Đơn Hàng</h3>
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded-lg text-zinc-400 hover:text-zinc-700 hover:bg-zinc-100 transition"
            aria-label="Đóng"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        <div className="mt-4 space-y-4 text-sm text-zinc-600">
          <p>
            Bạn có chắc chắn muốn hủy đơn hàng <strong>{order.orderNumber}</strong>?
          </p>

          <div className="bg-amber-50 border border-amber-200/80 rounded-xl p-3 text-xs text-amber-800 leading-relaxed">
            <strong>Lưu ý quan trọng:</strong> Hành động hủy đơn không thể hoàn tác (BR-001). Toàn bộ số lượng sản phẩm đang giữ chỗ sẽ được hoàn trả lại kho hàng.
          </div>

          <div className="space-y-2">
            <label className="text-xs font-bold text-zinc-900 uppercase tracking-wide block">
              Vui lòng chọn lý do hủy:
            </label>
            <select
              value={selectedReason}
              onChange={(e) => setSelectedReason(e.target.value)}
              className="w-full px-3.5 py-2.5 rounded-xl border border-zinc-200 bg-white text-xs sm:text-sm text-zinc-900 focus:outline-none focus:border-zinc-900"
            >
              {CANCEL_REASONS.map((r) => (
                <option key={r} value={r}>
                  {r}
                </option>
              ))}
            </select>

            {selectedReason === 'Lý do khác' && (
              <textarea
                placeholder="Nhập lý do chi tiết..."
                value={customReason}
                onChange={(e) => setCustomReason(e.target.value)}
                rows={3}
                className="w-full px-3.5 py-2.5 mt-2 rounded-xl border border-zinc-200 text-xs sm:text-sm text-zinc-900 focus:outline-none focus:border-zinc-900"
              />
            )}
          </div>
        </div>

        <div className="mt-6 pt-4 border-t border-zinc-100 flex items-center justify-end gap-3">
          <button
            onClick={onClose}
            disabled={isSubmitting}
            className="px-4 py-2 rounded-xl border border-zinc-200 text-xs font-semibold text-zinc-700 hover:bg-zinc-50 transition"
          >
            Đóng
          </button>
          <button
            onClick={handleCancelOrder}
            disabled={isSubmitting || (selectedReason === 'Lý do khác' && !customReason.trim())}
            className="px-4 py-2 rounded-xl bg-rose-600 hover:bg-rose-700 text-white text-xs font-semibold flex items-center gap-1.5 shadow-sm transition disabled:opacity-50"
          >
            {isSubmitting ? (
              <>
                <Loader2 className="w-3.5 h-3.5 animate-spin" />
                <span>Đang xử lý...</span>
              </>
            ) : (
              <span>Xác nhận Hủy Đơn</span>
            )}
          </button>
        </div>
      </div>
    </div>
  );
}
