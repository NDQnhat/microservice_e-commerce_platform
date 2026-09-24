'use client';

import React from 'react';

export default function HomePage() {
  return (
    <div className="space-y-12">
      <section className="bg-gradient-to-r from-indigo-700 via-indigo-600 to-purple-700 text-white rounded-2xl p-12 shadow-lg">
        <div className="max-w-2xl space-y-4">
          <span className="px-3 py-1 bg-white/20 rounded-full text-xs uppercase tracking-wider font-semibold">
            Spring Collection 2026
          </span>
          <h1 className="text-4xl md:text-5xl font-extrabold tracking-tight">
            Discover Exceptional Everyday Essentials
          </h1>
          <p className="text-indigo-100 text-lg">
            High performance, zero latency shopping powered by microservices architecture.
          </p>
          <div className="pt-4 flex gap-4">
            <a
              href="/catalog"
              className="px-6 py-3 bg-white text-indigo-700 rounded-lg font-semibold hover:bg-gray-100 shadow transition-all"
            >
              Shop Catalog
            </a>
            <a
              href="/cart"
              className="px-6 py-3 border border-white/40 text-white rounded-lg font-semibold hover:bg-white/10 transition-all"
            >
              View Cart
            </a>
          </div>
        </div>
      </section>

      <section className="space-y-6">
        <div className="flex items-center justify-between">
          <h2 className="text-2xl font-bold text-gray-900">Featured Products</h2>
          <a href="/catalog" className="text-sm font-semibold text-indigo-600 hover:text-indigo-700">
            Browse all &rarr;
          </a>
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
          {[1, 2, 3, 4].map((i) => (
            <div key={i} className="border border-gray-200 rounded-xl overflow-hidden bg-white shadow-sm hover:shadow-md transition">
              <div className="h-48 bg-gray-100 flex items-center justify-center text-gray-400">
                <span>Product Image {i}</span>
              </div>
              <div className="p-4 space-y-2">
                <span className="text-xs text-indigo-600 uppercase font-bold tracking-wider">Category</span>
                <h3 className="font-semibold text-gray-900">Sample Product {i}</h3>
                <p className="text-sm text-gray-500 line-clamp-2">Premium quality crafted for optimal performance and daily utility.</p>
                <div className="flex items-center justify-between pt-2">
                  <span className="text-lg font-bold text-gray-900">$49.99</span>
                  <button className="px-3 py-1.5 text-xs font-semibold bg-indigo-600 text-white rounded hover:bg-indigo-700">
                    Add to Cart
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}
