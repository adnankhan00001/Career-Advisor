"use client";

import { useState, useEffect, useRef, useCallback } from "react";
import { useAuth } from "@/context/AuthContext";
import { humanChatService, HumanConversation, RealTimeEvent } from "@/lib/humanChatService";
import { ChatMessage } from "@/lib/chatService";
import { wsService } from "@/lib/websocketService";
import {
  callApiService,
  CallSession,
  CallType,
  WebRtcSignal,
} from "@/lib/callApiService";
import { webrtcManager } from "@/lib/webrtcService";
import CallOverlay from "@/components/CallOverlay";

export default function AdminMessagesPage() {
  const { user } = useAuth();

  const [inbox, setInbox] = useState<HumanConversation[]>([]);
  const [activeConvId, setActiveConvId] = useState<number | null>(null);
  const [activeConv, setActiveConv] = useState<HumanConversation | null>(null);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [loading, setLoading] = useState(true);
  const [messagesLoading, setMessagesLoading] = useState(false);
  const [sending, setSending] = useState(false);
  const [inputMessage, setInputMessage] = useState("");

  // Search & Filtering State
  const [searchInput, setSearchInput] = useState("");
  const [appliedSearch, setAppliedSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState<"ALL" | "OPEN" | "RESOLVED">("ALL");

  // Feedback & Action State
  const [feedback, setFeedback] = useState<{ type: "success" | "error"; message: string } | null>(null);
  const [isUpdatingStatus, setIsUpdatingStatus] = useState(false);
  const [showResolveModal, setShowResolveModal] = useState(false);
  const [resolutionNote, setResolutionNote] = useState("");

  // WebRTC Call State
  const [activeCallSession, setActiveCallSession] = useState<CallSession | null>(null);
  const [callMode, setCallMode] = useState<"INCOMING" | "OUTGOING" | "ACTIVE" | null>(null);
  const [localStream, setLocalStream] = useState<MediaStream | null>(null);
  const [remoteStream, setRemoteStream] = useState<MediaStream | null>(null);
  const [isMuted, setIsMuted] = useState(false);
  const [isVideoOff, setIsVideoOff] = useState(false);
  const [callErrorMessage, setCallErrorMessage] = useState<string | null>(null);

  const messagesEndRef = useRef<HTMLDivElement>(null);
  const activeCallRef = useRef<CallSession | null>(null);
  activeCallRef.current = activeCallSession;

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  const loadInbox = useCallback(async () => {
    try {
      setLoading(true);
      const data = await humanChatService.getAdminInbox(appliedSearch, statusFilter);
      setInbox(data);
      if (data.length > 0) {
        // If current activeConvId is not in the filtered list, select the first one
        if (!activeConvId || !data.some((c) => c.id === activeConvId)) {
          setActiveConvId(data[0].id);
        }
      } else {
        setActiveConvId(null);
        setActiveConv(null);
      }
    } catch (err: any) {
      console.error("Failed to load admin inbox:", err);
      setFeedback({ type: "error", message: err.message || "Failed to load support inquiries." });
    } finally {
      setLoading(false);
    }
  }, [appliedSearch, statusFilter, activeConvId]);

  useEffect(() => {
    if (user?.role === "ADMIN") {
      loadInbox();
    }
  }, [user, loadInbox]);

  // Connect to STOMP WebSocket
  useEffect(() => {
    wsService.connect();
  }, []);

  // Escape key handler for dialogs
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        setShowResolveModal(false);
      }
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, []);

  const cleanupCallUI = useCallback(() => {
    webrtcManager.closeAll();
    setActiveCallSession(null);
    setCallMode(null);
    setLocalStream(null);
    setRemoteStream(null);
    setIsMuted(false);
    setIsVideoOff(false);
    setCallErrorMessage(null);
  }, []);

  // Setup WebRTC callbacks
  useEffect(() => {
    webrtcManager.setCallbacks({
      onRemoteStream: (stream) => {
        setRemoteStream(stream);
      },
      onIceCandidate: (candidate) => {
        if (activeCallRef.current) {
          callApiService.sendSignal(activeCallRef.current.id, {
            type: "WEBRTC_ICE_CANDIDATE",
            callId: activeCallRef.current.id,
            conversationId: activeCallRef.current.conversationId,
            candidate,
          });
        }
      },
      onConnectionState: (state) => {
        if (state === "FAILED" || state === "CLOSED") {
          cleanupCallUI();
        }
      },
    });
  }, [cleanupCallUI]);

  // Load active conversation messages and subscribe to topic
  useEffect(() => {
    if (!activeConvId) return;

    let isMounted = true;
    const fetchActive = async () => {
      try {
        setMessagesLoading(true);
        const conv = await humanChatService.getHumanConversation(activeConvId);
        if (isMounted) {
          setActiveConv(conv);
          setMessages(conv.messages || []);
          humanChatService.markAsRead(activeConvId);
        }
      } catch (err: any) {
        console.error("Failed to fetch conversation details:", err);
      } finally {
        if (isMounted) setMessagesLoading(false);
      }
    };

    fetchActive();

    const destination = `/topic/conversations/${activeConvId}`;
    const unsubConv = wsService.subscribe(destination, async (event: any) => {
      if (!isMounted) return;

      if (event.type === "MESSAGE_SENT") {
        const newMsg: ChatMessage = {
          id: event.messageId || Date.now(),
          conversationId: activeConvId,
          senderType: (event.senderRole as any) || "USER",
          senderName: event.senderName || "User",
          content: event.content || "",
          sequenceNumber: event.sequenceNumber || messages.length + 1,
          status: event.status || "SENT",
          createdAt: event.timestamp || new Date().toISOString(),
        };

        setMessages((prev) => {
          if (prev.some((m) => m.id === newMsg.id)) return prev;
          return [...prev, newMsg];
        });
      } else if (event.type === "MESSAGE_READ") {
        setMessages((prev) =>
          prev.map((m) => (m.status !== "READ" ? { ...m, status: "READ" } : m))
        );
      }
      // WebRTC Call Events for Admin
      else if (event.type === "INCOMING_CALL") {
        if (!activeCallRef.current) {
          const session: CallSession = {
            id: event.callId,
            conversationId: activeConvId,
            callerId: event.senderId,
            callerName: event.senderName || "Candidate",
            receiverId: user?.id || 0,
            receiverName: user?.name || "Admin",
            callType: event.callType || "AUDIO",
            status: "RINGING",
            startedAt: event.timestamp || new Date().toISOString(),
            durationSeconds: 0,
            createdAt: event.timestamp || new Date().toISOString(),
          };
          setActiveCallSession(session);
          setCallMode("INCOMING");
        }
      } else if (event.type === "CALL_ACCEPTED") {
        if (activeCallRef.current && event.senderId !== user?.id) {
          setCallMode("ACTIVE");
          try {
            const offer = await webrtcManager.createOffer();
            await callApiService.sendSignal(activeCallRef.current.id, {
              type: "WEBRTC_OFFER",
              callId: activeCallRef.current.id,
              conversationId: activeConvId,
              sdp: offer.sdp,
            });
          } catch (err: any) {
            setCallErrorMessage(err.message);
          }
        }
      } else if (
        event.type === "CALL_REJECTED" ||
        event.type === "CALL_CANCELLED" ||
        event.type === "CALL_ENDED" ||
        event.type === "CALL_MISSED"
      ) {
        if (activeCallRef.current && activeCallRef.current.id === event.callId) {
          cleanupCallUI();
        }
      } else if (event.type === "WEBRTC_OFFER") {
        if (activeCallRef.current && event.senderId !== user?.id && event.sdp) {
          try {
            const answer = await webrtcManager.handleOfferAndCreateAnswer(event.sdp);
            await callApiService.sendSignal(activeCallRef.current.id, {
              type: "WEBRTC_ANSWER",
              callId: activeCallRef.current.id,
              conversationId: activeConvId,
              sdp: answer.sdp,
            });
            setCallMode("ACTIVE");
          } catch (err: any) {
            setCallErrorMessage(err.message);
          }
        }
      } else if (event.type === "WEBRTC_ANSWER") {
        if (activeCallRef.current && event.senderId !== user?.id && event.sdp) {
          try {
            await webrtcManager.handleAnswer(event.sdp);
            setCallMode("ACTIVE");
          } catch (err: any) {
            setCallErrorMessage(err.message);
          }
        }
      } else if (event.type === "WEBRTC_ICE_CANDIDATE") {
        if (activeCallRef.current && event.senderId !== user?.id && event.candidate) {
          webrtcManager.addIceCandidate(event.candidate);
        }
      }
    });

    return () => {
      isMounted = false;
      unsubConv();
    };
  }, [activeConvId, user?.id, user?.name, cleanupCallUI, messages.length]);

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  // Search handler
  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setAppliedSearch(searchInput.trim());
  };

  const handleClearSearch = () => {
    setSearchInput("");
    setAppliedSearch("");
  };

  // Support Status Lifecycle Handlers
  const handleOpenResolveModal = () => {
    setResolutionNote("");
    setShowResolveModal(true);
  };

  const handleConfirmResolve = async () => {
    if (!activeConvId) return;

    try {
      setIsUpdatingStatus(true);
      await humanChatService.updateSupportStatus(activeConvId, "RESOLVED", resolutionNote.trim());
      setFeedback({
        type: "success",
        message: `Support Ticket #${activeConvId} was marked as RESOLVED.`,
      });
      setShowResolveModal(false);
      setResolutionNote("");
      // Refresh conversation state and inbox
      setInbox((prev) =>
        prev.map((c) => (c.id === activeConvId ? { ...c, archived: true } : c))
      );
      if (activeConv) {
        setActiveConv({ ...activeConv, archived: true });
      }
    } catch (err: any) {
      console.error("Failed to resolve support ticket:", err);
      setFeedback({
        type: "error",
        message: err.message || "Failed to resolve support ticket.",
      });
    } finally {
      setIsUpdatingStatus(false);
    }
  };

  const handleReopenTicket = async () => {
    if (!activeConvId) return;

    try {
      setIsUpdatingStatus(true);
      await humanChatService.updateSupportStatus(activeConvId, "OPEN");
      setFeedback({
        type: "success",
        message: `Support Ticket #${activeConvId} has been REOPENED.`,
      });
      // Refresh conversation state and inbox
      setInbox((prev) =>
        prev.map((c) => (c.id === activeConvId ? { ...c, archived: false } : c))
      );
      if (activeConv) {
        setActiveConv({ ...activeConv, archived: false });
      }
    } catch (err: any) {
      console.error("Failed to reopen support ticket:", err);
      setFeedback({
        type: "error",
        message: err.message || "Failed to reopen support ticket.",
      });
    } finally {
      setIsUpdatingStatus(false);
    }
  };

  // Admin Call Handlers
  const handleStartCall = async (type: CallType) => {
    if (!activeConvId) return;

    try {
      setCallErrorMessage(null);
      const stream = await webrtcManager.acquireLocalMedia({
        audio: true,
        video: type === "VIDEO",
      });
      setLocalStream(stream);
      webrtcManager.initPeerConnection();

      const session = await callApiService.initiateCall(activeConvId, type);
      setActiveCallSession(session);
      setCallMode("OUTGOING");
    } catch (err: any) {
      setCallErrorMessage(err.message || "Failed to start support call.");
      cleanupCallUI();
    }
  };

  const handleAcceptCall = async () => {
    if (!activeCallSession) return;

    try {
      setCallErrorMessage(null);
      const stream = await webrtcManager.acquireLocalMedia({
        audio: true,
        video: activeCallSession.callType === "VIDEO",
      });
      setLocalStream(stream);
      webrtcManager.initPeerConnection();

      const accepted = await callApiService.acceptCall(activeCallSession.id);
      setActiveCallSession(accepted);
      setCallMode("ACTIVE");
    } catch (err: any) {
      setCallErrorMessage(err.message || "Failed to accept support call.");
      cleanupCallUI();
    }
  };

  const handleRejectCall = async () => {
    if (!activeCallSession) return;
    try {
      await callApiService.rejectCall(activeCallSession.id);
    } catch {
      // ignore
    } finally {
      cleanupCallUI();
    }
  };

  const handleCancelCall = async () => {
    if (!activeCallSession) return;
    try {
      await callApiService.cancelCall(activeCallSession.id);
    } catch {
      // ignore
    } finally {
      cleanupCallUI();
    }
  };

  const handleEndCall = async () => {
    if (!activeCallSession) return;
    try {
      await callApiService.endCall(activeCallSession.id);
    } catch {
      // ignore
    } finally {
      cleanupCallUI();
    }
  };

  const handleToggleMute = () => {
    const nextState = !isMuted;
    webrtcManager.toggleAudio(!nextState);
    setIsMuted(nextState);
  };

  const handleToggleVideo = () => {
    const nextState = !isVideoOff;
    webrtcManager.toggleVideo(!nextState);
    setIsVideoOff(nextState);
  };

  const handleSendMessage = async () => {
    if (!inputMessage.trim() || !activeConvId || sending) return;

    const content = inputMessage.trim();
    setInputMessage("");

    try {
      setSending(true);
      const sent = await humanChatService.sendHumanMessage(activeConvId, content);
      setMessages((prev) => {
        if (prev.some((m) => m.id === sent.id)) return prev;
        return [...prev, sent];
      });

      setInbox((prev) =>
        prev.map((c) =>
          c.id === activeConvId
            ? { ...c, lastMessageAt: sent.createdAt, updatedAt: sent.createdAt }
            : c
        )
      );
    } catch (err: any) {
      console.error("Failed to send message:", err);
      setFeedback({
        type: "error",
        message: err.message || "Failed to send message.",
      });
    } finally {
      setSending(false);
    }
  };

  if (user?.role !== "ADMIN") {
    return (
      <div className="p-8 text-center bg-white rounded-2xl border border-gray-200 shadow-xs">
        <h2 className="text-lg font-bold text-red-600 mb-2">Access Denied</h2>
        <p className="text-sm text-gray-500">
          Only platform administrators can access the support inbox.
        </p>
      </div>
    );
  }

  // Summary counts from current inbox
  const openCount = inbox.filter((c) => !c.archived).length;
  const resolvedCount = inbox.filter((c) => c.archived).length;

  return (
    <div className="space-y-4">
      {/* Top Breadcrumb Navigation */}
      <nav aria-label="Breadcrumb" className="text-xs text-gray-500 flex items-center gap-1.5 pb-1">
        <a href="/admin" className="hover:text-emerald-700 transition font-medium">Admin Dashboard</a>
        <span>›</span>
        <span className="text-gray-800 font-semibold">Support Inbox</span>
      </nav>

      {/* Feedback Banner */}
      {feedback && (
        <div
          className={`p-3.5 rounded-2xl flex items-center justify-between text-xs sm:text-sm font-medium ${
            feedback.type === "success"
              ? "bg-emerald-50 border border-emerald-200 text-emerald-800"
              : "bg-red-50 border border-red-200 text-red-800"
          }`}
        >
          <span>{feedback.type === "success" ? "✓" : "⚠️"} {feedback.message}</span>
          <button
            onClick={() => setFeedback(null)}
            aria-label="Dismiss feedback notification"
            className="text-xs font-bold px-2 py-1 rounded-lg hover:bg-black/5 cursor-pointer"
          >
            ✕
          </button>
        </div>
      )}

      {/* Main Console Container */}
      <div className="h-[calc(100vh-10rem)] flex flex-col bg-white rounded-2xl border border-gray-200 overflow-hidden shadow-xs relative">
        {/* Active Call Overlay */}
        {callMode && (
          <CallOverlay
            session={activeCallSession}
            callMode={callMode}
            localStream={localStream}
            remoteStream={remoteStream}
            isMuted={isMuted}
            isVideoOff={isVideoOff}
            errorMessage={callErrorMessage}
            onAccept={handleAcceptCall}
            onReject={handleRejectCall}
            onCancel={handleCancelCall}
            onEnd={handleEndCall}
            onToggleMute={handleToggleMute}
            onToggleVideo={handleToggleVideo}
          />
        )}

        <div className="flex h-full">
          {/* Left Sidebar: Support Ticket Queue */}
          <div className="w-80 md:w-96 border-r border-gray-200 flex flex-col bg-gray-50/50">
            {/* Header & Metrics */}
            <div className="p-4 border-b border-gray-200 bg-white space-y-3">
              <div className="flex items-center justify-between">
                <h2 className="font-extrabold text-gray-900 text-base sm:text-lg flex items-center gap-2">
                  <span>🛡️</span> Support Inbox
                </h2>
                <div className="flex items-center gap-1.5">
                  <span className="px-2 py-0.5 text-[10px] font-extrabold bg-emerald-50 text-emerald-700 border border-emerald-200 rounded-full">
                    {openCount} Open
                  </span>
                  <span className="px-2 py-0.5 text-[10px] font-bold bg-gray-100 text-gray-600 rounded-full">
                    {resolvedCount} Resolved
                  </span>
                </div>
              </div>

              {/* Search Bar */}
              <form onSubmit={handleSearchSubmit} className="relative">
                <input
                  type="text"
                  value={searchInput}
                  onChange={(e) => setSearchInput(e.target.value)}
                  placeholder="Search user, email, subject..."
                  aria-label="Search support tickets"
                  className="w-full pl-8 pr-7 py-1.5 rounded-xl border border-gray-200 bg-gray-50 text-xs text-gray-900 focus:outline-none focus:ring-2 focus:ring-emerald-600 focus:bg-white"
                />
                <span className="absolute left-2.5 top-2 text-gray-400 text-xs" aria-hidden="true">🔍</span>
                {searchInput && (
                  <button
                    type="button"
                    onClick={handleClearSearch}
                    aria-label="Clear search input"
                    className="absolute right-2.5 top-1.5 text-gray-400 hover:text-gray-700 text-xs font-bold"
                  >
                    ✕
                  </button>
                )}
              </form>

              {/* Status Filter Tabs */}
              <div className="flex items-center gap-1 pt-1">
                {(["ALL", "OPEN", "RESOLVED"] as const).map((tab) => (
                  <button
                    key={tab}
                    onClick={() => setStatusFilter(tab)}
                    className={`flex-1 py-1 text-[11px] font-bold rounded-lg transition-colors cursor-pointer ${
                      statusFilter === tab
                        ? "bg-emerald-600 text-white shadow-2xs"
                        : "bg-gray-100 text-gray-600 hover:bg-gray-200"
                    }`}
                  >
                    {tab}
                  </button>
                ))}
              </div>
            </div>

            {/* Ticket List */}
            <div className="flex-1 overflow-y-auto divide-y divide-gray-100">
              {loading ? (
                <div className="p-8 text-center text-gray-400 text-xs sm:text-sm animate-pulse space-y-2">
                  <div className="w-5 h-5 border-2 border-emerald-600 border-t-transparent rounded-full animate-spin mx-auto"></div>
                  <span>Loading support tickets...</span>
                </div>
              ) : inbox.length === 0 ? (
                <div className="p-8 text-center space-y-3">
                  <div className="w-12 h-12 rounded-full bg-gray-100 flex items-center justify-center mx-auto text-xl">
                    🛡️
                  </div>
                  <p className="text-sm font-bold text-gray-700">No support tickets found</p>
                  <p className="text-xs text-gray-500">
                    {appliedSearch || statusFilter !== "ALL"
                      ? "No tickets match your search or filter parameters."
                      : "All user support inquiries are resolved."}
                  </p>
                  {(appliedSearch || statusFilter !== "ALL") && (
                    <button
                      onClick={() => {
                        handleClearSearch();
                        setStatusFilter("ALL");
                      }}
                      className="text-xs font-bold text-emerald-700 hover:underline cursor-pointer"
                    >
                      Reset filters
                    </button>
                  )}
                </div>
              ) : (
                inbox.map((c) => {
                  const isSelected = c.id === activeConvId;
                  const candidate = c.participants?.find((p) => p.role !== "ADMIN");
                  const isResolved = c.archived;

                  return (
                    <button
                      key={c.id}
                      onClick={() => setActiveConvId(c.id)}
                      className={`w-full text-left p-3.5 transition-colors flex items-start gap-3 relative cursor-pointer ${
                        isSelected
                          ? "bg-white border-l-4 border-emerald-600 shadow-2xs"
                          : "hover:bg-gray-100/70"
                      }`}
                    >
                      <div
                        className={`w-9 h-9 rounded-full font-bold flex items-center justify-center text-xs flex-shrink-0 ${
                          isResolved
                            ? "bg-gray-200 text-gray-600"
                            : "bg-emerald-100 text-emerald-800 border border-emerald-200"
                        }`}
                      >
                        {candidate?.userName?.charAt(0) || "U"}
                      </div>
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center justify-between mb-0.5">
                          <h4 className="text-xs sm:text-sm font-bold text-gray-900 truncate">
                            {c.title}
                          </h4>
                          <span className="text-[10px] text-gray-400 font-medium ml-1 flex-shrink-0">
                            {c.lastMessageAt
                              ? new Date(c.lastMessageAt).toLocaleDateString([], {
                                  month: "short",
                                  day: "numeric",
                                })
                              : ""}
                          </span>
                        </div>
                        <div className="flex items-center justify-between text-xs text-gray-500">
                          <span className="truncate">
                            {candidate?.userName || "User"}
                            {candidate?.userEmail && ` (${candidate.userEmail})`}
                          </span>
                          <span
                            className={`text-[9px] font-extrabold px-1.5 py-0.2 rounded uppercase ml-2 ${
                              isResolved
                                ? "bg-gray-100 text-gray-600"
                                : "bg-emerald-50 text-emerald-700 border border-emerald-200"
                            }`}
                          >
                            {isResolved ? "Resolved" : "Open"}
                          </span>
                        </div>
                      </div>
                    </button>
                  );
                })
              )}
            </div>
          </div>

          {/* Right Pane: Support Ticket Chat & Live Calling */}
          <div className="flex-1 flex flex-col bg-white">
            {activeConvId && activeConv ? (
              <>
                {/* Header with Calling & Resolution Controls */}
                <div className="p-3.5 border-b border-gray-200 flex flex-wrap items-center justify-between gap-3 bg-white/95 backdrop-blur-xs">
                  <div>
                    <div className="flex items-center gap-2">
                      <h3 className="font-extrabold text-gray-900 text-sm sm:text-base">
                        {activeConv.title}
                      </h3>
                      <span className="px-2 py-0.5 text-[10px] font-bold bg-gray-100 text-gray-700 rounded-full">
                        #{activeConv.id}
                      </span>
                      <span
                        className={`px-2 py-0.5 text-[10px] font-extrabold rounded-full border ${
                          activeConv.archived
                            ? "bg-gray-100 text-gray-700 border-gray-200"
                            : "bg-emerald-50 text-emerald-700 border-emerald-200"
                        }`}
                      >
                        {activeConv.archived ? "RESOLVED" : "ACTIVE"}
                      </span>
                    </div>
                    <p className="text-xs text-gray-500 mt-0.5">
                      Requester:{" "}
                      <strong className="text-gray-800">
                        {activeConv.participants?.find((p) => p.role !== "ADMIN")?.userName || "User"}
                      </strong>
                      {activeConv.participants?.find((p) => p.role !== "ADMIN")?.userEmail && (
                        <span>
                          {" "}
                          ({activeConv.participants?.find((p) => p.role !== "ADMIN")?.userEmail})
                        </span>
                      )}
                    </p>
                  </div>

                  <div className="flex items-center gap-2">
                    {/* Status Resolution Controls */}
                    {activeConv.archived ? (
                      <button
                        onClick={handleReopenTicket}
                        disabled={isUpdatingStatus}
                        className="px-3 py-1.5 text-xs font-bold bg-emerald-50 text-emerald-800 hover:bg-emerald-100 border border-emerald-200 rounded-xl transition cursor-pointer flex items-center gap-1 shadow-2xs"
                        title="Reopen this resolved ticket"
                      >
                        <span>↺</span> Reopen Ticket
                      </button>
                    ) : (
                      <button
                        onClick={handleOpenResolveModal}
                        disabled={isUpdatingStatus}
                        className="px-3 py-1.5 text-xs font-bold bg-gray-100 text-gray-700 hover:bg-gray-200 border border-gray-200 rounded-xl transition cursor-pointer flex items-center gap-1 shadow-2xs"
                        title="Mark inquiry as resolved"
                      >
                        <span>✓</span> Resolve Ticket
                      </button>
                    )}

                    {/* Audio & Video Calling Buttons */}
                    <button
                      onClick={() => handleStartCall("AUDIO")}
                      className="px-3 py-1.5 text-xs font-semibold bg-gray-100 text-gray-800 hover:bg-gray-200 rounded-xl transition-colors flex items-center gap-1.5 border border-gray-200 shadow-2xs cursor-pointer"
                      title="Start Voice Call with Candidate"
                    >
                      <span>📞</span> Audio
                    </button>
                    <button
                      onClick={() => handleStartCall("VIDEO")}
                      className="px-3 py-1.5 text-xs font-semibold bg-black text-white hover:bg-gray-800 rounded-xl transition-colors flex items-center gap-1.5 shadow-2xs cursor-pointer"
                      title="Start Video Call with Candidate"
                    >
                      <span>📹</span> Video
                    </button>
                  </div>
                </div>

                {/* Message Timeline */}
                <div className="flex-1 overflow-y-auto p-4 space-y-4 bg-gray-50/40">
                  {messagesLoading ? (
                    <div className="p-8 text-center text-gray-400 text-xs sm:text-sm animate-pulse">
                      Loading conversation messages...
                    </div>
                  ) : messages.length === 0 ? (
                    <div className="text-center py-12 text-gray-400">
                      <p className="text-sm font-semibold">No messages in this support inquiry</p>
                    </div>
                  ) : (
                    messages.map((m) => {
                      const isMe = m.senderName === user?.name || m.senderType === "ADMIN";
                      return (
                        <div
                          key={m.id}
                          className={`flex flex-col ${isMe ? "items-end" : "items-start"}`}
                        >
                          <div className="flex items-baseline gap-2 mb-1 px-1">
                            <span className="text-[11px] font-bold text-gray-600">
                              {isMe ? "You (Admin Support)" : m.senderName}
                            </span>
                            <span className="text-[10px] text-gray-400">
                              {new Date(m.createdAt).toLocaleTimeString([], {
                                hour: "2-digit",
                                minute: "2-digit",
                              })}
                            </span>
                          </div>
                          <div
                            className={`max-w-[75%] rounded-2xl px-4 py-2.5 text-xs sm:text-sm shadow-2xs whitespace-pre-wrap leading-relaxed ${
                              isMe
                                ? "bg-emerald-600 text-white rounded-br-xs"
                                : "bg-white text-gray-900 border border-gray-200 rounded-bl-xs"
                            }`}
                          >
                            {m.content}
                          </div>
                        </div>
                      );
                    })
                  )}
                  <div ref={messagesEndRef} />
                </div>

                {/* Reply Area or Resolved Banner */}
                {activeConv.archived ? (
                  <div className="p-3 bg-gray-50 border-t border-gray-200 flex items-center justify-between text-xs text-gray-600">
                    <div className="flex items-center gap-2">
                      <span className="text-sm">🔒</span>
                      <span>
                        This support ticket is marked as <strong>Resolved</strong>. Communication is currently archived.
                      </span>
                    </div>
                    <button
                      onClick={handleReopenTicket}
                      className="px-3 py-1 rounded-lg bg-emerald-600 text-white font-bold text-xs hover:bg-emerald-700 cursor-pointer transition"
                    >
                      Reopen to Reply
                    </button>
                  </div>
                ) : (
                  <div className="p-3 border-t border-gray-200 bg-white">
                    <div className="flex items-end gap-2 bg-gray-50 p-2 rounded-xl border border-gray-200 focus-within:border-emerald-600 focus-within:ring-1 focus-within:ring-emerald-600 transition-all">
                      <textarea
                        value={inputMessage}
                        onChange={(e) => setInputMessage(e.target.value)}
                        onKeyDown={(e) => {
                          if (e.key === "Enter" && !e.shiftKey) {
                            e.preventDefault();
                            handleSendMessage();
                          }
                        }}
                        aria-label="Reply to candidate support ticket"
                        placeholder="Reply to candidate support ticket (Enter to send, Shift+Enter for new line)..."
                        className="flex-1 bg-transparent border-0 resize-none text-xs sm:text-sm text-gray-900 focus:outline-none min-h-[40px] max-h-32 p-1"
                        rows={1}
                      />
                      <button
                        onClick={handleSendMessage}
                        disabled={!inputMessage.trim() || sending}
                        className="px-4 py-2 bg-emerald-600 text-white text-xs font-bold rounded-lg hover:bg-emerald-700 disabled:opacity-40 transition-colors flex items-center gap-1 shadow-xs cursor-pointer"
                      >
                        {sending ? (
                          <div className="w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                        ) : (
                          <>
                            <span>Reply</span>
                            <span>↑</span>
                          </>
                        )}
                      </button>
                    </div>
                  </div>
                )}
              </>
            ) : (
              <div className="flex-1 flex flex-col items-center justify-center p-8 text-center text-gray-400 space-y-4">
                <div className="w-16 h-16 rounded-full bg-gray-100 flex items-center justify-center text-2xl">
                  🛡️
                </div>
                <p className="text-sm font-bold text-gray-700">Support Ticket Console</p>
                <p className="text-xs text-gray-500 max-w-sm">
                  Select a support ticket from the inbox to reply to candidates or initiate voice/video support.
                </p>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Resolve Confirmation Modal */}
      {showResolveModal && (
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="resolve-ticket-title"
          className="fixed inset-0 z-50 bg-black/60 backdrop-blur-xs flex items-center justify-center p-4"
        >
          <div className="bg-white rounded-3xl max-w-md w-full p-6 shadow-2xl border border-gray-200 space-y-4 animate-in fade-in duration-200">
            <div className="flex items-center justify-between pb-3 border-b border-gray-100">
              <div className="flex items-center gap-2.5">
                <div className="w-8 h-8 rounded-xl bg-emerald-100 text-emerald-700 flex items-center justify-center font-bold text-base" aria-hidden="true">
                  ✓
                </div>
                <div>
                  <h3 id="resolve-ticket-title" className="text-base font-bold text-gray-900">Resolve Support Ticket</h3>
                  <p className="text-xs text-gray-500">Ticket #{activeConvId}</p>
                </div>
              </div>
              <button
                onClick={() => setShowResolveModal(false)}
                aria-label="Close resolve ticket modal"
                className="w-7 h-7 rounded-full bg-gray-100 hover:bg-gray-200 text-gray-600 flex items-center justify-center font-bold text-xs cursor-pointer focus:outline-none focus:ring-2 focus:ring-emerald-600"
              >
                ✕
              </button>
            </div>

            <p className="text-xs text-gray-600 leading-relaxed">
              Marking this ticket as <strong>Resolved</strong> archives the inquiry and preserves the full message transcript in platform audit logs. You can reopen it at any time if further follow-up is requested.
            </p>

            <div>
              <label htmlFor="resolution-note-input" className="block text-xs font-bold text-gray-700 mb-1">
                Resolution Note (Optional)
              </label>
              <textarea
                id="resolution-note-input"
                value={resolutionNote}
                onChange={(e) => setResolutionNote(e.target.value)}
                placeholder="Briefly state how the issue was resolved..."
                rows={2}
                className="w-full px-3 py-2 rounded-xl border border-gray-200 text-xs focus:outline-none focus:ring-2 focus:ring-emerald-600"
              />
            </div>

            <div className="flex items-center justify-end gap-2.5 pt-2 border-t border-gray-100">
              <button
                type="button"
                onClick={() => setShowResolveModal(false)}
                disabled={isUpdatingStatus}
                className="px-4 py-2 rounded-xl bg-gray-100 hover:bg-gray-200 text-gray-700 font-semibold text-xs cursor-pointer"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleConfirmResolve}
                disabled={isUpdatingStatus}
                className="px-5 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs transition cursor-pointer flex items-center gap-1.5 focus:outline-none focus:ring-2 focus:ring-emerald-600"
              >
                {isUpdatingStatus ? (
                  <>
                    <div className="w-3 h-3 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                    <span>Resolving...</span>
                  </>
                ) : (
                  <span>Confirm Resolution</span>
                )}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
