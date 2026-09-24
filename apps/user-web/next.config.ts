import type { NextConfig } from 'next';

const nextConfig: NextConfig = {
  reactStrictMode: true,
  output: 'standalone',
  async rewrites() {
    return [
      {
        source: '/api/:path*',
        destination: process.env.API_GATEWAY_URL || 'http://localhost:8080/api/:path*',
      },
    ];
  },
};

export default nextConfig;
