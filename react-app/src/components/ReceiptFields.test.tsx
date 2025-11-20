import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ReceiptFields } from './ReceiptFields';
import type { Receipt } from '../types';

describe('ReceiptFields', () => {
  const mockReceipt: Receipt = {
    vendor: 'Test Vendor',
    receiptDate: '2024-01-15',
    currency: 'USD',
    subtotal: 100,
    tax: 8,
    tip: 15,
    total: 123,
    taxTipDistribution: 'proportional',
  };

  const mockOnUpdate = vi.fn();

  beforeEach(() => {
    mockOnUpdate.mockClear();
  });

  it('should render all receipt field labels', () => {
    render(<ReceiptFields receipt={mockReceipt} onUpdate={mockOnUpdate} />);

    expect(screen.getByText('Vendor')).toBeInTheDocument();
    expect(screen.getByText('Date')).toBeInTheDocument();
    expect(screen.getByText('Currency')).toBeInTheDocument();
    expect(screen.getByText('Subtotal')).toBeInTheDocument();
    expect(screen.getByText('Tax')).toBeInTheDocument();
    expect(screen.getByText('Tip')).toBeInTheDocument();
    expect(screen.getByText('Total')).toBeInTheDocument();
  });

  it('should display receipt values', () => {
    const { container } = render(<ReceiptFields receipt={mockReceipt} onUpdate={mockOnUpdate} />);

    expect(screen.getByDisplayValue('Test Vendor')).toBeInTheDocument();
    expect(screen.getByDisplayValue('2024-01-15')).toBeInTheDocument();

    // Check select value directly from DOM
    const currencySelect = container.querySelector('select') as HTMLSelectElement;
    expect(currencySelect.value).toBe('USD');

    expect(screen.getByDisplayValue('100')).toBeInTheDocument();
    expect(screen.getByDisplayValue('8')).toBeInTheDocument();
    expect(screen.getByDisplayValue('15')).toBeInTheDocument();
    expect(screen.getByDisplayValue('123')).toBeInTheDocument();
  });

  it('should call onUpdate when vendor is changed', async () => {
    const user = userEvent.setup();
    render(<ReceiptFields receipt={mockReceipt} onUpdate={mockOnUpdate} />);

    const vendorInput = screen.getByDisplayValue('Test Vendor');
    await user.clear(vendorInput);
    await user.type(vendorInput, 'N');

    // Verify onUpdate was called with vendor field
    expect(mockOnUpdate).toHaveBeenCalled();
    const vendorCalls = mockOnUpdate.mock.calls.filter(call => call[0] === 'vendor');
    expect(vendorCalls.length).toBeGreaterThan(0);
  });

  it('should call onUpdate when date is changed', async () => {
    const user = userEvent.setup();
    render(<ReceiptFields receipt={mockReceipt} onUpdate={mockOnUpdate} />);

    const dateInput = screen.getByDisplayValue('2024-01-15');
    await user.clear(dateInput);
    await user.type(dateInput, '2024');

    // Verify onUpdate was called with receiptDate field
    expect(mockOnUpdate).toHaveBeenCalled();
    const dateCalls = mockOnUpdate.mock.calls.filter(call => call[0] === 'receiptDate');
    expect(dateCalls.length).toBeGreaterThan(0);
  });

  it('should call onUpdate when currency is changed', async () => {
    const user = userEvent.setup();
    const { container } = render(<ReceiptFields receipt={mockReceipt} onUpdate={mockOnUpdate} />);

    const currencySelect = container.querySelector('select') as HTMLSelectElement;
    await user.selectOptions(currencySelect, 'EUR');

    expect(mockOnUpdate).toHaveBeenCalledWith('currency', 'EUR');
  });

  it('should call onUpdate with numeric value when subtotal is changed', async () => {
    const user = userEvent.setup();
    const { container } = render(<ReceiptFields receipt={mockReceipt} onUpdate={mockOnUpdate} />);

    const subtotalInput = container.querySelector('input[type="number"][step="0.01"]') as HTMLInputElement;
    await user.clear(subtotalInput);
    await user.type(subtotalInput, '2');

    // Verify onUpdate was called with subtotal field
    expect(mockOnUpdate).toHaveBeenCalled();
    const subtotalCalls = mockOnUpdate.mock.calls.filter(call => call[0] === 'subtotal');
    expect(subtotalCalls.length).toBeGreaterThan(0);
  });

  it('should make total field readonly', () => {
    const { container } = render(<ReceiptFields receipt={mockReceipt} onUpdate={mockOnUpdate} />);

    const readonlyInputs = container.querySelectorAll('input[readonly]');
    expect(readonlyInputs.length).toBeGreaterThan(0);
  });

  it('should display all currency options', () => {
    const { container } = render(<ReceiptFields receipt={mockReceipt} onUpdate={mockOnUpdate} />);

    const currencySelect = container.querySelector('select') as HTMLSelectElement;
    const options = currencySelect.querySelectorAll('option');

    expect(options.length).toBeGreaterThan(0);
    expect(Array.from(options).some(opt => opt.value === 'USD')).toBe(true);
    expect(Array.from(options).some(opt => opt.value === 'EUR')).toBe(true);
    expect(Array.from(options).some(opt => opt.value === 'GBP')).toBe(true);
  });

  it('should handle empty receipt values', () => {
    const emptyReceipt: Receipt = {};
    const { container } = render(<ReceiptFields receipt={emptyReceipt} onUpdate={mockOnUpdate} />);

    const emptyInputs = container.querySelectorAll('input[value=""]');
    expect(emptyInputs.length).toBeGreaterThan(0);

    // Check default currency
    const currencySelect = container.querySelector('select') as HTMLSelectElement;
    expect(currencySelect.value).toBe('USD');
  });

  it('should render TaxTipDistributionToggle component', () => {
    render(<ReceiptFields receipt={mockReceipt} onUpdate={mockOnUpdate} />);

    expect(screen.getByText('Tax & Tip Distribution')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /proportional/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /even/i })).toBeInTheDocument();
  });
});
