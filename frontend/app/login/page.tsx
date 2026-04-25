"use client";

import { useState } from "react";
import {useRouter} from "next/navigation";
export default function Login() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [message, setMessage] = useState("");
const router = useRouter();
  const handleLogin = async (e) => {
      e.preventDefault();
    if (!email || !password) {
      setMessage("Please fill all fields");
      return;
    }

    try {
      const res = await fetch("http://localhost:8080/auth/login", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ email, password }),
      });

      const data = await res.text();
      if(res.ok && data== "Login Successful"){
          localStorage.setItem("user", JSON.stringify({
              email: email
              }));
          router.push("/dashboard");
      }else{
          setMessage(data || "Login failed");
      }

    } catch (error) {
      setMessage("Server error");
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-[url('/bg.webp')] bg-cover bg-center">
      <div className="bg-white/90 backdrop-blur-md shadow-xl rounded-2xl p-8 w-96">

        <h1 className="text-2xl font-bold text-center mb-6">
          Login
        </h1>

        <input
          type="email"
          placeholder="Email"
          className="w-full p-3 mb-4 border rounded-lg focus:outline-none focus:ring-2 focus:ring-black"
          onChange={(e) => setEmail(e.target.value)}
        />

        <input
          type="password"
          placeholder="Password"
          className="w-full p-3 mb-4 border rounded-lg focus:outline-none focus:ring-2 focus:ring-black"
          onChange={(e) => setPassword(e.target.value)}
        />

        <button
          onClick={handleLogin}
          className="w-full bg-black text-white p-3 rounded-lg hover:bg-gray-800 transition"
        >
          Login
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