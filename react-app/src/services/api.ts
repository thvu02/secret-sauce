import type { Receipt, SaveReceiptResponse } from '../types';
import type { Friend } from '../types/friend';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

export class ApiError extends Error {
    status?: number;

    constructor(message: string, status?: number) {
        super(message);
        this.name = 'ApiError';
        this.status = status;
    }
}

// Helper to get auth token from localStorage
const getAuthToken = (): string | null => {
    return localStorage.getItem('authToken');
};

// Helper to get headers with optional auth
const getHeaders = (includeAuth = false): HeadersInit => {
    const headers: HeadersInit = {
        'Content-Type': 'application/json',
    };

    if (includeAuth) {
        const token = getAuthToken();
        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }
    }

    return headers;
};

export const receiptApi = {
    /**
     * Upload and parse a receipt image using OCR
     */
    async uploadReceipt(file: File): Promise<Receipt> {
        const formData = new FormData();
        formData.append('file', file);

        const response = await fetch(`${API_BASE_URL}/ocr/upload`, {
            method: 'POST',
            body: formData,
        });

        if (!response.ok) {
            const error = await response.json();
            throw new ApiError(error.error || 'Upload failed', response.status);
        }

        return response.json();
    },

    /**
     * Save a receipt to the database
     */
    async saveReceipt(receipt: Receipt): Promise<SaveReceiptResponse> {
        const response = await fetch(`${API_BASE_URL}/receipts`, {
            method: 'POST',
            headers: getHeaders(true),
            body: JSON.stringify(receipt),
        });

        if (!response.ok) {
            const error = await response.json();
            throw new ApiError(error.error || 'Save failed', response.status);
        }

        return response.json();
    },

    /**
     * Generate and download PDF report for a receipt
     */
    async generateReport(receipt: Receipt): Promise<void> {
        const token = getAuthToken();
        const headers: HeadersInit = {
            'Content-Type': 'application/json',
        };
        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }

        const response = await fetch(`${API_BASE_URL}/receipts/generate-report`, {
            method: 'POST',
            headers,
            body: JSON.stringify(receipt),
        });

        if (!response.ok) {
            const error = await response.json();
            throw new ApiError(error.error || 'Report generation failed', response.status);
        }

        // Get the PDF blob
        const blob = await response.blob();

        // Create download link
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `receipt-report-${receipt.uid || 'download'}.pdf`;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        document.body.removeChild(a);
    },
};

// Auth API types
export interface LoginRequest {
    email: string;
    password: string;
}

export interface SignupRequest {
    email: string;
    password: string;
}

export interface AuthResponse {
    token?: string;
    email: string;
    userId: string;
    emailVerified: boolean;
    message?: string;
}

export interface ForgotPasswordRequest {
    email: string;
}

export interface ResetPasswordRequest {
    token: string;
    newPassword: string;
}

export const authApi = {
    /**
     * Register a new user
     */
    async signup(data: SignupRequest): Promise<AuthResponse> {
        const response = await fetch(`${API_BASE_URL}/auth/signup`, {
            method: 'POST',
            headers: getHeaders(),
            body: JSON.stringify(data),
        });

        if (!response.ok) {
            const error = await response.json();
            throw new ApiError(error.message || error.error || 'Signup failed', response.status);
        }

        return response.json();
    },

    /**
     * Login user
     */
    async login(data: LoginRequest): Promise<AuthResponse> {
        const response = await fetch(`${API_BASE_URL}/auth/login`, {
            method: 'POST',
            headers: getHeaders(),
            body: JSON.stringify(data),
        });

        if (!response.ok) {
            const error = await response.json();
            throw new ApiError(error.message || error.error || 'Login failed', response.status);
        }

        return response.json();
    },

    /**
     * Verify email with token
     */
    async verifyEmail(token: string): Promise<{ message: string }> {
        const response = await fetch(`${API_BASE_URL}/auth/verify-email?token=${token}`, {
            method: 'GET',
        });

        if (!response.ok) {
            const error = await response.json();
            throw new ApiError(error.message || error.error || 'Verification failed', response.status);
        }

        return response.json();
    },

    /**
     * Request password reset
     */
    async forgotPassword(data: ForgotPasswordRequest): Promise<{ message: string }> {
        const response = await fetch(`${API_BASE_URL}/auth/forgot-password`, {
            method: 'POST',
            headers: getHeaders(),
            body: JSON.stringify(data),
        });

        if (!response.ok) {
            const error = await response.json();
            throw new ApiError(error.message || error.error || 'Request failed', response.status);
        }

        return response.json();
    },

    /**
     * Reset password with token
     */
    async resetPassword(data: ResetPasswordRequest): Promise<{ message: string }> {
        const response = await fetch(`${API_BASE_URL}/auth/reset-password`, {
            method: 'POST',
            headers: getHeaders(),
            body: JSON.stringify(data),
        });

        if (!response.ok) {
            const error = await response.json();
            throw new ApiError(error.message || error.error || 'Reset failed', response.status);
        }

        return response.json();
    },

    /**
     * Get current user info
     */
    async getCurrentUser(): Promise<AuthResponse> {
        const response = await fetch(`${API_BASE_URL}/auth/me`, {
            method: 'GET',
            headers: getHeaders(true),
        });

        if (!response.ok) {
            const error = await response.json();
            throw new ApiError(error.message || error.error || 'Failed to get user', response.status);
        }

        return response.json();
    },
};

