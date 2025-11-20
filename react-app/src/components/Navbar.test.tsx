import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { BrowserRouter } from 'react-router-dom';
import { Navbar } from './Navbar';
import { AuthContext } from '../contexts/AuthContext';

const mockNavigate = vi.fn();

vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual('react-router-dom');
    return {
        ...actual,
        useNavigate: () => mockNavigate,
    };
});

const renderWithAuth = (authValue: any) => {
    return render(
        <BrowserRouter>
            <AuthContext.Provider value={authValue}>
                <Navbar />
            </AuthContext.Provider>
        </BrowserRouter>
    );
};

describe('Navbar', () => {
    beforeEach(() => {
        mockNavigate.mockClear();
    });

    describe('when user is not authenticated', () => {
        const authValue = {
            user: null,
            token: null,
            isLoading: false,
            isAuthenticated: false,
            login: vi.fn(),
            signup: vi.fn(),
            logout: vi.fn(),
            refreshUser: vi.fn(),
        };

        it('should render the brand name', () => {
            renderWithAuth(authValue);
            expect(screen.getByText('SplittyDupe')).toBeInTheDocument();
        });

        it('should display login and signup links', () => {
            renderWithAuth(authValue);
            expect(screen.getByRole('link', { name: /login/i })).toBeInTheDocument();
            expect(screen.getByRole('link', { name: /sign up/i })).toBeInTheDocument();
        });

        it('should not display authenticated user links', () => {
            renderWithAuth(authValue);
            expect(screen.queryByRole('link', { name: /dashboard/i })).not.toBeInTheDocument();
            expect(screen.queryByRole('link', { name: /profile/i })).not.toBeInTheDocument();
            expect(screen.queryByRole('button', { name: /logout/i })).not.toBeInTheDocument();
        });

        it('should display home link', () => {
            renderWithAuth(authValue);
            expect(screen.getByRole('link', { name: /^home$/i })).toBeInTheDocument();
        });
    });

    describe('when user is authenticated', () => {
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

        it('should display user email', () => {
            renderWithAuth(authValue);
            expect(screen.getByText('test@example.com')).toBeInTheDocument();
        });

        it('should display dashboard and profile links', () => {
            renderWithAuth(authValue);
            expect(screen.getByRole('link', { name: /dashboard/i })).toBeInTheDocument();
            expect(screen.getByRole('link', { name: /profile/i })).toBeInTheDocument();
        });

        it('should display logout button', () => {
            renderWithAuth(authValue);
            expect(screen.getByRole('button', { name: /logout/i })).toBeInTheDocument();
        });

        it('should not display login and signup links', () => {
            renderWithAuth(authValue);
            expect(screen.queryByRole('link', { name: /^login$/i })).not.toBeInTheDocument();
            expect(screen.queryByRole('link', { name: /sign up/i })).not.toBeInTheDocument();
        });

        it('should call logout and navigate to home when logout is clicked', async () => {
            const user = userEvent.setup();
            renderWithAuth(authValue);

            const logoutButton = screen.getByRole('button', { name: /logout/i });
            await user.click(logoutButton);

            expect(authValue.logout).toHaveBeenCalledTimes(1);
            expect(mockNavigate).toHaveBeenCalledWith('/');
        });
    });
});
