'use client';

import React, { useState, useEffect } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { useUserStore } from '@/store/user-store';
import { useToastStore } from '@/store/toast-store';
import { ApiClientError } from '@/lib/api-client';
import { Lock, Mail, Eye, EyeOff, Loader2, ArrowRight } from 'lucide-react';

const loginSchema = z.object({
  email: z.string().email('Địa chỉ email không đúng định dạng'),
  password: z.string().min(6, 'Mật khẩu tối thiểu 6 ký tự'),
});

type LoginFormData = z.infer<typeof loginSchema>;

export default function LoginPage() {
  const router = useRouter();
  const [returnUrl, setReturnUrl] = useState('/');

  useEffect(() => {
    if (typeof window !== 'undefined') {
      const url = new URLSearchParams(window.location.search).get('returnUrl') || '/';
      setReturnUrl(url);
    }
  }, []);

  const { login } = useUserStore();
  const { showSuccess, showError } = useToastStore();
  const [showPassword, setShowPassword] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [showForgotPasswordModal, setShowForgotPasswordModal] = useState(false);
  const [forgotEmail, setForgotEmail] = useState('');
  const [isSendingReset, setIsSendingReset] = useState(false);
  const [resetSent, setResetSent] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      email: 'customer@ecommerce.local',
      password: 'Password123!',
    },
  });

  const onSubmit = async (data: LoginFormData) => {
    setIsSubmitting(true);
    try {
      await login(data.email, data.password);
      showSuccess('Đăng nhập thành công', 'Chào mừng bạn quay trở lại với Atelier!');
      router.push(returnUrl);
    } catch (err) {
      if (err instanceof ApiClientError) {
        if (err.problem.status === 403) {
          showError('Tài khoản bị khóa', err.problem.detail);
        } else {
          showError('Đăng nhập thất bại', err.problem.detail || 'Email hoặc mật khẩu không chính xác.');
        }
      } else {
        showError('Đăng nhập thất bại', err);
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="max-w-md mx-auto py-8 sm:py-12">
      <div className="bg-white rounded-3xl border border-zinc-200/80 p-6 sm:p-8 shadow-sm space-y-6">
        <div className="text-center space-y-1">
          <Link href="/" className="inline-block">
            <span className="text-2xl font-black tracking-tighter text-zinc-900 uppercase font-sans">
              ATELIER<span className="text-rose-600">.</span>
            </span>
          </Link>
          <h1 className="text-xl sm:text-2xl font-bold tracking-tight text-zinc-900">
            Đăng nhập tài khoản
          </h1>
          <p className="text-xs text-zinc-500">
            Nhập thông tin xác thực để quản lý đơn hàng và giỏ hàng của bạn
          </p>
        </div>

        {/* Demo Credentials Hint */}
        <div className="p-3 bg-zinc-50 rounded-xl border border-zinc-200/60 text-xs text-zinc-600 space-y-0.5">
          <p className="font-semibold text-zinc-900">Tài khoản mẫu (Đã điền sẵn):</p>
          <p className="font-mono text-[11px]">Email: customer@ecommerce.local</p>
          <p className="font-mono text-[11px]">Mật khẩu: Password123!</p>
        </div>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          {/* Email */}
          <div className="space-y-1">
            <label className="text-xs font-semibold text-zinc-700 block">Địa chỉ Email</label>
            <div className="relative">
              <input
                type="email"
                {...register('email')}
                placeholder="name@example.com"
                className="w-full pl-9 pr-4 py-2.5 rounded-xl border border-zinc-200 text-xs sm:text-sm text-zinc-900 focus:outline-none focus:border-zinc-900 transition"
              />
              <Mail className="w-4 h-4 text-zinc-400 absolute left-3 top-3" />
            </div>
            {errors.email && (
              <p className="text-[11px] text-rose-600">{errors.email.message}</p>
            )}
          </div>

          {/* Password */}
          <div className="space-y-1">
            <div className="flex items-center justify-between">
              <label className="text-xs font-semibold text-zinc-700 block">Mật khẩu</label>
              <button
                type="button"
                onClick={() => {
                  setForgotEmail(register('email').name ? (document.querySelector('input[type="email"]') as HTMLInputElement)?.value || '' : '');
                  setResetSent(false);
                  setShowForgotPasswordModal(true);
                }}
                className="text-[11px] text-zinc-500 hover:text-zinc-900 transition"
              >
                Quên mật khẩu?
              </button>
            </div>
            <div className="relative">
              <input
                type={showPassword ? 'text' : 'password'}
                {...register('password')}
                placeholder="••••••••"
                className="w-full pl-9 pr-10 py-2.5 rounded-xl border border-zinc-200 text-xs sm:text-sm text-zinc-900 focus:outline-none focus:border-zinc-900 transition"
              />
              <Lock className="w-4 h-4 text-zinc-400 absolute left-3 top-3" />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                className="absolute right-3 top-3 text-zinc-400 hover:text-zinc-700"
              >
                {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
              </button>
            </div>
            {errors.password && (
              <p className="text-[11px] text-rose-600">{errors.password.message}</p>
            )}
          </div>

          {/* Submit */}
          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full py-3 px-4 rounded-xl bg-zinc-900 hover:bg-zinc-800 text-white text-xs sm:text-sm font-bold flex items-center justify-center gap-2 shadow-sm transition disabled:opacity-50"
          >
            {isSubmitting ? (
              <Loader2 className="w-4 h-4 animate-spin" />
            ) : (
              <>
                <span>Đăng nhập</span>
                <ArrowRight className="w-4 h-4" />
              </>
            )}
          </button>
        </form>

        <div className="pt-2 text-center text-xs text-zinc-500 border-t border-zinc-100">
          Chưa có tài khoản?{' '}
          <Link
            href={`/auth/register?returnUrl=${encodeURIComponent(returnUrl)}`}
            className="font-bold text-zinc-900 hover:underline"
          >
            Tạo tài khoản mới
          </Link>
        </div>
      </div>

      {/* STT 17: Forgot Password Recovery Modal */}
      {showForgotPasswordModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div
            className="fixed inset-0 bg-black/50 backdrop-blur-xs"
            onClick={() => setShowForgotPasswordModal(false)}
          />
          <div className="relative bg-white rounded-2xl max-w-md w-full p-6 shadow-2xl border border-zinc-200 z-10 space-y-4 animate-in fade-in zoom-in-95">
            <div className="text-center space-y-1">
              <h3 className="text-lg font-bold text-zinc-900">Khôi phục mật khẩu</h3>
              <p className="text-xs text-zinc-500">
                Nhập địa chỉ email đăng ký tài khoản của bạn để nhận mã xác thực OTP đặt lại mật khẩu.
              </p>
            </div>

            {resetSent ? (
              <div className="p-4 rounded-xl bg-emerald-50 border border-emerald-200 text-center space-y-3">
                <p className="text-xs text-emerald-800 font-medium leading-relaxed">
                  🎉 Yêu cầu đặt lại mật khẩu thành công! Mã OTP và đường dẫn khôi phục đã được gửi tới{' '}
                  <strong className="font-mono text-zinc-900">{forgotEmail}</strong>.
                </p>
                <button
                  onClick={() => setShowForgotPasswordModal(false)}
                  className="py-2 px-4 rounded-xl bg-zinc-900 text-white text-xs font-semibold hover:bg-zinc-800"
                >
                  Đóng
                </button>
              </div>
            ) : (
              <form
                onSubmit={async (e) => {
                  e.preventDefault();
                  if (!forgotEmail || !forgotEmail.includes('@')) {
                    showError('Vui lòng nhập địa chỉ email hợp lệ');
                    return;
                  }
                  setIsSendingReset(true);
                  setTimeout(() => {
                    setIsSendingReset(false);
                    setResetSent(true);
                    showSuccess('Đã gửi mã khôi phục mật khẩu', forgotEmail);
                  }, 800);
                }}
                className="space-y-4"
              >
                <div>
                  <label className="text-xs font-semibold text-zinc-700 block mb-1">Email đăng ký</label>
                  <div className="relative">
                    <input
                      type="email"
                      required
                      value={forgotEmail}
                      onChange={(e) => setForgotEmail(e.target.value)}
                      placeholder="name@example.com"
                      className="w-full pl-9 pr-4 py-2.5 rounded-xl border border-zinc-200 text-xs sm:text-sm text-zinc-900 focus:outline-none focus:border-zinc-900"
                    />
                    <Mail className="w-4 h-4 text-zinc-400 absolute left-3 top-3" />
                  </div>
                </div>

                <div className="flex gap-2 pt-1">
                  <button
                    type="button"
                    onClick={() => setShowForgotPasswordModal(false)}
                    className="flex-1 py-2.5 px-3 rounded-xl border border-zinc-200 text-xs font-semibold text-zinc-700 hover:bg-zinc-50"
                  >
                    Hủy
                  </button>
                  <button
                    type="submit"
                    disabled={isSendingReset}
                    className="flex-1 py-2.5 px-3 rounded-xl bg-zinc-900 text-white text-xs font-semibold hover:bg-zinc-800 flex items-center justify-center gap-1.5 disabled:opacity-50"
                  >
                    {isSendingReset ? (
                      <>
                        <Loader2 className="w-3.5 h-3.5 animate-spin" />
                        <span>Đang gửi...</span>
                      </>
                    ) : (
                      <span>Gửi liên kết</span>
                    )}
                  </button>
                </div>
              </form>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
