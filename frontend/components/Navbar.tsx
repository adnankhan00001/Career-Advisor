"use client";

import Link from "next/link";

export default function Navbar() {
  return (
    <nav className="bg-black text-white px-6 py-4 flex justify-between items-center">

      {/* Logo */}
      <h1 className="text-xl font-bold">Career Advisor</h1>

      {/* Links */}
      <div className="space-x-4">
        <Link href="/login" className="hover:underline">
          Login
        </Link>
        <Link href="/signup" className="hover:underline">
          Signup
        </Link>
      </div>

    </nav>
  );
}