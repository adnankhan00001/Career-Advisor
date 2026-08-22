"use client";

import { motion } from "framer-motion";
import Link from "next/link";
import Navbar from "@/components/Navbar";

export default function Home() {
  return (
    <div className="relative min-h-screen text-white bg-black overflow-hidden flex flex-col justify-between">
      {/* Background Image & Overlay */}
      <div className="absolute inset-0 z-0">
        <img
          src="/bg.webp"
          alt="Career Advisor Background"
          className="w-full h-full object-cover opacity-35 scale-105"
        />
        <div className="absolute inset-0 bg-gradient-to-b from-black/80 via-black/60 to-black"></div>
      </div>

      {/* Main Content */}
      <div className="relative z-10 flex flex-col min-h-screen justify-between">
        {/* Top Navbar */}
        <Navbar />

        {/* Hero Section */}
        <motion.div
          initial={{ opacity: 0, y: 30 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.8 }}
          className="flex-1 flex items-center justify-center text-center px-6 py-12"
        >
          <div className="max-w-3xl mx-auto">
            <span className="inline-block text-xs uppercase tracking-widest text-blue-400 font-bold bg-blue-950/60 border border-blue-800/60 px-3.5 py-1.5 rounded-full mb-6">
              AI-Powered Career & Interview Intelligence
            </span>

            <h1 className="text-4xl sm:text-6xl font-extrabold tracking-tight mb-6 leading-tight">
              Design Your Tech Career <br />
              <span className="bg-gradient-to-r from-blue-400 via-indigo-300 to-purple-400 bg-clip-text text-transparent">
                With Total Clarity 🚀
              </span>
            </h1>

            <p className="text-gray-300 text-base sm:text-lg mb-8 max-w-2xl mx-auto leading-relaxed">
              Discover industry-aligned career tracks, map your technical skills,
              execute code in our interactive browser IDE, and pass technical interviews
              with timed mock rounds and AI intelligence.
            </p>

            <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
              <Link href="/signup" className="w-full sm:w-auto">
                <motion.button
                  whileHover={{ scale: 1.05 }}
                  whileTap={{ scale: 0.98 }}
                  className="w-full sm:w-auto bg-white text-black px-8 py-3.5 rounded-xl font-bold text-sm sm:text-base shadow-xl hover:bg-gray-100 transition cursor-pointer"
                >
                  Start Assessment & Roadmap →
                </motion.button>
              </Link>

              <Link href="/login" className="w-full sm:w-auto">
                <motion.button
                  whileHover={{ scale: 1.05 }}
                  whileTap={{ scale: 0.98 }}
                  className="w-full sm:w-auto border border-white/40 bg-white/5 backdrop-blur-md px-8 py-3.5 rounded-xl font-semibold text-sm sm:text-base hover:bg-white/10 transition cursor-pointer"
                >
                  Sign In
                </motion.button>
              </Link>
            </div>
          </div>
        </motion.div>

        {/* Features Grid Section */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5 px-6 sm:px-12 pb-12 max-w-7xl mx-auto w-full">
          {[
            {
              icon: "🎯",
              title: "Career Track Discovery",
              desc: "Explore 7 in-demand career paths with skill breakdowns and compensation benchmarks.",
            },
            {
              icon: "🗺️",
              title: "Interactive Roadmaps",
              desc: "Step-by-step milestone checklists for frontend, backend, cloud, data, and AI/ML.",
            },
            {
              icon: "💻",
              title: "In-Browser Code IDE",
              desc: "Practice 22 curated DSA problems with line numbering, sample runner, and test suites.",
            },
            {
              icon: "⏱️",
              title: "Timed Mock Interviews",
              desc: "Simulate real tech interview rounds with backend-authoritative timers and explanations.",
            },
          ].map((item, index) => (
            <motion.div
              key={index}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.2 + index * 0.1 }}
              whileHover={{ scale: 1.02 }}
              className="bg-white/5 p-5 rounded-2xl backdrop-blur-md border border-white/10 hover:border-white/20 transition flex flex-col justify-between"
            >
              <div>
                <div className="text-2xl mb-2">{item.icon}</div>
                <h3 className="text-sm font-bold text-white mb-1">{item.title}</h3>
                <p className="text-gray-400 text-xs leading-relaxed">{item.desc}</p>
              </div>
            </motion.div>
          ))}
        </div>
      </div>
    </div>
  );
}