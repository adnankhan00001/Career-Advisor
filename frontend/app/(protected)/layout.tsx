"use client";

import { useState, useEffect } from "react";
import Link from "next/link";
import { useRouter, usePathname } from "next/navigation";
import { useAuth } from "@/context/AuthContext";

export default function ProtectedLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const router = useRouter();
  const pathname = usePathname();
  const { user, isAuthenticated, isLoading, logout } = useAuth();

  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      router.replace("/login");
    }
  }, [isLoading, isAuthenticated, router]);

  // Close mobile menu whenever route changes
  useEffect(() => {
    setMobileMenuOpen(false);
  }, [pathname]);

  const handleLogout = () => {
    logout();
    router.push("/login");
  };

  const navItems = [
    { label: "Dashboard", href: "/dashboard", icon: "📊" },
    ...(user?.role === "ADMIN"
      ? [{ label: "Admin Dashboard", href: "/admin", icon: "🛡️" }]
      : []),
    { label: "Resume Analyzer", href: "/resume", icon: "📄" },
    { label: "Mock Interview", href: "/mock-interview", icon: "🎯" },
    { label: "Practice Hub", href: "/practice", icon: "💡" },
    { label: "Careers", href: "/careers", icon: "🚀" },
    { label: "Roadmap", href: "/roadmap", icon: "🗺️" },
    { label: "Skills", href: "/skills", icon: "⚡" },
    { label: "Assessment", href: "/quiz", icon: "📝" },
    { label: "Profile", href: "/profile", icon: "👤" },
  ];

  const getPageTitle = () => {
    if (pathname.startsWith("/admin/users")) return "Admin User Governance";
    if (pathname.startsWith("/admin")) return "Admin Platform Governance";
    if (pathname.startsWith("/resume")) return "Resume Analyzer & Skill Match";
    if (pathname.startsWith("/mock-interview")) return "Technical Mock Interview";
    if (pathname.startsWith("/practice")) return "Interview Practice Hub";
    if (pathname.startsWith("/careers")) return "Career Explorer";
    if (pathname.startsWith("/roadmap")) return "Career Roadmap";
    if (pathname.startsWith("/skills")) return "Skills Portfolio";
    if (pathname.startsWith("/quiz")) return "Skill Assessment";
    if (pathname.startsWith("/profile")) return "User Profile";
    return "Dashboard Overview";
  };

  if (isLoading) {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center bg-gray-50 text-gray-500 gap-3">
        <div className="w-8 h-8 border-4 border-black border-t-transparent rounded-full animate-spin"></div>
        <p className="text-sm font-medium">Verifying authentication...</p>
      </div>
    );
  }

  if (!isAuthenticated) {
    return null;
  }

  const displayName = user?.name || "Learner";
  const displayEmail = user?.email || "";
  const avatarInitial = displayName.charAt(0).toUpperCase();

  return (
    <div className="flex min-h-screen bg-gray-50">
      {/* Mobile Drawer Overlay */}
      {mobileMenuOpen && (
        <div
          className="fixed inset-0 z-40 bg-black/60 backdrop-blur-sm lg:hidden"
          onClick={() => setMobileMenuOpen(false)}
        />
      )}

      {/* Sidebar (Desktop & Mobile Drawer) */}
      <aside
        className={`fixed inset-y-0 left-0 z-50 w-64 bg-black text-white flex flex-col justify-between p-6 transition-transform duration-300 ease-in-out lg:static lg:translate-x-0 ${
          mobileMenuOpen ? "translate-x-0" : "-translate-x-full"
        }`}
      >
        <div>
          {/* Brand Header */}
          <div className="flex items-center justify-between pb-6 border-b border-gray-800">
            <Link
              href="/dashboard"
              className="flex items-center gap-2.5 font-extrabold text-xl tracking-tight text-white"
            >
              <span className="w-8 h-8 rounded-lg bg-blue-600 flex items-center justify-center text-sm font-bold shadow">
                CA
              </span>
              <span>Career Advisor</span>
            </Link>

            <button
              onClick={() => setMobileMenuOpen(false)}
              className="lg:hidden text-gray-400 hover:text-white p-1 cursor-pointer"
              aria-label="Close menu"
            >
              ✕
            </button>
          </div>

          {/* Navigation Links */}
          <nav className="mt-6 space-y-1.5">
            {navItems.map((item) => {
              const isActive =
                item.href === "/dashboard"
                  ? pathname === "/dashboard"
                  : pathname.startsWith(item.href);

              return (
                <Link
                  key={item.href}
                  href={item.href}
                  className={`flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-medium transition ${
                    isActive
                      ? "bg-white/15 text-white font-bold shadow-sm"
                      : "text-gray-400 hover:text-white hover:bg-white/5"
                  }`}
                >
                  <span className="text-base">{item.icon}</span>
                  <span>{item.label}</span>
                </Link>
              );
            })}
          </nav>
        </div>

        {/* User Card & Logout */}
        <div className="pt-6 border-t border-gray-800 space-y-4">
          <Link
            href="/profile"
            className="flex items-center gap-3 p-2 rounded-xl hover:bg-white/5 transition"
          >
            <div className="w-9 h-9 rounded-xl bg-blue-600 text-white flex items-center justify-center font-bold text-sm shadow">
              {avatarInitial}
            </div>
            <div className="overflow-hidden flex-1">
              <div className="flex items-center gap-1.5">
                <p className="text-xs font-bold text-white truncate">{displayName}</p>
                {user?.role === "ADMIN" && (
                  <span className="px-1.5 py-0.5 rounded text-[9px] font-extrabold bg-purple-600 text-white">
                    ADMIN
                  </span>
                )}
              </div>
              <p className="text-[11px] text-gray-400 truncate">
                {displayEmail || "Learner"}
              </p>
            </div>
          </Link>

          <button
            onClick={handleLogout}
            className="w-full flex items-center justify-center gap-2 py-2 px-3 bg-red-950/40 text-red-400 border border-red-900/50 rounded-xl text-xs font-semibold hover:bg-red-900/60 hover:text-red-300 transition cursor-pointer"
          >
            <span>Sign Out</span>
            <span>🚪</span>
          </button>
        </div>
      </aside>

      {/* Main Content Area */}
      <div className="flex-1 flex flex-col min-w-0">
        {/* Top Navbar */}
        <header className="sticky top-0 z-30 bg-white border-b border-gray-200 px-6 py-4 flex justify-between items-center shadow-xs">
          <div className="flex items-center gap-3">
            <button
              onClick={() => setMobileMenuOpen(true)}
              className="lg:hidden p-2 rounded-xl text-gray-700 hover:bg-gray-100 transition cursor-pointer"
              aria-label="Open sidebar menu"
            >
              <span className="text-lg">☰</span>
            </button>
            <h1 className="text-lg sm:text-xl font-bold text-gray-900">
              {getPageTitle()}
            </h1>
          </div>

          <div className="flex items-center gap-3">
            <Link
              href="/mock-interview"
              className="hidden sm:inline-flex items-center gap-1 text-xs font-semibold text-purple-700 bg-purple-50 hover:bg-purple-100 border border-purple-200 px-3 py-1.5 rounded-lg transition"
            >
              <span>🎯 Mock Interview</span>
            </Link>

            <Link
              href="/profile"
              className="flex items-center gap-2 text-xs font-semibold text-gray-800 hover:text-blue-600 transition"
            >
              <div className="w-7 h-7 rounded-lg bg-black text-white flex items-center justify-center text-xs font-bold">
                {avatarInitial}
              </div>
              <span className="hidden md:inline">{displayName}</span>
            </Link>
          </div>
        </header>

        {/* Page Content */}
        <main className="p-4 sm:p-6 lg:p-8 max-w-7xl w-full mx-auto flex-1">
          {children}
        </main>
      </div>
    </div>
  );
}