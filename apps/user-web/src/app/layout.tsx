import type { Metadata } from 'next';
import { Providers } from './providers';
import { Header } from '@/components/common/Header';
import { Footer } from '@/components/common/Footer';
import { MobileBottomNav } from '@/components/common/MobileBottomNav';
import { MiniCartDrawer } from '@/components/cart/MiniCartDrawer';
import { ToastContainer } from '@/components/common/ToastContainer';
import '@/styles/globals.css';

export const metadata: Metadata = {
  title: 'ATELIER | Clean Editorial Modern E-Commerce',
  description:
    'Nền tảng mua sắm trực tuyến cao cấp, tối ưu hóa trải nghiệm khách hàng với kiến trúc vi dịch vụ phân tán.',
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="vi">
      <body className="min-h-screen flex flex-col bg-zinc-50/50 text-zinc-900 antialiased font-sans selection:bg-zinc-900 selection:text-white">
        <Providers>
          <Header />
          <main className="flex-1 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 sm:py-8 w-full pb-20 md:pb-8">
            {children}
          </main>
          <Footer />
          <MobileBottomNav />
          <MiniCartDrawer />
          <ToastContainer />
        </Providers>
      </body>
    </html>
  );
}
