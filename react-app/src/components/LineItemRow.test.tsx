import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { LineItemRow } from './LineItemRow';
import type { LineItem } from '../types';

describe('LineItemRow', () => {
  const mockLineItem: LineItem = {
    name: 'Pizza',
    price: 20,
    quantity: 2,
    assignees: ['Alice', 'Bob'],
    splitMode: 'equal',
    assigneePercentages: {},
  };

  const mockProps = {
    lineItem: mockLineItem,
    index: 0,
    onUpdate: vi.fn(),
    onRemove: vi.fn(),
    onAssigneeBlur: vi.fn(),
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should render line item fields', () => {
    render(<LineItemRow {...mockProps} />);

    expect(screen.getByPlaceholderText('Item name')).toHaveValue('Pizza');
    expect(screen.getByPlaceholderText('Price')).toHaveValue(20);
    expect(screen.getByPlaceholderText('Qty')).toHaveValue(2);
  });

  it('should call onUpdate when name is changed', async () => {
    const user = userEvent.setup();
    render(<LineItemRow {...mockProps} />);

    const nameInput = screen.getByPlaceholderText('Item name');
    await user.clear(nameInput);
    await user.type(nameInput, 'B');

    // Check that onUpdate was called
    expect(mockProps.onUpdate).toHaveBeenCalled();
    // Check the last call has the name field
    const lastCall = mockProps.onUpdate.mock.calls[mockProps.onUpdate.mock.calls.length - 1];
    expect(lastCall[0]).toBe(0);
    expect(lastCall[1]).toBe('name');
  });

  it('should call onUpdate when price is changed', async () => {
    const user = userEvent.setup();
    render(<LineItemRow {...mockProps} />);

    const priceInput = screen.getByPlaceholderText('Price');
    await user.clear(priceInput);
    await user.type(priceInput, '25');

    expect(mockProps.onUpdate).toHaveBeenCalled();
    // Check one of the calls has the price update
    const priceCalls = mockProps.onUpdate.mock.calls.filter(call => call[1] === 'price');
    expect(priceCalls.length).toBeGreaterThan(0);
  });

  it('should call onUpdate when quantity is changed', async () => {
    const user = userEvent.setup();
    render(<LineItemRow {...mockProps} />);

    const qtyInput = screen.getByPlaceholderText('Qty');
    await user.clear(qtyInput);
    await user.type(qtyInput, '3');

    expect(mockProps.onUpdate).toHaveBeenCalled();
    const qtyCalls = mockProps.onUpdate.mock.calls.filter(call => call[1] === 'quantity');
    expect(qtyCalls.length).toBeGreaterThan(0);
  });

  it('should call onUpdate when assignees are changed', async () => {
    const user = userEvent.setup();
    render(<LineItemRow {...mockProps} />);

    const assigneesInput = screen.getByPlaceholderText('Assignees (comma-separated)');
    await user.clear(assigneesInput);
    await user.type(assigneesInput, 'Charlie');

    expect(mockProps.onUpdate).toHaveBeenCalled();
    const lastCall = mockProps.onUpdate.mock.calls[mockProps.onUpdate.mock.calls.length - 1];
    expect(lastCall[1]).toBe('assignees');
  });

  it('should call onAssigneeBlur when assignees input loses focus', async () => {
    const user = userEvent.setup();
    render(<LineItemRow {...mockProps} />);

    const assigneesInput = screen.getByPlaceholderText('Assignees (comma-separated)');
    await user.click(assigneesInput);
    await user.tab();

    expect(mockProps.onAssigneeBlur).toHaveBeenCalledWith(0);
  });

  it('should call onRemove when remove button is clicked', async () => {
    const user = userEvent.setup();
    render(<LineItemRow {...mockProps} />);

    const removeButton = screen.getByRole('button', { name: /remove/i });
    await user.click(removeButton);

    expect(mockProps.onRemove).toHaveBeenCalledWith(0);
  });

  it('should display equal split summary when in equal mode', () => {
    render(<LineItemRow {...mockProps} />);

    expect(screen.getByText('Equal Split:')).toBeInTheDocument();
    expect(screen.getByText('Alice')).toBeInTheDocument();
    expect(screen.getByText('Bob')).toBeInTheDocument();
    // There may be multiple $20.00 elements, just verify at least one exists
    expect(screen.getAllByText('$20.00').length).toBeGreaterThan(0); // 40 total / 2 people = 20 each
  });

  it('should not display split summary when no assignees', () => {
    const lineItemNoAssignees: LineItem = {
      ...mockLineItem,
      assignees: [],
    };

    render(
      <LineItemRow
        {...mockProps}
        lineItem={lineItemNoAssignees}
      />
    );

    expect(screen.queryByText('Equal Split:')).not.toBeInTheDocument();
  });

  it('should toggle split mode when toggle button is clicked', async () => {
    const user = userEvent.setup();
    render(<LineItemRow {...mockProps} />);

    const toggleButton = screen.getByRole('button', { name: /equal/i });
    await user.click(toggleButton);

    expect(mockProps.onAssigneeBlur).toHaveBeenCalled();
    expect(mockProps.onUpdate).toHaveBeenCalledWith(0, 'splitMode', 'percentage');
  });

  it('should disable split mode toggle when no assignees', () => {
    const lineItemNoAssignees: LineItem = {
      ...mockLineItem,
      assignees: [],
    };

    render(
      <LineItemRow
        {...mockProps}
        lineItem={lineItemNoAssignees}
      />
    );

    const toggleButton = screen.getByRole('button', { name: /equal/i });
    expect(toggleButton).toBeDisabled();
  });

  it('should display percentage split panel when in percentage mode', () => {
    const lineItemPercentage: LineItem = {
      ...mockLineItem,
      splitMode: 'percentage',
      assigneePercentages: { Alice: 60, Bob: 40 },
    };

    render(
      <LineItemRow
        {...mockProps}
        lineItem={lineItemPercentage}
      />
    );

    expect(screen.getByText('💰 Custom Split Distribution')).toBeInTheDocument();
    expect(screen.getByText('✓ Valid (100%)')).toBeInTheDocument();
  });

  it('should show validation error when percentages do not sum to 100', () => {
    const lineItemInvalid: LineItem = {
      ...mockLineItem,
      splitMode: 'percentage',
      assigneePercentages: { Alice: 60, Bob: 30 }, // Only 90%
    };

    render(
      <LineItemRow
        {...mockProps}
        lineItem={lineItemInvalid}
      />
    );

    expect(screen.getByText(/total: 90%/i)).toBeInTheDocument();
    expect(screen.getByText(/must equal 100%/i)).toBeInTheDocument();
  });

  it('should initialize equal percentages when switching to percentage mode', async () => {
    const user = userEvent.setup();
    render(<LineItemRow {...mockProps} />);

    const toggleButton = screen.getByRole('button', { name: /equal/i });
    await user.click(toggleButton);

    // Should call onUpdate with assigneePercentages
    const percentageCalls = mockProps.onUpdate.mock.calls.filter(
      call => call[1] === 'assigneePercentages'
    );
    expect(percentageCalls.length).toBeGreaterThan(0);
    expect(percentageCalls[0][2]).toEqual({ Alice: 50, Bob: 50 });
  });

  it('should calculate correct dollar amounts in percentage mode', () => {
    const lineItemPercentage: LineItem = {
      name: 'Pizza',
      price: 40,
      quantity: 1,
      assignees: ['Alice', 'Bob'],
      splitMode: 'percentage',
      assigneePercentages: { Alice: 75, Bob: 25 },
    };

    render(
      <LineItemRow
        {...mockProps}
        lineItem={lineItemPercentage}
      />
    );

    // Alice gets 75% of $40 = $30
    expect(screen.getByText('$30.00')).toBeInTheDocument();

    // Bob gets 25% of $40 = $10
    expect(screen.getByText('$10.00')).toBeInTheDocument();
  });

  it('should handle assigneesText instead of assignees array', () => {
    const lineItemWithText: LineItem = {
      ...mockLineItem,
      assignees: undefined,
      assigneesText: 'Charlie, Dave, Eve',
    };

    render(
      <LineItemRow
        {...mockProps}
        lineItem={lineItemWithText}
      />
    );

    const assigneesInput = screen.getByPlaceholderText('Assignees (comma-separated)');
    expect(assigneesInput).toHaveValue('Charlie, Dave, Eve');
  });

  it('should render percentage sliders in percentage mode', () => {
    const lineItemPercentage: LineItem = {
      ...mockLineItem,
      splitMode: 'percentage',
      assigneePercentages: { Alice: 50, Bob: 50 },
    };

    render(
      <LineItemRow
        {...mockProps}
        lineItem={lineItemPercentage}
      />
    );

    // Verify sliders are rendered
    const sliders = screen.getAllByRole('slider');
    expect(sliders.length).toBe(2); // One for each assignee
  });
});
