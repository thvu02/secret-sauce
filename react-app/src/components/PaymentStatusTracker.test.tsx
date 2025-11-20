import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { PaymentStatusTracker } from './PaymentStatusTracker';
import { paymentApi } from '../services/api';
import type { LineItem } from '../types';

vi.mock('../services/api', () => ({
    paymentApi: {
        updateAssigneePaymentStatus: vi.fn(),
    },
}));

describe('PaymentStatusTracker', () => {
    const mockOnUpdate = vi.fn();

    beforeEach(() => {
        vi.clearAllMocks();
    });

    describe('when there are no assignees', () => {
        it('should display no assignees message', () => {
            render(
                <PaymentStatusTracker
                    receiptId="receipt-123"
                    lineItems={[]}
                    assigneePaymentStatus={{}}
                />
            );

            expect(screen.getByText('No assignees found')).toBeInTheDocument();
        });
    });

    describe('with line items and assignees', () => {
        const lineItems: LineItem[] = [
            {
                name: 'Pizza',
                price: 20,
                quantity: 1,
                assignees: ['Alice', 'Bob'],
            },
            {
                name: 'Burger',
                price: 15,
                quantity: 1,
                assignees: ['Alice'],
            },
        ];

        it('should display all assignees', () => {
            render(
                <PaymentStatusTracker
                    receiptId="receipt-123"
                    lineItems={lineItems}
                    assigneePaymentStatus={{}}
                />
            );

            expect(screen.getByText('Alice')).toBeInTheDocument();
            expect(screen.getByText('Bob')).toBeInTheDocument();
        });

        it('should calculate amounts correctly for even split items', () => {
            render(
                <PaymentStatusTracker
                    receiptId="receipt-123"
                    lineItems={lineItems}
                    assigneePaymentStatus={{}}
                />
            );

            // Alice: (20/2) + 15 = 25
            // Bob: 20/2 = 10
            expect(screen.getByText('Alice')).toBeInTheDocument();
            expect(screen.getByText('Bob')).toBeInTheDocument();

            // Check for the specific total amounts in the "Total owed" sections
            const aliceSection = screen.getByText('Alice').closest('div');
            expect(aliceSection).toHaveTextContent('Total owed: $25.00');

            const bobSection = screen.getByText('Bob').closest('div');
            expect(bobSection).toHaveTextContent('Total owed: $10.00');
        });

        it('should show items breakdown for each assignee', () => {
            render(
                <PaymentStatusTracker
                    receiptId="receipt-123"
                    lineItems={lineItems}
                    assigneePaymentStatus={{}}
                />
            );

            expect(screen.getAllByText('Pizza')).toHaveLength(2); // Alice and Bob
            expect(screen.getByText('Burger')).toBeInTheDocument(); // Alice only
        });

        it('should display payment status as unpaid by default', () => {
            render(
                <PaymentStatusTracker
                    receiptId="receipt-123"
                    lineItems={lineItems}
                    assigneePaymentStatus={{}}
                />
            );

            const markAsPaidButtons = screen.getAllByRole('button', { name: /mark as paid/i });
            expect(markAsPaidButtons.length).toBeGreaterThan(0);
        });

        it('should display payment status as paid when provided', () => {
            render(
                <PaymentStatusTracker
                    receiptId="receipt-123"
                    lineItems={lineItems}
                    assigneePaymentStatus={{ Alice: 'paid' }}
                />
            );

            expect(screen.getByRole('button', { name: /✓ paid/i })).toBeInTheDocument();
        });
    });

    describe('tax and tip calculations', () => {
        const lineItems: LineItem[] = [
            {
                name: 'Item 1',
                price: 50,
                quantity: 1,
                assignees: ['Alice'],
            },
            {
                name: 'Item 2',
                price: 50,
                quantity: 1,
                assignees: ['Bob'],
            },
        ];

        it('should distribute tax and tip evenly when mode is even', () => {
            render(
                <PaymentStatusTracker
                    receiptId="receipt-123"
                    lineItems={lineItems}
                    assigneePaymentStatus={{}}
                    subtotal={100}
                    tax={10}
                    tip={20}
                    taxTipDistribution="even"
                />
            );

            // Each person should get: 50 (items) + 5 (tax/2) + 10 (tip/2) = 65
            const totalAmounts = screen.getAllByText(/\$65\.00/);
            expect(totalAmounts.length).toBeGreaterThan(0);
        });

        it('should distribute tax and tip proportionally when mode is proportional', () => {
            render(
                <PaymentStatusTracker
                    receiptId="receipt-123"
                    lineItems={lineItems}
                    assigneePaymentStatus={{}}
                    subtotal={100}
                    tax={10}
                    tip={20}
                    taxTipDistribution="proportional"
                />
            );

            // Each person has 50% of items, so should get: 50 + 5 (50% of tax) + 10 (50% of tip) = 65
            const totalAmounts = screen.getAllByText(/\$65\.00/);
            expect(totalAmounts.length).toBeGreaterThan(0);
        });
    });

    describe('percentage split items', () => {
        const lineItems: LineItem[] = [
            {
                name: 'Shared Pizza',
                price: 100,
                quantity: 1,
                assignees: ['Alice', 'Bob'],
                splitMode: 'percentage',
                assigneePercentages: {
                    Alice: 70,
                    Bob: 30,
                },
            },
        ];

        it('should calculate amounts based on percentages', () => {
            render(
                <PaymentStatusTracker
                    receiptId="receipt-123"
                    lineItems={lineItems}
                    assigneePaymentStatus={{}}
                />
            );

            // Alice should owe 70% = $70
            // Bob should owe 30% = $30
            const aliceSection = screen.getByText('Alice').closest('div');
            expect(aliceSection).toHaveTextContent('Total owed: $70.00');

            const bobSection = screen.getByText('Bob').closest('div');
            expect(bobSection).toHaveTextContent('Total owed: $30.00');
        });
    });

    describe('payment status updates', () => {
        const lineItems: LineItem[] = [
            {
                name: 'Item',
                price: 10,
                quantity: 1,
                assignees: ['Alice'],
            },
        ];

        it('should call API to update payment status when button is clicked', async () => {
            const user = userEvent.setup();
            (paymentApi.updateAssigneePaymentStatus as any).mockResolvedValue({});

            render(
                <PaymentStatusTracker
                    receiptId="receipt-123"
                    lineItems={lineItems}
                    assigneePaymentStatus={{}}
                    onUpdate={mockOnUpdate}
                />
            );

            const markAsPaidButton = screen.getByRole('button', { name: /mark as paid/i });
            await user.click(markAsPaidButton);

            await waitFor(() => {
                expect(paymentApi.updateAssigneePaymentStatus).toHaveBeenCalledWith(
                    'receipt-123',
                    'Alice',
                    'paid'
                );
            });

            expect(mockOnUpdate).toHaveBeenCalledTimes(1);
        });

        it('should toggle from paid to unpaid when clicked', async () => {
            const user = userEvent.setup();
            (paymentApi.updateAssigneePaymentStatus as any).mockResolvedValue({});

            render(
                <PaymentStatusTracker
                    receiptId="receipt-123"
                    lineItems={lineItems}
                    assigneePaymentStatus={{ Alice: 'paid' }}
                    onUpdate={mockOnUpdate}
                />
            );

            const paidButton = screen.getByRole('button', { name: /✓ paid/i });
            await user.click(paidButton);

            await waitFor(() => {
                expect(paymentApi.updateAssigneePaymentStatus).toHaveBeenCalledWith(
                    'receipt-123',
                    'Alice',
                    'unpaid'
                );
            });

            expect(mockOnUpdate).toHaveBeenCalledTimes(1);
        });

        it('should show updating state while API call is in progress', async () => {
            const user = userEvent.setup();
            let resolvePromise: any;
            const promise = new Promise((resolve) => {
                resolvePromise = resolve;
            });
            (paymentApi.updateAssigneePaymentStatus as any).mockReturnValue(promise);

            render(
                <PaymentStatusTracker
                    receiptId="receipt-123"
                    lineItems={lineItems}
                    assigneePaymentStatus={{}}
                />
            );

            const markAsPaidButton = screen.getByRole('button', { name: /mark as paid/i });
            await user.click(markAsPaidButton);

            expect(screen.getByText(/updating/i)).toBeInTheDocument();

            resolvePromise({});
            await waitFor(() => {
                expect(screen.queryByText(/updating/i)).not.toBeInTheDocument();
            });
        });

        it('should handle API errors gracefully', async () => {
            const user = userEvent.setup();
            const alertSpy = vi.spyOn(window, 'alert').mockImplementation(() => {});
            const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

            (paymentApi.updateAssigneePaymentStatus as any).mockRejectedValue(
                new Error('API Error')
            );

            render(
                <PaymentStatusTracker
                    receiptId="receipt-123"
                    lineItems={lineItems}
                    assigneePaymentStatus={{}}
                />
            );

            const markAsPaidButton = screen.getByRole('button', { name: /mark as paid/i });
            await user.click(markAsPaidButton);

            await waitFor(() => {
                expect(consoleErrorSpy).toHaveBeenCalled();
                expect(alertSpy).toHaveBeenCalledWith('Failed to update payment status');
            });

            alertSpy.mockRestore();
            consoleErrorSpy.mockRestore();
        });
    });
});
