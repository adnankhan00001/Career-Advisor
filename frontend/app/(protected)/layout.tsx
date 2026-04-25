"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
export default function ProtectedLayout({ children }) {

const router = useRouter();

const handleLogout = () => {
  localStorage.removeItem("user");
  localStorage.removeItem("skills");
  localStorage.removeItem("completedSteps");

  router.push("/login");
};

  return (
    <div className="flex min-h-screen bg-gray-100">

      {/* Sidebar */}
      <div className="w-64 bg-black text-white p-5 space-y-6">

        <h1 className="text-2xl font-bold">Career Advisor</h1>

        <nav className="flex flex-col space-y-4">
          <Link href="/dashboard" className="hover:text-gray-300">
            Dashboard
          </Link>
          <Link href="/skills" className="hover:text-gray-300">
            Skills
          </Link>
          <Link href="/careers" className="hover:text-gray-300">
            Careers
          </Link>
          <Link href="/roadmap" className="hover:text-gray-300">
            Roadmap
          </Link>
          <Link href="/profile" className="hover:text-gray-300">
            Profile
          </Link>
        </nav>

      </div>

      {/* Main Area */}
      <div className="flex-1 flex flex-col">

        {/* Top Navbar */}
        <div className="bg-white shadow px-6 py-4 flex justify-between items-center">
          <h2 className="text-lg font-semibold">Dashboard</h2>

          <button
            onClick={handleLogout}
            className="bg-black text-white px-4 py-1 rounded"
          >
            Logout
          </button>
        </div>

        {/* Page Content */}
        <div className="p-6">
          {children}
        </div>

      </div>

    </div>
  );
}