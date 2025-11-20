import React, { useState, useMemo } from 'react';
import { paymentApi } from '../services/api';
import type { LineItem } from '../types';

interface PaymentStatusTrackerProps {
    receiptId: string;
    lineItems: LineItem[];
    assigneePaymentStatus?: Record<string, string>;
    subtotal?: number;
    tax?: number;
    tip?: number;
    taxTipDistribution?: 'proportional' | 'even';
    onUpdate?: () => void;
}

interface AssigneeInfo {
    name: string;
    itemsSubtotal: number;
    tax: number;
    tip: number;
    totalOwed: number;
    items: Array<{
        name: string;
        amount: number;
    }>;
    paymentStatus: 'paid' | 'unpaid';
}

export const PaymentStatusTracker: React.FC<PaymentStatusTrackerProps> = ({
    receiptId,
    lineItems,
    assigneePaymentStatus = {},
    subtotal = 0,
    tax = 0,
    tip = 0,
    taxTipDistribution = 'proportional',
    onUpdate,
}) => {
    const [updating, setUpdating] = useState<string | null>(null);

    // Group line items by assignee and calculate amounts including tax and tip
    const assigneeData = useMemo(() => {
        const assigneeMap = new Map<string, AssigneeInfo>();

        // First pass: calculate item subtotals
        lineItems.forEach((item) => {
            if (!item.assignees || item.assignees.length === 0) return;

            const itemTotal = (item.price || 0) * (item.quantity || 1);

            item.assignees.forEach((assignee) => {
                if (!assigneeMap.has(assignee)) {
                    assigneeMap.set(assignee, {
                        name: assignee,
                        itemsSubtotal: 0,
                        tax: 0,
                        tip: 0,
                        totalOwed: 0,
                        items: [],
                        paymentStatus: (assigneePaymentStatus[assignee] as 'paid' | 'unpaid') || 'unpaid',
                    });
                }

                const assigneeInfo = assigneeMap.get(assignee)!;

                // Calculate assignee's share based on split mode
                let assigneeShare: number;
                if (item.splitMode === 'percentage' && item.assigneePercentages?.[assignee]) {
                    assigneeShare = itemTotal * (item.assigneePercentages[assignee] / 100);
                } else {
                    // Equal split (we know assignees exists since we're iterating over it)
                    assigneeShare = itemTotal / (item.assignees?.length || 1);
                }

                assigneeInfo.itemsSubtotal += assigneeShare;
                assigneeInfo.items.push({
                    name: item.name || 'Unknown Item',
                    amount: assigneeShare,
                });
            });
        });

        // Second pass: calculate tax and tip for each assignee
        const totalAssignees = assigneeMap.size;
        assigneeMap.forEach((assigneeInfo) => {
            if (taxTipDistribution === 'even') {
                // Even distribution
                assigneeInfo.tax = totalAssignees > 0 ? tax / totalAssignees : 0;
                assigneeInfo.tip = totalAssignees > 0 ? tip / totalAssignees : 0;
            } else {
                // Proportional distribution
                const proportion = subtotal > 0 ? assigneeInfo.itemsSubtotal / subtotal : 0;
                assigneeInfo.tax = proportion * tax;
                assigneeInfo.tip = proportion * tip;
            }

            assigneeInfo.totalOwed = assigneeInfo.itemsSubtotal + assigneeInfo.tax + assigneeInfo.tip;
        });

        return Array.from(assigneeMap.values()).sort((a, b) => a.name.localeCompare(b.name));
    }, [lineItems, assigneePaymentStatus, subtotal, tax, tip, taxTipDistribution]);

    const handleStatusChange = async (assignee: string, newStatus: 'paid' | 'unpaid') => {
        setUpdating(assignee);

        try {
            await paymentApi.updateAssigneePaymentStatus(receiptId, assignee, newStatus);

            if (onUpdate) {
                onUpdate();
            }
        } catch (error) {
            console.error('Failed to update payment status:', error);
            alert('Failed to update payment status');
        } finally {
            setUpdating(null);
        }
    };

    return (
        <div className="mt-6">
            <h3 className="text-lg font-semibold mb-4">Payment Status by Assignee</h3>

            {assigneeData.length === 0 ? (
                <p className="text-sm text-gray-500">No assignees found</p>
            ) : (
                <div className="space-y-4">
                    {assigneeData.map((assignee) => {
                        const isPaid = assignee.paymentStatus === 'paid';
                        const isUpdating = updating === assignee.name;

                        return (
                            <div
                                key={assignee.name}
                                className="border rounded-lg p-4 bg-gray-50"
                            >
                                <div className="flex items-center justify-between mb-3">
                                    <div>
                                        <h4 className="font-medium text-gray-900">
                                            {assignee.name}
                                        </h4>
                                        <p className="text-sm text-gray-600 mt-1">
                                            Total owed: ${assignee.totalOwed.toFixed(2)}
                                        </p>
                                    </div>

                                    <button
                                        onClick={() =>
                                            handleStatusChange(
                                                assignee.name,
                                                isPaid ? 'unpaid' : 'paid'
                                            )
                                        }
                                        disabled={isUpdating}
                                        className={`px-4 py-2 text-sm font-semibold rounded-lg transition-colors ${
                                            isPaid
                                                ? 'bg-green-100 text-green-800 hover:bg-green-200'
                                                : 'bg-yellow-100 text-yellow-800 hover:bg-yellow-200'
                                        } disabled:opacity-50 disabled:cursor-not-allowed`}
                                    >
                                        {isUpdating ? (
                                            <span>Updating...</span>
                                        ) : isPaid ? (
                                            <span>✓ Paid</span>
                                        ) : (
                                            <span>Mark as Paid</span>
                                        )}
                                    </button>
                                </div>

                                {/* Show item breakdown */}
                                <div className="mt-3 pt-3 border-t border-gray-200">
                                    <p className="text-xs font-medium text-gray-500 mb-2">
                                        Breakdown:
                                    </p>
                                    <ul className="space-y-1">
                                        {assignee.items.map((item, idx) => (
                                            <li
                                                key={idx}
                                                className="text-xs text-gray-600 flex justify-between"
                                            >
                                                <span>{item.name}</span>
                                                <span>${item.amount.toFixed(2)}</span>
                                            </li>
                                        ))}
                                        {assignee.items.length > 0 && (
                                            <li className="text-xs text-gray-700 flex justify-between font-medium pt-1">
                                                <span>Items Subtotal:</span>
                                                <span>${assignee.itemsSubtotal.toFixed(2)}</span>
                                            </li>
                                        )}
                                        {assignee.tax > 0 && (
                                            <li className="text-xs text-gray-600 flex justify-between">
                                                <span>
                                                    {taxTipDistribution === 'even' ? 'Even Share Tax' : 'Proportional Tax'}:
                                                </span>
                                                <span>${assignee.tax.toFixed(2)}</span>
                                            </li>
                                        )}
                                        {assignee.tip > 0 && (
                                            <li className="text-xs text-gray-600 flex justify-between">
                                                <span>
                                                    {taxTipDistribution === 'even' ? 'Even Share Tip' : 'Proportional Tip'}:
                                                </span>
                                                <span>${assignee.tip.toFixed(2)}</span>
                                            </li>
                                        )}
                                        <li className="text-xs text-gray-800 flex justify-between font-bold pt-1 border-t border-gray-300">
                                            <span>Total:</span>
                                            <span>${assignee.totalOwed.toFixed(2)}</span>
                                        </li>
                                    </ul>
                                </div>
                            </div>
                        );
                    })}
                </div>
            )}
        </div>
    );
};
