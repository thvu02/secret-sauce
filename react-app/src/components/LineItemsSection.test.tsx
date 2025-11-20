import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { LineItemsSection } from './LineItemsSection';
import type { LineItem } from '../types';

describe('LineItemsSection', () => {
  const mockLineItems: LineItem[] = [
    {
      name: 'Pizza',
      price: 20,
      quantity: 2,
      assignees: ['Alice'],
      splitMode: 'equal',
      assigneePercentages: {},
    },
    {
      name: 'Burger',
      price: 15,
      quantity: 1,
      assignees: ['Bob'],
      splitMode: 'equal',
      assigneePercentages: {},
    },
  ];

  const mockProps = {
    lineItems: mockLineItems,
    onUpdate: vi.fn(),
    onAdd: vi.fn(),
    onRemove: vi.fn(),
    onAssigneeBlur: vi.fn(),
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should render section heading', () => {
    render(<LineItemsSection {...mockProps} />);

    expect(screen.getByText('Line Items')).toBeInTheDocument();
  });

  it('should render column headers', () => {
    render(<LineItemsSection {...mockProps} />);

    expect(screen.getByText('Item Name')).toBeInTheDocument();
    expect(screen.getByText('Price')).toBeInTheDocument();
    expect(screen.getByText('Qty')).toBeInTheDocument();
    expect(screen.getByText('Assignees')).toBeInTheDocument();
    expect(screen.getByText('Split')).toBeInTheDocument();
    expect(screen.getByText('Action')).toBeInTheDocument();
  });

  it('should render all line items', () => {
    render(<LineItemsSection {...mockProps} />);

    expect(screen.getByDisplayValue('Pizza')).toBeInTheDocument();
    expect(screen.getByDisplayValue('Burger')).toBeInTheDocument();
  });

  it('should render add button', () => {
    render(<LineItemsSection {...mockProps} />);

    expect(screen.getByRole('button', { name: /add line item/i })).toBeInTheDocument();
  });

  it('should call onAdd when add button is clicked', async () => {
    const user = userEvent.setup();
    render(<LineItemsSection {...mockProps} />);

    const addButton = screen.getByRole('button', { name: /add line item/i });
    await user.click(addButton);

    expect(mockProps.onAdd).toHaveBeenCalledTimes(1);
  });

  it('should render empty state with no line items', () => {
    render(
      <LineItemsSection
        {...mockProps}
        lineItems={[]}
      />
    );

    expect(screen.queryByDisplayValue('Pizza')).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: /add line item/i })).toBeInTheDocument();
  });

  it('should pass correct props to LineItemRow components', () => {
    render(<LineItemsSection {...mockProps} />);

    // Check that both line items are rendered
    const pizzaInput = screen.getByDisplayValue('Pizza');
    const burgerInput = screen.getByDisplayValue('Burger');

    expect(pizzaInput).toBeInTheDocument();
    expect(burgerInput).toBeInTheDocument();
  });

  it('should pass onUpdate callback to line items', async () => {
    const user = userEvent.setup();
    render(<LineItemsSection {...mockProps} />);

    const nameInput = screen.getByDisplayValue('Pizza');
    await user.type(nameInput, 'A');

    // Verify onUpdate was called
    expect(mockProps.onUpdate).toHaveBeenCalled();
    expect(mockProps.onUpdate.mock.calls.length).toBeGreaterThan(0);
  });

  it('should pass onRemove callback to line items', async () => {
    const user = userEvent.setup();
    render(<LineItemsSection {...mockProps} />);

    const removeButtons = screen.getAllByRole('button', { name: /remove/i });
    await user.click(removeButtons[0]);

    expect(mockProps.onRemove).toHaveBeenCalledWith(0);
  });

  it('should handle single line item', () => {
    const singleItem = [mockLineItems[0]];

    render(
      <LineItemsSection
        {...mockProps}
        lineItems={singleItem}
      />
    );

    expect(screen.getByDisplayValue('Pizza')).toBeInTheDocument();
    expect(screen.queryByDisplayValue('Burger')).not.toBeInTheDocument();
  });

  it('should render multiple line items with correct indices', async () => {
    const user = userEvent.setup();
    render(<LineItemsSection {...mockProps} />);

    const removeButtons = screen.getAllByRole('button', { name: /remove/i });

    // Click second remove button
    await user.click(removeButtons[1]);

    expect(mockProps.onRemove).toHaveBeenCalledWith(1);
  });
});
