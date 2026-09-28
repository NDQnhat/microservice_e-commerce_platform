'use client';

import React, { useState, useEffect } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { useUserStore } from '@/store/user-store';
import { useToastStore } from '@/store/toast-store';
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
      showError('Đăng nhập thất bại', err);
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
              <a href="#" className="text-[11px] text-zinc-500 hover:text-zinc-900">
                Quên mật khẩu?
              </a>
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
    </div>
  );
}
