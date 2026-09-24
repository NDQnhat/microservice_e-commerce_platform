import type { Metadata } from 'next';
import { Providers } from './providers';
import '@/styles/globals.css';

export const metadata: Metadata = {
  title: 'NextGen E-Commerce Platform',
  description: 'Enterprise grade e-commerce storefront',
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body className="min-h-screen flex flex-col bg-gray-50 text-gray-900">
        <Providers>
          <header className="border-b bg-white shadow-sm sticky top-0 z-50">
            <div className="max-w-7xl mx-auto px-4 h-16 flex items-center justify-between">
              <a href="/" className="text-xl font-bold tracking-tight text-indigo-600">
                ShopFlow
              </a>
              <nav className="flex items-center space-x-6 text-sm font-medium">
                <a href="/catalog" className="text-gray-600 hover:text-gray-900">
                  Products
                </a>
                <a href="/cart" className="text-gray-600 hover:text-gray-900">
                  Cart
                </a>
                <a href="/orders" className="text-gray-600 hover:text-gray-900">
                  Orders
                </a>
                <a href="/login" className="px-3 py-1.5 rounded-md bg-indigo-600 text-white hover:bg-indigo-700">
                  Sign In
                </a>
              </nav>
            </div>
          </header>
          <main className="flex-1 max-w-7xl mx-auto px-4 py-8 w-full">{children}</main>
          <footer className="border-t bg-white py-6 text-center text-sm text-gray-500">
            &copy; 2026 E-Commerce Platform. All rights reserved.
          </footer>
        </Providers>
      </body>
    </html>
  );
}
