import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest';
import {
  receiptApi,
  authApi,
  dashboardApi,
  paymentApi,
  friendApi,
  ApiError,
} from './api';
import type { Receipt } from '../types';
import type { Friend } from '../types/friend';

describe('ApiError', () => {
  it('should create an error with message and status', () => {
    const error = new ApiError('Test error', 400);

    expect(error.message).toBe('Test error');
    expect(error.status).toBe(400);
    expect(error.name).toBe('ApiError');
  });

  it('should create an error with just a message', () => {
    const error = new ApiError('Test error');

    expect(error.message).toBe('Test error');
    expect(error.status).toBeUndefined();
  });
});

describe('receiptApi', () => {
  beforeEach(() => {
    global.fetch = vi.fn();
    localStorage.clear();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  describe('uploadReceipt', () => {
    it('should upload a receipt file successfully', async () => {
      const mockReceipt: Receipt = {
        vendor: 'Test Vendor',
        subtotal: 100,
      };

      (global.fetch as any).mockResolvedValueOnce({
        ok: true,
        json: async () => mockReceipt,
      });

      const file = new File(['test'], 'receipt.jpg', { type: 'image/jpeg' });
      const result = await receiptApi.uploadReceipt(file);

      expect(result).toEqual(mockReceipt);
      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining('/ocr/upload'),
        expect.objectContaining({
          method: 'POST',
        })
      );
    });

    it('should throw ApiError on upload failure', async () => {
      (global.fetch as any).mockResolvedValue({
        ok: false,
        status: 500,
        json: async () => ({ error: 'Upload failed' }),
      });

      const file = new File(['test'], 'receipt.jpg', { type: 'image/jpeg' });

      await expect(receiptApi.uploadReceipt(file)).rejects.toThrow(ApiError);
    });

    it('should use default error message if none provided', async () => {
      (global.fetch as any).mockResolvedValueOnce({
        ok: false,
        status: 500,
        json: async () => ({}),
      });

      const file = new File(['test'], 'receipt.jpg', { type: 'image/jpeg' });

      await expect(receiptApi.uploadReceipt(file)).rejects.toThrow('Upload failed');
    });
  });

  describe('saveReceipt', () => {
    it('should save a receipt successfully with auth token', async () => {
      localStorage.setItem('authToken', 'test-token');

      const mockReceipt: Receipt = {
        vendor: 'Test Vendor',
        subtotal: 100,
      };

      (global.fetch as any).mockResolvedValueOnce({
        ok: true,
        json: async () => ({ saved: true }),
      });

      const result = await receiptApi.saveReceipt(mockReceipt);

      expect(result).toEqual({ saved: true });
      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining('/receipts'),
        expect.objectContaining({
          method: 'POST',
          headers: expect.objectContaining({
            'Content-Type': 'application/json',
            'Authorization': 'Bearer test-token',
          }),
        })
      );
    });

    it('should throw ApiError on save failure', async () => {
      (global.fetch as any).mockResolvedValueOnce({
        ok: false,
        status: 400,
        json: async () => ({ error: 'Save failed' }),
      });

      const mockReceipt: Receipt = {};

      await expect(receiptApi.saveReceipt(mockReceipt)).rejects.toThrow('Save failed');
    });
  });

  describe('generateReport', () => {
    it('should generate and download PDF report', async () => {
      localStorage.setItem('authToken', 'test-token');

      const mockBlob = new Blob(['pdf content'], { type: 'application/pdf' });
      (global.fetch as any).mockResolvedValueOnce({
        ok: true,
        blob: async () => mockBlob,
      });

      const mockReceipt: Receipt = { uid: '123' };

      await receiptApi.generateReport(mockReceipt);

      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining('/receipts/generate-report'),
        expect.objectContaining({
          method: 'POST',
        })
      );
    });

    it('should throw ApiError on report generation failure', async () => {
      (global.fetch as any).mockResolvedValueOnce({
        ok: false,
        status: 500,
        json: async () => ({ error: 'Report generation failed' }),
      });

      const mockReceipt: Receipt = {};

      await expect(receiptApi.generateReport(mockReceipt)).rejects.toThrow(
        'Report generation failed'
      );
    });
  });
});

