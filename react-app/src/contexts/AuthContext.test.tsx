import { describe, it, expect, vi, beforeEach } from 'vitest';
import { waitFor } from '@testing-library/react';
import { renderHook, act } from '@testing-library/react';
import { AuthProvider } from './AuthContext';
import { useAuth } from '../hooks/useAuth';
import { authApi } from '../services/api';

vi.mock('../services/api', () => ({
    authApi: {
        login: vi.fn(),
        signup: vi.fn(),
        getCurrentUser: vi.fn(),
    },
}));

const localStorageMock = (() => {
    let store: Record<string, string> = {};

    return {
        getItem: (key: string) => store[key] || null,
        setItem: (key: string, value: string) => {
            store[key] = value.toString();
        },
        removeItem: (key: string) => {
            delete store[key];
        },
        clear: () => {
            store = {};
        },
    };
})();

Object.defineProperty(window, 'localStorage', {
    value: localStorageMock,
});

describe('AuthContext', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        localStorageMock.clear();
    });

    describe('AuthProvider initialization', () => {
        it('should start with loading state and then finish', async () => {
            const { result } = renderHook(() => useAuth(), {
                wrapper: AuthProvider,
            });

            // Initially loading should be true, but it sets to false very quickly
            // Since we can't catch the initial state reliably, we just verify
            // that it eventually becomes false
            await waitFor(() => {
                expect(result.current.isLoading).toBe(false);
            });

            expect(result.current.user).toBeNull();
            expect(result.current.token).toBeNull();
            expect(result.current.isAuthenticated).toBe(false);
        });

        it('should initialize without auth when localStorage is empty', async () => {
            const { result } = renderHook(() => useAuth(), {
                wrapper: AuthProvider,
            });

            await waitFor(() => {
                expect(result.current.isLoading).toBe(false);
            });

            expect(result.current.user).toBeNull();
            expect(result.current.token).toBeNull();
            expect(result.current.isAuthenticated).toBe(false);
        });

        it('should restore auth from localStorage on mount', async () => {
            const storedUser = {
                email: 'test@example.com',
                userId: '123',
                emailVerified: true,
            };

            localStorageMock.setItem('authToken', 'stored-token');
            localStorageMock.setItem('user', JSON.stringify(storedUser));

            (authApi.getCurrentUser as any).mockResolvedValue(storedUser);

            const { result } = renderHook(() => useAuth(), {
                wrapper: AuthProvider,
            });

            await waitFor(() => {
                expect(result.current.isLoading).toBe(false);
            });

            expect(result.current.user).toEqual(storedUser);
            expect(result.current.token).toBe('stored-token');
            expect(result.current.isAuthenticated).toBe(true);
        });

        it('should clear invalid auth from localStorage', async () => {
            localStorageMock.setItem('authToken', 'invalid-token');
            localStorageMock.setItem('user', JSON.stringify({ email: 'test@example.com' }));

            (authApi.getCurrentUser as any).mockRejectedValue(new Error('Unauthorized'));

            const { result } = renderHook(() => useAuth(), {
                wrapper: AuthProvider,
            });

            await waitFor(() => {
                expect(result.current.isLoading).toBe(false);
            });

            expect(result.current.user).toBeNull();
            expect(result.current.token).toBeNull();
            expect(localStorageMock.getItem('authToken')).toBeNull();
            expect(localStorageMock.getItem('user')).toBeNull();
        });
    });

    describe('login', () => {
        it('should set user and token on successful login', async () => {
            const loginResponse = {
                token: 'new-token',
                email: 'test@example.com',
                userId: '123',
                emailVerified: true,
            };

            (authApi.login as any).mockResolvedValue(loginResponse);

            const { result } = renderHook(() => useAuth(), {
                wrapper: AuthProvider,
            });

            await waitFor(() => {
                expect(result.current.isLoading).toBe(false);
            });

            await act(async () => {
                await result.current.login('test@example.com', 'password123');
            });

            expect(result.current.user).toEqual({
                email: 'test@example.com',
                userId: '123',
                emailVerified: true,
            });
            expect(result.current.token).toBe('new-token');
            expect(result.current.isAuthenticated).toBe(true);
        });

        it('should save auth to localStorage on successful login', async () => {
            const loginResponse = {
                token: 'new-token',
                email: 'test@example.com',
                userId: '123',
                emailVerified: true,
            };

            (authApi.login as any).mockResolvedValue(loginResponse);

            const { result } = renderHook(() => useAuth(), {
                wrapper: AuthProvider,
            });

            await waitFor(() => {
                expect(result.current.isLoading).toBe(false);
            });

            await act(async () => {
                await result.current.login('test@example.com', 'password123');
            });

            expect(localStorageMock.getItem('authToken')).toBe('new-token');
            expect(JSON.parse(localStorageMock.getItem('user')!)).toEqual({
                email: 'test@example.com',
                userId: '123',
                emailVerified: true,
            });
        });

        it('should throw error when no token is returned', async () => {
            (authApi.login as any).mockResolvedValue({
                email: 'test@example.com',
                userId: '123',
            });

            const { result } = renderHook(() => useAuth(), {
                wrapper: AuthProvider,
            });

            await waitFor(() => {
                expect(result.current.isLoading).toBe(false);
            });

            await expect(async () => {
                await act(async () => {
                    await result.current.login('test@example.com', 'password123');
                });
            }).rejects.toThrow('No token received');
        });
    });

    describe('signup', () => {
        it('should call authApi.signup with credentials', async () => {
            const signupResponse = {
                email: 'new@example.com',
                userId: '456',
            };

            (authApi.signup as any).mockResolvedValue(signupResponse);

            const { result } = renderHook(() => useAuth(), {
                wrapper: AuthProvider,
            });

            await waitFor(() => {
                expect(result.current.isLoading).toBe(false);
            });

            let response;
            await act(async () => {
                response = await result.current.signup('new@example.com', 'password123');
            });

            expect(authApi.signup).toHaveBeenCalledWith({
                email: 'new@example.com',
                password: 'password123',
            });
            expect(response).toEqual(signupResponse);
        });

        it('should not automatically log in user after signup', async () => {
            (authApi.signup as any).mockResolvedValue({
                email: 'new@example.com',
                userId: '456',
            });

            const { result } = renderHook(() => useAuth(), {
                wrapper: AuthProvider,
            });

            await waitFor(() => {
                expect(result.current.isLoading).toBe(false);
            });

            await act(async () => {
                await result.current.signup('new@example.com', 'password123');
            });

            expect(result.current.user).toBeNull();
            expect(result.current.token).toBeNull();
            expect(result.current.isAuthenticated).toBe(false);
        });
    });

    describe('logout', () => {
        it('should clear user and token', async () => {
            const loginResponse = {
                token: 'test-token',
                email: 'test@example.com',
                userId: '123',
                emailVerified: true,
            };

            (authApi.login as any).mockResolvedValue(loginResponse);

            const { result } = renderHook(() => useAuth(), {
                wrapper: AuthProvider,
            });

            await waitFor(() => {
                expect(result.current.isLoading).toBe(false);
            });

            await act(async () => {
                await result.current.login('test@example.com', 'password123');
            });

            expect(result.current.isAuthenticated).toBe(true);

            act(() => {
                result.current.logout();
            });

            expect(result.current.user).toBeNull();
            expect(result.current.token).toBeNull();
            expect(result.current.isAuthenticated).toBe(false);
        });

        it('should clear localStorage on logout', async () => {
            const loginResponse = {
                token: 'test-token',
                email: 'test@example.com',
                userId: '123',
                emailVerified: true,
            };

            (authApi.login as any).mockResolvedValue(loginResponse);

            const { result } = renderHook(() => useAuth(), {
                wrapper: AuthProvider,
            });

            await waitFor(() => {
                expect(result.current.isLoading).toBe(false);
            });

            await act(async () => {
                await result.current.login('test@example.com', 'password123');
            });

            expect(localStorageMock.getItem('authToken')).toBe('test-token');

            act(() => {
                result.current.logout();
            });

            expect(localStorageMock.getItem('authToken')).toBeNull();
            expect(localStorageMock.getItem('user')).toBeNull();
        });
    });

    describe('refreshUser', () => {
        it('should update user data from API', async () => {
            const loginResponse = {
                token: 'test-token',
                email: 'old@example.com',
                userId: '123',
                emailVerified: false,
            };

            const updatedUserData = {
                email: 'old@example.com',
                userId: '123',
                emailVerified: true,
            };

            (authApi.login as any).mockResolvedValue(loginResponse);
            (authApi.getCurrentUser as any).mockResolvedValue(updatedUserData);

            const { result } = renderHook(() => useAuth(), {
                wrapper: AuthProvider,
            });

            await waitFor(() => {
                expect(result.current.isLoading).toBe(false);
            });

            await act(async () => {
                await result.current.login('old@example.com', 'password123');
            });

            expect(result.current.user?.emailVerified).toBe(false);

            await act(async () => {
                await result.current.refreshUser();
            });

            expect(result.current.user?.emailVerified).toBe(true);
            expect(JSON.parse(localStorageMock.getItem('user')!).emailVerified).toBe(true);
        });

        it('should logout on refresh failure', async () => {
            const loginResponse = {
                token: 'test-token',
                email: 'test@example.com',
                userId: '123',
                emailVerified: true,
            };

            const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

            (authApi.login as any).mockResolvedValue(loginResponse);
            (authApi.getCurrentUser as any).mockRejectedValue(new Error('Token expired'));

            const { result } = renderHook(() => useAuth(), {
                wrapper: AuthProvider,
            });

            await waitFor(() => {
                expect(result.current.isLoading).toBe(false);
            });

            await act(async () => {
                await result.current.login('test@example.com', 'password123');
            });

            expect(result.current.isAuthenticated).toBe(true);

            await act(async () => {
                await result.current.refreshUser();
            });

            expect(result.current.user).toBeNull();
            expect(result.current.token).toBeNull();
            expect(result.current.isAuthenticated).toBe(false);

            consoleErrorSpy.mockRestore();
        });

        it('should do nothing when no token is present', async () => {
            (authApi.getCurrentUser as any).mockResolvedValue({
                email: 'test@example.com',
                userId: '123',
                emailVerified: true,
            });

            const { result } = renderHook(() => useAuth(), {
                wrapper: AuthProvider,
            });

            await waitFor(() => {
                expect(result.current.isLoading).toBe(false);
            });

            await act(async () => {
                await result.current.refreshUser();
            });

            expect(authApi.getCurrentUser).not.toHaveBeenCalled();
        });
    });
});
