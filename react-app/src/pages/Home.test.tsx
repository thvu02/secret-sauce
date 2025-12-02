import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { BrowserRouter } from 'react-router-dom';
import { Home } from './Home';
import { receiptApi } from '../services/api';
import { AuthContext } from '../contexts/AuthContext';
import React from 'react';

// Mock the API
vi.mock('../services/api', () => ({
  receiptApi: {
    uploadReceipt: vi.fn(),
    saveReceipt: vi.fn(),
    generateReport: vi.fn(),
  },
  friendApi: {
    getAllFriends: vi.fn().mockResolvedValue([]),
    getFriend: vi.fn(),
    createFriend: vi.fn(),
    updateFriend: vi.fn(),
    deleteFriend: vi.fn(),
  },
  userProfileApi: {
    getProfile: vi.fn().mockResolvedValue({ displayName: 'Test User' }),
    saveProfile: vi.fn(),
  },
  ApiError: class ApiError extends Error {
    status?: number;
    constructor(message: string, status?: number) {
      super(message);
      this.status = status;
    }
  },
}));

describe('Home Page Integration Tests', () => {
  const mockAuthValue = {
    isAuthenticated: true,
    user: { email: 'test@example.com', userId: '123', emailVerified: true },
    token: 'test-token',
    isLoading: false,
    login: vi.fn(),
    signup: vi.fn(),
    logout: vi.fn(),
    refreshUser: vi.fn(),
  };

  const renderWithProviders = (component: React.ReactElement) => {
    return render(
      <BrowserRouter>
        <AuthContext.Provider value={mockAuthValue}>
          {component}
        </AuthContext.Provider>
      </BrowserRouter>
    );
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should render the home page with initial state', () => {
    renderWithProviders(<Home />);

    expect(screen.getByText('Upload Receipt Image')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /upload & parse/i })).toBeInTheDocument();
  });

  it('should initialize with empty receipt and one line item', async () => {
    renderWithProviders(<Home />);

    await waitFor(() => {
      expect(screen.getByText('Line Items')).toBeInTheDocument();
      expect(screen.getByPlaceholderText('Item name')).toBeInTheDocument();
    });
  });

  it('should enable upload button when file is selected', async () => {
    const user = userEvent.setup();
    const { container } = renderWithProviders(<Home />);

    const file = new File(['test'], 'receipt.jpg', { type: 'image/jpeg' });
    const fileInput = container.querySelector('input[type="file"]') as HTMLInputElement;

    await user.upload(fileInput, file);

    const uploadButton = screen.getByRole('button', { name: /upload & parse/i });
    expect(uploadButton).not.toBeDisabled();
  });

  it('should upload and parse receipt successfully', async () => {
    const user = userEvent.setup();
    const mockReceipt = {
      vendor: 'Test Vendor',
      subtotal: 100,
      tax: 8,
      tip: 15,
      total: 123,
      lineItems: [
        { name: 'Item 1', price: 50, quantity: 1, assignees: [] },
        { name: 'Item 2', price: 50, quantity: 1, assignees: [] },
      ],
    };

    (receiptApi.uploadReceipt as any).mockResolvedValue(mockReceipt);

    const { container } = renderWithProviders(<Home />);

    const file = new File(['test'], 'receipt.jpg', { type: 'image/jpeg' });
    const fileInput = container.querySelector('input[type="file"]') as HTMLInputElement;

    await user.upload(fileInput, file);

    const uploadButton = screen.getByRole('button', { name: /upload & parse/i });
    await user.click(uploadButton);

    await waitFor(() => {
      expect(receiptApi.uploadReceipt).toHaveBeenCalledWith(file);
      expect(screen.getByText(/receipt data populated from image/i)).toBeInTheDocument();
    });

    // Verify receipt data is displayed
    expect(screen.getByDisplayValue('Test Vendor')).toBeInTheDocument();
    expect(screen.getByDisplayValue('100')).toBeInTheDocument();
  });

  it('should show error message when upload fails', async () => {
    const user = userEvent.setup();
    const ApiError = (await import('../services/api')).ApiError;
    (receiptApi.uploadReceipt as any).mockRejectedValue(
      new ApiError('Upload failed', 500)
    );

    const { container } = renderWithProviders(<Home />);

    const file = new File(['test'], 'receipt.jpg', { type: 'image/jpeg' });
    const fileInput = container.querySelector('input[type="file"]') as HTMLInputElement;

    await user.upload(fileInput, file);

    const uploadButton = screen.getByRole('button', { name: /upload & parse/i });
    await user.click(uploadButton);

    await waitFor(() => {
      expect(screen.getByText(/error: upload failed/i)).toBeInTheDocument();
    });
  });

  it('should save receipt to database successfully', async () => {
    const user = userEvent.setup();
    (receiptApi.saveReceipt as any).mockResolvedValue({ saved: true });

    renderWithProviders(<Home />);

    // Wait for initial receipt to be set
    await waitFor(() => {
      expect(screen.getByText('Receipt Details')).toBeInTheDocument();
    });

    const saveButton = screen.getByRole('button', { name: /save to database/i });
    await user.click(saveButton);

    await waitFor(() => {
      expect(receiptApi.saveReceipt).toHaveBeenCalled();
      expect(screen.getByText(/saved to database successfully/i)).toBeInTheDocument();
    });
  });

  it('should show error message when save fails', async () => {
    const user = userEvent.setup();
    const ApiError = (await import('../services/api')).ApiError;
    (receiptApi.saveReceipt as any).mockRejectedValue(
      new ApiError('Save failed', 500)
    );

    renderWithProviders(<Home />);

    await waitFor(() => {
      expect(screen.getByText('Receipt Details')).toBeInTheDocument();
    });

    const saveButton = screen.getByRole('button', { name: /save to database/i });
    await user.click(saveButton);

    await waitFor(() => {
      expect(screen.getByText(/error: save failed/i)).toBeInTheDocument();
    });
  });

  it('should generate PDF report successfully', async () => {
    const user = userEvent.setup();
    (receiptApi.generateReport as any).mockResolvedValue(undefined);

    renderWithProviders(<Home />);

    await waitFor(() => {
      expect(screen.getByText('Receipt Details')).toBeInTheDocument();
    });

    const generateButton = screen.getByRole('button', { name: /generate report/i });
    await user.click(generateButton);

    await waitFor(() => {
      expect(receiptApi.generateReport).toHaveBeenCalled();
      expect(screen.getByText(/pdf report generated and downloaded successfully/i)).toBeInTheDocument();
    });
  });

  it('should allow editing receipt fields', async () => {
    const user = userEvent.setup();
    const { container } = renderWithProviders(<Home />);

    await waitFor(() => {
      expect(screen.getByText('Receipt Details')).toBeInTheDocument();
    });

    const vendorInput = container.querySelector('input[value=""]') as HTMLInputElement;
    await user.type(vendorInput, 'New Vendor');

    expect(vendorInput).toHaveValue('New Vendor');
  });

  it('should allow adding and removing line items', async () => {
    const user = userEvent.setup();
    renderWithProviders(<Home />);

    await waitFor(() => {
      expect(screen.getByText('Line Items')).toBeInTheDocument();
    });

    // Add a line item
    const addButton = screen.getByRole('button', { name: /add line item/i });
    await user.click(addButton);

    // Should now have 2 line items (1 initial + 1 added)
    const removeButtons = screen.getAllByRole('button', { name: /remove/i });
    expect(removeButtons).toHaveLength(2);

    // Remove a line item
    await user.click(removeButtons[0]);

    // Should be back to 1 line item
    await waitFor(() => {
      const remainingRemoveButtons = screen.getAllByRole('button', { name: /remove/i });
      expect(remainingRemoveButtons).toHaveLength(1);
    });
  });

  it('should update line item values', async () => {
    const user = userEvent.setup();
    renderWithProviders(<Home />);

    await waitFor(() => {
      expect(screen.getByPlaceholderText('Item name')).toBeInTheDocument();
    });

    const nameInput = screen.getByPlaceholderText('Item name') as HTMLInputElement;
    await user.type(nameInput, 'Pizza');

    await waitFor(() => {
      expect(nameInput.value).toContain('Pizza');
    });

    const priceInput = screen.getByPlaceholderText('Price');
    await user.clear(priceInput);
    await user.type(priceInput, '25');

    await waitFor(() => {
      expect(priceInput).toHaveValue(25);
    });
  });

  it('should recalculate totals when financial fields change', async () => {
    const user = userEvent.setup();
    const { container } = renderWithProviders(<Home />);

    await waitFor(() => {
      expect(screen.getByText('Receipt Details')).toBeInTheDocument();
    });

    // Get all number inputs and filter out readonly ones
    const allNumberInputs = Array.from(container.querySelectorAll('input[type="number"]')) as HTMLInputElement[];
    const editableInputs = allNumberInputs.filter(input => !input.readOnly);

    // Find subtotal, tax, and tip inputs (first 3 editable number inputs in receipt fields section)
    const subtotalInput = editableInputs[0];
    const taxInput = editableInputs[1];
    const tipInput = editableInputs[2];

    // Find the readonly total input
    const totalInput = allNumberInputs.find(input => input.readOnly);

    await user.clear(subtotalInput);
    await user.type(subtotalInput, '100');

    await user.clear(taxInput);
    await user.type(taxInput, '8');

    await user.clear(tipInput);
    await user.type(tipInput, '15');

    await waitFor(() => {
      expect(totalInput).toHaveValue(123); // 100 + 8 + 15
    }, { timeout: 3000 });
  });

  it('should show loading state during operations', async () => {
    const user = userEvent.setup();
    let resolveUpload: (value: any) => void;
    const uploadPromise = new Promise((resolve) => {
      resolveUpload = resolve;
    });

    (receiptApi.uploadReceipt as any).mockReturnValue(uploadPromise);

    const { container } = renderWithProviders(<Home />);

    const file = new File(['test'], 'receipt.jpg', { type: 'image/jpeg' });
    const fileInput = container.querySelector('input[type="file"]') as HTMLInputElement;

    await user.upload(fileInput, file);

    const uploadButton = screen.getByRole('button', { name: /upload & parse/i });
    await user.click(uploadButton);

    // Should show loading state
    expect(screen.getByText('Processing receipt with OCR...')).toBeInTheDocument();
    expect(screen.getByText(/this may take a few moments/i)).toBeInTheDocument();

    // Resolve the promise
    resolveUpload!({ vendor: 'Test', subtotal: 100 });

    await waitFor(() => {
      expect(screen.queryByText('Processing receipt with OCR...')).not.toBeInTheDocument();
    });
  });

  it('should handle assignee normalization on blur', async () => {
    const user = userEvent.setup();
    renderWithProviders(<Home />);

    await waitFor(() => {
      expect(screen.getByPlaceholderText('Assignees (comma-separated)')).toBeInTheDocument();
    });

    const assigneesInput = screen.getByPlaceholderText('Assignees (comma-separated)');
    await user.type(assigneesInput, 'Alice, Bob, Charlie');
    await user.tab(); // Trigger blur

    // Assignees should be normalized (spaces trimmed, then joined back)
    await waitFor(() => {
      expect(assigneesInput).toHaveValue('Alice,Bob,Charlie');
    });
  });
});
