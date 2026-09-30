'use client';

import React, { useState, useEffect, useRef } from 'react';
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
import { useCartStore } from '@/store/cart-store';
import { useUserStore } from '@/store/user-store';
import { useToastStore } from '@/store/toast-store';
import { apiClient, ApiClientError } from '@/lib/api-client';
import { Order, CustomerAddress, PaymentTransaction } from '@/types';
import { formatCurrency, generateUUID } from '@/lib/utils';

export default function CheckoutPage() {
  const router = useRouter();
  const {
    items,
    subtotal,
    cartId,
    clearCart,
    isHydrated,
    loadCart,
    freeShippingThreshold,
    standardShippingFee,
  } = useCartStore();
  const { user, isAuthenticated, addresses, addAddress } = useUserStore();
  const { showError, showSuccess, showWarning } = useToastStore();

  // STT 2: checkoutSessionKey fixed for this checkout session and reused upon retry (BR-010, NFR-IDEMPOTENCY-001)
  const [checkoutSessionKey] = useState(() => {
    if (typeof window === 'undefined') return generateUUID();
    const STORAGE_KEY = 'checkout_idempotency_key';
    const existing = sessionStorage.getItem(STORAGE_KEY);
    if (existing) return existing;
    const newKey = generateUUID();
    sessionStorage.setItem(STORAGE_KEY, newKey);
    return newKey;
  });

  const [selectedAddressId, setSelectedAddressId] = useState<string>('');
  const [paymentMethod, setPaymentMethod] = useState<'MOCK_GATEWAY' | 'VNPAY'>('MOCK_GATEWAY');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [orderErrorMessage, setOrderErrorMessage] = useState<string | null>(null);
  const [stockContentionError, setStockContentionError] = useState<{ code: string; detail: string } | null>(null);

  // Quick new address state
  const [showNewAddressForm, setShowNewAddressForm] = useState(false);
  const [newRecipientName, setNewRecipientName] = useState('');
  const [newPhone, setNewPhone] = useState('');
  const [newLine1, setNewLine1] = useState('');
  const [newWard, setNewWard] = useState('');
  const [newDistrict, setNewDistrict] = useState('');
  const [newCity, setNewCity] = useState('');
  const [addressFormErrors, setAddressFormErrors] = useState<Record<string, string>>({});

  const hasRedirectedRef = useRef(false);

  // STT 15: Auto-redirect to cart if hydrated and cart is empty
  useEffect(() => {
    if (isHydrated && items.length === 0 && !hasRedirectedRef.current) {
      hasRedirectedRef.current = true;
      useToastStore.getState().showWarning(
        'Giỏ hàng trống',
        'Giỏ hàng của bạn đang trống, vui lòng chọn sản phẩm trước khi thanh toán.'
      );
      if (typeof router?.replace === 'function') {
        router.replace('/cart');
      } else if (typeof router?.push === 'function') {
        router.push('/cart');
      }
    }
  }, [isHydrated, items.length, router]);

  // Default address selection
  useEffect(() => {
    if (addresses.length > 0 && !selectedAddressId) {
      const defaultAddr = addresses.find((a) => a.isDefault) || addresses[0];
      setSelectedAddressId(defaultAddr.id);
    }
  }, [addresses, selectedAddressId]);

  // STT 12: Dynamic freeship and standard shipping fee calculation
  const threshold = freeShippingThreshold || 500000;
  const standardFee = standardShippingFee || 30000;
  const isFreeShipping = subtotal >= threshold;
  const shippingFee = isFreeShipping || items.length === 0 ? 0 : standardFee;
  const grandTotal = subtotal + shippingFee;

  // STT 7: Validated address creation
  const handleCreateNewAddress = async (e: React.FormEvent) => {
    e.preventDefault();
    const errors: Record<string, string> = {};

    const trimmedName = newRecipientName.trim();
    if (!trimmedName || trimmedName.length < 2 || trimmedName.length > 100) {
      errors.recipientName = 'Họ tên người nhận phải từ 2 đến 100 ký tự';
    }

    const vnPhoneRegex = /^(0|\+84)(3|5|7|8|9)[0-9]{8}$/;
    const trimmedPhone = newPhone.trim();
    if (!vnPhoneRegex.test(trimmedPhone)) {
      errors.phone = 'Số điện thoại di động Việt Nam không đúng định dạng (VD: 0912345678 hoặc +84912345678)';
    }

    const trimmedLine1 = newLine1.trim();
    if (!trimmedLine1 || trimmedLine1.length < 5) {
      errors.line1 = 'Địa chỉ chi tiết tối thiểu 5 ký tự';
    }

    const trimmedDistrict = newDistrict.trim();
    if (!trimmedDistrict) {
      errors.district = 'Vui lòng nhập quận / huyện';
    }

    const trimmedCity = newCity.trim();
    if (!trimmedCity) {
      errors.city = 'Vui lòng nhập tỉnh / thành phố';
    }

    if (Object.keys(errors).length > 0) {
      setAddressFormErrors(errors);
      showError('Thông tin địa chỉ không hợp lệ', Object.values(errors)[0]);
      return;
    }

    setAddressFormErrors({});

    try {
      const created = await addAddress({
        recipientName: trimmedName,
        phone: trimmedPhone,
        line1: trimmedLine1,
        ward: newWard.trim() || '',
        district: trimmedDistrict,
        city: trimmedCity,
        isDefault: addresses.length === 0,
      });
      setSelectedAddressId(created.id);
      setShowNewAddressForm(false);
      setNewRecipientName('');
      setNewPhone('');
      setNewLine1('');
      setNewWard('');
      setNewDistrict('');
      setNewCity('');
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

  // STT 1 & STT 2: handlePlaceOrder without client-side price tampering
  const handlePlaceOrder = async () => {
    if (!selectedAddressId) {
      showError('Vui lòng chọn địa chỉ giao hàng');
      return;
    }

    setIsSubmitting(true);
    setStockContentionError(null);
    setOrderErrorMessage(null);

    try {
      // STT 1: Client sends ONLY address_id, cart_id, payment_method.
      // Prices and items are calculated and snapshotted authoritatively by Order Service.
      const order = await apiClient<Order>(`/api/v1/customers/${user.id}/orders`, {
        method: 'POST',
        headers: {
          'Idempotency-Key': checkoutSessionKey,
        },
        body: JSON.stringify({
          address_id: selectedAddressId,
          cart_id: cartId || 'cart-demo-001',
          payment_method: paymentMethod,
        }),
      });

      // Clear client cart on order creation
      await clearCart(user.id);

      // FIX-M1: Clear idempotency key from sessionStorage after success
      if (typeof window !== 'undefined') {
        sessionStorage.removeItem('checkout_idempotency_key');
      }

      // FIX-C1: Initiate payment automatically right after Order creation (Section 9 Step 1)
      try {
        await apiClient<PaymentTransaction>('/api/v1/payments/initiate', {
          method: 'POST',
          body: JSON.stringify({
            order_id: order.id,
            amount: order.grandTotalAmount,
            currency: order.currency,
            payment_method: paymentMethod,
          }),
        });
      } catch {
        // Non-blocking: nếu initiate fail, vẫn redirect để user thấy payment sandbox
      }

      // STT 6: Payment method branching
      if (paymentMethod === 'VNPAY') {
        showSuccess('Chuyển hướng VNPay', 'Đang chuyển hướng đến cổng thanh toán VNPay Sandbox...');
        router.push(`/checkout/payment/${order.id}?gateway=vnpay`);
      } else {
        router.push(`/checkout/payment/${order.id}`);
      }
    } catch (err: unknown) {
      if (err instanceof ApiClientError && err.problem.code === 'INSUFFICIENT_STOCK') {
        // STT 4: Atomic Reservation Contention (422)
        setStockContentionError({
          code: err.problem.code,
          detail: err.problem.detail || 'Một hoặc nhiều sản phẩm trong giỏ hàng vừa hết hàng hoặc không đủ tồn kho khả dụng.',
        });
        // Trigger cart reload to adjust or remove conflicting SKUs
        await loadCart(user.id);
      } else {
        const msg = err instanceof Error ? err.message : 'Không thể tạo đơn hàng';
        setOrderErrorMessage(msg);
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
                <span>{showNewAddressForm ? 'Đóng biểu mẫu' : 'Thêm địa chỉ mới'}</span>
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

            {/* New Address Form (Toggleable with STT 7 validation) */}
            {showNewAddressForm && (
              <form onSubmit={handleCreateNewAddress} className="space-y-3 pt-2">
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <div>
                    <label className="text-xs font-medium text-zinc-700 block mb-1">
                      Họ tên người nhận *
                    </label>
                    <input
                      type="text"
                      value={newRecipientName}
                      onChange={(e) => setNewRecipientName(e.target.value)}
                      placeholder="Nguyễn Văn An"
                      className={`w-full px-3 py-2 rounded-xl border text-xs text-zinc-900 focus:outline-none transition ${
                        addressFormErrors.recipientName
                          ? 'border-rose-500 focus:border-rose-600 bg-rose-50/20'
                          : 'border-zinc-200 focus:border-zinc-900'
                      }`}
                    />
                    {addressFormErrors.recipientName && (
                      <p className="text-[11px] text-rose-600 mt-1">{addressFormErrors.recipientName}</p>
                    )}
                  </div>
                  <div>
                    <label className="text-xs font-medium text-zinc-700 block mb-1">
                      Số điện thoại di động *
                    </label>
                    <input
                      type="tel"
                      value={newPhone}
                      onChange={(e) => setNewPhone(e.target.value)}
                      placeholder="0912345678"
                      className={`w-full px-3 py-2 rounded-xl border text-xs text-zinc-900 focus:outline-none transition ${
                        addressFormErrors.phone
                          ? 'border-rose-500 focus:border-rose-600 bg-rose-50/20'
                          : 'border-zinc-200 focus:border-zinc-900'
                      }`}
                    />
                    {addressFormErrors.phone && (
                      <p className="text-[11px] text-rose-600 mt-1">{addressFormErrors.phone}</p>
                    )}
                  </div>
                </div>

                <div>
                  <label className="text-xs font-medium text-zinc-700 block mb-1">
                    Địa chỉ chi tiết (Số nhà, tên đường) *
                  </label>
                  <input
                    type="text"
                    value={newLine1}
                    onChange={(e) => setNewLine1(e.target.value)}
                    placeholder="Số 123 Đường Lê Lợi"
                    className={`w-full px-3 py-2 rounded-xl border text-xs text-zinc-900 focus:outline-none transition ${
                      addressFormErrors.line1
                        ? 'border-rose-500 focus:border-rose-600 bg-rose-50/20'
                        : 'border-zinc-200 focus:border-zinc-900'
                    }`}
                  />
                  {addressFormErrors.line1 && (
                    <p className="text-[11px] text-rose-600 mt-1">{addressFormErrors.line1}</p>
                  )}
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
                      value={newDistrict}
                      onChange={(e) => setNewDistrict(e.target.value)}
                      placeholder="Quận 1"
                      className={`w-full px-3 py-2 rounded-xl border text-xs text-zinc-900 focus:outline-none transition ${
                        addressFormErrors.district
                          ? 'border-rose-500 focus:border-rose-600 bg-rose-50/20'
                          : 'border-zinc-200 focus:border-zinc-900'
                      }`}
                    />
                    {addressFormErrors.district && (
                      <p className="text-[11px] text-rose-600 mt-1">{addressFormErrors.district}</p>
                    )}
                  </div>
                  <div>
                    <label className="text-xs font-medium text-zinc-700 block mb-1">Tỉnh / Thành phố *</label>
                    <input
                      type="text"
                      value={newCity}
                      onChange={(e) => setNewCity(e.target.value)}
                      placeholder="Hồ Chí Minh"
                      className={`w-full px-3 py-2 rounded-xl border text-xs text-zinc-900 focus:outline-none transition ${
                        addressFormErrors.city
                          ? 'border-rose-500 focus:border-rose-600 bg-rose-50/20'
                          : 'border-zinc-200 focus:border-zinc-900'
                      }`}
                    />
                    {addressFormErrors.city && (
                      <p className="text-[11px] text-rose-600 mt-1">{addressFormErrors.city}</p>
                    )}
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
                    onClick={() => {
                      setShowNewAddressForm(false);
                      setAddressFormErrors({});
                    }}
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
                  <div className="flex items-center gap-2">
                    <span className="text-sm font-bold text-zinc-900">VNPay QR / Thẻ nội địa ATM</span>
                    <span className="px-1.5 py-0.5 rounded text-[10px] font-semibold bg-blue-50 text-blue-600">Sandbox</span>
                  </div>
                  <p className="text-xs text-zinc-500 mt-1">
                    Thanh toán mô phỏng qua cổng VNPay Sandbox (không xử lý tiền thực).
                  </p>
                </div>
              </label>

              {/* COD — Out of scope for MVP per SRS ASM-003. Disabled. */}
              <label className="flex items-start gap-3 p-4 rounded-xl border-2 border-zinc-100 bg-zinc-50 opacity-50 cursor-not-allowed">
                <input type="radio" name="payment" disabled className="mt-1 w-4 h-4" />
                <div>
                  <div className="flex items-center gap-2">
                    <span className="text-sm font-bold text-zinc-400">Thanh toán khi nhận hàng (COD)</span>
                    <span className="px-2 py-0.5 rounded text-[10px] font-semibold bg-zinc-100 text-zinc-400">Sắp ra mắt</span>
                  </div>
                  <p className="text-xs text-zinc-400 mt-1">Tính năng đang phát triển (Phase 2).</p>
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

            {orderErrorMessage && (
              <div className="p-3.5 bg-rose-50 border border-rose-200 rounded-xl text-xs text-rose-700 flex items-start gap-2 animate-in fade-in">
                <AlertTriangle className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
                <p>{orderErrorMessage}</p>
              </div>
            )}

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

      {/* STT 4: Atomic Reservation Contention Error Modal */}
      {stockContentionError && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div
            className="fixed inset-0 bg-black/50 backdrop-blur-xs"
            onClick={() => setStockContentionError(null)}
          />
          <div className="relative bg-white rounded-2xl max-w-md w-full p-6 shadow-2xl border border-zinc-200 z-10 space-y-4 animate-in fade-in zoom-in-95">
            <div className="flex items-center gap-2.5 text-rose-600">
              <AlertTriangle className="w-6 h-6 shrink-0" />
              <div>
                <h3 className="text-base font-bold text-zinc-900">Lỗi giữ chỗ tồn kho (Contention)</h3>
                <span className="text-[10px] font-mono uppercase bg-rose-50 text-rose-700 px-1.5 py-0.5 rounded font-semibold">
                  Mã lỗi: {stockContentionError.code}
                </span>
              </div>
            </div>
            <p className="text-xs sm:text-sm text-zinc-600 leading-relaxed">
              {stockContentionError.detail}
            </p>
            <p className="text-xs text-zinc-500">
              Do nhiều khách hàng cùng lúc mua sắm, số lượng sản phẩm khả dụng trong kho đã thay đổi. Giỏ hàng của bạn đã được tự động đồng bộ lại. Vui lòng quay lại giỏ hàng để cập nhật.
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
