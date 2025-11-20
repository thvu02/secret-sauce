export interface LineItem {
    name?: string;
    price?: number;
    quantity?: number;
    assignees?: string[];
    numAssignees?: number;
    assigneesText?: string;
    splitMode?: 'equal' | 'percentage';
    assigneePercentages?: Record<string, number>; // Map of assignee name to percentage
    assigneePaymentStatus?: Record<string, string>; // Map of assignee name to payment status ('paid' | 'unpaid')
}

export interface Receipt {
    uid?: string;
    vendor?: string;
    receiptDate?: string;
    currency?: string;
    subtotal?: number;
    tax?: number;
    taxPercentage?: number;
    tip?: number;
    tipPercentage?: number;
    total?: number;
    lineItems?: LineItem[];
    userId?: string;
    createdAt?: number; // Epoch milliseconds
    expiresAt?: number; // Epoch milliseconds
    paymentStatus?: 'pending' | 'partial' | 'complete';
    assigneePaymentStatus?: Record<string, string>; // Map of assignee name to payment status ('paid' | 'unpaid')
    taxTipDistribution?: 'proportional' | 'even'; // How tax and tip are distributed
}

export interface ErrorResponse {
    error: string;
}

export interface SaveReceiptResponse {
    saved: boolean;
}
