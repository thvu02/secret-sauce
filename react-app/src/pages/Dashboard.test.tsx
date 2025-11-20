import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { BrowserRouter } from 'react-router-dom';
import { Dashboard } from './Dashboard';
import { AuthContext } from '../contexts/AuthContext';
import { dashboardApi, receiptApi } from '../services/api';
import type { Receipt } from '../types';

vi.mock('../services/api', () => ({
    dashboardApi: {
        getUserReceipts: vi.fn(),
        getReceipt: vi.fn(),
        deleteReceipt: vi.fn(),
    },
    receiptApi: {
        generateReport: vi.fn(),
    },
    ApiError: class ApiError extends Error {
        statusCode: number;
        constructor(message: string, statusCode: number) {
            super(message);
            this.statusCode = statusCode;
        }
    },
}));

vi.mock('../components', () => ({
    Navbar: () => <div>Navbar</div>,
}));

vi.mock('../components/PaymentStatusTracker', () => ({
    PaymentStatusTracker: () => <div>PaymentStatusTracker</div>,
}));

const renderDashboard = (authValue: any) => {
    return render(
        <BrowserRouter>
            <AuthContext.Provider value={authValue}>
                <Dashboard />
            </AuthContext.Provider>
        </BrowserRouter>
    );
};

describe('Dashboard', () => {
    const mockUser = {
        email: 'test@example.com',
        userId: '123',
        emailVerified: true,
    };

    const authValue = {
        user: mockUser,
        token: 'test-token',
        isLoading: false,
        isAuthenticated: true,
        login: vi.fn(),
        signup: vi.fn(),
        logout: vi.fn(),
        refreshUser: vi.fn(),
    };

    beforeEach(() => {
        vi.clearAllMocks();
    });

    describe('rendering', () => {
        it('should show loading state initially', () => {
            (dashboardApi.getUserReceipts as any).mockImplementation(
                () => new Promise(() => {})
            );

            renderDashboard(authValue);

            expect(document.querySelector('.animate-spin')).toBeInTheDocument();
        });

        it('should display user email in welcome message', async () => {
            (dashboardApi.getUserReceipts as any).mockResolvedValue([]);

            renderDashboard(authValue);

            await waitFor(() => {
                expect(screen.getByText(/welcome back, test@example.com/i)).toBeInTheDocument();
            });
        });
    });

    describe('when there are no receipts', () => {
        it('should display empty state message', async () => {
            (dashboardApi.getUserReceipts as any).mockResolvedValue([]);

            renderDashboard(authValue);

            await waitFor(() => {
                expect(screen.getByText(/no receipts/i)).toBeInTheDocument();
                expect(screen.getByText(/get started by uploading a receipt/i)).toBeInTheDocument();
            });
        });

        it('should show upload receipt button', async () => {
            (dashboardApi.getUserReceipts as any).mockResolvedValue([]);

            renderDashboard(authValue);

            await waitFor(() => {
                const uploadButton = screen.getByRole('link', { name: /upload receipt/i });
                expect(uploadButton).toBeInTheDocument();
                expect(uploadButton).toHaveAttribute('href', '/');
            });
        });
    });

    describe('when there are receipts', () => {
        const mockReceipts: Receipt[] = [
            {
                uid: 'receipt-1',
                vendor: 'Pizza Place',
                receiptDate: '2024-01-15',
                total: 50.00,
                currency: 'USD',
                lineItems: [
                    { name: 'Pizza', price: 40, quantity: 1, assignees: ['Alice'] },
                ],
                paymentStatus: 'pending',
            },
            {
                uid: 'receipt-2',
                vendor: 'Coffee Shop',
                receiptDate: '2024-01-16',
                total: 15.50,
                currency: 'USD',
                lineItems: [],
                paymentStatus: 'complete',
            },
        ];

        beforeEach(() => {
            (dashboardApi.getUserReceipts as any).mockResolvedValue(mockReceipts);
        });

        it('should display all receipts', async () => {
            renderDashboard(authValue);

            await waitFor(() => {
                expect(screen.getByText('Pizza Place')).toBeInTheDocument();
                expect(screen.getByText('Coffee Shop')).toBeInTheDocument();
            });
        });

        it('should display receipt details', async () => {
            renderDashboard(authValue);

            await waitFor(() => {
                expect(screen.getByText(/2024-01-15/)).toBeInTheDocument();
                expect(screen.getByText(/50\.00/)).toBeInTheDocument();
                expect(screen.getByText(/1 item\(s\)/)).toBeInTheDocument();
            });
        });

        it('should display payment status badges', async () => {
            renderDashboard(authValue);

            await waitFor(() => {
                expect(screen.getByText('Pending')).toBeInTheDocument();
                expect(screen.getByText('Complete')).toBeInTheDocument();
            });
        });

        it('should have view details, download, and delete buttons for each receipt', async () => {
            renderDashboard(authValue);

            await waitFor(() => {
                const viewButtons = screen.getAllByRole('button', { name: /view details/i });
                const downloadButtons = screen.getAllByRole('button', { name: /download pdf/i });
                const deleteButtons = screen.getAllByRole('button', { name: /delete/i });

                expect(viewButtons).toHaveLength(2);
                expect(downloadButtons).toHaveLength(2);
                expect(deleteButtons).toHaveLength(2);
            });
        });
    });

    describe('receipt details modal', () => {
        const mockReceipts: Receipt[] = [
            {
                uid: 'receipt-1',
                vendor: 'Pizza Place',
                receiptDate: '2024-01-15',
                subtotal: 40,
                tax: 3.20,
                tip: 6.80,
                total: 50.00,
                lineItems: [
                    { name: 'Pizza', price: 40, quantity: 1, assignees: ['Alice'] },
                ],
                paymentStatus: 'pending',
            },
        ];

        it('should open modal when view details is clicked', async () => {
            const user = userEvent.setup();
            (dashboardApi.getUserReceipts as any).mockResolvedValue(mockReceipts);

            renderDashboard(authValue);

            await waitFor(() => {
                expect(screen.getByText('Pizza Place')).toBeInTheDocument();
            });

            const viewButton = screen.getByRole('button', { name: /view details/i });
            await user.click(viewButton);

            expect(screen.getByRole('dialog')).toBeInTheDocument();
            expect(screen.getByText('Receipt Details')).toBeInTheDocument();
        });

        it('should close modal when close button is clicked', async () => {
            const user = userEvent.setup();
            (dashboardApi.getUserReceipts as any).mockResolvedValue(mockReceipts);

            renderDashboard(authValue);

            await waitFor(() => {
                expect(screen.getByText('Pizza Place')).toBeInTheDocument();
            });

            await user.click(screen.getByRole('button', { name: /view details/i }));

            const closeButton = screen.getByRole('button', { name: /^close$/i });
            await user.click(closeButton);

            expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
        });
    });

    describe('PDF generation', () => {
        it('should call generateReport when download PDF is clicked', async () => {
            const user = userEvent.setup();
            const mockReceipts: Receipt[] = [
                {
                    uid: 'receipt-1',
                    vendor: 'Pizza Place',
                    total: 50.00,
                },
            ];

            (dashboardApi.getUserReceipts as any).mockResolvedValue(mockReceipts);
            (receiptApi.generateReport as any).mockResolvedValue(undefined);

            renderDashboard(authValue);

            await waitFor(() => {
                expect(screen.getByText('Pizza Place')).toBeInTheDocument();
            });

            const downloadButton = screen.getByRole('button', { name: /download pdf/i });
            await user.click(downloadButton);

            expect(receiptApi.generateReport).toHaveBeenCalledWith(mockReceipts[0]);
        });
    });

    describe('receipt deletion', () => {
        const mockReceipts: Receipt[] = [
            {
                uid: 'receipt-1',
                vendor: 'Pizza Place',
                total: 50.00,
            },
        ];

        it('should show confirmation dialog when delete is clicked', async () => {
            const user = userEvent.setup();
            const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(false);

            (dashboardApi.getUserReceipts as any).mockResolvedValue(mockReceipts);

            renderDashboard(authValue);

            await waitFor(() => {
                expect(screen.getByText('Pizza Place')).toBeInTheDocument();
            });

            const deleteButtons = screen.getAllByRole('button', { name: /delete/i });
            await user.click(deleteButtons[0]);

            expect(confirmSpy).toHaveBeenCalledWith(
                expect.stringContaining('Pizza Place')
            );

            confirmSpy.mockRestore();
        });

        it('should delete receipt when confirmed', async () => {
            const user = userEvent.setup();
            const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(true);

            (dashboardApi.getUserReceipts as any).mockResolvedValue(mockReceipts);
            (dashboardApi.deleteReceipt as any).mockResolvedValue(undefined);

            renderDashboard(authValue);

            await waitFor(() => {
                expect(screen.getByText('Pizza Place')).toBeInTheDocument();
            });

            const deleteButtons = screen.getAllByRole('button', { name: /delete/i });
            await user.click(deleteButtons[0]);

            await waitFor(() => {
                expect(dashboardApi.deleteReceipt).toHaveBeenCalledWith('receipt-1');
            });

            confirmSpy.mockRestore();
        });
    });

    describe('error handling', () => {
        it('should display error message when loading receipts fails', async () => {
            (dashboardApi.getUserReceipts as any).mockRejectedValue(
                new Error('Failed to load receipts')
            );

            renderDashboard(authValue);

            await waitFor(() => {
                expect(screen.getByText(/failed to load receipts/i)).toBeInTheDocument();
            });
        });
    });
});
