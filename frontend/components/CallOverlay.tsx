"use client";

import { useEffect, useRef, useState } from "react";
import { CallSession, CallType } from "@/lib/callApiService";

interface CallOverlayProps {
  session: CallSession | null;
  callMode: "INCOMING" | "OUTGOING" | "ACTIVE";
  localStream: MediaStream | null;
  remoteStream: MediaStream | null;
  isMuted: boolean;
  isVideoOff: boolean;
  errorMessage?: string | null;
  onAccept: () => void;
  onReject: () => void;
  onCancel: () => void;
  onEnd: () => void;
  onToggleMute: () => void;
  onToggleVideo: () => void;
}

export default function CallOverlay({
  session,
  callMode,
  localStream,
  remoteStream,
  isMuted,
  isVideoOff,
  errorMessage,
  onAccept,
  onReject,
  onCancel,
  onEnd,
  onToggleMute,
  onToggleVideo,
}: CallOverlayProps) {
  const localVideoRef = useRef<HTMLVideoElement>(null);
  const remoteVideoRef = useRef<HTMLVideoElement>(null);
  const [durationSeconds, setDurationSeconds] = useState(0);

  // Timer for active call
  useEffect(() => {
    if (callMode !== "ACTIVE") {
      setDurationSeconds(0);
      return;
    }

    const timer = setInterval(() => {
      setDurationSeconds((prev) => prev + 1);
    }, 1000);

    return () => clearInterval(timer);
  }, [callMode]);

  // Bind local stream
  useEffect(() => {
    if (localVideoRef.current && localStream) {
      localVideoRef.current.srcObject = localStream;
    }
  }, [localStream, callMode]);

  // Bind remote stream
  useEffect(() => {
    if (remoteVideoRef.current && remoteStream) {
      remoteVideoRef.current.srcObject = remoteStream;
    }
  }, [remoteStream, callMode]);

  if (!session) return null;

  const isVideo = session.callType === "VIDEO";
  const partnerName =
    callMode === "INCOMING" ? session.callerName : session.receiverName;

  const formatDuration = (totalSeconds: number) => {
    const mins = Math.floor(totalSeconds / 60);
    const secs = totalSeconds % 60;
    return `${mins.toString().padStart(2, "0")}:${secs
      .toString()
      .padStart(2, "0")}`;
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-md p-4 animate-in fade-in duration-200">
      <div className="relative w-full max-w-4xl bg-gray-900 border border-gray-800 rounded-3xl overflow-hidden shadow-2xl flex flex-col h-[85vh] max-h-[750px]">
        {/* Error Banner */}
        {errorMessage && (
          <div className="absolute top-4 left-4 right-4 z-20 bg-red-600/90 text-white text-xs font-semibold px-4 py-2.5 rounded-xl text-center shadow-lg backdrop-blur-xs">
            ⚠️ {errorMessage}
          </div>
        )}

        {/* Top Header */}
        <div className="absolute top-4 left-6 z-10 flex items-center gap-3">
          <div className="w-10 h-10 rounded-full bg-gray-800 border border-gray-700 text-white font-bold flex items-center justify-center text-sm">
            {partnerName.charAt(0)}
          </div>
          <div>
            <h3 className="text-white font-bold text-base leading-tight">
              {partnerName}
            </h3>
            <p className="text-gray-400 text-xs flex items-center gap-1.5 mt-0.5">
              <span>{isVideo ? "📹 Video Call" : "📞 Audio Call"}</span>
              {callMode === "ACTIVE" && (
                <>
                  <span>•</span>
                  <span className="text-emerald-400 font-mono font-semibold">
                    {formatDuration(durationSeconds)}
                  </span>
                </>
              )}
            </p>
          </div>
        </div>

        {/* Media / Main Area */}
        <div className="flex-1 relative flex items-center justify-center bg-gray-950 overflow-hidden">
          {callMode === "ACTIVE" && isVideo ? (
            <>
              {/* Remote Video (Full Container) */}
              <video
                ref={remoteVideoRef}
                autoPlay
                playsInline
                className="w-full h-full object-cover"
              />

              {/* Local Video Picture-in-Picture */}
              <div className="absolute bottom-20 right-6 w-36 sm:w-48 h-28 sm:h-36 bg-gray-900 border-2 border-gray-700 rounded-2xl overflow-hidden shadow-2xl z-10">
                {!isVideoOff && localStream ? (
                  <video
                    ref={localVideoRef}
                    autoPlay
                    playsInline
                    muted
                    className="w-full h-full object-cover mirror"
                  />
                ) : (
                  <div className="w-full h-full flex items-center justify-center text-gray-500 text-xs bg-gray-900 font-medium">
                    Camera Off
                  </div>
                )}
              </div>
            </>
          ) : (
            /* Audio Call / Calling / Incoming Visualizer */
            <div className="flex flex-col items-center justify-center gap-6 p-8 text-center">
              <div className="relative">
                <div
                  className={`w-28 h-28 sm:w-36 sm:h-36 rounded-full bg-gradient-to-tr from-emerald-600 to-teal-400 text-white font-black text-4xl sm:text-5xl flex items-center justify-center shadow-2xl border-4 border-gray-800 ${
                    callMode !== "ACTIVE" ? "animate-pulse" : ""
                  }`}
                >
                  {partnerName.charAt(0)}
                </div>
                {callMode === "ACTIVE" && (
                  <div className="absolute -bottom-2 right-2 w-6 h-6 rounded-full bg-emerald-500 border-2 border-gray-900 flex items-center justify-center text-[10px] text-white">
                    ✓
                  </div>
                )}
              </div>

              <div className="space-y-1">
                <h2 className="text-white text-xl font-bold">{partnerName}</h2>
                <p className="text-gray-400 text-sm font-medium">
                  {callMode === "INCOMING"
                    ? "Incoming call request..."
                    : callMode === "OUTGOING"
                    ? "Calling..."
                    : `In call (${formatDuration(durationSeconds)})`}
                </p>
              </div>

              {/* Audio Track Visualizer (Simulated Wave) */}
              {callMode === "ACTIVE" && (
                <div className="flex items-center gap-1.5 h-6">
                  <span className="w-1 bg-emerald-500 rounded-full h-3 animate-pulse"></span>
                  <span className="w-1 bg-emerald-400 rounded-full h-6 animate-pulse [animation-delay:0.2s]"></span>
                  <span className="w-1 bg-emerald-500 rounded-full h-4 animate-pulse [animation-delay:0.4s]"></span>
                  <span className="w-1 bg-emerald-400 rounded-full h-5 animate-pulse [animation-delay:0.1s]"></span>
                  <span className="w-1 bg-emerald-500 rounded-full h-3 animate-pulse [animation-delay:0.3s]"></span>
                </div>
              )}
            </div>
          )}
        </div>

        {/* Bottom Action Bar */}
        <div className="p-6 bg-gray-900 border-t border-gray-800 flex items-center justify-center gap-4 z-10">
          {callMode === "INCOMING" ? (
            /* Incoming Controls */
            <div className="flex items-center gap-6">
              <button
                onClick={onReject}
                className="px-6 py-3 rounded-full bg-red-600 hover:bg-red-700 text-white font-bold text-sm transition-all shadow-lg flex items-center gap-2"
              >
                <span>✕</span> Reject
              </button>
              <button
                onClick={onAccept}
                className="px-8 py-3.5 rounded-full bg-emerald-500 hover:bg-emerald-600 text-white font-bold text-sm transition-all shadow-xl flex items-center gap-2 animate-bounce"
              >
                <span>📞</span> Accept Call
              </button>
            </div>
          ) : callMode === "OUTGOING" ? (
            /* Outgoing Ringing Controls */
            <div>
              <button
                onClick={onCancel}
                className="px-8 py-3.5 rounded-full bg-red-600 hover:bg-red-700 text-white font-bold text-sm transition-all shadow-xl flex items-center gap-2"
              >
                <span>✕</span> Cancel Call
              </button>
            </div>
          ) : (
            /* Active Call Controls */
            <div className="flex items-center gap-3 sm:gap-4">
              <button
                onClick={onToggleMute}
                className={`p-3.5 rounded-full text-white text-sm font-semibold transition-all shadow-md flex items-center justify-center w-12 h-12 ${
                  isMuted
                    ? "bg-red-600 hover:bg-red-700"
                    : "bg-gray-800 hover:bg-gray-700 border border-gray-700"
                }`}
                title={isMuted ? "Unmute Microphone" : "Mute Microphone"}
              >
                {isMuted ? "🔇" : "🎙️"}
              </button>

              {isVideo && (
                <button
                  onClick={onToggleVideo}
                  className={`p-3.5 rounded-full text-white text-sm font-semibold transition-all shadow-md flex items-center justify-center w-12 h-12 ${
                    isVideoOff
                      ? "bg-red-600 hover:bg-red-700"
                      : "bg-gray-800 hover:bg-gray-700 border border-gray-700"
                  }`}
                  title={isVideoOff ? "Turn Video On" : "Turn Video Off"}
                >
                  {isVideoOff ? "🚫" : "📹"}
                </button>
              )}

              <button
                onClick={onEnd}
                className="px-6 py-3 rounded-full bg-red-600 hover:bg-red-700 text-white font-bold text-sm transition-all shadow-xl flex items-center gap-2"
                title="End Call"
              >
                <span>🔴</span> End Call
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