describe('authApi', () => {
  beforeEach(() => {
    global.fetch = vi.fn();
    localStorage.clear();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  describe('signup', () => {
    it('should sign up a new user successfully', async () => {
      const mockResponse = {
        email: 'test@example.com',
        userId: '123',
        emailVerified: false,
        message: 'Signup successful',
      };

      (global.fetch as any).mockResolvedValueOnce({
        ok: true,
        json: async () => mockResponse,
      });

      const result = await authApi.signup({
        email: 'test@example.com',
        password: 'password123',
      });

      expect(result).toEqual(mockResponse);
      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining('/auth/signup'),
        expect.objectContaining({
          method: 'POST',
        })
      );
    });

    it('should throw ApiError on signup failure', async () => {
      (global.fetch as any).mockResolvedValueOnce({
        ok: false,
        status: 400,
        json: async () => ({ error: 'Email already exists' }),
      });

      await expect(
        authApi.signup({ email: 'test@example.com', password: 'password123' })
      ).rejects.toThrow('Email already exists');
    });
  });

  describe('login', () => {
    it('should login successfully', async () => {
      const mockResponse = {
        token: 'jwt-token',
        email: 'test@example.com',
        userId: '123',
        emailVerified: true,
      };

      (global.fetch as any).mockResolvedValueOnce({
        ok: true,
        json: async () => mockResponse,
      });

      const result = await authApi.login({
        email: 'test@example.com',
        password: 'password123',
      });

      expect(result).toEqual(mockResponse);
    });

    it('should throw ApiError on login failure', async () => {
      (global.fetch as any).mockResolvedValueOnce({
        ok: false,
        status: 401,
        json: async () => ({ error: 'Invalid credentials' }),
      });

      await expect(
        authApi.login({ email: 'test@example.com', password: 'wrong' })
      ).rejects.toThrow('Invalid credentials');
    });
  });

  describe('verifyEmail', () => {
    it('should verify email successfully', async () => {
      (global.fetch as any).mockResolvedValueOnce({
        ok: true,
        json: async () => ({ message: 'Email verified' }),
      });

      const result = await authApi.verifyEmail('token123');

      expect(result).toEqual({ message: 'Email verified' });
      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining('/auth/verify-email?token=token123'),
        expect.objectContaining({
          method: 'GET',
        })
      );
    });
  });

  describe('forgotPassword', () => {
    it('should send forgot password request successfully', async () => {
      (global.fetch as any).mockResolvedValueOnce({
        ok: true,
        json: async () => ({ message: 'Reset email sent' }),
      });

      const result = await authApi.forgotPassword({ email: 'test@example.com' });

      expect(result).toEqual({ message: 'Reset email sent' });
    });
  });

  describe('resetPassword', () => {
    it('should reset password successfully', async () => {
      (global.fetch as any).mockResolvedValueOnce({
        ok: true,
        json: async () => ({ message: 'Password reset successful' }),
      });

      const result = await authApi.resetPassword({
        token: 'reset-token',
        newPassword: 'newpassword123',
      });

      expect(result).toEqual({ message: 'Password reset successful' });
    });
  });

  describe('getCurrentUser', () => {
    it('should get current user with auth token', async () => {
      localStorage.setItem('authToken', 'test-token');

      const mockUser = {
        email: 'test@example.com',
        userId: '123',
        emailVerified: true,
      };

      (global.fetch as any).mockResolvedValueOnce({
        ok: true,
        json: async () => mockUser,
      });

      const result = await authApi.getCurrentUser();

      expect(result).toEqual(mockUser);
      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining('/auth/me'),
        expect.objectContaining({
          headers: expect.objectContaining({
            'Authorization': 'Bearer test-token',
          }),
        })
      );
    });
  });
});

describe('dashboardApi', () => {
  beforeEach(() => {
    global.fetch = vi.fn();
    localStorage.setItem('authToken', 'test-token');
  });

  afterEach(() => {
    vi.restoreAllMocks();
    localStorage.clear();
  });

  describe('getUserReceipts', () => {
    it('should fetch user receipts successfully', async () => {
      const mockReceipts: Receipt[] = [
        { uid: '1', vendor: 'Vendor 1' },
        { uid: '2', vendor: 'Vendor 2' },
      ];

      (global.fetch as any).mockResolvedValueOnce({
        ok: true,
        json: async () => mockReceipts,
      });

      const result = await dashboardApi.getUserReceipts();

      expect(result).toEqual(mockReceipts);
      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining('/dashboard/receipts'),
        expect.objectContaining({
          headers: expect.objectContaining({
            'Authorization': 'Bearer test-token',
          }),
        })
      );
    });
  });

  describe('getReceipt', () => {
    it('should fetch specific receipt successfully', async () => {
      const mockReceipt: Receipt = { uid: '123', vendor: 'Test Vendor' };

      (global.fetch as any).mockResolvedValueOnce({
        ok: true,
        json: async () => mockReceipt,
      });

      const result = await dashboardApi.getReceipt('123');

      expect(result).toEqual(mockReceipt);
      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining('/dashboard/receipts/123'),
        expect.any(Object)
      );
    });
  });

  describe('deleteReceipt', () => {
    it('should delete receipt successfully', async () => {
      (global.fetch as any).mockResolvedValueOnce({
        ok: true,
        json: async () => ({ deleted: true, message: 'Deleted' }),
      });

      const result = await dashboardApi.deleteReceipt('123');

      expect(result).toEqual({ deleted: true, message: 'Deleted' });
      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining('/dashboard/receipts/123'),
        expect.objectContaining({
          method: 'DELETE',
        })
      );
    });
  });
});