// Dashboard API
export const dashboardApi = {
    /**
     * Get all receipts for authenticated user
     */
    async getUserReceipts(): Promise<Receipt[]> {
        const response = await fetch(`${API_BASE_URL}/dashboard/receipts`, {
            method: 'GET',
            headers: getHeaders(true),
        });

        if (!response.ok) {
            const error = await response.json();
            throw new ApiError(error.message || error.error || 'Failed to fetch receipts', response.status);
        }

        return response.json();
    },

    /**
     * Get specific receipt by ID
     */
    async getReceipt(receiptId: string): Promise<Receipt> {
        const response = await fetch(`${API_BASE_URL}/dashboard/receipts/${receiptId}`, {
            method: 'GET',
            headers: getHeaders(true),
        });

        if (!response.ok) {
            const error = await response.json();
            throw new ApiError(error.message || error.error || 'Failed to fetch receipt', response.status);
        }

        return response.json();
    },

    /**
     * Delete a receipt
     */
    async deleteReceipt(receiptId: string): Promise<{ deleted: boolean; message: string }> {
        const response = await fetch(`${API_BASE_URL}/dashboard/receipts/${receiptId}`, {
            method: 'DELETE',
            headers: getHeaders(true),
        });

        if (!response.ok) {
            const error = await response.json();
            throw new ApiError(error.message || error.error || 'Failed to delete receipt', response.status);
        }

        return response.json();
    },
};

// Payment API
export const paymentApi = {
    /**
     * Update payment status for an assignee across all their line items
     */
    async updateAssigneePaymentStatus(
        receiptId: string,
        assignee: string,
        status: 'paid' | 'unpaid'
    ): Promise<Receipt> {
        const response = await fetch(
            `${API_BASE_URL}/payments/receipts/${receiptId}/assignees/${encodeURIComponent(assignee)}`,
            {
                method: 'PATCH',
                headers: getHeaders(true),
                body: JSON.stringify({ status }),
            }
        );

        if (!response.ok) {
            const error = await response.json();
            throw new ApiError(error.message || error.error || 'Failed to update payment status', response.status);
        }

        return response.json();
    },

    /**
     * Update payment status for a line item assignee
     * @deprecated Use updateAssigneePaymentStatus instead
     */
    async updatePaymentStatus(
        receiptId: string,
        lineItemIndex: number,
        assignee: string,
        status: 'paid' | 'unpaid'
    ): Promise<Receipt> {
        const response = await fetch(
            `${API_BASE_URL}/payments/receipts/${receiptId}/line-items/${lineItemIndex}/assignees/${encodeURIComponent(assignee)}`,
            {
                method: 'PATCH',
                headers: getHeaders(true),
                body: JSON.stringify({ status }),
            }
        );

        if (!response.ok) {
            const error = await response.json();
            throw new ApiError(error.message || error.error || 'Failed to update payment status', response.status);
        }

        return response.json();
    },
};

// Friend API
export const friendApi = {
    /**
     * Get all friends for authenticated user
     */
    async getAllFriends(): Promise<Friend[]> {
        const response = await fetch(`${API_BASE_URL}/friends`, {
            method: 'GET',
            headers: getHeaders(true),
        });

        if (!response.ok) {
            const error = await response.json();
            throw new ApiError(error.message || error.error || 'Failed to fetch friends', response.status);
        }

        return response.json();
    },

    /**
     * Get a specific friend by ID
     */
    async getFriend(friendId: string): Promise<Friend> {
        const response = await fetch(`${API_BASE_URL}/friends/${friendId}`, {
            method: 'GET',
            headers: getHeaders(true),
        });

        if (!response.ok) {
            const error = await response.json();
            throw new ApiError(error.message || error.error || 'Failed to fetch friend', response.status);
        }

        return response.json();
    },

    /**
     * Create a new friend
     */
    async createFriend(friend: Friend): Promise<Friend> {
        const response = await fetch(`${API_BASE_URL}/friends`, {
            method: 'POST',
            headers: getHeaders(true),
            body: JSON.stringify(friend),
        });

        if (!response.ok) {
            const error = await response.json();
            throw new ApiError(error.message || error.error || 'Failed to create friend', response.status);
        }

        return response.json();
    },

    /**
     * Update an existing friend
     */
    async updateFriend(friendId: string, friend: Friend): Promise<Friend> {
        const response = await fetch(`${API_BASE_URL}/friends/${friendId}`, {
            method: 'PUT',
            headers: getHeaders(true),
            body: JSON.stringify(friend),
        });

        if (!response.ok) {
            const error = await response.json();
            throw new ApiError(error.message || error.error || 'Failed to update friend', response.status);
        }

        return response.json();
    },

    /**
     * Delete a friend
     */
    async deleteFriend(friendId: string): Promise<void> {
        const response = await fetch(`${API_BASE_URL}/friends/${friendId}`, {
            method: 'DELETE',
            headers: getHeaders(true),
        });

        if (!response.ok) {
            const error = await response.json();
            throw new ApiError(error.message || error.error || 'Failed to delete friend', response.status);
        }
    },
};
