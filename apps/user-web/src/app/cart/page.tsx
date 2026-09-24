'use client';

import React from 'react';
import { useCartStore } from '@/store/cart-store';

export default function CartPage() {
  const { cart, itemCount, clearCart } = useCartStore();

  return (
    <div className="space-y-6">
      <h1 className="text-3xl font-bold text-gray-900">Shopping Cart</h1>
      {itemCount === 0 ? (
        <div className="bg-white rounded-xl p-12 text-center border border-gray-200 space-y-4">
          <p className="text-gray-500">Your shopping cart is currently empty.</p>
          <a
            href="/"
            className="inline-block px-5 py-2.5 bg-indigo-600 text-white font-medium rounded-lg hover:bg-indigo-700 transition"
          >
            Continue Shopping
          </a>
        </div>
      ) : (
        <div className="bg-white rounded-xl p-6 border border-gray-200 space-y-4">
          <div className="divide-y divide-gray-100">
            {cart?.items.map((item) => (
              <div key={item.id} className="py-4 flex justify-between items-center">
                <div>
                  <h4 className="font-semibold text-gray-900">{item.productName}</h4>
                  <p className="text-sm text-gray-500">SKU: {item.skuCode} | Qty: {item.quantity}</p>
                </div>
                <div className="text-right">
                  <p className="font-bold text-gray-900">${item.totalPrice.toFixed(2)}</p>
                </div>
              </div>
            ))}
          </div>
          <div className="border-t pt-4 flex justify-between items-center">
            <button
              onClick={clearCart}
              className="text-sm text-red-600 hover:text-red-700 font-medium"
            >
              Clear Cart
            </button>
            <a
              href="/checkout"
              className="px-6 py-2.5 bg-indigo-600 text-white font-semibold rounded-lg hover:bg-indigo-700"
            >
              Proceed to Checkout
            </a>
          </div>
        </div>
      )}
    </div>
  );
}
