import React, { createContext, useState, useEffect } from 'react';
import type { ReactNode } from 'react';
import { authApi, type AuthResponse } from '../services/api';

interface User {
    email: string;
    userId: string;
    emailVerified: boolean;
}

interface AuthContextType {
    user: User | null;
    token: string | null;
    isLoading: boolean;
    isAuthenticated: boolean;
    login: (email: string, password: string) => Promise<void>;
    signup: (email: string, password: string, displayName: string) => Promise<AuthResponse>;
    logout: () => void;
    refreshUser: () => Promise<void>;
}

export const AuthContext = createContext<AuthContextType | undefined>(undefined);

interface AuthProviderProps {
    children: ReactNode;
}

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
    const [user, setUser] = useState<User | null>(null);
    const [token, setToken] = useState<string | null>(null);
    const [isLoading, setIsLoading] = useState(true);

    // Load token and user from localStorage on mount
    useEffect(() => {
        const initAuth = async () => {
            const storedToken = localStorage.getItem('authToken');
            const storedUser = localStorage.getItem('user');

            if (storedToken && storedUser) {
                setToken(storedToken);
                setUser(JSON.parse(storedUser));

                // Verify token is still valid
                try {
                    const userData = await authApi.getCurrentUser();
                    setUser({
                        email: userData.email,
                        userId: userData.userId,
                        emailVerified: userData.emailVerified,
                    });
                } catch (error) {
                    // Token is invalid, clear auth
                    localStorage.removeItem('authToken');
                    localStorage.removeItem('user');
                    setToken(null);
                    setUser(null);
                }
            }

            setIsLoading(false);
        };

        initAuth();
    }, []);

    const login = async (email: string, password: string) => {
        const response = await authApi.login({ email, password });

        if (!response.token) {
            throw new Error('No token received');
        }

        const userData: User = {
            email: response.email,
            userId: response.userId,
            emailVerified: response.emailVerified,
        };

        setToken(response.token);
        setUser(userData);

        localStorage.setItem('authToken', response.token);
        localStorage.setItem('user', JSON.stringify(userData));
    };

    const signup = async (email: string, password: string, displayName: string): Promise<AuthResponse> => {
        const response = await authApi.signup({ email, password, displayName });
        return response;
    };

    const logout = () => {
        setToken(null);
        setUser(null);
        localStorage.removeItem('authToken');
        localStorage.removeItem('user');
    };

    const refreshUser = async () => {
        if (!token) return;

        try {
            const userData = await authApi.getCurrentUser();
            const updatedUser: User = {
                email: userData.email,
                userId: userData.userId,
                emailVerified: userData.emailVerified,
            };

            setUser(updatedUser);
            localStorage.setItem('user', JSON.stringify(updatedUser));
        } catch (error) {
            console.error('Failed to refresh user:', error);
            logout();
        }
    };

    const value: AuthContextType = {
        user,
        token,
        isLoading,
        isAuthenticated: !!user && !!token,
        login,
        signup,
        logout,
        refreshUser,
    };

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};
