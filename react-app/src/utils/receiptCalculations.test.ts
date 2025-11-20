import { describe, it, expect } from 'vitest';
import {
  recalculateTotals,
  calculateSubtotal,
  normalizeLineItemAssignees,
  normalizeReceiptLineItems,
} from './receiptCalculations';
import type { Receipt, LineItem } from '../types';

describe('receiptCalculations', () => {
  describe('recalculateTotals', () => {
    it('should calculate total from subtotal, tax, and tip', () => {
      const receipt: Receipt = {
        subtotal: 100,
        tax: 8.5,
        tip: 15,
      };

      const result = recalculateTotals(receipt);

      expect(result.total).toBe(123.5);
    });

    it('should round values to 2 decimal places', () => {
      const receipt: Receipt = {
        subtotal: 100.123,
        tax: 8.567,
        tip: 15.999,
      };

      const result = recalculateTotals(receipt);

      expect(result.subtotal).toBe(100.12);
      expect(result.tax).toBe(8.57);
      expect(result.tip).toBe(16);
      expect(result.total).toBe(124.69);
    });

    it('should calculate tax percentage', () => {
      const receipt: Receipt = {
        subtotal: 100,
        tax: 8.5,
        tip: 0,
      };

      const result = recalculateTotals(receipt);

      expect(result.taxPercentage).toBe(8.5);
    });

    it('should calculate tip percentage', () => {
      const receipt: Receipt = {
        subtotal: 100,
        tax: 0,
        tip: 18,
      };

      const result = recalculateTotals(receipt);

      expect(result.tipPercentage).toBe(18);
    });

    it('should handle zero subtotal without division by zero', () => {
      const receipt: Receipt = {
        subtotal: 0,
        tax: 5,
        tip: 10,
      };

      const result = recalculateTotals(receipt);

      expect(result.taxPercentage).toBe(0);
      expect(result.tipPercentage).toBe(0);
      expect(result.total).toBe(15);
    });

    it('should handle undefined values as zero', () => {
      const receipt: Receipt = {};

      const result = recalculateTotals(receipt);

      expect(result.subtotal).toBe(0);
      expect(result.tax).toBe(0);
      expect(result.tip).toBe(0);
      expect(result.total).toBe(0);
      expect(result.taxPercentage).toBe(0);
      expect(result.tipPercentage).toBe(0);
    });

    it('should preserve other receipt properties', () => {
      const receipt: Receipt = {
        uid: '123',
        vendor: 'Test Vendor',
        subtotal: 50,
        tax: 5,
        tip: 10,
      };

      const result = recalculateTotals(receipt);

      expect(result.uid).toBe('123');
      expect(result.vendor).toBe('Test Vendor');
    });
  });

  describe('calculateSubtotal', () => {
    it('should calculate subtotal from line items', () => {
      const lineItems: LineItem[] = [
        { price: 10, quantity: 2 },
        { price: 15, quantity: 1 },
        { price: 8.5, quantity: 3 },
      ];

      const result = calculateSubtotal(lineItems);

      expect(result).toBe(60.5); // (10*2) + (15*1) + (8.5*3)
    });

    it('should default quantity to 1 if not provided', () => {
      const lineItems: LineItem[] = [
        { price: 10 },
        { price: 15 },
      ];

      const result = calculateSubtotal(lineItems);

      expect(result).toBe(25); // 10 + 15
    });

    it('should handle undefined prices as zero', () => {
      const lineItems: LineItem[] = [
        { price: 10, quantity: 2 },
        { quantity: 3 },
      ];

      const result = calculateSubtotal(lineItems);

      expect(result).toBe(20); // 10*2 + 0*3
    });

    it('should round result to 2 decimal places', () => {
      const lineItems: LineItem[] = [
        { price: 10.123, quantity: 2 },
        { price: 5.456, quantity: 1 },
      ];

      const result = calculateSubtotal(lineItems);

      expect(result).toBe(25.7); // (10.12*2) + (5.46*1) = 25.70
    });

    it('should return 0 for empty array', () => {
      const result = calculateSubtotal([]);

      expect(result).toBe(0);
    });
  });

  describe('normalizeLineItemAssignees', () => {
    it('should convert assigneesText to assignees array', () => {
      const lineItem: LineItem = {
        assigneesText: 'Alice, Bob, Charlie',
      };

      const result = normalizeLineItemAssignees(lineItem);

      expect(result.assignees).toEqual(['Alice', 'Bob', 'Charlie']);
    });

    it('should trim whitespace from assignee names', () => {
      const lineItem: LineItem = {
        assigneesText: '  Alice  ,  Bob  ,  Charlie  ',
      };

      const result = normalizeLineItemAssignees(lineItem);

      expect(result.assignees).toEqual(['Alice', 'Bob', 'Charlie']);
    });

    it('should filter out empty strings', () => {
      const lineItem: LineItem = {
        assigneesText: 'Alice,,Bob,,,Charlie,',
      };

      const result = normalizeLineItemAssignees(lineItem);

      expect(result.assignees).toEqual(['Alice', 'Bob', 'Charlie']);
    });

    it('should use existing assignees array if assigneesText is not provided', () => {
      const lineItem: LineItem = {
        assignees: ['Alice', 'Bob'],
      };

      const result = normalizeLineItemAssignees(lineItem);

      expect(result.assignees).toEqual(['Alice', 'Bob']);
    });

    it('should handle empty assigneesText', () => {
      const lineItem: LineItem = {
        assigneesText: '',
      };

      const result = normalizeLineItemAssignees(lineItem);

      expect(result.assignees).toEqual([]);
    });

    it('should handle undefined assignees and assigneesText', () => {
      const lineItem: LineItem = {
        name: 'Test Item',
      };

      const result = normalizeLineItemAssignees(lineItem);

      expect(result.assignees).toEqual([]);
    });

    it('should preserve other lineItem properties', () => {
      const lineItem: LineItem = {
        name: 'Pizza',
        price: 15.99,
        quantity: 2,
        assigneesText: 'Alice, Bob',
      };

      const result = normalizeLineItemAssignees(lineItem);

      expect(result.name).toBe('Pizza');
      expect(result.price).toBe(15.99);
      expect(result.quantity).toBe(2);
      expect(result.assignees).toEqual(['Alice', 'Bob']);
    });
  });

  describe('normalizeReceiptLineItems', () => {
    it('should normalize all line items in a receipt', () => {
      const receipt: Receipt = {
        lineItems: [
          { assigneesText: 'Alice, Bob' },
          { assigneesText: 'Charlie, Dave' },
          { assigneesText: 'Eve' },
        ],
      };

      const result = normalizeReceiptLineItems(receipt);

      expect(result.lineItems).toEqual([
        { assigneesText: 'Alice, Bob', assignees: ['Alice', 'Bob'] },
        { assigneesText: 'Charlie, Dave', assignees: ['Charlie', 'Dave'] },
        { assigneesText: 'Eve', assignees: ['Eve'] },
      ]);
    });

    it('should handle receipt with no lineItems', () => {
      const receipt: Receipt = {
        vendor: 'Test',
      };

      const result = normalizeReceiptLineItems(receipt);

      expect(result.lineItems).toEqual([]);
    });

    it('should handle empty lineItems array', () => {
      const receipt: Receipt = {
        lineItems: [],
      };

      const result = normalizeReceiptLineItems(receipt);

      expect(result.lineItems).toEqual([]);
    });

    it('should preserve other receipt properties', () => {
      const receipt: Receipt = {
        uid: '123',
        vendor: 'Test Vendor',
        total: 100,
        lineItems: [
          { assigneesText: 'Alice' },
        ],
      };

      const result = normalizeReceiptLineItems(receipt);

      expect(result.uid).toBe('123');
      expect(result.vendor).toBe('Test Vendor');
      expect(result.total).toBe(100);
    });
  });
});
