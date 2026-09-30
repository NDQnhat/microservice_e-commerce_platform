'use client';

import React, { useState, useEffect } from 'react';
import { useUserStore } from '@/store/user-store';
import { CustomerAddress } from '@/types';
import { useToastStore } from '@/store/toast-store';
import {
  MapPin,
  Plus,
  Edit2,
  Trash2,
  CheckCircle2,
  Info,
  X,
  Loader2,
  ShieldCheck,
} from 'lucide-react';
import { EmptyState } from '@/components/common/EmptyState';

export default function CustomerAddressesPage() {
  const {
    user,
    isAuthenticated,
    addresses,
    addAddress,
    updateAddress,
    deleteAddress,
    setDefaultAddress,
  } = useUserStore();
  const { showSuccess, showError } = useToastStore();

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingAddress, setEditingAddress] = useState<CustomerAddress | null>(null);
  const [deletingAddressId, setDeletingAddressId] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Form fields
  const [recipientName, setRecipientName] = useState('');
  const [phone, setPhone] = useState('');
  const [line1, setLine1] = useState('');
  const [line2, setLine2] = useState('');
  const [ward, setWard] = useState('');
  const [district, setDistrict] = useState('');
  const [city, setCity] = useState('');
  const [isDefault, setIsDefault] = useState(false);
  const [formErrors, setFormErrors] = useState<Record<string, string>>({});

  const openAddModal = () => {
    setEditingAddress(null);
    setRecipientName(user?.fullName || '');
    setPhone(user?.phone || '');
    setLine1('');
    setLine2('');
    setWard('');
    setDistrict('');
    setCity('');
    setIsDefault(addresses.length === 0);
    setFormErrors({});
    setIsModalOpen(true);
  };

  const openEditModal = (addr: CustomerAddress) => {
    setEditingAddress(addr);
    setRecipientName(addr.recipientName);
    setPhone(addr.phone);
    setLine1(addr.line1);
    setLine2(addr.line2 || '');
    setWard(addr.ward);
    setDistrict(addr.district);
    setCity(addr.city);
    setIsDefault(addr.isDefault);
    setFormErrors({});
    setIsModalOpen(true);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    const errors: Record<string, string> = {};
    const trimmedName = recipientName.trim();
    if (!trimmedName || trimmedName.length < 2 || trimmedName.length > 100) {
      errors.recipientName = 'Họ tên người nhận phải từ 2 đến 100 ký tự';
    }

    const vnPhoneRegex = /^(0|\+84)(3|5|7|8|9)[0-9]{8}$/;
    const trimmedPhone = phone.trim();
    if (!vnPhoneRegex.test(trimmedPhone)) {
      errors.phone = 'Số điện thoại di động Việt Nam không đúng định dạng (VD: 0912345678)';
    }

    const trimmedLine1 = line1.trim();
    if (!trimmedLine1 || trimmedLine1.length < 5) {
      errors.line1 = 'Địa chỉ chi tiết tối thiểu 5 ký tự';
    }

    const trimmedDistrict = district.trim();
    if (!trimmedDistrict) {
      errors.district = 'Vui lòng nhập quận / huyện';
    }

    const trimmedCity = city.trim();
    if (!trimmedCity) {
      errors.city = 'Vui lòng nhập tỉnh / thành phố';
    }

    if (Object.keys(errors).length > 0) {
      setFormErrors(errors);
      showError('Thông tin địa chỉ không hợp lệ', Object.values(errors)[0]);
      return;
    }

    setFormErrors({});
    setIsSubmitting(true);

    try {
      if (editingAddress) {
        await updateAddress(editingAddress.id, {
          recipientName: trimmedName,
          phone: trimmedPhone,
          line1: trimmedLine1,
          line2: line2.trim() || undefined,
          ward: ward.trim() || '',
          district: trimmedDistrict,
          city: trimmedCity,
          isDefault,
        });
        showSuccess('Cập nhật địa chỉ thành công');
      } else {
        await addAddress({
          recipientName: trimmedName,
          phone: trimmedPhone,
          line1: trimmedLine1,
          line2: line2.trim() || undefined,
          ward: ward.trim() || '',
          district: trimmedDistrict,
          city: trimmedCity,
          isDefault,
        });
        showSuccess('Thêm địa chỉ giao hàng thành công');
      }
      setIsModalOpen(false);
    } catch (err) {
      showError('Không thể lưu địa chỉ', err);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDelete = (id: string) => {
    setDeletingAddressId(id);
  };

  const handleConfirmDelete = async () => {
    if (!deletingAddressId) return;
    try {
      await deleteAddress(deletingAddressId);
      showSuccess('Đã xóa địa chỉ thành công');
    } catch (err) {
      showError('Không thể xóa địa chỉ', err);
    } finally {
      setDeletingAddressId(null);
    }
  };

  const handleSetDefault = async (id: string) => {
    try {
      await setDefaultAddress(id);
      showSuccess('Đã đặt làm địa chỉ mặc định');
    } catch (err) {
      showError('Lỗi cập nhật', err);
    }
  };

  if (!isAuthenticated || !user) {
    return (
      <div className="py-16 text-center">
        <EmptyState
          title="Yêu cầu đăng nhập"
          description="Vui lòng đăng nhập để quản lý sổ địa chỉ giao hàng của bạn."
          actionText="Đăng nhập"
          actionHref="/auth/login?returnUrl=/account/addresses"
        />
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto py-6 space-y-6 sm:space-y-8">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b border-zinc-200 pb-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-bold tracking-tight text-zinc-900">
            Sổ địa chỉ giao hàng
          </h1>
          <p className="text-xs sm:text-sm text-zinc-500 mt-1">
            Quản lý danh sách địa chỉ nhận hàng để thanh toán nhanh chóng hơn
          </p>
        </div>

        <button
          onClick={openAddModal}
          className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-zinc-900 hover:bg-zinc-800 text-white text-xs font-semibold shadow-xs transition self-start sm:self-auto"
        >
          <Plus className="w-4 h-4" />
          <span>Thêm địa chỉ mới</span>
        </button>
      </div>

      {/* Invariant BR-017 Callout Banner */}
      <div className="flex items-start gap-3 p-4 rounded-2xl bg-zinc-50 border border-zinc-200/80 text-xs sm:text-sm text-zinc-700">
        <ShieldCheck className="w-5 h-5 text-zinc-900 shrink-0 mt-0.5" />
        <div className="space-y-1">
          <h4 className="font-bold text-zinc-900">Bất biến BR-017 (Address Snapshot Isolation)</h4>
          <p className="text-xs text-zinc-600 leading-relaxed">
            Mỗi khi bạn hoàn tất đặt hàng, thông tin địa chỉ giao hàng được hệ thống chụp lại (snapshot) bất biến cùng đơn hàng. Việc thêm, chỉnh sửa hoặc xóa địa chỉ tại đây sẽ <strong>không làm thay đổi</strong> địa chỉ giao hàng của các đơn hàng đã đặt trước đó.
          </p>
        </div>
      </div>

      {/* Addresses Grid */}
      {addresses.length === 0 ? (
        <div className="py-12">
          <EmptyState
            icon={<MapPin className="w-8 h-8 text-zinc-400" />}
            title="Chưa có địa chỉ giao hàng"
            description="Hãy thêm địa chỉ giao hàng đầu tiên của bạn để quá trình đặt hàng thuận tiện hơn."
            actionText="Thêm địa chỉ ngay"
            onAction={openAddModal}
          />
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 sm:gap-6">
          {addresses.map((addr) => (
            <div
              key={addr.id}
              className={`p-5 rounded-2xl border-2 transition flex flex-col justify-between ${
                addr.isDefault
                  ? 'border-zinc-900 bg-white shadow-xs'
                  : 'border-zinc-200/80 bg-white hover:border-zinc-300'
              }`}
            >
              <div className="space-y-2">
                <div className="flex items-start justify-between gap-2">
                  <span className="text-sm font-bold text-zinc-900">{addr.recipientName}</span>
                  {addr.isDefault ? (
                    <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-zinc-900 text-white uppercase tracking-wider">
                      <CheckCircle2 className="w-3 h-3 text-emerald-400" />
                      Mặc định
                    </span>
                  ) : (
                    <button
                      onClick={() => handleSetDefault(addr.id)}
                      className="text-xs text-zinc-500 hover:text-zinc-900 font-medium"
                    >
                      Đặt làm mặc định
                    </button>
                  )}
                </div>

                <p className="text-xs text-zinc-500 font-mono">{addr.phone}</p>
                <p className="text-xs text-zinc-700 leading-relaxed">
                  {addr.line1}
                  {addr.line2 ? `, ${addr.line2}` : ''}, {addr.ward}, {addr.district}, {addr.city}
                </p>
              </div>

              <div className="pt-4 border-t border-zinc-100 flex items-center justify-end gap-2 mt-4">
                <button
                  onClick={() => openEditModal(addr)}
                  className="p-1.5 rounded-lg text-zinc-500 hover:text-zinc-900 hover:bg-zinc-100 transition"
                  aria-label="Sửa địa chỉ"
                >
                  <Edit2 className="w-4 h-4" />
                </button>
                <button
                  onClick={() => handleDelete(addr.id)}
                  className="p-1.5 rounded-lg text-zinc-500 hover:text-rose-600 hover:bg-rose-50 transition"
                  aria-label="Xóa địa chỉ"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Add / Edit Address Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div
            className="fixed inset-0 bg-black/50 backdrop-blur-xs"
            onClick={() => setIsModalOpen(false)}
          />
          <div className="relative bg-white rounded-2xl max-w-lg w-full p-6 shadow-2xl border border-zinc-200 z-10 animate-in fade-in zoom-in-95 space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-zinc-100">
              <h3 className="text-base font-bold text-zinc-900">
                {editingAddress ? 'Chỉnh sửa địa chỉ' : 'Thêm địa chỉ giao hàng mới'}
              </h3>
              <button
                onClick={() => setIsModalOpen(false)}
                className="p-1 rounded-lg text-zinc-400 hover:text-zinc-700"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <form onSubmit={handleSubmit} className="space-y-3 pt-1">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="text-xs font-semibold text-zinc-700 block mb-1">Họ tên người nhận *</label>
                  <input
                    type="text"
                    value={recipientName}
                    onChange={(e) => setRecipientName(e.target.value)}
                    placeholder="Nguyễn Văn An"
                    className={`w-full px-3 py-2 rounded-xl border text-xs text-zinc-900 focus:outline-none transition ${
                      formErrors.recipientName
                        ? 'border-rose-500 bg-rose-50/20'
                        : 'border-zinc-200 focus:border-zinc-900'
                    }`}
                  />
                  {formErrors.recipientName && (
                    <p className="text-[11px] text-rose-600 mt-1">{formErrors.recipientName}</p>
                  )}
                </div>
                <div>
                  <label className="text-xs font-semibold text-zinc-700 block mb-1">Số điện thoại di động *</label>
                  <input
                    type="tel"
                    value={phone}
                    onChange={(e) => setPhone(e.target.value)}
                    placeholder="0912345678"
                    className={`w-full px-3 py-2 rounded-xl border text-xs text-zinc-900 focus:outline-none transition ${
                      formErrors.phone
                        ? 'border-rose-500 bg-rose-50/20'
                        : 'border-zinc-200 focus:border-zinc-900'
                    }`}
                  />
                  {formErrors.phone && (
                    <p className="text-[11px] text-rose-600 mt-1">{formErrors.phone}</p>
                  )}
                </div>
              </div>

              <div>
                <label className="text-xs font-semibold text-zinc-700 block mb-1">Địa chỉ chi tiết (Số nhà, tên đường) *</label>
                <input
                  type="text"
                  value={line1}
                  onChange={(e) => setLine1(e.target.value)}
                  placeholder="Số 45 Đường Lê Duẩn"
                  className={`w-full px-3 py-2 rounded-xl border text-xs text-zinc-900 focus:outline-none transition ${
                    formErrors.line1
                      ? 'border-rose-500 bg-rose-50/20'
                      : 'border-zinc-200 focus:border-zinc-900'
                  }`}
                />
                {formErrors.line1 && (
                  <p className="text-[11px] text-rose-600 mt-1">{formErrors.line1}</p>
                )}
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                <div>
                  <label className="text-xs font-semibold text-zinc-700 block mb-1">Phường / Xã</label>
                  <input
                    type="text"
                    value={ward}
                    onChange={(e) => setWard(e.target.value)}
                    placeholder="Phường Bến Nghé"
                    className="w-full px-3 py-2 rounded-xl border border-zinc-200 text-xs text-zinc-900 focus:outline-none focus:border-zinc-900"
                  />
                </div>
                <div>
                  <label className="text-xs font-semibold text-zinc-700 block mb-1">Quận / Huyện *</label>
                  <input
                    type="text"
                    value={district}
                    onChange={(e) => setDistrict(e.target.value)}
                    placeholder="Quận 1"
                    className={`w-full px-3 py-2 rounded-xl border text-xs text-zinc-900 focus:outline-none transition ${
                      formErrors.district
                        ? 'border-rose-500 bg-rose-50/20'
                        : 'border-zinc-200 focus:border-zinc-900'
                    }`}
                  />
                  {formErrors.district && (
                    <p className="text-[11px] text-rose-600 mt-1">{formErrors.district}</p>
                  )}
                </div>
                <div>
                  <label className="text-xs font-semibold text-zinc-700 block mb-1">Tỉnh / Thành phố *</label>
                  <input
                    type="text"
                    value={city}
                    onChange={(e) => setCity(e.target.value)}
                    placeholder="TP. Hồ Chí Minh"
                    className={`w-full px-3 py-2 rounded-xl border text-xs text-zinc-900 focus:outline-none transition ${
                      formErrors.city
                        ? 'border-rose-500 bg-rose-50/20'
                        : 'border-zinc-200 focus:border-zinc-900'
                    }`}
                  />
                  {formErrors.city && (
                    <p className="text-[11px] text-rose-600 mt-1">{formErrors.city}</p>
                  )}
                </div>
              </div>

              <div className="pt-2">
                <label className="flex items-center gap-2 cursor-pointer text-xs font-medium text-zinc-800">
                  <input
                    type="checkbox"
                    checked={isDefault}
                    onChange={(e) => setIsDefault(e.target.checked)}
                    className="w-4 h-4 rounded border-zinc-300 text-zinc-900 focus:ring-zinc-900"
                  />
                  <span>Đặt làm địa chỉ nhận hàng mặc định</span>
                </label>
              </div>

              <div className="pt-4 border-t border-zinc-100 flex items-center justify-end gap-3">
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  className="px-4 py-2 rounded-xl border border-zinc-200 text-xs font-semibold text-zinc-700 hover:bg-zinc-50"
                >
                  Hủy
                </button>
                <button
                  type="submit"
                  disabled={isSubmitting}
                  className="px-5 py-2 rounded-xl bg-zinc-900 hover:bg-zinc-800 text-white text-xs font-semibold flex items-center gap-1.5 shadow-sm"
                >
                  {isSubmitting && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
                  <span>{editingAddress ? 'Cập nhật' : 'Thêm mới'}</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Delete Address Confirmation Modal (FIX-M11) */}
      {deletingAddressId && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-xs">
          <div className="bg-white rounded-2xl max-w-sm w-full p-6 shadow-xl space-y-4">
            <div className="flex items-center gap-3 text-rose-600">
              <Trash2 className="w-5 h-5 shrink-0" />
              <h3 className="text-base font-bold text-zinc-900">Xóa địa chỉ giao hàng</h3>
            </div>
            <p className="text-xs text-zinc-600 leading-relaxed">
              Bạn có chắc chắn muốn xóa địa chỉ này? Thao tác này không thể hoàn tác.
            </p>
            <div className="pt-2 flex items-center justify-end gap-2">
              <button
                type="button"
                onClick={() => setDeletingAddressId(null)}
                className="px-4 py-2 rounded-xl border border-zinc-200 text-xs font-semibold text-zinc-700 hover:bg-zinc-50 transition"
              >
                Hủy
              </button>
              <button
                type="button"
                onClick={handleConfirmDelete}
                className="px-4 py-2 rounded-xl bg-rose-600 hover:bg-rose-700 text-white text-xs font-semibold shadow-xs transition"
              >
                Xác nhận xóa
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
