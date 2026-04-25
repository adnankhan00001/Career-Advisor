"use client";

import { useEffect, useState } from "react";
import { getCareerSuggestions } from "@/lib/careerLogic";
import { useRouter } from "next/navigation";
const mockUser = {
  name: "Adnan",
};

export default function Dashboard() {
    const router = useRouter();
    useEffect(() => {
        const user = localStorage.getItem("user");
        if(!user){
            router.replace("/login");
            }
        }, []);
  const [skills, setSkills] = useState<string[]>([]);

  // Load skills from localStorage
  useEffect(() => {
    const savedSkills = JSON.parse(localStorage.getItem("skills") || "[]");
    setSkills(savedSkills);
  }, []);

  // Generate suggestions dynamically
  const suggestions = getCareerSuggestions(skills);

  return (
    <div className="space-y-6">

      {/* Welcome Card */}
      <div className="bg-gradient-to-r from-blue-600 to-indigo-600 text-white p-6 rounded-xl shadow">
        <h1 className="text-2xl font-bold">
          Hello {mockUser.name} 👋
        </h1>
        <p className="mt-2">
          Let’s plan your career path smartly.
        </p>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-3 gap-4">
        <div className="bg-white p-4 rounded-lg shadow text-center">
          <h2 className="text-xl font-bold">{skills.length}</h2>
          <p className="text-gray-500">Skills</p>
        </div>

        <div className="bg-white p-4 rounded-lg shadow text-center">
          <h2 className="text-xl font-bold">{suggestions.length}</h2>
          <p className="text-gray-500">Suggestions</p>
        </div>

        <div className="bg-white p-4 rounded-lg shadow text-center">
          <h2 className="text-xl font-bold">
            {skills.length > 0 ? suggestions.length : 0}
          </h2>
          <p className="text-gray-500">Paths</p>
        </div>
      </div>

      {/* Main Section */}
      <div className="grid grid-cols-2 gap-6">

        {/* Career Suggestions */}
        <div className="bg-white p-5 rounded-xl shadow">
          <h2 className="text-lg font-semibold mb-4">
            Career Suggestions
          </h2>

          {skills.length === 0 ? (
            <p className="text-gray-500">
              Add skills to get career suggestions
            </p>
          ) : (
            <div className="space-y-3">
              {suggestions.map((item, index) => (
                <div
                  key={index}
                  className="flex justify-between items-center p-3 border rounded-lg"
                >
                  <span>{item.title}</span>
                  <span className="text-blue-600 font-semibold">
                    {item.match}
                  </span>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Skills Section */}
        <div className="bg-white p-5 rounded-xl shadow">
          <h2 className="text-lg font-semibold mb-4">
            Your Skills
          </h2>

          {skills.length === 0 ? (
            <p className="text-gray-500">No skills added yet</p>
          ) : (
            <div className="flex flex-wrap gap-2">
              {skills.map((skill, index) => (
                <span
                  key={index}
                  className="bg-gray-200 px-3 py-1 rounded-full text-sm"
                >
                  {skill}
                </span>
              ))}
            </div>
          )}

          <button
            onClick={() => window.location.href = "/skills"}
            className="mt-4 w-full bg-black text-white py-2 rounded-lg"
          >
            Add Skill
          </button>
        </div>

      </div>

    </div>
  );
}