import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import HomePage from '../app/page';

describe('HomePage', () => {
  it('renders storefront hero banner and featured products section', () => {
    render(<HomePage />);
    expect(
      screen.getByText('Discover Exceptional Everyday Essentials')
    ).toBeInTheDocument();
    expect(screen.getByText('Featured Products')).toBeInTheDocument();
  });
});
