import { describe, it, expect, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { ProtectedRoute } from './ProtectedRoute';
import { AuthContext } from '../contexts/AuthContext';

const renderProtectedRoute = (authValue: any, initialEntry = '/') => {
    return render(
        <AuthContext.Provider value={authValue}>
            <MemoryRouter initialEntries={[initialEntry]}>
                <Routes>
                    <Route
                        path="/"
                        element={
                            <ProtectedRoute>
                                <div>Protected Content</div>
                            </ProtectedRoute>
                        }
                    />
                    <Route path="/login" element={<div>Login Page</div>} />
                </Routes>
            </MemoryRouter>
        </AuthContext.Provider>
    );
};

describe('ProtectedRoute', () => {
    describe('when loading', () => {
        it('should display loading spinner', () => {
            const authValue = {
                user: null,
                token: null,
                isLoading: true,
                isAuthenticated: false,
                login: vi.fn(),
                signup: vi.fn(),
                logout: vi.fn(),
                refreshUser: vi.fn(),
            };

            renderProtectedRoute(authValue);

            // Check for the spinner (using class name since it's a div)
            const spinner = document.querySelector('.animate-spin');
            expect(spinner).toBeInTheDocument();
            expect(screen.queryByText('Protected Content')).not.toBeInTheDocument();
        });
    });

    describe('when user is not authenticated', () => {
        it('should redirect to login page', () => {
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

            renderProtectedRoute(authValue);

            expect(screen.getByText('Login Page')).toBeInTheDocument();
            expect(screen.queryByText('Protected Content')).not.toBeInTheDocument();
        });
    });

    describe('when user is authenticated', () => {
        it('should render protected content', async () => {
            const authValue = {
                user: {
                    email: 'test@example.com',
                    userId: '123',
                    emailVerified: true,
                },
                token: 'test-token',
                isLoading: false,
                isAuthenticated: true,
                login: vi.fn(),
                signup: vi.fn(),
                logout: vi.fn(),
                refreshUser: vi.fn(),
            };

            renderProtectedRoute(authValue);

            await waitFor(() => {
                expect(screen.getByText('Protected Content')).toBeInTheDocument();
            });
            expect(screen.queryByText('Login Page')).not.toBeInTheDocument();
        });
    });
});