describe('paymentApi', () => {
  beforeEach(() => {
    global.fetch = vi.fn();
    localStorage.setItem('authToken', 'test-token');
  });

  afterEach(() => {
    vi.restoreAllMocks();
    localStorage.clear();
  });

  describe('updateAssigneePaymentStatus', () => {
    it('should update assignee payment status successfully', async () => {
      const mockReceipt: Receipt = { uid: '123' };

      (global.fetch as any).mockResolvedValueOnce({
        ok: true,
        json: async () => mockReceipt,
      });

      const result = await paymentApi.updateAssigneePaymentStatus('123', 'Alice', 'paid');

      expect(result).toEqual(mockReceipt);
      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining('/payments/receipts/123/assignees/Alice'),
        expect.objectContaining({
          method: 'PATCH',
          body: JSON.stringify({ status: 'paid' }),
        })
      );
    });
  });

  describe('updatePaymentStatus', () => {
    it('should update line item payment status successfully', async () => {
      const mockReceipt: Receipt = { uid: '123' };

      (global.fetch as any).mockResolvedValueOnce({
        ok: true,
        json: async () => mockReceipt,
      });

      const result = await paymentApi.updatePaymentStatus('123', 0, 'Alice', 'paid');

      expect(result).toEqual(mockReceipt);
      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining('/payments/receipts/123/line-items/0/assignees/Alice'),
        expect.objectContaining({
          method: 'PATCH',
        })
      );
    });
  });
});

describe('friendApi', () => {
  beforeEach(() => {
    global.fetch = vi.fn();
    localStorage.setItem('authToken', 'test-token');
  });

  afterEach(() => {
    vi.restoreAllMocks();
    localStorage.clear();
  });

  describe('getAllFriends', () => {
    it('should fetch all friends successfully', async () => {
      const mockFriends: Friend[] = [
        { id: '1', displayName: 'Alice Smith' },
        { id: '2', displayName: 'Bob Jones' },
      ];

      (global.fetch as any).mockResolvedValueOnce({
        ok: true,
        json: async () => mockFriends,
      });

      const result = await friendApi.getAllFriends();

      expect(result).toEqual(mockFriends);
    });
  });

  describe('getFriend', () => {
    it('should fetch specific friend successfully', async () => {
      const mockFriend: Friend = { id: '1', displayName: 'Alice Smith' };

      (global.fetch as any).mockResolvedValueOnce({
        ok: true,
        json: async () => mockFriend,
      });

      const result = await friendApi.getFriend('1');

      expect(result).toEqual(mockFriend);
    });
  });

  describe('createFriend', () => {
    it('should create friend successfully', async () => {
      const newFriend: Friend = { displayName: 'Alice Smith' };
      const createdFriend: Friend = { ...newFriend, id: '1' };

      (global.fetch as any).mockResolvedValueOnce({
        ok: true,
        json: async () => createdFriend,
      });

      const result = await friendApi.createFriend(newFriend);

      expect(result).toEqual(createdFriend);
      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining('/friends'),
        expect.objectContaining({
          method: 'POST',
          body: JSON.stringify(newFriend),
        })
      );
    });
  });

  describe('updateFriend', () => {
    it('should update friend successfully', async () => {
      const updatedFriend: Friend = { id: '1', displayName: 'Alice Updated' };

      (global.fetch as any).mockResolvedValueOnce({
        ok: true,
        json: async () => updatedFriend,
      });

      const result = await friendApi.updateFriend('1', updatedFriend);

      expect(result).toEqual(updatedFriend);
      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining('/friends/1'),
        expect.objectContaining({
          method: 'PUT',
        })
      );
    });
  });

  describe('deleteFriend', () => {
    it('should delete friend successfully', async () => {
      (global.fetch as any).mockResolvedValueOnce({
        ok: true,
        json: async () => ({}),
      });

      await friendApi.deleteFriend('1');

      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining('/friends/1'),
        expect.objectContaining({
          method: 'DELETE',
        })
      );
    });
  });
});
