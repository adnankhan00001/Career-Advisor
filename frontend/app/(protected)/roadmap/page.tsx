"use client";

import { useEffect, useState } from "react";
import { getCareerSuggestions } from "@/lib/careerLogic";
import { roadmapData } from "@/lib/roadmapData";
import { useRouter } from "next/navigation";

export default function Roadmap() {
  const [skills, setSkills] = useState<string[]>([]);
  const [selectedCareer, setSelectedCareer] = useState<string | null>(null);
  const [completed, setCompleted] = useState<string[]>([]);
  const router = useRouter();
  useEffect(() => {
      const user = localStorage.getItem("user");
      if(!user){
          router.replace("/login");
          }
      },[]);
  useEffect(() => {
    const savedSkills = JSON.parse(localStorage.getItem("skills") || "[]");
    const savedCompleted = JSON.parse(localStorage.getItem("completedSteps") || "[]");

    setSkills(savedSkills);
    setCompleted(savedCompleted);
  }, []);

  const suggestions = getCareerSuggestions(skills);

  const toggleStep = (step: string) => {
    let updated;

    if (completed.includes(step)) {
      updated = completed.filter((s) => s !== step);
    } else {
      updated = [...completed, step];
    }

    setCompleted(updated);
    localStorage.setItem("completedSteps", JSON.stringify(updated));
  };

  return (
    <div className="space-y-6">

      {/* Title */}
      <h1 className="text-2xl font-bold">Career Roadmap</h1>

      {/* Career Selection */}
      <div className="bg-white p-5 rounded-xl shadow">
        <h2 className="text-lg font-semibold mb-4">Choose a Career</h2>

        {suggestions.length === 0 ? (
          <p className="text-gray-500">Add skills to get suggestions</p>
        ) : (
          <div className="flex flex-wrap gap-3">
            {suggestions.map((item, index) => (
              <button
                key={index}
                onClick={() => setSelectedCareer(item.title)}
                className={`px-4 py-2 rounded-lg border transition ${
                  selectedCareer === item.title
                    ? "bg-black text-white"
                    : "bg-white hover:bg-gray-100"
                }`}
              >
                {item.title}
              </button>
            ))}
          </div>
        )}
      </div>

      {/* Roadmap */}
      {selectedCareer && (
        <div className="bg-white p-6 rounded-xl shadow">

          <h2 className="text-xl font-semibold mb-6">
            {selectedCareer} Roadmap
          </h2>

          {roadmapData[selectedCareer]?.map((section, index) => (
            <div key={index} className="mb-8">

              {/* Section Title */}
              <h3 className="text-lg font-semibold mb-4 text-blue-600">
                {section.title}
              </h3>

              {/* Timeline */}
              <div className="border-l-2 border-gray-300 ml-3 space-y-4">
                {section.steps.map((step, i) => {
                  const isDone = completed.includes(step);

                  return (
                    <div
                      key={i}
                      className="flex items-start gap-4 relative"
                    >
                      {/* Circle */}
                      <div
                        className={`w-4 h-4 rounded-full mt-2 ${
                          isDone ? "bg-green-500" : "bg-gray-400"
                        }`}
                      ></div>

                      {/* Step Card */}
                      <div
                        onClick={() => toggleStep(step)}
                        className={`p-3 border rounded-lg w-full cursor-pointer transition ${
                          isDone
                            ? "bg-green-50 border-green-400"
                            : "hover:bg-gray-50"
                        }`}
                      >
                        <span className={`${isDone ? "line-through text-gray-500" : ""}`}>
                          {step}
                        </span>
                      </div>
                    </div>
                  );
                })}
              </div>

            </div>
          ))}

        </div>
      )}

    </div>
  );
}