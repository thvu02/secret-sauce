import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { BrowserRouter } from 'react-router-dom';
import { Login } from './Login';
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

const renderLogin = (authValue: any) => {
    return render(
        <BrowserRouter>
            <AuthContext.Provider value={authValue}>
                <Login />
            </AuthContext.Provider>
        </BrowserRouter>
    );
};

describe('Login', () => {
    const mockLogin = vi.fn();
    const authValue = {
        user: null,
        token: null,
        isLoading: false,
        isAuthenticated: false,
        login: mockLogin,
        signup: vi.fn(),
        logout: vi.fn(),
        refreshUser: vi.fn(),
    };

    beforeEach(() => {
        vi.clearAllMocks();
    });

    describe('rendering', () => {
        it('should render login form', () => {
            renderLogin(authValue);

            expect(screen.getByRole('heading', { name: /sign in to your account/i })).toBeInTheDocument();
            expect(screen.getByLabelText(/email address/i)).toBeInTheDocument();
            expect(screen.getByLabelText(/^password$/i)).toBeInTheDocument();
            expect(screen.getByRole('button', { name: /sign in/i })).toBeInTheDocument();
        });

        it('should render link to signup page', () => {
            renderLogin(authValue);

            const signupLink = screen.getByRole('link', { name: /create a new account/i });
            expect(signupLink).toBeInTheDocument();
            expect(signupLink).toHaveAttribute('href', '/signup');
        });

        it('should render link to forgot password page', () => {
            renderLogin(authValue);

            const forgotPasswordLink = screen.getByRole('link', { name: /forgot your password/i });
            expect(forgotPasswordLink).toBeInTheDocument();
            expect(forgotPasswordLink).toHaveAttribute('href', '/forgot-password');
        });

        it('should render link back to home', () => {
            renderLogin(authValue);

            const homeLink = screen.getByRole('link', { name: /back to home/i });
            expect(homeLink).toBeInTheDocument();
            expect(homeLink).toHaveAttribute('href', '/');
        });
    });

    describe('form interactions', () => {
        it('should allow typing in email field', async () => {
            const user = userEvent.setup();
            renderLogin(authValue);

            const emailInput = screen.getByLabelText(/email address/i);
            await user.type(emailInput, 'test@example.com');

            expect(emailInput).toHaveValue('test@example.com');
        });

        it('should allow typing in password field', async () => {
            const user = userEvent.setup();
            renderLogin(authValue);

            const passwordInput = screen.getByLabelText(/^password$/i);
            await user.type(passwordInput, 'password123');

            expect(passwordInput).toHaveValue('password123');
        });
    });

    describe('form submission', () => {
        it('should call login with email and password on submit', async () => {
            const user = userEvent.setup();
            mockLogin.mockResolvedValue(undefined);
            renderLogin(authValue);

            await user.type(screen.getByLabelText(/email address/i), 'test@example.com');
            await user.type(screen.getByLabelText(/^password$/i), 'password123');
            await user.click(screen.getByRole('button', { name: /sign in/i }));

            await waitFor(() => {
                expect(mockLogin).toHaveBeenCalledWith('test@example.com', 'password123');
            });
        });

        it('should navigate to dashboard on successful login', async () => {
            const user = userEvent.setup();
            mockLogin.mockResolvedValue(undefined);
            renderLogin(authValue);

            await user.type(screen.getByLabelText(/email address/i), 'test@example.com');
            await user.type(screen.getByLabelText(/^password$/i), 'password123');
            await user.click(screen.getByRole('button', { name: /sign in/i }));

            await waitFor(() => {
                expect(mockNavigate).toHaveBeenCalledWith('/dashboard');
            });
        });

        it('should show loading state during login', async () => {
            const user = userEvent.setup();
            let resolveLogin: any;
            const loginPromise = new Promise((resolve) => {
                resolveLogin = resolve;
            });
            mockLogin.mockReturnValue(loginPromise);

            renderLogin(authValue);

            await user.type(screen.getByLabelText(/email address/i), 'test@example.com');
            await user.type(screen.getByLabelText(/^password$/i), 'password123');
            await user.click(screen.getByRole('button', { name: /sign in/i }));

            expect(screen.getByRole('button', { name: /signing in/i })).toBeDisabled();

            resolveLogin(undefined);

            await waitFor(() => {
                expect(screen.getByRole('button', { name: /sign in/i })).not.toBeDisabled();
            });
        });

        it('should display error message on ApiError', async () => {
            const user = userEvent.setup();
            mockLogin.mockRejectedValue(new ApiError('Invalid credentials', 401));
            renderLogin(authValue);

            await user.type(screen.getByLabelText(/email address/i), 'test@example.com');
            await user.type(screen.getByLabelText(/^password$/i), 'wrongpassword');
            await user.click(screen.getByRole('button', { name: /sign in/i }));

            await waitFor(() => {
                expect(screen.getByText('Invalid credentials')).toBeInTheDocument();
            });
        });

        it('should display generic error message on unexpected error', async () => {
            const user = userEvent.setup();
            mockLogin.mockRejectedValue(new Error('Network error'));
            renderLogin(authValue);

            await user.type(screen.getByLabelText(/email address/i), 'test@example.com');
            await user.type(screen.getByLabelText(/^password$/i), 'password123');
            await user.click(screen.getByRole('button', { name: /sign in/i }));

            await waitFor(() => {
                expect(screen.getByText('An unexpected error occurred')).toBeInTheDocument();
            });
        });

        it('should clear previous error on new submission', async () => {
            const user = userEvent.setup();
            mockLogin.mockRejectedValueOnce(new ApiError('Invalid credentials', 401));
            renderLogin(authValue);

            // First submission with error
            await user.type(screen.getByLabelText(/email address/i), 'test@example.com');
            await user.type(screen.getByLabelText(/^password$/i), 'wrongpassword');
            await user.click(screen.getByRole('button', { name: /sign in/i }));

            await waitFor(() => {
                expect(screen.getByText('Invalid credentials')).toBeInTheDocument();
            });

            // Second submission (should clear error before attempting login)
            mockLogin.mockResolvedValueOnce(undefined);
            await user.clear(screen.getByLabelText(/^password$/i));
            await user.type(screen.getByLabelText(/^password$/i), 'correctpassword');
            await user.click(screen.getByRole('button', { name: /sign in/i }));

            await waitFor(() => {
                expect(screen.queryByText('Invalid credentials')).not.toBeInTheDocument();
            });
        });
    });

    describe('accessibility', () => {
        it('should have proper form labels', () => {
            renderLogin(authValue);

            expect(screen.getByLabelText(/email address/i)).toHaveAttribute('type', 'email');
            expect(screen.getByLabelText(/^password$/i)).toHaveAttribute('type', 'password');
        });

        it('should mark email and password fields as required', () => {
            renderLogin(authValue);

            expect(screen.getByLabelText(/email address/i)).toBeRequired();
            expect(screen.getByLabelText(/^password$/i)).toBeRequired();
        });
    });
});
