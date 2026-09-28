import React from 'react';
import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import HomePage from '../app/page';

describe('HomePage Component', () => {
  it('renders editorial hero banner and key sections', () => {
    render(<HomePage />);
    expect(screen.getByText(/Bộ sưu tập Xuân Hè 2026/)).toBeInTheDocument();
    expect(screen.getByText('Danh mục nổi bật')).toBeInTheDocument();
    expect(screen.getByText('Ưu đãi chớp nhoáng')).toBeInTheDocument();
    expect(screen.getByText('Sản phẩm bán chạy nhất')).toBeInTheDocument();
    expect(screen.getByText('Khám phá ngay')).toBeInTheDocument();
  });
});
