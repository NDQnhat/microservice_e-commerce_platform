'use client';

import React, { useState } from 'react';

interface ProductGalleryProps {
  mediaUrls: string[];
  productName: string;
}

export function ProductGallery({ mediaUrls, productName }: ProductGalleryProps) {
  const images = mediaUrls.length > 0 ? mediaUrls : [
    'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=800'
  ];
  const [selectedIdx, setSelectedIdx] = useState(0);

  return (
    <div className="flex flex-col-reverse lg:flex-row gap-4">
      {/* Thumbnail Strip */}
      {images.length > 1 && (
        <div className="flex lg:flex-col gap-3 overflow-x-auto lg:overflow-y-auto pb-2 lg:pb-0 scrollbar-thin">
          {images.map((url, idx) => (
            <button
              key={idx}
              onClick={() => setSelectedIdx(idx)}
              className={`relative w-16 h-16 sm:w-20 sm:h-20 rounded-xl overflow-hidden bg-zinc-100 border-2 transition shrink-0 ${
                selectedIdx === idx ? 'border-zinc-900 shadow-xs' : 'border-transparent hover:border-zinc-300'
              }`}
            >
              <img
                src={url}
                alt={`${productName} thumbnail ${idx + 1}`}
                className="w-full h-full object-cover object-center"
              />
            </button>
          ))}
        </div>
      )}

      {/* Main Large Image Frame (3:4 or 1:1 portrait) */}
      <div className="flex-1 rounded-2xl overflow-hidden bg-zinc-100 border border-zinc-200/80 aspect-square sm:aspect-4/3 lg:aspect-square relative group">
        <img
          src={images[selectedIdx]}
          alt={productName}
          className="w-full h-full object-cover object-center transition-transform duration-300 group-hover:scale-105"
        />
      </div>
    </div>
  );
}
