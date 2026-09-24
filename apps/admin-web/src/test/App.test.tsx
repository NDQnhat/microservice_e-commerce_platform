import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { App } from '../App';

describe('Admin Web App', () => {
  it('renders admin web layout and operational dashboard', () => {
    render(<App />);
    expect(screen.getByText('Backoffice Ops')).toBeInTheDocument();
    expect(screen.getByText('Dashboard')).toBeInTheDocument();
    expect(screen.getByText('Exceptions Board')).toBeInTheDocument();
  });
});
