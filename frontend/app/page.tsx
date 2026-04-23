"use client";

import { motion } from "framer-motion";
import Link from "next/link";

export default function Home() {
  return (
    <div className="relative min-h-screen text-white">

      {/* Background */}
      <div className="absolute inset-0">
        <img
          src="/bg.webp"
          alt="background"
          className="w-full h-full object-cover scale-105"
        />
        <div className="absolute inset-0 bg-black/60"></div>
      </div>

      {/* Animated Wrapper */}
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ duration: 1 }}
        className="relative z-10 flex flex-col min-h-screen"
      >

        {/* Navbar */}


        {/* Hero Section */}
        <motion.div
          initial={{ opacity: 0, y: 40 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.8 }}
          className="flex flex-1 items-center justify-center text-center px-6"
        >
          <div>
            <h2 className="text-5xl font-bold mb-6 leading-tight">
              Design Your Career <br /> With Confidence 🚀
            </h2>

            <p className="text-gray-300 mb-8 max-w-xl mx-auto">
              A smarter way to discover your future. Get guided paths, skill insights,
              and real direction tailored just for you.
            </p>

            <div className="space-x-4">
              <Link href="/signup">
                <motion.button
                  whileHover={{ scale: 1.1 }}
                  className="bg-white text-black px-6 py-3 rounded-lg font-semibold shadow-lg hover:shadow-white/30 transition"
                >
                  Get Started
                </motion.button>
              </Link>

              <Link href="/login">
                <motion.button
                  whileHover={{ scale: 1.1 }}
                  className="border border-white px-6 py-3 rounded-lg hover:bg-white hover:text-black transition"
                >
                  Login
                </motion.button>
              </Link>
            </div>
          </div>
        </motion.div>

        {/* Features Section */}
        <div className="grid md:grid-cols-3 gap-6 px-10 pb-12">

          {[
            {
              title: "Smart Suggestions",
              desc: "AI-driven recommendations based on your profile.",
            },
            {
              title: "Skill Mapping",
              desc: "Know exactly what to learn next to grow faster.",
            },
            {
              title: "Future Ready",
              desc: "Stay aligned with industry trends and opportunities.",
            },
          ].map((item, index) => (
            <motion.div
              key={index}
              initial={{ opacity: 0, y: 30 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.2 + index * 0.2 }}
              whileHover={{ scale: 1.05 }}
              className="bg-white/10 p-6 rounded-xl backdrop-blur-lg border border-white/20"
            >
              <h3 className="text-xl font-semibold mb-2">{item.title}</h3>
              <p className="text-gray-300">{item.desc}</p>
            </motion.div>
          ))}

        </div>

      </motion.div>
    </div>
  );
}