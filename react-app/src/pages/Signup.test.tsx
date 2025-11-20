import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { BrowserRouter } from 'react-router-dom';
import { Signup } from './Signup';
import { AuthContext } from '../contexts/AuthContext';
import { ApiError } from '../services/api';

const mockNavigate = vi.fn();

vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual('react-router-dom');
    return {
        ...actual,
        useNavigate: () => mockNavigate,
    };
});

const renderSignup = (authValue: any) => {
    return render(
        <BrowserRouter>
            <AuthContext.Provider value={authValue}>
                <Signup />
            </AuthContext.Provider>
        </BrowserRouter>
    );
};

describe('Signup', () => {
    const mockSignup = vi.fn();
    const authValue = {
        user: null,
        token: null,
        isLoading: false,
        isAuthenticated: false,
        login: vi.fn(),
        signup: mockSignup,
        logout: vi.fn(),
        refreshUser: vi.fn(),
    };

    beforeEach(() => {
        vi.clearAllMocks();
    });

    afterEach(() => {
        vi.clearAllTimers();
    });

    describe('rendering', () => {
        it('should render signup form', () => {
            renderSignup(authValue);

            expect(screen.getByRole('heading', { name: /create your account/i })).toBeInTheDocument();
            expect(screen.getByLabelText(/email address/i)).toBeInTheDocument();
            expect(screen.getByLabelText(/^password$/i)).toBeInTheDocument();
            expect(screen.getByLabelText(/confirm password/i)).toBeInTheDocument();
            expect(screen.getByRole('button', { name: /sign up/i })).toBeInTheDocument();
        });

        it('should render link to login page', () => {
            renderSignup(authValue);

            const loginLink = screen.getByRole('link', { name: /sign in to existing account/i });
            expect(loginLink).toBeInTheDocument();
            expect(loginLink).toHaveAttribute('href', '/login');
        });

        it('should render link back to home', () => {
            renderSignup(authValue);

            const homeLink = screen.getByRole('link', { name: /back to home/i });
            expect(homeLink).toBeInTheDocument();
            expect(homeLink).toHaveAttribute('href', '/');
        });
    });

    describe('form interactions', () => {
        it('should allow typing in email field', async () => {
            const user = userEvent.setup();
            renderSignup(authValue);

            const emailInput = screen.getByLabelText(/email address/i);
            await user.type(emailInput, 'test@example.com');

            expect(emailInput).toHaveValue('test@example.com');
        }, 10000);

        it('should allow typing in password fields', async () => {
            const user = userEvent.setup();
            renderSignup(authValue);

            const passwordInput = screen.getByLabelText(/^password$/i);
            const confirmPasswordInput = screen.getByLabelText(/confirm password/i);

            await user.type(passwordInput, 'password123');
            await user.type(confirmPasswordInput, 'password123');

            expect(passwordInput).toHaveValue('password123');
            expect(confirmPasswordInput).toHaveValue('password123');
        }, 10000);
    });

    describe('validation', () => {
        it('should show error when password is too short', async () => {
            const user = userEvent.setup();
            renderSignup(authValue);

            await user.type(screen.getByLabelText(/email address/i), 'test@example.com');
            await user.type(screen.getByLabelText(/^password$/i), 'short');
            await user.type(screen.getByLabelText(/confirm password/i), 'short');
            await user.click(screen.getByRole('button', { name: /sign up/i }));

            expect(screen.getByText('Password must be at least 8 characters long')).toBeInTheDocument();
            expect(mockSignup).not.toHaveBeenCalled();
        }, 15000);

        it('should show error when passwords do not match', async () => {
            const user = userEvent.setup();
            renderSignup(authValue);

            await user.type(screen.getByLabelText(/email address/i), 'test@example.com');
            await user.type(screen.getByLabelText(/^password$/i), 'password123');
            await user.type(screen.getByLabelText(/confirm password/i), 'differentpass');
            await user.click(screen.getByRole('button', { name: /sign up/i }));

            expect(screen.getByText('Passwords do not match')).toBeInTheDocument();
            expect(mockSignup).not.toHaveBeenCalled();
        }, 15000);
    });

    describe('form submission', () => {
        it('should call signup with email and password on valid submission', async () => {
            const user = userEvent.setup();
            mockSignup.mockResolvedValue({ email: 'test@example.com' });
            renderSignup(authValue);

            await user.type(screen.getByLabelText(/email address/i), 'test@example.com');
            await user.type(screen.getByLabelText(/^password$/i), 'password123');
            await user.type(screen.getByLabelText(/confirm password/i), 'password123');
            await user.click(screen.getByRole('button', { name: /sign up/i }));

            await waitFor(() => {
                expect(mockSignup).toHaveBeenCalledWith('test@example.com', 'password123');
            });
        }, 15000);

        it('should show success message after successful signup', async () => {
            const user = userEvent.setup();
            mockSignup.mockResolvedValue({ email: 'test@example.com' });
            renderSignup(authValue);

            await user.type(screen.getByLabelText(/email address/i), 'test@example.com');
            await user.type(screen.getByLabelText(/^password$/i), 'password123');
            await user.type(screen.getByLabelText(/confirm password/i), 'password123');
            await user.click(screen.getByRole('button', { name: /sign up/i }));

            await waitFor(() => {
                expect(screen.getByText('Account created successfully!')).toBeInTheDocument();
                expect(screen.getByText(/check your email to verify/i)).toBeInTheDocument();
            });
        }, 15000);

        it.skip('should redirect to login after 3 seconds on success', async () => {
            mockSignup.mockResolvedValue({ email: 'test@example.com' });

            vi.useFakeTimers();

            renderSignup(authValue);

            const emailInput = screen.getByLabelText(/email address/i) as HTMLInputElement;
            const passwordInput = screen.getByLabelText(/^password$/i) as HTMLInputElement;
            const confirmPasswordInput = screen.getByLabelText(/confirm password/i) as HTMLInputElement;

            fireEvent.change(emailInput, { target: { value: 'test@example.com' } });
            fireEvent.change(passwordInput, { target: { value: 'password123' } });
            fireEvent.change(confirmPasswordInput, { target: { value: 'password123' } });

            const submitButton = screen.getByRole('button', { name: /sign up/i });
            fireEvent.click(submitButton);

            await waitFor(() => {
                expect(screen.getByText('Account created successfully!')).toBeInTheDocument();
            }, { timeout: 3000 });

            expect(mockNavigate).not.toHaveBeenCalled();

            vi.advanceTimersByTime(3000);

            expect(mockNavigate).toHaveBeenCalledWith('/login');

            vi.useRealTimers();
        }, 10000);

        it.skip('should show loading state during signup', async () => {
            let resolveSignup: any;
            const signupPromise = new Promise((resolve) => {
                resolveSignup = resolve;
            });
            mockSignup.mockReturnValue(signupPromise);

            renderSignup(authValue);

            const emailInput = screen.getByLabelText(/email address/i) as HTMLInputElement;
            const passwordInput = screen.getByLabelText(/^password$/i) as HTMLInputElement;
            const confirmPasswordInput = screen.getByLabelText(/confirm password/i) as HTMLInputElement;

            fireEvent.change(emailInput, { target: { value: 'test@example.com' } });
            fireEvent.change(passwordInput, { target: { value: 'password123' } });
            fireEvent.change(confirmPasswordInput, { target: { value: 'password123' } });

            const submitButton = screen.getByRole('button', { name: /sign up/i });
            fireEvent.click(submitButton);

            await waitFor(() => {
                expect(screen.getByRole('button', { name: /creating account/i })).toBeDisabled();
            }, { timeout: 3000 });

            resolveSignup({ email: 'test@example.com' });

            await waitFor(() => {
                expect(screen.queryByRole('button', { name: /creating account/i })).not.toBeInTheDocument();
            }, { timeout: 3000 });
        }, 10000);

        it.skip('should display error message on ApiError', async () => {
            mockSignup.mockRejectedValue(new ApiError('Email already exists', 400));
            renderSignup(authValue);

            const emailInput = screen.getByLabelText(/email address/i) as HTMLInputElement;
            const passwordInput = screen.getByLabelText(/^password$/i) as HTMLInputElement;
            const confirmPasswordInput = screen.getByLabelText(/confirm password/i) as HTMLInputElement;

            fireEvent.change(emailInput, { target: { value: 'existing@example.com' } });
            fireEvent.change(passwordInput, { target: { value: 'password123' } });
            fireEvent.change(confirmPasswordInput, { target: { value: 'password123' } });

            const submitButton = screen.getByRole('button', { name: /sign up/i });
            fireEvent.click(submitButton);

            await waitFor(() => {
                expect(screen.getByText('Email already exists')).toBeInTheDocument();
            }, { timeout: 3000 });
        }, 10000);

        it.skip('should display generic error message on unexpected error', async () => {
            mockSignup.mockRejectedValue(new Error('Network error'));
            renderSignup(authValue);

            const emailInput = screen.getByLabelText(/email address/i) as HTMLInputElement;
            const passwordInput = screen.getByLabelText(/^password$/i) as HTMLInputElement;
            const confirmPasswordInput = screen.getByLabelText(/confirm password/i) as HTMLInputElement;

            fireEvent.change(emailInput, { target: { value: 'test@example.com' } });
            fireEvent.change(passwordInput, { target: { value: 'password123' } });
            fireEvent.change(confirmPasswordInput, { target: { value: 'password123' } });

            const submitButton = screen.getByRole('button', { name: /sign up/i });
            fireEvent.click(submitButton);

            await waitFor(() => {
                expect(screen.getByText('An unexpected error occurred')).toBeInTheDocument();
            }, { timeout: 3000 });
        }, 10000);

        it.skip('should clear previous errors on new submission', async () => {
            renderSignup(authValue);

            const emailInput = screen.getByLabelText(/email address/i) as HTMLInputElement;
            const passwordInput = screen.getByLabelText(/^password$/i) as HTMLInputElement;
            const confirmPasswordInput = screen.getByLabelText(/confirm password/i) as HTMLInputElement;

            // First submission with validation error
            fireEvent.change(emailInput, { target: { value: 'test@example.com' } });
            fireEvent.change(passwordInput, { target: { value: 'short' } });
            fireEvent.change(confirmPasswordInput, { target: { value: 'short' } });

            const submitButton = screen.getByRole('button', { name: /sign up/i });
            fireEvent.click(submitButton);

            expect(screen.getByText('Password must be at least 8 characters long')).toBeInTheDocument();

            // Second submission should clear error
            fireEvent.change(passwordInput, { target: { value: 'validpassword123' } });
            fireEvent.change(confirmPasswordInput, { target: { value: 'validpassword123' } });

            mockSignup.mockResolvedValue({ email: 'test@example.com' });
            fireEvent.click(submitButton);

            await waitFor(() => {
                expect(screen.queryByText('Password must be at least 8 characters long')).not.toBeInTheDocument();
            }, { timeout: 3000 });
        }, 10000);
    });

    describe('accessibility', () => {
        it('should have proper form labels', () => {
            renderSignup(authValue);

            expect(screen.getByLabelText(/email address/i)).toHaveAttribute('type', 'email');
            expect(screen.getByLabelText(/^password$/i)).toHaveAttribute('type', 'password');
            expect(screen.getByLabelText(/confirm password/i)).toHaveAttribute('type', 'password');
        });

        it('should mark all fields as required', () => {
            renderSignup(authValue);

            expect(screen.getByLabelText(/email address/i)).toBeRequired();
            expect(screen.getByLabelText(/^password$/i)).toBeRequired();
            expect(screen.getByLabelText(/confirm password/i)).toBeRequired();
        });
    });
});
