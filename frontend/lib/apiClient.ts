export class ApiError extends Error {
  status: number;
  error: string;
  details?: string[];

  constructor(status: number, message: string, error: string = "ApiError", details?: string[]) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.error = error;
    this.details = details;
  }
}

const TOKEN_KEY = "auth_token";
const USER_KEY = "user";

export function getStoredToken(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem(TOKEN_KEY);
}

export function setStoredToken(token: string): void {
  if (typeof window === "undefined") return;
  localStorage.setItem(TOKEN_KEY, token);
}

export function removeStoredToken(): void {
  if (typeof window === "undefined") return;
  localStorage.removeItem(TOKEN_KEY);
}

export function getStoredUser(): any | null {
  if (typeof window === "undefined") return null;
  try {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

export function setStoredUser(user: any): void {
  if (typeof window === "undefined") return;
  localStorage.setItem(USER_KEY, JSON.stringify(user));
}

export function removeStoredUser(): void {
  if (typeof window === "undefined") return;
  localStorage.removeItem(USER_KEY);
}

export async function apiRequest<T = any>(
  url: string,
  options: RequestInit = {}
): Promise<T> {
  const isFormData = typeof FormData !== "undefined" && options.body instanceof FormData;

  const headers: Record<string, string> = {
    ...(isFormData ? {} : { "Content-Type": "application/json" }),
    ...(options.headers as Record<string, string>),
  };

  const token = getStoredToken();
  if (token && !headers["Authorization"]) {
    headers["Authorization"] = `Bearer ${token}`;
  }

  const config: RequestInit = {
    ...options,
    headers,
  };

  let response: Response;
  try {
    response = await fetch(url, config);
  } catch (networkErr: any) {
    throw new ApiError(
      0,
      "Unable to connect to the backend server. Please verify the backend is running.",
      "NetworkError"
    );
  }

  // Parse response body
  const contentType = response.headers.get("content-type");
  let data: any = null;
  if (contentType && contentType.includes("application/json")) {
    try {
      data = await response.json();
    } catch {
      data = null;
    }
  } else {
    try {
      data = await response.text();
    } catch {
      data = null;
    }
  }

  if (!response.ok) {
    let message = "Request failed";
    let details: string[] | undefined = undefined;
    let errorType = "HttpError";

    if (data && typeof data === "object") {
      message = data.message || data.error || message;
      details = data.details;
      errorType = data.error || errorType;
    } else if (typeof data === "string" && data.trim()) {
      message = data;
    }

    if (response.status === 401) {
      // If token expired/invalid on a protected call (not login/register)
      if (token && !url.includes("/login") && !url.includes("/register") && !url.includes("/signup")) {
        removeStoredToken();
        removeStoredUser();
        if (typeof window !== "undefined" && !window.location.pathname.includes("/login")) {
          window.location.href = "/login?expired=true";
        }
      }
    }

    throw new ApiError(response.status, message, errorType, details);
  }

  return data as T;
}
