import React from 'react';
import type { Receipt } from '../types';
import { TaxTipDistributionToggle } from './TaxTipDistributionToggle';

interface ReceiptFieldsProps {
    receipt: Receipt;
    onUpdate: <K extends keyof Receipt>(key: K, value: Receipt[K]) => void;
}

const CURRENCY_OPTIONS = [
    { code: 'USD', label: 'USD - US Dollar' },
    { code: 'EUR', label: 'EUR - Euro' },
    { code: 'GBP', label: 'GBP - British Pound' },
    { code: 'CAD', label: 'CAD - Canadian Dollar' },
    { code: 'AUD', label: 'AUD - Australian Dollar' },
    { code: 'JPY', label: 'JPY - Japanese Yen' },
    { code: 'CNY', label: 'CNY - Chinese Yuan' },
    { code: 'INR', label: 'INR - Indian Rupee' },
    { code: 'MXN', label: 'MXN - Mexican Peso' },
    { code: 'CHF', label: 'CHF - Swiss Franc' },
    { code: 'NZD', label: 'NZD - New Zealand Dollar' },
    { code: 'SGD', label: 'SGD - Singapore Dollar' },
];

export const ReceiptFields: React.FC<ReceiptFieldsProps> = ({ receipt, onUpdate }) => {
    return (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
                <label className="block text-sm font-medium mb-1">Vendor</label>
                <input
                    className="w-full border p-2 rounded"
                    value={receipt.vendor || ''}
                    onChange={(e) => onUpdate('vendor', e.target.value)}
                />
            </div>

            <div>
                <label className="block text-sm font-medium mb-1">Date</label>
                <input
                    className="w-full border p-2 rounded"
                    value={receipt.receiptDate || ''}
                    onChange={(e) => onUpdate('receiptDate', e.target.value)}
                />
            </div>

            <div>
                <label className="block text-sm font-medium mb-1">Currency</label>
                <select
                    className="w-full border p-2 rounded"
                    value={receipt.currency || 'USD'}
                    onChange={(e) => onUpdate('currency', e.target.value)}
                >
                    {CURRENCY_OPTIONS.map((currency) => (
                        <option key={currency.code} value={currency.code}>
                            {currency.label}
                        </option>
                    ))}
                </select>
            </div>

            <div>
                <label className="block text-sm font-medium mb-1">Subtotal</label>
                <input
                    className="w-full border p-2 rounded"
                    type="number"
                    step="0.01"
                    value={receipt.subtotal ?? 0}
                    onChange={(e) => onUpdate('subtotal', parseFloat(e.target.value || '0'))}
                />
            </div>

            <div>
                <label className="block text-sm font-medium mb-1">Tax</label>
                <input
                    className="w-full border p-2 rounded"
                    type="number"
                    step="0.01"
                    value={receipt.tax ?? 0}
                    onChange={(e) => onUpdate('tax', parseFloat(e.target.value || '0'))}
                />
            </div>

            <div>
                <label className="block text-sm font-medium mb-1">Tip</label>
                <input
                    className="w-full border p-2 rounded"
                    type="number"
                    step="0.01"
                    value={receipt.tip ?? 0}
                    onChange={(e) => onUpdate('tip', parseFloat(e.target.value || '0'))}
                />
            </div>

            <div>
                <label className="block text-sm font-medium mb-1">Total</label>
                <input
                    className="w-full border p-2 rounded bg-gray-50"
                    type="number"
                    step="0.01"
                    value={receipt.total ?? 0}
                    readOnly
                />
            </div>

            <div className="md:col-span-2">
                <TaxTipDistributionToggle
                    value={receipt.taxTipDistribution || 'proportional'}
                    onChange={(value) => onUpdate('taxTipDistribution', value)}
                />
            </div>
        </div>
    );
};
