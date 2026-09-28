'use client';

import React, { useState, useEffect } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import {
  ShieldCheck,
  CreditCard,
  MapPin,
  CheckCircle2,
  AlertTriangle,
  Loader2,
  Lock,
  Plus,
  ArrowRight,
  Info,
} from 'lucide-react';
import { useCartStore, FREE_SHIPPING_THRESHOLD } from '@/store/cart-store';
import { useUserStore } from '@/store/user-store';
import { useToastStore } from '@/store/toast-store';
import { apiClient, ApiClientError } from '@/lib/api-client';
import { Order, CustomerAddress } from '@/types';
import { formatCurrency, generateUUID } from '@/lib/utils';

export default function CheckoutPage() {
  const router = useRouter();
  const { items, subtotal, clearCart } = useCartStore();
  const { user, isAuthenticated, addresses, addAddress } = useUserStore();
  const { showError, showSuccess } = useToastStore();

  const [selectedAddressId, setSelectedAddressId] = useState<string>('');
  const [paymentMethod, setPaymentMethod] = useState<'MOCK_GATEWAY' | 'VNPAY' | 'COD'>('MOCK_GATEWAY');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [stockContentionError, setStockContentionError] = useState<string | null>(null);

  // Quick new address state
  const [showNewAddressForm, setShowNewAddressForm] = useState(false);
  const [newRecipientName, setNewRecipientName] = useState('');
  const [newPhone, setNewPhone] = useState('');
  const [newLine1, setNewLine1] = useState('');
  const [newWard, setNewWard] = useState('');
  const [newDistrict, setNewDistrict] = useState('');
  const [newCity, setNewCity] = useState('');

  // Default address selection
  useEffect(() => {
    if (addresses.length > 0 && !selectedAddressId) {
      const defaultAddr = addresses.find((a) => a.isDefault) || addresses[0];
      setSelectedAddressId(defaultAddr.id);
    }
  }, [addresses, selectedAddressId]);

  const isFreeShipping = subtotal >= FREE_SHIPPING_THRESHOLD;
  const shippingFee = isFreeShipping || items.length === 0 ? 0 : 30000;
  const grandTotal = subtotal + shippingFee;

  const handleCreateNewAddress = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newRecipientName || !newPhone || !newLine1 || !newDistrict || !newCity) {
      return;
    }
    try {
      const created = await addAddress({
        recipientName: newRecipientName,
        phone: newPhone,
        line1: newLine1,
        ward: newWard || 'Phường 1',
        district: newDistrict,
        city: newCity,
        isDefault: addresses.length === 0,
      });
      setSelectedAddressId(created.id);
      setShowNewAddressForm(false);
      showSuccess('Đã thêm địa chỉ giao hàng mới');
    } catch (err) {
      showError('Lỗi thêm địa chỉ', err);
    }
  };

  // Check login requirement (BR-009)
  if (!isAuthenticated || !user) {
    return (
      <div className="max-w-md mx-auto py-16 text-center space-y-4">
        <div className="w-16 h-16 rounded-full bg-zinc-100 flex items-center justify-center mx-auto text-zinc-900">
          <Lock className="w-8 h-8" />
        </div>
        <h2 className="text-2xl font-bold text-zinc-900">Yêu cầu đăng nhập</h2>
        <p className="text-sm text-zinc-600 leading-relaxed">
          Theo quy định bảo mật và quản lý giao dịch đơn hàng (BR-009), bạn cần đăng nhập tài khoản trước khi tiến hành thanh toán.
        </p>
        <div className="pt-4 flex flex-col gap-2">
          <Link
            href="/auth/login?returnUrl=/checkout"
            className="w-full py-3 px-4 rounded-xl bg-zinc-900 hover:bg-zinc-800 text-white font-semibold text-sm transition"
          >
            Đăng nhập ngay
          </Link>
          <Link
            href="/auth/register?returnUrl=/checkout"
            className="w-full py-3 px-4 rounded-xl border border-zinc-200 text-zinc-800 font-semibold text-sm hover:bg-zinc-50 transition"
          >
            Đăng ký tài khoản mới
          </Link>
        </div>
      </div>
    );
  }

  if (items.length === 0) {
    return (
      <div className="max-w-md mx-auto py-16 text-center space-y-4">
        <h2 className="text-xl font-bold text-zinc-900">Giỏ hàng trống</h2>
        <p className="text-sm text-zinc-500">Bạn chưa có sản phẩm nào để thanh toán.</p>
        <Link
          href="/products"
          className="inline-block py-2.5 px-6 rounded-xl bg-zinc-900 text-white text-xs font-semibold"
        >
          Khám phá sản phẩm
        </Link>
      </div>
    );
  }

  const handlePlaceOrder = async () => {
    if (!selectedAddressId) {
      showError('Vui lòng chọn địa chỉ giao hàng');
      return;
    }

    setIsSubmitting(true);
    setStockContentionError(null);

    // BẮT BUỘC sinh UUID Idempotency-Key (BR-010, NFR-IDEMPOTENCY-001)
    const idempotencyKey = generateUUID();

    try {
      const order = await apiClient<Order>(`/api/v1/customers/${user.id}/orders`, {
        method: 'POST',
        headers: {
          'Idempotency-Key': idempotencyKey,
        },
        body: JSON.stringify({
          address_id: selectedAddressId,
          items: items.map((i) => ({
            skuId: i.skuId,
            skuCode: i.skuCode,
            productName: i.productName,
            productImage: i.productImage,
            attributes: i.attributes,
            quantity: i.quantity,
            unitPrice: i.unitPrice,
            totalPrice: i.totalPrice,
          })),
          payment_method: paymentMethod,
        }),
      });

      // Clear client cart on order creation
      clearCart();

      // Navigate to payment simulation screen
      router.push(`/checkout/payment/${order.id}`);
    } catch (err: unknown) {
      if (err instanceof ApiClientError && err.problem.code === 'INSUFFICIENT_STOCK') {
        // Atomic Reservation Contention
        setStockContentionError(
          err.problem.detail || 'Một hoặc nhiều sản phẩm trong giỏ hàng vừa hết hàng hoặc không đủ tồn kho khả dụng.'
        );
      } else {
        showError('Không thể tạo đơn hàng', err);
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="space-y-8">
      {/* Page Title */}
      <div className="border-b border-zinc-200 pb-4">
        <h1 className="text-2xl sm:text-3xl font-bold tracking-tight text-zinc-900">
          Thanh toán đơn hàng
        </h1>
        <p className="text-xs sm:text-sm text-zinc-500 mt-1">
          Hoàn tất thông tin nhận hàng và phương thức thanh toán an toàn
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 items-start">
        {/* Main Form: Address + Payment */}
        <div className="lg:col-span-2 space-y-6">
          {/* Section 1: Shipping Address */}
          <div className="bg-white rounded-2xl border border-zinc-200/80 p-5 sm:p-6 shadow-xs space-y-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <MapPin className="w-5 h-5 text-zinc-900" />
                <h2 className="text-base font-bold text-zinc-900">1. Địa chỉ giao hàng</h2>
              </div>
              <button
                onClick={() => setShowNewAddressForm(!showNewAddressForm)}
                className="text-xs font-semibold text-zinc-900 hover:text-zinc-600 flex items-center gap-1"
              >
                <Plus className="w-3.5 h-3.5" />
                <span>Thêm địa chỉ mới</span>
              </button>
            </div>

            {/* BR-017 Callout: Address Snapshot Isolation Guarantee */}
            <div className="flex items-start gap-2.5 p-3 rounded-xl bg-zinc-50 border border-zinc-200/80 text-xs text-zinc-600">
              <Info className="w-4 h-4 text-zinc-500 shrink-0 mt-0.5" />
              <p>
                <strong>Bảo đảm Bất biến BR-017:</strong> Địa chỉ bạn chọn sẽ được sao chép cố định (snapshot) cho đơn hàng này ngay khi đặt. Các chỉnh sửa địa chỉ sau này trong sổ địa chỉ của bạn sẽ không làm thay đổi đơn hàng đã tạo.
              </p>
            </div>

            {/* Saved Addresses List */}
            {addresses.length > 0 && !showNewAddressForm && (
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-2">
                {addresses.map((addr) => {
                  const isSelected = selectedAddressId === addr.id;
                  return (
                    <div
                      key={addr.id}
                      onClick={() => setSelectedAddressId(addr.id)}
                      className={`p-3.5 rounded-xl border-2 cursor-pointer transition relative ${
                        isSelected
                          ? 'border-zinc-900 bg-zinc-50/50 shadow-xs'
                          : 'border-zinc-200 bg-white hover:border-zinc-300'
                      }`}
                    >
                      <div className="flex items-start justify-between">
                        <span className="text-xs font-bold text-zinc-900">{addr.recipientName}</span>
                        {isSelected && <CheckCircle2 className="w-4 h-4 text-zinc-900 shrink-0" />}
                      </div>
                      <p className="text-xs text-zinc-500 mt-1">{addr.phone}</p>
                      <p className="text-xs text-zinc-700 mt-1 line-clamp-2">
                        {addr.line1}, {addr.ward}, {addr.district}, {addr.city}
                      </p>
                      {addr.isDefault && (
                        <span className="inline-block mt-2 text-[10px] font-semibold text-zinc-500 uppercase tracking-wide">
                          Mặc định
                        </span>
                      )}
                    </div>
                  );
                })}
              </div>
            )}

            {/* New Address Form (Toggleable) */}
            {showNewAddressForm && (
              <form onSubmit={handleCreateNewAddress} className="space-y-3 pt-2">
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <div>
                    <label className="text-xs font-medium text-zinc-700 block mb-1">Họ tên người nhận *</label>
                    <input
                      type="text"
                      required
                      value={newRecipientName}
                      onChange={(e) => setNewRecipientName(e.target.value)}
                      placeholder="Nguyễn Văn A"
                      className="w-full px-3 py-2 rounded-xl border border-zinc-200 text-xs text-zinc-900 focus:outline-none focus:border-zinc-900"
                    />
                  </div>
                  <div>
                    <label className="text-xs font-medium text-zinc-700 block mb-1">Số điện thoại *</label>
                    <input
                      type="tel"
                      required
                      value={newPhone}
                      onChange={(e) => setNewPhone(e.target.value)}
                      placeholder="0912345678"
                      className="w-full px-3 py-2 rounded-xl border border-zinc-200 text-xs text-zinc-900 focus:outline-none focus:border-zinc-900"
                    />
                  </div>
                </div>

                <div>
                  <label className="text-xs font-medium text-zinc-700 block mb-1">Địa chỉ chi tiết (Số nhà, tên đường) *</label>
                  <input
                    type="text"
                    required
                    value={newLine1}
                    onChange={(e) => setNewLine1(e.target.value)}
                    placeholder="Số 123 Đường Nguyễn Huệ"
                    className="w-full px-3 py-2 rounded-xl border border-zinc-200 text-xs text-zinc-900 focus:outline-none focus:border-zinc-900"
                  />
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                  <div>
                    <label className="text-xs font-medium text-zinc-700 block mb-1">Phường / Xã</label>
                    <input
                      type="text"
                      value={newWard}
                      onChange={(e) => setNewWard(e.target.value)}
                      placeholder="Phường Bến Nghé"
                      className="w-full px-3 py-2 rounded-xl border border-zinc-200 text-xs text-zinc-900 focus:outline-none focus:border-zinc-900"
                    />
                  </div>
                  <div>
                    <label className="text-xs font-medium text-zinc-700 block mb-1">Quận / Huyện *</label>
                    <input
                      type="text"
                      required
                      value={newDistrict}
                      onChange={(e) => setNewDistrict(e.target.value)}
                      placeholder="Quận 1"
                      className="w-full px-3 py-2 rounded-xl border border-zinc-200 text-xs text-zinc-900 focus:outline-none focus:border-zinc-900"
                    />
                  </div>
                  <div>
                    <label className="text-xs font-medium text-zinc-700 block mb-1">Tỉnh / Thành phố *</label>
                    <input
                      type="text"
                      required
                      value={newCity}
                      onChange={(e) => setNewCity(e.target.value)}
                      placeholder="Hồ Chí Minh"
                      className="w-full px-3 py-2 rounded-xl border border-zinc-200 text-xs text-zinc-900 focus:outline-none focus:border-zinc-900"
                    />
                  </div>
                </div>

                <div className="flex gap-2 pt-2">
                  <button
                    type="submit"
                    className="px-4 py-2 rounded-xl bg-zinc-900 text-white text-xs font-semibold hover:bg-zinc-800 transition"
                  >
                    Lưu địa chỉ
                  </button>
                  <button
                    type="button"
                    onClick={() => setShowNewAddressForm(false)}
                    className="px-4 py-2 rounded-xl border border-zinc-200 text-zinc-600 text-xs font-semibold hover:bg-zinc-50 transition"
                  >
                    Hủy
                  </button>
                </div>
              </form>
            )}
          </div>

          {/* Section 2: Payment Method */}
          <div className="bg-white rounded-2xl border border-zinc-200/80 p-5 sm:p-6 shadow-xs space-y-4">
            <div className="flex items-center gap-2">
              <CreditCard className="w-5 h-5 text-zinc-900" />
              <h2 className="text-base font-bold text-zinc-900">2. Phương thức thanh toán</h2>
            </div>

            <div className="space-y-3 pt-2">
              <label
                className={`flex items-start gap-3 p-4 rounded-xl border-2 cursor-pointer transition ${
                  paymentMethod === 'MOCK_GATEWAY'
                    ? 'border-zinc-900 bg-zinc-50/50'
                    : 'border-zinc-200 hover:border-zinc-300'
                }`}
              >
                <input
                  type="radio"
                  name="payment"
                  checked={paymentMethod === 'MOCK_GATEWAY'}
                  onChange={() => setPaymentMethod('MOCK_GATEWAY')}
                  className="mt-1 w-4 h-4 text-zinc-900 focus:ring-zinc-900"
                />
                <div>
                  <div className="flex items-center gap-2">
                    <span className="text-sm font-bold text-zinc-900">
                      Cổng thanh toán Mô phỏng (Payment Gateway Sandbox)
                    </span>
                    <span className="px-2 py-0.5 rounded text-[10px] font-semibold bg-emerald-50 text-emerald-700">
                      Khuyên dùng
                    </span>
                  </div>
                  <p className="text-xs text-zinc-500 mt-1 leading-relaxed">
                    Mô phỏng quy trình xử lý thanh toán thực tế với đầy đủ callback SUCCESS, FAILED, TIMEOUT và cơ chế chống trùng lặp BR-002.
                  </p>
                </div>
              </label>

              <label
                className={`flex items-start gap-3 p-4 rounded-xl border-2 cursor-pointer transition ${
                  paymentMethod === 'VNPAY'
                    ? 'border-zinc-900 bg-zinc-50/50'
                    : 'border-zinc-200 hover:border-zinc-300'
                }`}
              >
                <input
                  type="radio"
                  name="payment"
                  checked={paymentMethod === 'VNPAY'}
                  onChange={() => setPaymentMethod('VNPAY')}
                  className="mt-1 w-4 h-4 text-zinc-900 focus:ring-zinc-900"
                />
                <div>
                  <span className="text-sm font-bold text-zinc-900">VNPay QR / Thẻ nội địa ATM</span>
                  <p className="text-xs text-zinc-500 mt-1">
                    Thanh toán an toàn qua cổng VNPay điện tử.
                  </p>
                </div>
              </label>

              <label
                className={`flex items-start gap-3 p-4 rounded-xl border-2 cursor-pointer transition ${
                  paymentMethod === 'COD'
                    ? 'border-zinc-900 bg-zinc-50/50'
                    : 'border-zinc-200 hover:border-zinc-300'
                }`}
              >
                <input
                  type="radio"
                  name="payment"
                  checked={paymentMethod === 'COD'}
                  onChange={() => setPaymentMethod('COD')}
                  className="mt-1 w-4 h-4 text-zinc-900 focus:ring-zinc-900"
                />
                <div>
                  <span className="text-sm font-bold text-zinc-900">Thanh toán khi nhận hàng (COD)</span>
                  <p className="text-xs text-zinc-500 mt-1">
                    Kiểm tra hàng và thanh toán tiền mặt trực tiếp cho nhân viên giao vận.
                  </p>
                </div>
              </label>
            </div>
          </div>
        </div>

        {/* Order Summary & Idempotent Submit */}
        <div className="lg:col-span-1 space-y-4">
          <div className="bg-white rounded-2xl border border-zinc-200/80 p-5 sm:p-6 shadow-xs space-y-5">
            <h3 className="text-base font-bold text-zinc-900 border-b border-zinc-100 pb-3">
              Chi tiết đơn hàng ({items.length} món)
            </h3>

            {/* Items mini list */}
            <div className="max-h-56 overflow-y-auto divide-y divide-zinc-100 pr-1">
              {items.map((item) => (
                <div key={item.id} className="py-2.5 flex items-center justify-between text-xs">
                  <div className="flex-1 min-w-0 pr-2">
                    <p className="font-semibold text-zinc-900 truncate">{item.productName}</p>
                    <p className="text-[11px] text-zinc-400">SL: {item.quantity}</p>
                  </div>
                  <span className="font-bold text-zinc-900 shrink-0">
                    {formatCurrency(item.totalPrice)}
                  </span>
                </div>
              ))}
            </div>

            {/* Financial breakdown */}
            <div className="space-y-2.5 text-xs sm:text-sm border-t border-zinc-100 pt-3">
              <div className="flex justify-between text-zinc-600">
                <span>Tạm tính:</span>
                <span className="font-semibold text-zinc-900">{formatCurrency(subtotal)}</span>
              </div>
              <div className="flex justify-between text-zinc-600">
                <span>Phí vận chuyển:</span>
                <span>
                  {shippingFee === 0 ? (
                    <span className="text-emerald-600 font-semibold uppercase text-xs">Miễn phí</span>
                  ) : (
                    formatCurrency(shippingFee)
                  )}
                </span>
              </div>
              <div className="pt-3 border-t border-zinc-200 flex justify-between items-baseline">
                <span className="text-sm font-bold text-zinc-900">Tổng thanh toán:</span>
                <span className="text-xl font-black text-zinc-900">{formatCurrency(grandTotal)}</span>
              </div>
            </div>

            {/* Submit button with Idempotency header */}
            <button
              onClick={handlePlaceOrder}
              disabled={isSubmitting || !selectedAddressId}
              className="w-full py-3.5 px-4 rounded-xl bg-zinc-900 hover:bg-zinc-800 text-white text-sm font-bold flex items-center justify-center gap-2 shadow-md transition disabled:opacity-50"
            >
              {isSubmitting ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" />
                  <span>Đang xử lý đặt hàng...</span>
                </>
              ) : (
                <>
                  <span>Xác nhận Đặt hàng</span>
                  <ArrowRight className="w-4 h-4" />
                </>
              )}
            </button>

            <div className="text-[10px] text-zinc-400 font-mono text-center">
              Header Idempotency-Key được sinh tự động chống trùng lặp (NFR-IDEMPOTENCY-001)
            </div>
          </div>
        </div>
      </div>

      {/* Atomic Reservation Contention Error Modal */}
      {stockContentionError && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div
            className="fixed inset-0 bg-black/50 backdrop-blur-xs"
            onClick={() => setStockContentionError(null)}
          />
          <div className="relative bg-white rounded-2xl max-w-md w-full p-6 shadow-2xl border border-zinc-200 z-10 space-y-4 animate-in fade-in zoom-in-95">
            <div className="flex items-center gap-2.5 text-rose-600">
              <AlertTriangle className="w-6 h-6 shrink-0" />
              <h3 className="text-base font-bold text-zinc-900">Lỗi giữ chỗ tồn kho (Contention)</h3>
            </div>
            <p className="text-xs sm:text-sm text-zinc-600 leading-relaxed">
              {stockContentionError}
            </p>
            <p className="text-xs text-zinc-500">
              Do nhiều khách hàng cùng lúc mua sắm, số lượng sản phẩm khả dụng trong kho đã thay đổi. Vui lòng quay lại giỏ hàng để cập nhật số lượng.
            </p>
            <div className="pt-2 flex gap-3">
              <button
                onClick={() => setStockContentionError(null)}
                className="flex-1 py-2 px-3 rounded-xl border border-zinc-200 text-xs font-semibold text-zinc-700 hover:bg-zinc-50"
              >
                Đóng
              </button>
              <Link
                href="/cart"
                className="flex-1 py-2 px-3 rounded-xl bg-zinc-900 text-white text-xs font-semibold text-center hover:bg-zinc-800"
              >
                Về giỏ hàng cập nhật
              </Link>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
