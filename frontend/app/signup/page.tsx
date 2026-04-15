"use client";

import { useState } from "react";

export default function Signup() {
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [message, setMessage] = useState("");

  const handleSignup = async () => {
    if (!name || !email || !password) {
      setMessage("Please fill all fields");
      return;
    }

    try {
      const res = await fetch("http://localhost:8080/auth/signup", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ name, email, password }),
      });

      const data = await res.json();
      setMessage("Signup Successful!");
    } catch (err) {
      setMessage("Server error");
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-gray-100">
      <div className="bg-white shadow-xl rounded-2xl p-8 w-96">

        <h1 className="text-2xl font-bold text-center mb-6">
          Signup
        </h1>

        <input
          type="text"
          placeholder="Name"
          className="w-full p-3 mb-4 border rounded-lg"
          onChange={(e) => setName(e.target.value)}
        />

        <input
          type="email"
          placeholder="Email"
          className="w-full p-3 mb-4 border rounded-lg"
          onChange={(e) => setEmail(e.target.value)}
        />

        <input
          type="password"
          placeholder="Password"
          className="w-full p-3 mb-4 border rounded-lg"
          onChange={(e) => setPassword(e.target.value)}
        />

        <button
          onClick={handleSignup}
          className="w-full bg-black text-white p-3 rounded-lg hover:bg-gray-800"
        >
          Signup
        </button>

        {message && (
          <p className="mt-4 text-center text-sm text-gray-600">
            {message}
          </p>
        )}

      </div>
    </div>
  );
}