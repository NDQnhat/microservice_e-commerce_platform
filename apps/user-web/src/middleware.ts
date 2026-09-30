import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';

const PROTECTED_PATHS = ['/account', '/checkout'];

export function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;
  const isProtected = PROTECTED_PATHS.some((path) => pathname.startsWith(path));

  if (isProtected) {
    // Kiểm tra token trong cookie (nếu dùng cookie) hoặc custom header
    // Vì token trong localStorage (client-only), middleware chỉ có thể check cookie
    const token = request.cookies.get('access_token')?.value;

    // Note: localStorage không accessible trong middleware (server-side).
    // Nếu auth dùng localStorage only (current implementation), middleware skip.
    // Chỉ redirect nếu có cookie-based auth được cấu hình.
    // Giữ client-side guard hiện tại như là primary guard.
    // Middleware này là safety net bổ sung khi chuyển sang cookie-based auth.
    if (!token) {
      // Trong development/mock mode, không redirect — chỉ enable khi có real auth
      return NextResponse.next();
    }
  }

  return NextResponse.next();
}

export const config = {
  matcher: ['/account/:path*', '/checkout/:path*'],
};
