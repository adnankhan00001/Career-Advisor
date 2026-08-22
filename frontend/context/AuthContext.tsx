"use client";

import React, {
  createContext,
  useContext,
  useState,
  useEffect,
  useCallback,
} from "react";
import {
  authService,
  User,
  LoginRequest,
  SignupRequest,
  AuthResponse,
} from "@/lib/authService";
import {
  getStoredToken,
  setStoredToken,
  removeStoredToken,
  getStoredUser,
  setStoredUser,
  removeStoredUser,
} from "@/lib/apiClient";

interface AuthContextType {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (credentials: LoginRequest) => Promise<AuthResponse>;
  signup: (data: SignupRequest) => Promise<AuthResponse>;
  logout: () => void;
  refreshUser: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);

  // Initialize and verify authentication on mount
  useEffect(() => {
    async function initAuth() {
      const storedToken = getStoredToken();
      const storedUser = getStoredUser();

      if (storedToken) {
        setToken(storedToken);
        if (storedUser) {
          setUser(storedUser);
        }

        try {
          // Verify token against backend and get authoritative user details
          const authoritativeUser = await authService.getCurrentUser(storedToken);
          setUser(authoritativeUser);
          setStoredUser(authoritativeUser);
        } catch (err: any) {
          // If token expired or rejected by backend (401), clean up
          if (err?.status === 401) {
            removeStoredToken();
            removeStoredUser();
            setToken(null);
            setUser(null);
          }
        }
      }
      setIsLoading(false);
    }

    initAuth();
  }, []);

  const login = useCallback(async (credentials: LoginRequest): Promise<AuthResponse> => {
    const response = await authService.login(credentials);
    const userData: User = {
      id: response.id,
      name: response.name,
      email: response.email,
      role: response.role || "USER",
    };

    setStoredToken(response.token);
    setStoredUser(userData);
    setToken(response.token);
    setUser(userData);

    return response;
  }, []);

  const signup = useCallback(async (data: SignupRequest): Promise<AuthResponse> => {
    const response = await authService.register(data);
    const userData: User = {
      id: response.id,
      name: response.name,
      email: response.email,
      role: response.role || "USER",
    };

    setStoredToken(response.token);
    setStoredUser(userData);
    setToken(response.token);
    setUser(userData);

    return response;
  }, []);

  const logout = useCallback(() => {
    removeStoredToken();
    removeStoredUser();
    setToken(null);
    setUser(null);
  }, []);

  const refreshUser = useCallback(async () => {
    const currentToken = getStoredToken();
    if (!currentToken) return;

    try {
      const authoritativeUser = await authService.getCurrentUser(currentToken);
      setUser(authoritativeUser);
      setStoredUser(authoritativeUser);
    } catch {
      // Ignore background refresh failure
    }
  }, []);

  const value: AuthContextType = {
    user,
    token,
    isAuthenticated: Boolean(token && user),
    isLoading,
    login,
    signup,
    logout,
    refreshUser,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextType {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
