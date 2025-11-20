import type { Receipt, LineItem } from '../types';

/**
 * Round to 2 decimal places for money values
 */
const round2 = (value: number): number => Math.round(value * 100) / 100;

/**
 * Recalculate receipt totals and percentages
 */
export function recalculateTotals(receipt: Receipt): Receipt {
    const subtotal = round2(receipt.subtotal || 0);
    const tax = round2(receipt.tax || 0);
    const tip = round2(receipt.tip || 0);
    const total = round2(subtotal + tax + tip);

    const taxPercentage = subtotal > 0 ? round2((tax / subtotal) * 100) : 0;
    const tipPercentage = subtotal > 0 ? round2((tip / subtotal) * 100) : 0;

    return {
        ...receipt,
        subtotal,
        tax,
        tip,
        total,
        taxPercentage,
        tipPercentage,
    };
}

/**
 * Calculate subtotal from line items
 */
export function calculateSubtotal(lineItems: LineItem[]): number {
    const total = lineItems.reduce(
        (sum, item) => sum + (round2(item.price || 0) * (item.quantity || 1)),
        0
    );
    return round2(total);
}

/**
 * Normalize assignees from text input to array
 */
export function normalizeLineItemAssignees(lineItem: LineItem): LineItem {
    const text = lineItem.assigneesText ?? (lineItem.assignees || []).join(', ');
    const assignees = String(text)
        .split(',')
        .map(s => s.trim())
        .filter(Boolean);

    return {
        ...lineItem,
        assignees,
    };
}

/**
 * Normalize all line items in a receipt
 */
export function normalizeReceiptLineItems(receipt: Receipt): Receipt {
    return {
        ...receipt,
        lineItems: (receipt.lineItems || []).map(normalizeLineItemAssignees),
    };
}
