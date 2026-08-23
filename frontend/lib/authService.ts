import { API_ENDPOINTS } from "./config";
import { apiRequest } from "./apiClient";

export interface SignupRequest {
  name: string;
  email: string;
  password: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  type: string;
  id: number;
  name: string;
  email: string;
  role?: "USER" | "ADMIN" | string;
}

export interface User {
  id: number;
  name: string;
  email: string;
  role?: "USER" | "ADMIN" | string;
  careerGoal?: string;
  userLevel?: string;
}

export const authService = {
  async register(data: SignupRequest): Promise<AuthResponse> {
    return apiRequest<AuthResponse>(API_ENDPOINTS.AUTH.REGISTER, {
      method: "POST",
      body: JSON.stringify(data),
    });
  },

  async login(data: LoginRequest): Promise<AuthResponse> {
    return apiRequest<AuthResponse>(API_ENDPOINTS.AUTH.LOGIN, {
      method: "POST",
      body: JSON.stringify(data),
    });
  },

  async getCurrentUser(token?: string): Promise<User> {
    const headers: Record<string, string> = {};
    if (token) {
      headers["Authorization"] = `Bearer ${token}`;
    }
    return apiRequest<User>(API_ENDPOINTS.AUTH.ME, {
      method: "GET",
      headers,
    });
  },
};
