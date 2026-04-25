"use client";

import { useState } from "react";
import { useEffect } from "react";
import {useRouter} from "next/navigation";
export default function Skills() {
  const [skills, setSkills] = useState(["Java", "HTML"]);
  const [input, setInput] = useState("");
  const router = useRouter();
  useEffect(() => {
    const user = localStorage.getItem("user");

    if (!user) {
      router.replace("/login");
    }
  }, []);
  useEffect(() => {
    const savedSkills = JSON.parse(localStorage.getItem("skills") || "[]");
    setSkills(savedSkills);
  }, []);

  // Add skill
  const handleAdd = () => {
    if (!input.trim()) return;

    const updatedSkills = [...skills, input];
    setSkills(updatedSkills);
    localStorage.setItem("skills", JSON.stringify(updatedSkills));

    setInput("");
  };

  // Remove skill
  const handleRemove = (skillToRemove) => {
    const updatedSkills = skills.filter((s) => s !== skillToRemove);
    setSkills(updatedSkills);
    localStorage.setItem("skills", JSON.stringify(updatedSkills));
  };

  return (
    <div className="max-w-3xl mx-auto space-y-6">

      {/* Title */}
      <h1 className="text-2xl font-bold">Your Skills</h1>

      {/* Add Skill */}
      <div className="flex gap-3">
        <input
          type="text"
          placeholder="Enter a skill"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          className="flex-1 border p-3 rounded-lg"
        />

        <button
          onClick={handleAdd}
          className="bg-black text-white px-5 rounded-lg"
        >
          Add
        </button>
      </div>

      {/* Skills List */}
      <div className="bg-white p-5 rounded-xl shadow">

        {skills.length === 0 ? (
          <p className="text-gray-500">No skills added yet</p>
        ) : (
          <div className="flex flex-wrap gap-3">
            {skills.map((skill, index) => (
              <div
                key={index}
                className="flex items-center gap-2 bg-gray-200 px-3 py-1 rounded-full"
              >
                <span>{skill}</span>

                <button
                  onClick={() => handleRemove(skill)}
                  className="text-red-500 font-bold"
                >
                  ×
                </button>
              </div>
            ))}
          </div>
        )}

      </div>

    </div>
  );
}