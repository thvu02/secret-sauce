import React from 'react';
import type { LineItem } from '../types';

interface LineItemRowProps {
    lineItem: LineItem;
    index: number;
    onUpdate: (index: number, key: keyof LineItem, value: unknown) => void;
    onRemove: (index: number) => void;
    onAssigneeBlur: (index: number) => void;
}

// Utility: Round to 2 decimal places
const round2 = (value: number): number => Math.round(value * 100) / 100;

// Utility: Parse assignees from text or array
const parseAssignees = (lineItem: LineItem): string[] => {
    const text = lineItem.assigneesText ?? (lineItem.assignees || []).join(', ');
    return text ? String(text).split(',').map(s => s.trim()).filter(Boolean) : [];
};

export const LineItemRow: React.FC<LineItemRowProps> = ({
    lineItem,
    index,
    onUpdate,
    onRemove,
    onAssigneeBlur,
}) => {
    const splitMode = lineItem.splitMode || 'equal';
    const percentages = lineItem.assigneePercentages || {};
    const assignees = parseAssignees(lineItem);
    const hasAssignees = assignees.length > 0;
    const totalPrice = round2((lineItem.price || 0) * (lineItem.quantity || 1));

    // Calculate total percentage
    const totalPercentage = Object.values(percentages).reduce((sum, val) => sum + Math.round(val || 0), 0);
    const isValidPercentage = totalPercentage === 100;

    // Calculate dollar amount per assignee
    const getAssigneeAmount = (assignee: string): number => {
        const amount = splitMode === 'percentage' && percentages[assignee]
            ? (totalPrice * percentages[assignee]) / 100
            : assignees.length > 0 ? totalPrice / assignees.length : 0;
        return round2(amount);
    };

    const toggleSplitMode = () => {
        // First, ensure assignees are processed from text
        onAssigneeBlur(index);

        const newMode = splitMode === 'equal' ? 'percentage' : 'equal';
        onUpdate(index, 'splitMode', newMode);

        if (newMode === 'percentage') {
            // Initialize equal percentages when switching to percentage mode
            // Use the assignees array that's already parsed at component level
            const equalPercentage = assignees.length > 0 ? Math.floor(100 / assignees.length) : 0;
            const newPercentages: Record<string, number> = {};
            assignees.forEach(assignee => {
                newPercentages[assignee] = equalPercentage;
            });
            onUpdate(index, 'assigneePercentages', newPercentages);
        }
    };

    const updatePercentage = (assignee: string, value: number) => {
        // Round to nearest integer
        const intValue = Math.round(value);
        const newPercentages = { ...percentages, [assignee]: intValue };
        onUpdate(index, 'assigneePercentages', newPercentages);
    };

    return (
        <div className="border rounded p-3 my-2 bg-gray-50">
            <div className="flex gap-2 items-center">
                <input
                    className="flex-1 border p-1 rounded"
                    value={lineItem.name || ''}
                    onChange={(e) => onUpdate(index, 'name', e.target.value)}
                    placeholder="Item name"
                />

                <input
                    className="w-28 border p-1 rounded"
                    type="number"
                    step="0.01"
                    value={lineItem.price ?? 0}
                    onChange={(e) => onUpdate(index, 'price', parseFloat(e.target.value || '0'))}
                    placeholder="Price"
                />

                <input
                    className="w-20 border p-1 rounded"
                    type="number"
                    value={lineItem.quantity ?? 1}
                    onChange={(e) => onUpdate(index, 'quantity', parseInt(e.target.value || '1'))}
                    placeholder="Qty"
                />

                <input
                    className="w-48 border p-1 rounded"
                    type="text"
                    value={lineItem.assigneesText ?? assignees.join(', ')}
                    onChange={(e) => onUpdate(index, 'assignees', e.target.value)}
                    onBlur={() => onAssigneeBlur(index)}
                    placeholder="Assignees (comma-separated)"
                />

                <button
                    className={`px-3 py-1 rounded text-sm font-medium ${
                        splitMode === 'percentage'
                            ? 'bg-blue-600 text-white'
                            : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
                    } ${!hasAssignees ? 'opacity-50 cursor-not-allowed' : ''}`}
                    onClick={toggleSplitMode}
                    disabled={!hasAssignees}
                    title={!hasAssignees ? 'Add assignees first' : 'Toggle split mode'}
                >
                    {splitMode === 'equal' ? 'Equal' : '%'}
                </button>

                <button
                    className="px-2 py-1 bg-red-500 text-white rounded hover:bg-red-600"
                    onClick={() => onRemove(index)}
                >
                    Remove
                </button>
            </div>

            {/* Equal split summary */}
            {splitMode === 'equal' && hasAssignees && (
                <div className="mt-2 px-3 py-2 bg-green-50 border border-green-200 rounded text-sm">
                    <span className="font-medium text-green-800">Equal Split:</span>
                    <span className="ml-2 text-gray-700">
                        {assignees.map((assignee, idx) => (
                            <span key={assignee}>
                                <span className="font-semibold">{assignee}</span>
                                {' = '}
                                <span className="text-green-700 font-bold">
                                    ${getAssigneeAmount(assignee).toFixed(2)}
                                </span>
                                {idx < assignees.length - 1 && ', '}
                            </span>
                        ))}
                    </span>
                </div>
            )}

            {/* Percentage split panel */}
            {splitMode === 'percentage' && hasAssignees && (
                <div className="mt-3 pl-4 border-l-4 border-blue-500 bg-blue-50 p-3 rounded-r">
                    <div className="flex items-center justify-between mb-3">
                        <div className="text-sm font-semibold text-gray-800">
                            💰 Custom Split Distribution
                        </div>
                        <div className="text-sm">
                            {!isValidPercentage && (
                                <span className="text-red-600 font-medium">
                                    ⚠ Total: {Math.round(totalPercentage)}% (must equal 100%)
                                </span>
                            )}
                            {isValidPercentage && (
                                <span className="text-green-600 font-medium">✓ Valid (100%)</span>
                            )}
                        </div>
                    </div>

                    <div className="space-y-3">
                        {assignees.map((assignee) => {
                            const percentage = percentages[assignee] ?? 0;
                            const amount = getAssigneeAmount(assignee);
                            return (
                                <div key={assignee} className="bg-white p-4 rounded shadow-sm border border-gray-200">
                                    <div className="flex items-center justify-between mb-3">
                                        <label className="text-sm font-semibold text-gray-700">
                                            {assignee}
                                        </label>
                                        <div className="flex items-center gap-2">
                                            <input
                                                className={`w-20 border-2 p-1 rounded text-sm font-medium ${
                                                    !isValidPercentage ? 'border-red-400 bg-red-50' : 'border-blue-300'
                                                }`}
                                                type="number"
                                                step="1"
                                                min="0"
                                                max="100"
                                                value={Math.round(percentage)}
                                                onChange={(e) => updatePercentage(assignee, parseInt(e.target.value || '0', 10))}
                                            />
                                            <span className="text-sm text-gray-600 font-medium">%</span>
                                            <span className="ml-2 text-sm font-bold text-green-700 min-w-[60px] text-right">
                                                ${amount.toFixed(2)}
                                            </span>
                                        </div>
                                    </div>

                                    {/* Slider */}
                                    <div className="mb-2">
                                        <input
                                            type="range"
                                            min="0"
                                            max="100"
                                            step="1"
                                            value={Math.round(percentage)}
                                            onChange={(e) => updatePercentage(assignee, parseInt(e.target.value, 10))}
                                            className={`w-full h-3 rounded-lg appearance-none cursor-pointer ${
                                                !isValidPercentage
                                                    ? 'accent-red-500'
                                                    : 'accent-blue-600'
                                            }`}
                                            style={{
                                                background: `linear-gradient(to right, ${
                                                    !isValidPercentage ? '#ef4444' : '#2563eb'
                                                } 0%, ${
                                                    !isValidPercentage ? '#ef4444' : '#2563eb'
                                                } ${Math.round(percentage)}%, #e5e7eb ${Math.round(percentage)}%, #e5e7eb 100%)`
                                            }}
                                        />
                                    </div>

                                    {/* Percentage markers */}
                                    <div className="flex justify-between text-xs text-gray-400 mb-2">
                                        <span>0%</span>
                                        <span>25%</span>
                                        <span>50%</span>
                                        <span>75%</span>
                                        <span>100%</span>
                                    </div>
                                </div>
                            );
                        })}
                    </div>

                    {/* Summary */}
                    <div className="mt-3 pt-3 border-t border-gray-300 flex justify-between items-center text-sm">
                        <span className="font-medium text-gray-700">Item Total:</span>
                        <span className="font-bold text-gray-900">${totalPrice.toFixed(2)}</span>
                    </div>
                </div>
            )}
        </div>
    );
};
