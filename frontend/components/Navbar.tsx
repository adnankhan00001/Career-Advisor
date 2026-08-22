"use client";

import Link from "next/link";
import { useAuth } from "@/context/AuthContext";

export default function Navbar() {
  const { isAuthenticated, user } = useAuth();

  return (
    <nav className="w-full z-20 px-6 sm:px-12 py-5 flex justify-between items-center bg-black/40 backdrop-blur-md border-b border-white/10 text-white">
      {/* Logo */}
      <Link
        href="/"
        className="flex items-center gap-2.5 font-bold text-lg sm:text-xl tracking-tight"
      >
        <span className="w-8 h-8 rounded-lg bg-blue-600 flex items-center justify-center text-sm font-extrabold text-white shadow">
          CA
        </span>
        <span>Career Advisor</span>
      </Link>

      {/* Action Links */}
      <div className="flex items-center gap-3 sm:gap-4">
        {isAuthenticated ? (
          <Link
            href="/dashboard"
            className="text-xs sm:text-sm font-semibold bg-white text-black px-4 py-2 rounded-xl hover:bg-gray-100 transition shadow-sm flex items-center gap-2"
          >
            <span>Dashboard</span>
            <span className="text-xs opacity-75">({user?.name || "Account"})</span>
            <span>→</span>
          </Link>
        ) : (
          <>
            <Link
              href="/login"
              className="text-xs sm:text-sm font-medium text-gray-200 hover:text-white transition px-3 py-1.5"
            >
              Sign In
            </Link>
            <Link
              href="/signup"
              className="text-xs sm:text-sm font-semibold bg-white text-black px-4 py-2 rounded-xl hover:bg-gray-100 transition shadow-sm"
            >
              Get Started
            </Link>
          </>
        )}
      </div>
    </nav>
  );
}