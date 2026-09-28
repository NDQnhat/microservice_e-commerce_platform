'use client';

import React, { useState, useEffect } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { useUserStore } from '@/store/user-store';
import { useToastStore } from '@/store/toast-store';
import { User, Mail, Lock, Eye, EyeOff, Loader2, ArrowRight } from 'lucide-react';

const registerSchema = z
  .object({
    fullName: z.string().min(2, 'Họ và tên tối thiểu 2 ký tự'),
    email: z.string().email('Địa chỉ email không đúng định dạng'),
    password: z.string().min(6, 'Mật khẩu tối thiểu 6 ký tự'),
    confirmPassword: z.string().min(6, 'Vui lòng xác nhận mật khẩu'),
    agreeTerms: z.literal(true, {
      errorMap: () => ({ message: 'Bạn phải đồng ý với điều khoản sử dụng' }),
    }),
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: 'Mật khẩu xác nhận không khớp',
    path: ['confirmPassword'],
  });

type RegisterFormData = z.infer<typeof registerSchema>;

export default function RegisterPage() {
  const router = useRouter();
  const [returnUrl, setReturnUrl] = useState('/');

  useEffect(() => {
    if (typeof window !== 'undefined') {
      const url = new URLSearchParams(window.location.search).get('returnUrl') || '/';
      setReturnUrl(url);
    }
  }, []);

  const { register: registerUser, login } = useUserStore();
  const { showSuccess, showError } = useToastStore();
  const [showPassword, setShowPassword] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<RegisterFormData>({
    resolver: zodResolver(registerSchema),
  });

  const onSubmit = async (data: RegisterFormData) => {
    setIsSubmitting(true);
    try {
      await registerUser(data.fullName, data.email, data.password);
      // Auto login
      await login(data.email, data.password);
      showSuccess('Đăng ký thành công', 'Chào mừng bạn gia nhập cộng đồng mua sắm Atelier!');
      router.push(returnUrl);
    } catch (err) {
      showError('Đăng ký thất bại', err);
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
            Tạo tài khoản mới
          </h1>
          <p className="text-xs text-zinc-500">
            Trải nghiệm mua sắm đẳng cấp cùng hệ sinh thái vi dịch vụ cao cấp
          </p>
        </div>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          {/* Full Name */}
          <div className="space-y-1">
            <label className="text-xs font-semibold text-zinc-700 block">Họ và tên *</label>
            <div className="relative">
              <input
                type="text"
                {...register('fullName')}
                placeholder="Nguyễn Văn An"
                className="w-full pl-9 pr-4 py-2.5 rounded-xl border border-zinc-200 text-xs sm:text-sm text-zinc-900 focus:outline-none focus:border-zinc-900 transition"
              />
              <User className="w-4 h-4 text-zinc-400 absolute left-3 top-3" />
            </div>
            {errors.fullName && (
              <p className="text-[11px] text-rose-600">{errors.fullName.message}</p>
            )}
          </div>

          {/* Email */}
          <div className="space-y-1">
            <label className="text-xs font-semibold text-zinc-700 block">Địa chỉ Email *</label>
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
            <label className="text-xs font-semibold text-zinc-700 block">Mật khẩu *</label>
            <div className="relative">
              <input
                type={showPassword ? 'text' : 'password'}
                {...register('password')}
                placeholder="Tối thiểu 6 ký tự"
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

          {/* Confirm Password */}
          <div className="space-y-1">
            <label className="text-xs font-semibold text-zinc-700 block">Xác nhận mật khẩu *</label>
            <div className="relative">
              <input
                type={showPassword ? 'text' : 'password'}
                {...register('confirmPassword')}
                placeholder="Nhập lại mật khẩu"
                className="w-full pl-9 pr-4 py-2.5 rounded-xl border border-zinc-200 text-xs sm:text-sm text-zinc-900 focus:outline-none focus:border-zinc-900 transition"
              />
              <Lock className="w-4 h-4 text-zinc-400 absolute left-3 top-3" />
            </div>
            {errors.confirmPassword && (
              <p className="text-[11px] text-rose-600">{errors.confirmPassword.message}</p>
            )}
          </div>

          {/* Terms checkbox */}
          <div className="space-y-1 pt-1">
            <label className="flex items-start gap-2 cursor-pointer text-xs text-zinc-600">
              <input
                type="checkbox"
                {...register('agreeTerms')}
                className="w-4 h-4 rounded border-zinc-300 text-zinc-900 focus:ring-zinc-900 mt-0.5"
              />
              <span>
                Tôi đồng ý với các <a href="#" className="font-semibold text-zinc-900 underline">Điều khoản dịch vụ</a> và <a href="#" className="font-semibold text-zinc-900 underline">Chính sách bảo mật</a>
              </span>
            </label>
            {errors.agreeTerms && (
              <p className="text-[11px] text-rose-600">{errors.agreeTerms.message}</p>
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
                <span>Đăng ký tài khoản</span>
                <ArrowRight className="w-4 h-4" />
              </>
            )}
          </button>
        </form>

        <div className="pt-2 text-center text-xs text-zinc-500 border-t border-zinc-100">
          Đã có tài khoản?{' '}
          <Link
            href={`/auth/login?returnUrl=${encodeURIComponent(returnUrl)}`}
            className="font-bold text-zinc-900 hover:underline"
          >
            Đăng nhập ngay
          </Link>
        </div>
      </div>
    </div>
  );
}
