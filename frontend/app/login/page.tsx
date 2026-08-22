"use client";

import { useState, useEffect, Suspense } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";
import { useAuth } from "@/context/AuthContext";
import { ApiError } from "@/lib/apiClient";

function LoginForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { login } = useAuth();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [message, setMessage] = useState("");
  const [isError, setIsError] = useState(false);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (searchParams.get("expired") === "true") {
      setMessage("Your session has expired. Please sign in again.");
      setIsError(true);
    }
  }, [searchParams]);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    if (loading) return;

    if (!email.trim() || !password.trim()) {
      setMessage("Please fill in all fields");
      setIsError(true);
      return;
    }

    setLoading(true);
    setMessage("");

    try {
      await login({
        email: email.trim(),
        password,
      });

      router.push("/dashboard");
    } catch (err: any) {
      setIsError(true);

      if (err instanceof ApiError) {
        if (err.status === 401) {
          setMessage("Invalid email or password.");
        } else if (err.status === 400) {
          if (err.details && err.details.length > 0) {
            setMessage(err.details.join(", "));
          } else {
            setMessage(err.message || "Invalid login input.");
          }
        } else {
          setMessage(err.message || "Login failed. Please try again.");
        }
      } else {
        setMessage("Unable to log in. Please check your credentials or backend server.");
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-[url('/bg.webp')] bg-cover bg-center p-4">
      <div className="bg-white/95 backdrop-blur-md shadow-2xl rounded-2xl p-8 w-full max-w-md border border-white/20">
        <div className="text-center mb-6">
          <h1 className="text-2xl sm:text-3xl font-extrabold text-gray-900 tracking-tight">
            Welcome Back
          </h1>
          <p className="text-gray-500 text-sm mt-1">
            Log in to continue your career journey
          </p>
        </div>

        <form onSubmit={handleLogin} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-gray-700 uppercase tracking-wider mb-1">
              Email Address
            </label>
            <input
              type="email"
              placeholder="alex@example.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="w-full p-3 border border-gray-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-black text-sm transition"
              required
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-gray-700 uppercase tracking-wider mb-1">
              Password
            </label>
            <input
              type="password"
              placeholder="Enter your password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full p-3 border border-gray-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-black text-sm transition"
              required
            />
          </div>

          {message && (
            <div
              className={`p-3 rounded-xl text-xs font-medium text-center ${
                isError
                  ? "bg-red-50 text-red-600 border border-red-200"
                  : "bg-green-50 text-green-700 border border-green-200"
              }`}
            >
              {message}
            </div>
          )}

          <button
            type="submit"
            disabled={loading}
            className={`w-full py-3.5 px-4 rounded-xl text-white font-semibold text-sm transition shadow-md flex items-center justify-center gap-2 ${
              loading
                ? "bg-gray-400 cursor-not-allowed"
                : "bg-black hover:bg-gray-800"
            }`}
          >
            {loading ? (
              <>
                <span className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></span>
                <span>Signing in...</span>
              </>
            ) : (
              "Sign In to Dashboard →"
            )}
          </button>
        </form>

        <div className="mt-6 pt-5 border-t border-gray-200 text-center">
          <p className="text-sm text-gray-600">
            Don't have an account?{" "}
            <Link
              href="/signup"
              className="text-black font-bold hover:underline"
            >
              Create one
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}

export default function Login() {
  return (
    <Suspense
      fallback={
        <div className="min-h-screen flex items-center justify-center bg-black text-white text-sm">
          Loading sign in...
        </div>
      }
    >
      <LoginForm />
    </Suspense>
  );
}