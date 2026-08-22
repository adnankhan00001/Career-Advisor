"use client";

import { useEffect, useState, use } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import {
  problemService,
  CodingProblem,
  CodeSubmissionResult,
} from "@/lib/problemService";

interface WorkspaceProps {
  params: Promise<{ id: string }>;
}

export default function ProblemWorkspace({ params }: WorkspaceProps) {
  const resolvedParams = use(params);
  const problemIdOrSlug = resolvedParams.id;
  const router = useRouter();

  const [problem, setProblem] = useState<CodingProblem | null>(null);
  const [loading, setLoading] = useState(true);
  const [language, setLanguage] = useState("java");
  const [code, setCode] = useState("");
  const [customInput, setCustomInput] = useState("");
  const [activeLeftTab, setActiveLeftTab] = useState<"desc" | "hints">("desc");
  const [activeBottomTab, setActiveBottomTab] = useState<"testcases" | "output">("testcases");
  const [executing, setExecuting] = useState(false);
  const [executionResult, setExecutionResult] = useState<CodeSubmissionResult | null>(null);
  const [solvedState, setSolvedState] = useState(false);

  useEffect(() => {
    async function loadProblem() {
      setLoading(true);
      try {
        const data = await problemService.getProblem(problemIdOrSlug);
        if (data) {
          setProblem(data);
          setSolvedState(data.solved);
          setCode(data.starterCode || getDefaultStarter(language, data.slug));
          setCustomInput(data.sampleInput || "");
        }
      } catch {
        // Safe fallback
      } finally {
        setLoading(false);
      }
    }

    loadProblem();
  }, [problemIdOrSlug, language]);

  function getDefaultStarter(lang: string, slug: string) {
    if (lang === "python") {
      return `# Python 3 Solution\nclass Solution:\n    def solve(self):\n        pass\n`;
    }
    if (lang === "javascript") {
      return `// JavaScript Solution\nfunction solve() {\n    // Write your solution here\n}\n`;
    }
    return `class Solution {\n    public void solve() {\n        // Write your Java solution here\n    }\n}`;
  }

  const handleResetCode = () => {
    if (window.confirm("Reset editor to initial starter code?")) {
      setCode(problem?.starterCode || getDefaultStarter(language, problem?.slug || ""));
      setExecutionResult(null);
    }
  };

  const handleRunCode = async () => {
    if (!problem) return;
    setExecuting(true);
    setActiveBottomTab("output");
    try {
      const res = await problemService.runCode(problem.id, {
        language,
        code,
        customInput,
      });
      setExecutionResult(res);
    } catch {
      setExecutionResult({
        status: "NOT_EXECUTED",
        message: "Failed to evaluate code.",
      });
    } finally {
      setExecuting(false);
    }
  };

  const handleSubmitCode = async () => {
    if (!problem) return;
    setExecuting(true);
    setActiveBottomTab("output");
    try {
      const res = await problemService.submitCode(problem.id, {
        language,
        code,
      });
      setExecutionResult(res);
      if (res?.status === "ACCEPTED") {
        setSolvedState(true);
      }
    } catch {
      setExecutionResult({
        status: "NOT_EXECUTED",
        message: "Submission failed.",
      });
    } finally {
      setExecuting(false);
    }
  };

  const handleToggleSolved = async () => {
    if (!problem) return;
    const res = await problemService.toggleProblemSolved(problem.id);
    if (res) {
      setSolvedState(res.solved);
    }
  };

  if (loading) {
    return (
      <div className="max-w-7xl mx-auto p-8 animate-pulse space-y-6">
        <div className="h-8 bg-gray-200 rounded-md w-1/4"></div>
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <div className="h-[600px] bg-gray-100 rounded-2xl"></div>
          <div className="h-[600px] bg-gray-100 rounded-2xl"></div>
        </div>
      </div>
    );
  }

  if (!problem) {
    return (
      <div className="max-w-4xl mx-auto p-12 text-center bg-white rounded-2xl border border-gray-200 my-12">
        <h2 className="text-xl font-bold text-gray-900 mb-2">Problem Not Found</h2>
        <p className="text-sm text-gray-500 mb-6">
          The requested coding challenge could not be loaded.
        </p>
        <Link
          href="/practice"
          className="px-4 py-2 bg-black text-white rounded-xl text-sm font-medium"
        >
          ← Back to Practice Hub
        </Link>
      </div>
    );
  }

  const difficultyColors = {
    EASY: "bg-emerald-50 text-emerald-700 border-emerald-200",
    MEDIUM: "bg-amber-50 text-amber-700 border-amber-200",
    HARD: "bg-rose-50 text-rose-700 border-rose-200",
  };

  return (
    <div className="max-w-7xl mx-auto space-y-4 pb-12">
      {/* Top Navigation & Action Header */}
      <div className="bg-white rounded-2xl border border-gray-200 p-4 sm:px-6 flex flex-col md:flex-row items-start md:items-center justify-between gap-4 shadow-xs">
        <div className="flex items-center gap-3 flex-wrap">
          <Link
            href="/practice"
            className="text-xs font-semibold text-gray-600 hover:text-black flex items-center gap-1 bg-gray-100 hover:bg-gray-200 px-3 py-1.5 rounded-lg transition"
          >
            ← Practice Hub
          </Link>
          <span className="text-gray-300">|</span>
          <h1 className="text-lg font-extrabold text-gray-900">{problem.title}</h1>
          <span
            className={`text-xs px-2.5 py-0.5 rounded-full font-bold border ${
              difficultyColors[problem.difficulty]
            }`}
          >
            {problem.difficulty}
          </span>
          <span className="text-xs text-gray-500 bg-gray-50 px-2 py-0.5 rounded-md border border-gray-200">
            {problem.topic}
          </span>
        </div>

        <div className="flex items-center gap-2.5 w-full md:w-auto justify-end">
          <button
            onClick={handleToggleSolved}
            className={`text-xs font-semibold px-3 py-2 rounded-xl border transition flex items-center gap-1.5 cursor-pointer ${
              solvedState
                ? "bg-emerald-50 text-emerald-700 border-emerald-300 hover:bg-emerald-100"
                : "bg-gray-50 text-gray-600 border-gray-200 hover:bg-gray-100"
            }`}
          >
            {solvedState ? "✓ Solved" : "○ Mark as Solved"}
          </button>

          <button
            onClick={handleResetCode}
            title="Reset code template"
            className="px-3 py-2 text-xs font-semibold bg-gray-100 hover:bg-gray-200 text-gray-700 rounded-xl transition cursor-pointer"
          >
            ↺ Reset
          </button>

          <button
            onClick={handleRunCode}
            disabled={executing}
            className="px-4 py-2 text-xs font-bold bg-gray-900 hover:bg-gray-800 text-white rounded-xl transition shadow-xs flex items-center gap-1 cursor-pointer disabled:opacity-50"
          >
            {executing ? "Running..." : "▷ Run Sample"}
          </button>

          <button
            onClick={handleSubmitCode}
            disabled={executing}
            className="px-5 py-2 text-xs font-bold bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl transition shadow-sm flex items-center gap-1 cursor-pointer disabled:opacity-50"
          >
            {executing ? "Evaluating..." : "Submit 🚀"}
          </button>
        </div>
      </div>

      {/* Main Split Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-5 items-start">
        {/* Left Column: Problem Details (5 Cols) */}
        <div className="lg:col-span-5 bg-white rounded-2xl border border-gray-200 shadow-sm flex flex-col h-[750px] overflow-hidden">
          {/* Tab Header */}
          <div className="flex border-b border-gray-200 bg-gray-50/70 px-4 pt-3 gap-2">
            <button
              onClick={() => setActiveLeftTab("desc")}
              className={`text-xs font-bold pb-2.5 px-3 border-b-2 transition cursor-pointer ${
                activeLeftTab === "desc"
                  ? "border-black text-black"
                  : "border-transparent text-gray-500 hover:text-black"
              }`}
            >
              Description 📄
            </button>
            <button
              onClick={() => setActiveLeftTab("hints")}
              className={`text-xs font-bold pb-2.5 px-3 border-b-2 transition cursor-pointer ${
                activeLeftTab === "hints"
                  ? "border-black text-black"
                  : "border-transparent text-gray-500 hover:text-black"
              }`}
            >
              Explanation & Hints 💡
            </button>
          </div>

          {/* Tab Content */}
          <div className="p-6 overflow-y-auto space-y-6 flex-1 text-sm text-gray-800 leading-relaxed">
            {activeLeftTab === "desc" ? (
              <>
                {/* Description */}
                <div>
                  <h3 className="text-xs font-bold uppercase tracking-wider text-gray-400 mb-2">
                    Problem Statement
                  </h3>
                  <p className="text-gray-800 text-sm whitespace-pre-line leading-relaxed">
                    {problem.description}
                  </p>
                </div>

                {/* Sample Test Case */}
                <div className="space-y-3">
                  <h3 className="text-xs font-bold uppercase tracking-wider text-gray-400">
                    Example 1
                  </h3>
                  <div className="bg-gray-50 p-4 rounded-xl border border-gray-200 font-mono text-xs space-y-2">
                    <div>
                      <span className="text-gray-500 font-bold block">Input:</span>
                      <span className="text-gray-900 font-medium">
                        {problem.sampleInput}
                      </span>
                    </div>
                    <div>
                      <span className="text-gray-500 font-bold block">Output:</span>
                      <span className="text-emerald-700 font-bold">
                        {problem.sampleOutput}
                      </span>
                    </div>
                    {problem.explanation && (
                      <div className="pt-2 border-t border-gray-200 text-gray-600 font-sans text-[11px]">
                        <span className="font-bold text-gray-700">Explanation: </span>
                        {problem.explanation}
                      </div>
                    )}
                  </div>
                </div>

                {/* Constraints */}
                {problem.constraints && (
                  <div>
                    <h3 className="text-xs font-bold uppercase tracking-wider text-gray-400 mb-2">
                      Constraints
                    </h3>
                    <div className="bg-gray-50 p-3.5 rounded-xl border border-gray-200 font-mono text-xs text-gray-700 whitespace-pre-line">
                      {problem.constraints}
                    </div>
                  </div>
                )}

                {/* Tags & External Link */}
                <div className="pt-4 border-t border-gray-100 flex flex-col gap-3">
                  <div>
                    <h3 className="text-[11px] font-bold uppercase tracking-wider text-gray-400 mb-2">
                      Related Topics
                    </h3>
                    <div className="flex flex-wrap gap-1.5">
                      {problem.tags.map((t, idx) => (
                        <span
                          key={idx}
                          className="bg-gray-100 text-gray-700 text-xs px-2.5 py-1 rounded-md font-medium"
                        >
                          {t}
                        </span>
                      ))}
                    </div>
                  </div>

                  {problem.externalUrl && (
                    <a
                      href={problem.externalUrl}
                      target="_blank"
                      rel="noreferrer"
                      className="text-xs text-blue-600 font-semibold hover:underline inline-flex items-center gap-1"
                    >
                      View on Official Resource ↗
                    </a>
                  )}
                </div>
              </>
            ) : (
              <div className="space-y-4">
                <div className="bg-blue-50/70 p-4 rounded-xl border border-blue-200">
                  <h4 className="text-xs font-bold text-blue-900 uppercase mb-1">
                    Algorithm & Complexity Hints
                  </h4>
                  <p className="text-xs text-blue-800 leading-relaxed">
                    {problem.explanation ||
                      "Analyze time and space complexity trade-offs before coding."}
                  </p>
                </div>

                <div className="p-4 bg-gray-50 rounded-xl border border-gray-200 text-xs space-y-2">
                  <span className="font-bold text-gray-800 block">
                    Recommended Technical Approach:
                  </span>
                  <ul className="list-disc pl-4 text-gray-600 space-y-1">
                    <li>Optimal Time Complexity: O(N) or O(N log N)</li>
                    <li>Space Complexity: O(1) auxiliary where possible</li>
                    <li>Verify edge cases: null inputs, single-element collections, bounds.</li>
                  </ul>
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Right Column: Code Editor & Execution Terminal (7 Cols) */}
        <div className="lg:col-span-7 flex flex-col h-[750px] gap-4">
          {/* Top: Code Editor */}
          <div className="bg-gray-950 rounded-2xl border border-gray-800 shadow-md flex flex-col flex-1 overflow-hidden">
            {/* Editor Control Bar */}
            <div className="bg-gray-900 border-b border-gray-800 px-4 py-2.5 flex items-center justify-between text-xs text-gray-400">
              <div className="flex items-center gap-3">
                <span className="font-bold text-gray-200">Language:</span>
                <select
                  value={language}
                  onChange={(e) => setLanguage(e.target.value)}
                  className="bg-gray-800 text-white font-medium px-3 py-1 rounded-lg border border-gray-700 outline-none focus:ring-1 focus:ring-blue-500"
                >
                  <option value="java">Java 17 (Primary)</option>
                  <option value="python">Python 3</option>
                  <option value="javascript">JavaScript (Node.js)</option>
                </select>
              </div>

              <span className="text-[11px] text-gray-500 font-mono">
                Tab: 4 spaces · UTF-8
              </span>
            </div>

            {/* Code Input Area with line numbers */}
            <div className="flex-1 flex overflow-hidden font-mono text-xs bg-gray-950">
              {/* Line numbers column */}
              <div className="bg-gray-900/60 text-gray-600 p-4 select-none text-right font-mono pr-3 border-r border-gray-800/80 leading-5">
                {code.split("\n").map((_, i) => (
                  <div key={i}>{i + 1}</div>
                ))}
              </div>

              {/* Textarea code input */}
              <textarea
                value={code}
                onChange={(e) => setCode(e.target.value)}
                spellCheck={false}
                className="flex-1 bg-transparent text-gray-100 p-4 outline-none resize-none font-mono leading-5 overflow-auto selection:bg-blue-900"
                placeholder="// Write your code here..."
              />
            </div>
          </div>

          {/* Bottom: Test Cases & Execution Output Terminal */}
          <div className="bg-white rounded-2xl border border-gray-200 shadow-sm flex flex-col h-[260px] overflow-hidden">
            {/* Bottom Tabs */}
            <div className="flex border-b border-gray-200 bg-gray-50/80 px-4 pt-2.5 gap-2">
              <button
                onClick={() => setActiveBottomTab("testcases")}
                className={`text-xs font-bold pb-2 px-3 border-b-2 transition cursor-pointer ${
                  activeBottomTab === "testcases"
                    ? "border-black text-black"
                    : "border-transparent text-gray-500 hover:text-black"
                }`}
              >
                Test Cases 🧪
              </button>
              <button
                onClick={() => setActiveBottomTab("output")}
                className={`text-xs font-bold pb-2 px-3 border-b-2 transition cursor-pointer flex items-center gap-1.5 ${
                  activeBottomTab === "output"
                    ? "border-black text-black"
                    : "border-transparent text-gray-500 hover:text-black"
                }`}
              >
                Execution Result 💻
                {executionResult && (
                  <span
                    className={`w-2 h-2 rounded-full ${
                      executionResult.status === "ACCEPTED"
                        ? "bg-emerald-500"
                        : "bg-rose-500"
                    }`}
                  />
                )}
              </button>
            </div>

            {/* Bottom Tab Content */}
            <div className="p-4 overflow-y-auto flex-1 font-mono text-xs">
              {activeBottomTab === "testcases" ? (
                <div className="space-y-3">
                  <div className="flex items-center justify-between text-gray-600 font-sans text-xs">
                    <span className="font-bold">Sample Input (Customizable):</span>
                    <button
                      onClick={() => setCustomInput(problem.sampleInput || "")}
                      className="text-[11px] text-blue-600 hover:underline cursor-pointer font-medium"
                    >
                      Reset to Default Input
                    </button>
                  </div>
                  <textarea
                    value={customInput}
                    onChange={(e) => setCustomInput(e.target.value)}
                    rows={2}
                    className="w-full bg-gray-50 border border-gray-200 p-2.5 rounded-xl text-xs font-mono focus:bg-white focus:ring-1 focus:ring-black outline-none resize-none"
                  />
                  <div className="text-[11px] text-gray-500 font-sans flex items-center justify-between">
                    <span>Expected Output: <span className="font-bold text-gray-900 font-mono">{problem.sampleOutput}</span></span>
                    <span className="text-gray-400">Sandbox: Safe Execution Mode</span>
                  </div>
                </div>
              ) : (
                <div>
                  {executing ? (
                    <div className="flex items-center justify-center h-full py-8 text-gray-500 font-sans text-xs gap-2">
                      <span className="w-2 h-2 rounded-full bg-blue-600 animate-ping"></span>
                      Evaluating code in safe sandbox environment...
                    </div>
                  ) : executionResult ? (
                    <div className="space-y-3 font-sans">
                      <div className="flex items-center justify-between flex-wrap gap-2">
                        <span
                          className={`text-xs font-bold px-3 py-1 rounded-lg border ${
                            executionResult.status === "ACCEPTED"
                              ? "bg-emerald-50 text-emerald-700 border-emerald-200"
                              : "bg-rose-50 text-rose-700 border-rose-200"
                          }`}
                        >
                          Status: {executionResult.status}
                        </span>

                        <div className="flex items-center gap-3 text-xs text-gray-500 font-mono">
                          {executionResult.executionTimeMs !== undefined && (
                            <span>⏱️ {executionResult.executionTimeMs}ms</span>
                          )}
                          {executionResult.memoryKb !== undefined && (
                            <span>💾 {(executionResult.memoryKb / 1024).toFixed(1)}MB</span>
                          )}
                          {executionResult.testCasesPassed !== undefined && (
                            <span>
                              Passed: {executionResult.testCasesPassed}/
                              {executionResult.totalTestCases}
                            </span>
                          )}
                        </div>
                      </div>

                      <div className="bg-gray-50 p-3 rounded-xl border border-gray-200 space-y-1.5 font-mono text-xs">
                        <span className="text-[11px] text-gray-400 font-bold block uppercase">
                          Output / Message:
                        </span>
                        <div className="text-gray-900 whitespace-pre-wrap">
                          {executionResult.output || executionResult.message}
                        </div>
                      </div>

                      {executionResult.sandboxInfo && (
                        <div className="text-[11px] text-gray-400">
                          🛡️ {executionResult.sandboxInfo}
                        </div>
                      )}
                    </div>
                  ) : (
                    <div className="text-center py-8 text-gray-400 font-sans text-xs">
                      Click <strong>"Run Sample"</strong> or <strong>"Submit"</strong> to execute your solution against test cases.
                    </div>
                  )}
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
