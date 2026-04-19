"use client";

import { useState } from "react";

export default function SignupPage() {
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [message, setMessage] = useState("");

  const handleSignup = async () => {
    try {
      const res = await fetch("http://localhost:8080/auth/signup", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ name, email, password }),
      });

      const data = await res.text();
      setMessage(data);
    } catch {
      setMessage("Server error");
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center">
      <div className="bg-white p-8 shadow rounded w-96">
        <h1 className="text-xl font-bold mb-4 text-center">Signup</h1>

        <input
          placeholder="Name"
          onChange={(e) => setName(e.target.value)}
          className="border p-2 mb-2 w-full"
        />

        <input
          placeholder="Email"
          onChange={(e) => setEmail(e.target.value)}
          className="border p-2 mb-2 w-full"
        />

        <input
          type="password"
          placeholder="Password"
          onChange={(e) => setPassword(e.target.value)}
          className="border p-2 mb-2 w-full"
        />

        <button
          onClick={handleSignup}
          className="bg-black text-white px-4 py-2 w-full"
        >
          Signup
        </button>

        <p className="mt-2 text-center">{message}</p>
      </div>
    </div>
  );
}