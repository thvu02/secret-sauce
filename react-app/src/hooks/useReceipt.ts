import { useState } from 'react';
import type { Receipt, LineItem } from '../types';
import { recalculateTotals, calculateSubtotal } from '../utils/receiptCalculations';

export function useReceipt() {
    const [receipt, setReceipt] = useState<Receipt | null>(null);

    const updateField = <K extends keyof Receipt>(key: K, value: Receipt[K]) => {
        setReceipt((current) => {
            if (!current) return current;

            const updated = { ...current, [key]: value };

            // Recalculate totals if financial fields change
            if (['subtotal', 'tax', 'tip'].includes(key)) {
                return recalculateTotals(updated);
            }

            return updated;
        });
    };

    const updateLineItem = (index: number, key: keyof LineItem, value: unknown) => {
        setReceipt((current) => {
            if (!current) return current;

            const items = (current.lineItems || []).map((item, i) => {
                if (i !== index) return item;

                // Handle assignees text input
                if (key === 'assignees') {
                    return { ...item, assigneesText: String(value) };
                }

                // Convert numeric fields
                if (key === 'price' || key === 'quantity') {
                    return { ...item, [key]: Number(value) };
                }

                return { ...item, [key]: value };
            });

            const subtotal = calculateSubtotal(items);
            return recalculateTotals({ ...current, lineItems: items, subtotal });
        });
    };

    const addLineItem = () => {
        setReceipt((current) => ({
            ...current!,
            lineItems: [
                ...(current?.lineItems || []),
                {
                    name: '',
                    price: 0,
                    quantity: 1,
                    assignees: [],
                    splitMode: 'equal',
                    assigneePercentages: {}
                },
            ],
        }));
    };

    const removeLineItem = (index: number) => {
        setReceipt((current) => {
            if (!current) return current;

            const lineItems = (current.lineItems || []).filter((_, i) => i !== index);
            const subtotal = calculateSubtotal(lineItems);
            const updated = { ...current, lineItems, subtotal };

            return recalculateTotals(updated);
        });
    };

    return {
        receipt,
        setReceipt,
        updateField,
        updateLineItem,
        addLineItem,
        removeLineItem,
    };
}
