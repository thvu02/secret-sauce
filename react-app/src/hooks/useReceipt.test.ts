import { describe, it, expect } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { useReceipt } from './useReceipt';
import type { Receipt } from '../types';

describe('useReceipt', () => {
  const mockReceipt: Receipt = {
    uid: '123',
    vendor: 'Test Vendor',
    subtotal: 100,
    tax: 8,
    tip: 15,
    total: 123,
    lineItems: [
      { name: 'Item 1', price: 50, quantity: 1, assignees: [] },
      { name: 'Item 2', price: 50, quantity: 1, assignees: [] },
    ],
  };

  it('should initialize with null receipt', () => {
    const { result } = renderHook(() => useReceipt());

    expect(result.current.receipt).toBeNull();
  });

  it('should set receipt', () => {
    const { result } = renderHook(() => useReceipt());

    act(() => {
      result.current.setReceipt(mockReceipt);
    });

    expect(result.current.receipt).toEqual(mockReceipt);
  });

  describe('updateField', () => {
    it('should update a simple field', () => {
      const { result } = renderHook(() => useReceipt());

      act(() => {
        result.current.setReceipt(mockReceipt);
      });

      act(() => {
        result.current.updateField('vendor', 'New Vendor');
      });

      expect(result.current.receipt?.vendor).toBe('New Vendor');
    });

    it('should recalculate totals when updating subtotal', () => {
      const { result } = renderHook(() => useReceipt());

      act(() => {
        result.current.setReceipt(mockReceipt);
      });

      act(() => {
        result.current.updateField('subtotal', 200);
      });

      expect(result.current.receipt?.subtotal).toBe(200);
      expect(result.current.receipt?.total).toBe(223); // 200 + 8 + 15
      expect(result.current.receipt?.taxPercentage).toBe(4); // 8/200 * 100
      expect(result.current.receipt?.tipPercentage).toBe(7.5); // 15/200 * 100
    });

    it('should recalculate totals when updating tax', () => {
      const { result } = renderHook(() => useReceipt());

      act(() => {
        result.current.setReceipt(mockReceipt);
      });

      act(() => {
        result.current.updateField('tax', 10);
      });

      expect(result.current.receipt?.tax).toBe(10);
      expect(result.current.receipt?.total).toBe(125); // 100 + 10 + 15
      expect(result.current.receipt?.taxPercentage).toBe(10); // 10/100 * 100
    });

    it('should recalculate totals when updating tip', () => {
      const { result } = renderHook(() => useReceipt());

      act(() => {
        result.current.setReceipt(mockReceipt);
      });

      act(() => {
        result.current.updateField('tip', 20);
      });

      expect(result.current.receipt?.tip).toBe(20);
      expect(result.current.receipt?.total).toBe(128); // 100 + 8 + 20
      expect(result.current.receipt?.tipPercentage).toBe(20); // 20/100 * 100
    });

    it('should not update when receipt is null', () => {
      const { result } = renderHook(() => useReceipt());

      act(() => {
        result.current.updateField('vendor', 'New Vendor');
      });

      expect(result.current.receipt).toBeNull();
    });
  });

  describe('updateLineItem', () => {
    it('should update line item name', () => {
      const { result } = renderHook(() => useReceipt());

      act(() => {
        result.current.setReceipt(mockReceipt);
      });

      act(() => {
        result.current.updateLineItem(0, 'name', 'Updated Item');
      });

      expect(result.current.receipt?.lineItems?.[0].name).toBe('Updated Item');
    });

    it('should update line item price and recalculate subtotal', () => {
      const { result } = renderHook(() => useReceipt());

      act(() => {
        result.current.setReceipt(mockReceipt);
      });

      act(() => {
        result.current.updateLineItem(0, 'price', 75);
      });

      expect(result.current.receipt?.lineItems?.[0].price).toBe(75);
      expect(result.current.receipt?.subtotal).toBe(125); // 75 + 50
      expect(result.current.receipt?.total).toBe(148); // 125 + 8 + 15
    });

    it('should update line item quantity and recalculate subtotal', () => {
      const { result } = renderHook(() => useReceipt());

      act(() => {
        result.current.setReceipt(mockReceipt);
      });

      act(() => {
        result.current.updateLineItem(0, 'quantity', 3);
      });

      expect(result.current.receipt?.lineItems?.[0].quantity).toBe(3);
      expect(result.current.receipt?.subtotal).toBe(200); // (50*3) + 50
      expect(result.current.receipt?.total).toBe(223); // 200 + 8 + 15
    });

    it('should update assignees as assigneesText', () => {
      const { result } = renderHook(() => useReceipt());

      act(() => {
        result.current.setReceipt(mockReceipt);
      });

      act(() => {
        result.current.updateLineItem(0, 'assignees', 'Alice, Bob');
      });

      expect(result.current.receipt?.lineItems?.[0].assigneesText).toBe('Alice, Bob');
    });

    it('should convert string values to numbers for price', () => {
      const { result } = renderHook(() => useReceipt());

      act(() => {
        result.current.setReceipt(mockReceipt);
      });

      act(() => {
        result.current.updateLineItem(0, 'price', '99.99');
      });

      expect(result.current.receipt?.lineItems?.[0].price).toBe(99.99);
      expect(typeof result.current.receipt?.lineItems?.[0].price).toBe('number');
    });

    it('should not modify other line items', () => {
      const { result } = renderHook(() => useReceipt());

      act(() => {
        result.current.setReceipt(mockReceipt);
      });

      act(() => {
        result.current.updateLineItem(0, 'name', 'Updated Item');
      });

      expect(result.current.receipt?.lineItems?.[0].name).toBe('Updated Item');
      expect(result.current.receipt?.lineItems?.[1].name).toBe('Item 2');
    });

    it('should not update when receipt is null', () => {
      const { result } = renderHook(() => useReceipt());

      act(() => {
        result.current.updateLineItem(0, 'name', 'Updated');
      });

      expect(result.current.receipt).toBeNull();
    });
  });

  describe('addLineItem', () => {
    it('should add a new line item with default values', () => {
      const { result } = renderHook(() => useReceipt());

      act(() => {
        result.current.setReceipt(mockReceipt);
      });

      act(() => {
        result.current.addLineItem();
      });

      const lineItems = result.current.receipt?.lineItems;
      expect(lineItems).toHaveLength(3);
      expect(lineItems?.[2]).toEqual({
        name: '',
        price: 0,
        quantity: 1,
        assignees: [],
        splitMode: 'equal',
        assigneePercentages: {},
      });
    });

    it('should add line item to empty lineItems array', () => {
      const { result } = renderHook(() => useReceipt());

      act(() => {
        result.current.setReceipt({ ...mockReceipt, lineItems: [] });
      });

      act(() => {
        result.current.addLineItem();
      });

      expect(result.current.receipt?.lineItems).toHaveLength(1);
    });

    it('should add line item when lineItems is undefined', () => {
      const { result } = renderHook(() => useReceipt());

      act(() => {
        result.current.setReceipt({ ...mockReceipt, lineItems: undefined });
      });

      act(() => {
        result.current.addLineItem();
      });

      expect(result.current.receipt?.lineItems).toHaveLength(1);
    });
  });

  describe('removeLineItem', () => {
    it('should remove line item at specified index', () => {
      const { result } = renderHook(() => useReceipt());

      act(() => {
        result.current.setReceipt(mockReceipt);
      });

      act(() => {
        result.current.removeLineItem(0);
      });

      expect(result.current.receipt?.lineItems).toHaveLength(1);
      expect(result.current.receipt?.lineItems?.[0].name).toBe('Item 2');
    });

    it('should recalculate subtotal after removing line item', () => {
      const { result } = renderHook(() => useReceipt());

      act(() => {
        result.current.setReceipt(mockReceipt);
      });

      act(() => {
        result.current.removeLineItem(0);
      });

      expect(result.current.receipt?.subtotal).toBe(50); // Only Item 2 remains
      expect(result.current.receipt?.total).toBe(73); // 50 + 8 + 15
    });

    it('should handle removing last line item', () => {
      const { result } = renderHook(() => useReceipt());

      act(() => {
        result.current.setReceipt(mockReceipt);
      });

      act(() => {
        result.current.removeLineItem(0);
        result.current.removeLineItem(0);
      });

      expect(result.current.receipt?.lineItems).toHaveLength(0);
      expect(result.current.receipt?.subtotal).toBe(0);
      expect(result.current.receipt?.total).toBe(23); // 0 + 8 + 15
    });

    it('should not update when receipt is null', () => {
      const { result } = renderHook(() => useReceipt());

      act(() => {
        result.current.removeLineItem(0);
      });

      expect(result.current.receipt).toBeNull();
    });
  });
});
