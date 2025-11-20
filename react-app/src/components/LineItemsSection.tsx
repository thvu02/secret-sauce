import React from 'react';
import type { LineItem } from '../types';
import { LineItemRow } from './LineItemRow';

interface LineItemsSectionProps {
    lineItems: LineItem[];
    onUpdate: (index: number, key: keyof LineItem, value: unknown) => void;
    onAdd: () => void;
    onRemove: (index: number) => void;
    onAssigneeBlur: (index: number) => void;
}

export const LineItemsSection: React.FC<LineItemsSectionProps> = ({
    lineItems,
    onUpdate,
    onAdd,
    onRemove,
    onAssigneeBlur,
}) => {
    return (
        <div className="mt-4">
            <h4 className="font-medium mb-3 text-lg">Line Items</h4>

            {/* Column Headers */}
            <div className="mb-2 px-3 py-2 bg-gray-100 rounded flex gap-2 items-center text-sm font-medium text-gray-700">
                <div className="flex-1">Item Name</div>
                <div className="w-28">Price</div>
                <div className="w-20">Qty</div>
                <div className="w-48">Assignees</div>
                <div className="w-20">Split</div>
                <div className="w-20">Action</div>
            </div>

            {lineItems.map((lineItem, index) => (
                <LineItemRow
                    key={index}
                    lineItem={lineItem}
                    index={index}
                    onUpdate={onUpdate}
                    onRemove={onRemove}
                    onAssigneeBlur={onAssigneeBlur}
                />
            ))}

            <button
                className="mt-3 px-4 py-2 bg-green-600 text-white rounded hover:bg-green-700 font-medium"
                onClick={onAdd}
            >
                + Add Line Item
            </button>
        </div>
    );
};
