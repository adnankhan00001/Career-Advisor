"use client";

import { useState, useEffect, useRef, useCallback } from "react";
import { useAuth } from "@/context/AuthContext";
import {
  humanChatService,
  HumanConversation,
  UserSearchItem,
  RealTimeEvent,
} from "@/lib/humanChatService";
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

export default function MessagesPage() {
  const { user } = useAuth();

  const [conversations, setConversations] = useState<HumanConversation[]>([]);
  const [activeConvId, setActiveConvId] = useState<number | null>(null);
  const [activeConv, setActiveConv] = useState<HumanConversation | null>(null);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [loading, setLoading] = useState(true);
  const [messagesLoading, setMessagesLoading] = useState(false);
  const [sending, setSending] = useState(false);
  const [inputMessage, setInputMessage] = useState("");
  const [activeTab, setActiveTab] = useState<
    "ALL" | "USER_TO_USER" | "USER_TO_ADMIN" | "CALLS"
  >("ALL");

  // Call history state
  const [callHistory, setCallHistory] = useState<CallSession[]>([]);
  const [callHistoryLoading, setCallHistoryLoading] = useState(false);

  // User search modal state
  const [showSearchModal, setShowSearchModal] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");
  const [searchResults, setSearchResults] = useState<UserSearchItem[]>([]);
  const [searching, setSearching] = useState(false);

  // New Support Ticket modal state
  const [showSupportModal, setShowSupportModal] = useState(false);
  const [supportSubject, setSupportSubject] = useState("");
  const [supportInitialMsg, setSupportInitialMsg] = useState("");
  const [creatingSupport, setCreatingSupport] = useState(false);

  // Real-time typing state for active conversation
  const [typingUsers, setTypingUsers] = useState<Map<number, string>>(new Map());
  const typingTimeoutRef = useRef<NodeJS.Timeout | null>(null);
  const isTypingRef = useRef(false);

  // Online status map
  const [onlineUserIds, setOnlineUserIds] = useState<Set<number>>(new Set());

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

  // Load conversations on mount
  const loadConversations = useCallback(async () => {
    try {
      setLoading(true);
      const data = await humanChatService.getHumanConversations();
      setConversations(data);
      if (data.length > 0 && activeConvId === null) {
        setActiveConvId(data[0].id);
      }
    } catch {
      // Graceful error state
    } finally {
      setLoading(false);
    }
  }, [activeConvId]);

  useEffect(() => {
    loadConversations();
  }, [loadConversations]);

  // Load Call History when tab active
  useEffect(() => {
    if (activeTab === "CALLS") {
      setCallHistoryLoading(true);
      callApiService
        .getCallHistory()
        .then(setCallHistory)
        .catch(() => {})
        .finally(() => setCallHistoryLoading(false));
    }
  }, [activeTab]);

  // Connect to WebSocket & presence channel
  useEffect(() => {
    wsService.connect();

    const unsubPresence = wsService.subscribe("/topic/presence", (event: RealTimeEvent) => {
      if (event.senderId) {
        setOnlineUserIds((prev) => {
          const next = new Set(prev);
          if (event.type === "USER_ONLINE") {
            next.add(event.senderId!);
          } else if (event.type === "USER_OFFLINE") {
            next.delete(event.senderId!);
          }
          return next;
        });
      }
    });

    return () => {
      unsubPresence();
    };
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

  // WebRTC Manager Callbacks Setup
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

  // Load active conversation details and messages
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
      } catch {
        // Error handling
      } finally {
        if (isMounted) setMessagesLoading(false);
      }
    };

    fetchActive();

    // Subscribe to STOMP topic for active conversation (Messages & Call Signaling)
    const destination = `/topic/conversations/${activeConvId}`;
    const unsubConv = wsService.subscribe(destination, async (event: any) => {
      if (!isMounted) return;

      // 1. Text Message Events
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

        if (event.senderId && event.senderId !== user?.id) {
          humanChatService.markAsRead(activeConvId);
        }
      } else if (event.type === "MESSAGE_READ") {
        setMessages((prev) =>
          prev.map((m) => (m.status !== "READ" ? { ...m, status: "READ" } : m))
        );
      } else if (event.type === "TYPING_STARTED") {
        if (event.senderId && event.senderId !== user?.id) {
          setTypingUsers((prev) => {
            const next = new Map(prev);
            next.set(event.senderId!, event.senderName || "User");
            return next;
          });
        }
      } else if (event.type === "TYPING_STOPPED") {
        if (event.senderId) {
          setTypingUsers((prev) => {
            const next = new Map(prev);
            next.delete(event.senderId!);
            return next;
          });
        }
      }

      // 2. WebRTC Call Events
      else if (event.type === "INCOMING_CALL") {
        if (event.targetUserId === user?.id && !activeCallRef.current) {
          const session: CallSession = {
            id: event.callId,
            conversationId: activeConvId,
            callerId: event.senderId,
            callerName: event.senderName || "Caller",
            receiverId: user?.id || 0,
            receiverName: user?.name || "User",
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
      setTypingUsers(new Map());
    };
  }, [activeConvId, user?.id, user?.name, cleanupCallUI, messages.length]);

  useEffect(() => {
    scrollToBottom();
  }, [messages, typingUsers]);

  // Handle user search input
  useEffect(() => {
    if (searchQuery.trim().length < 2) {
      setSearchResults([]);
      return;
    }

    const timer = setTimeout(async () => {
      setSearching(true);
      try {
        const results = await humanChatService.searchUsers(searchQuery);
        setSearchResults(results);
      } catch {
        setSearchResults([]);
      } finally {
        setSearching(false);
      }
    }, 300);

    return () => clearTimeout(timer);
  }, [searchQuery]);

  const handleStartPeerChat = async (targetUserId: number) => {
    try {
      const conv = await humanChatService.startUserConversation(targetUserId);
      setShowSearchModal(false);
      setSearchQuery("");
      setSearchResults([]);
      setConversations((prev) => {
        const exists = prev.find((c) => c.id === conv.id);
        if (exists) return prev;
        return [conv, ...prev];
      });
      setActiveConvId(conv.id);
    } catch {
      // Error handling
    }
  };

  const handleCreateSupport = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!supportSubject.trim()) return;

    try {
      setCreatingSupport(true);
      const conv = await humanChatService.startAdminConversation(
        supportSubject.trim(),
        supportInitialMsg.trim() || undefined
      );
      setShowSupportModal(false);
      setSupportSubject("");
      setSupportInitialMsg("");
      setConversations((prev) => [conv, ...prev]);
      setActiveConvId(conv.id);
    } catch {
      // Error handling
    } finally {
      setCreatingSupport(false);
    }
  };

  // WebRTC Call Initiation
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
      setCallErrorMessage(err.message || "Failed to start call.");
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
      setCallErrorMessage(err.message || "Failed to accept call.");
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

  const handleInputChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    setInputMessage(e.target.value);

    if (!activeConvId) return;

    if (!isTypingRef.current) {
      isTypingRef.current = true;
      humanChatService.sendTyping(activeConvId, true);
    }

    if (typingTimeoutRef.current) clearTimeout(typingTimeoutRef.current);
    typingTimeoutRef.current = setTimeout(() => {
      isTypingRef.current = false;
      humanChatService.sendTyping(activeConvId, false);
    }, 2000);
  };

  const handleSendMessage = async () => {
    if (!inputMessage.trim() || !activeConvId || sending) return;

    const content = inputMessage.trim();
    setInputMessage("");

    if (isTypingRef.current) {
      isTypingRef.current = false;
      humanChatService.sendTyping(activeConvId, false);
    }

    try {
      setSending(true);
      const sent = await humanChatService.sendHumanMessage(activeConvId, content);
      setMessages((prev) => {
        if (prev.some((m) => m.id === sent.id)) return prev;
        return [...prev, sent];
      });

      setConversations((prev) =>
        prev.map((c) =>
          c.id === activeConvId
            ? { ...c, lastMessageAt: sent.createdAt, updatedAt: sent.createdAt }
            : c
        )
      );
    } catch {
      // Error handling
    } finally {
      setSending(false);
    }
  };

  const filteredConversations = conversations.filter((c) => {
    if (activeTab === "ALL") return true;
    return c.conversationType === activeTab;
  });

  const getOtherParticipant = (conv: HumanConversation) => {
    if (!conv.participants) return null;
    return conv.participants.find((p) => p.userId !== user?.id);
  };

  return (
    <div className="h-[calc(100vh-8.5rem)] flex flex-col bg-white rounded-2xl border border-gray-200 overflow-hidden shadow-sm relative">
      {/* Active WebRTC Call Overlay */}
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
        {/* Left Sidebar: Conversations & Actions */}
        <div className="w-80 md:w-96 border-r border-gray-200 flex flex-col bg-gray-50/50">
          {/* Header */}
          <div className="p-4 border-b border-gray-200 space-y-3 bg-white">
            <div className="flex items-center justify-between">
              <h2 className="font-bold text-gray-900 text-lg flex items-center gap-2">
                <span>💬</span> Messages
              </h2>
              <div className="flex items-center gap-1">
                <button
                  onClick={() => setShowSearchModal(true)}
                  className="p-2 text-xs font-semibold bg-black text-white hover:bg-gray-800 rounded-lg transition-colors flex items-center gap-1 shadow-sm"
                  title="New Direct Message"
                >
                  <span>+</span> Chat
                </button>
                <button
                  onClick={() => setShowSupportModal(true)}
                  className="p-2 text-xs font-semibold bg-emerald-600 text-white hover:bg-emerald-700 rounded-lg transition-colors flex items-center gap-1 shadow-sm"
                  title="Get Support from Admin"
                >
                  <span>🛡️</span> Support
                </button>
              </div>
            </div>

            {/* Filter Tabs */}
            <div className="flex bg-gray-100 p-1 rounded-lg text-xs font-semibold">
              <button
                onClick={() => setActiveTab("ALL")}
                className={`flex-1 py-1.5 rounded-md transition-colors ${
                  activeTab === "ALL"
                    ? "bg-white text-black shadow-xs font-bold"
                    : "text-gray-500 hover:text-gray-900"
                }`}
              >
                All
              </button>
              <button
                onClick={() => setActiveTab("USER_TO_USER")}
                className={`flex-1 py-1.5 rounded-md transition-colors ${
                  activeTab === "USER_TO_USER"
                    ? "bg-white text-black shadow-xs font-bold"
                    : "text-gray-500 hover:text-gray-900"
                }`}
              >
                Direct
              </button>
              <button
                onClick={() => setActiveTab("USER_TO_ADMIN")}
                className={`flex-1 py-1.5 rounded-md transition-colors ${
                  activeTab === "USER_TO_ADMIN"
                    ? "bg-white text-black shadow-xs font-bold"
                    : "text-gray-500 hover:text-gray-900"
                }`}
              >
                Support
              </button>
              <button
                onClick={() => setActiveTab("CALLS")}
                className={`flex-1 py-1.5 rounded-md transition-colors ${
                  activeTab === "CALLS"
                    ? "bg-white text-black shadow-xs font-bold"
                    : "text-gray-500 hover:text-gray-900"
                }`}
              >
                Calls
              </button>
            </div>
          </div>

          {/* Sidebar Content (Conversations or Call History) */}
          {activeTab === "CALLS" ? (
            <div className="flex-1 overflow-y-auto divide-y divide-gray-100">
              {callHistoryLoading ? (
                <div className="p-8 text-center text-gray-400 text-sm animate-pulse">
                  Loading call history...
                </div>
              ) : callHistory.length === 0 ? (
                <div className="p-8 text-center space-y-2 text-gray-500">
                  <div className="w-12 h-12 rounded-full bg-gray-100 flex items-center justify-center mx-auto text-xl">
                    📞
                  </div>
                  <p className="text-sm font-semibold text-gray-700">No calls yet</p>
                  <p className="text-xs">Your voice and video call logs will appear here.</p>
                </div>
              ) : (
                callHistory.map((call) => {
                  const isOutgoing = call.callerId === user?.id;
                  const partner = isOutgoing ? call.receiverName : call.callerName;

                  return (
                    <div
                      key={call.id}
                      className="p-3.5 hover:bg-gray-100/70 transition-colors flex items-center justify-between"
                    >
                      <div className="flex items-center gap-3">
                        <div className="w-9 h-9 rounded-full bg-gray-200 text-gray-700 font-bold flex items-center justify-center text-xs">
                          {call.callType === "VIDEO" ? "📹" : "📞"}
                        </div>
                        <div>
                          <div className="flex items-center gap-1.5">
                            <h4 className="text-sm font-semibold text-gray-900">
                              {partner}
                            </h4>
                            <span className="text-[10px] text-gray-400">
                              {isOutgoing ? "↗ Outgoing" : "↙ Incoming"}
                            </span>
                          </div>
                          <p className="text-xs text-gray-500">
                            {call.status === "ACCEPTED"
                              ? `Completed (${call.durationSeconds}s)`
                              : call.status === "MISSED"
                              ? "Missed call"
                              : call.status}
                          </p>
                        </div>
                      </div>
                      <span className="text-[10px] text-gray-400">
                        {new Date(call.createdAt).toLocaleDateString([], {
                          month: "short",
                          day: "numeric",
                        })}
                      </span>
                    </div>
                  );
                })
              )}
            </div>
          ) : (
            /* Conversation List */
            <div className="flex-1 overflow-y-auto divide-y divide-gray-100">
              {loading ? (
                <div className="p-8 text-center text-gray-400 text-sm animate-pulse">
                  Loading conversations...
                </div>
              ) : filteredConversations.length === 0 ? (
                <div className="p-8 text-center space-y-3">
                  <div className="w-12 h-12 rounded-full bg-gray-100 flex items-center justify-center mx-auto text-xl">
                    💬
                  </div>
                  <p className="text-sm font-semibold text-gray-700">No conversations yet</p>
                  <p className="text-xs text-gray-500">
                    Search for peers or reach out to admin support to get started.
                  </p>
                </div>
              ) : (
                filteredConversations.map((c) => {
                  const isSelected = c.id === activeConvId;
                  const other = getOtherParticipant(c);
                  const isOtherOnline = other
                    ? onlineUserIds.has(other.userId) || other.online
                    : false;

                  return (
                    <button
                      key={c.id}
                      onClick={() => setActiveConvId(c.id)}
                      className={`w-full text-left p-3.5 transition-colors flex items-start gap-3 relative ${
                        isSelected
                          ? "bg-white border-l-4 border-black shadow-xs"
                          : "hover:bg-gray-100/70"
                      }`}
                    >
                      <div className="relative flex-shrink-0">
                        <div className="w-10 h-10 rounded-full bg-gray-200 text-gray-700 font-bold flex items-center justify-center text-sm uppercase border border-gray-300">
                          {c.conversationType === "USER_TO_ADMIN"
                            ? "🛡️"
                            : other?.userName?.charAt(0) || "U"}
                        </div>
                        {c.conversationType === "USER_TO_USER" && (
                          <span
                            className={`absolute bottom-0 right-0 w-3 h-3 rounded-full border-2 border-white ${
                              isOtherOnline ? "bg-emerald-500" : "bg-gray-400"
                            }`}
                          />
                        )}
                      </div>

                      <div className="flex-1 min-w-0">
                        <div className="flex items-center justify-between mb-0.5">
                          <h4 className="text-sm font-semibold text-gray-900 truncate">
                            {c.conversationType === "USER_TO_ADMIN"
                              ? c.title
                              : other?.userName || c.title}
                          </h4>
                          <span className="text-[10px] text-gray-400">
                            {c.lastMessageAt
                              ? new Date(c.lastMessageAt).toLocaleTimeString([], {
                                  hour: "2-digit",
                                  minute: "2-digit",
                                })
                              : ""}
                          </span>
                        </div>
                        <div className="flex items-center justify-between">
                          <p className="text-xs text-gray-500 truncate">
                            {c.conversationType === "USER_TO_ADMIN"
                              ? "Support Ticket"
                              : other?.userEmail || "Peer Conversation"}
                          </p>
                          {c.unreadCount && c.unreadCount > 0 ? (
                            <span className="px-1.5 py-0.5 text-[10px] font-bold bg-black text-white rounded-full">
                              {c.unreadCount}
                            </span>
                          ) : null}
                        </div>
                      </div>
                    </button>
                  );
                })
              )}
            </div>
          )}
        </div>

        {/* Right Pane: Active Chat Room */}
        <div className="flex-1 flex flex-col bg-white">
          {activeConvId && activeConv ? (
            <>
              {/* Chat Header with Audio & Video Call Buttons */}
              <div className="p-4 border-b border-gray-200 flex items-center justify-between bg-white/90 backdrop-blur-xs">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-full bg-gray-100 flex items-center justify-center font-bold text-gray-800 text-sm border border-gray-200">
                    {activeConv.conversationType === "USER_TO_ADMIN"
                      ? "🛡️"
                      : getOtherParticipant(activeConv)?.userName?.charAt(0) || "U"}
                  </div>
                  <div>
                    <h3 className="font-bold text-gray-900 text-sm flex items-center gap-2">
                      {activeConv.conversationType === "USER_TO_ADMIN"
                        ? activeConv.title
                        : getOtherParticipant(activeConv)?.userName || activeConv.title}
                      {activeConv.conversationType === "USER_TO_ADMIN" && (
                        <span className="px-2 py-0.5 text-[10px] font-semibold bg-emerald-100 text-emerald-800 rounded-full">
                          Support
                        </span>
                      )}
                    </h3>
                    <div className="flex items-center gap-1.5 text-xs text-gray-500">
                      {activeConv.conversationType === "USER_TO_USER" && (
                        <>
                          <span
                            className={`w-2 h-2 rounded-full ${
                              getOtherParticipant(activeConv) &&
                              (onlineUserIds.has(getOtherParticipant(activeConv)!.userId) ||
                                getOtherParticipant(activeConv)!.online)
                                ? "bg-emerald-500"
                                : "bg-gray-400"
                            }`}
                          />
                          <span>
                            {getOtherParticipant(activeConv) &&
                            (onlineUserIds.has(getOtherParticipant(activeConv)!.userId) ||
                              getOtherParticipant(activeConv)!.online)
                              ? "Online"
                              : "Offline"}
                          </span>
                        </>
                      )}
                      {activeConv.conversationType === "USER_TO_ADMIN" && (
                        <span>Verified Support Ticket</span>
                      )}
                    </div>
                  </div>
                </div>

                {/* Call Action Triggers */}
                <div className="flex items-center gap-2">
                  <button
                    onClick={() => handleStartCall("AUDIO")}
                    className="px-3 py-1.5 text-xs font-semibold bg-gray-100 text-gray-800 hover:bg-gray-200 rounded-xl transition-colors flex items-center gap-1.5 border border-gray-200 shadow-xs"
                    title="Start Audio Call"
                  >
                    <span>📞</span> Audio
                  </button>
                  <button
                    onClick={() => handleStartCall("VIDEO")}
                    className="px-3 py-1.5 text-xs font-semibold bg-black text-white hover:bg-gray-800 rounded-xl transition-colors flex items-center gap-1.5 shadow-xs"
                    title="Start Video Call"
                  >
                    <span>📹</span> Video
                  </button>
                </div>
              </div>

              {/* Message Timeline */}
              <div className="flex-1 overflow-y-auto p-4 space-y-4 bg-gray-50/40">
                {messagesLoading ? (
                  <div className="p-8 text-center text-gray-400 text-sm">
                    Loading messages...
                  </div>
                ) : messages.length === 0 ? (
                  <div className="text-center py-12 text-gray-400 space-y-2">
                    <p className="text-sm font-semibold">Start the conversation!</p>
                    <p className="text-xs">Say hello or initiate a direct audio/video call above.</p>
                  </div>
                ) : (
                  messages.map((m) => {
                    const isMe = m.senderName === user?.name || m.senderType === "USER";
                    return (
                      <div
                        key={m.id}
                        className={`flex flex-col ${
                          isMe ? "items-end" : "items-start"
                        }`}
                      >
                        <div className="flex items-baseline gap-2 mb-1 px-1">
                          <span className="text-[11px] font-bold text-gray-600">
                            {isMe ? "You" : m.senderName}
                          </span>
                          <span className="text-[10px] text-gray-400">
                            {new Date(m.createdAt).toLocaleTimeString([], {
                              hour: "2-digit",
                              minute: "2-digit",
                            })}
                          </span>
                        </div>
                        <div
                          className={`max-w-[75%] rounded-2xl px-4 py-2.5 text-sm shadow-xs whitespace-pre-wrap leading-relaxed ${
                            isMe
                              ? "bg-black text-white rounded-br-xs"
                              : "bg-white text-gray-900 border border-gray-200 rounded-bl-xs"
                          }`}
                        >
                          {m.content}
                        </div>
                        {isMe && (
                          <div className="text-[10px] text-gray-400 mt-0.5 px-1 flex items-center gap-1">
                            {m.status === "READ" ? (
                              <span className="text-emerald-600 font-semibold">✓✓ Read</span>
                            ) : (
                              <span>✓ Sent</span>
                            )}
                          </div>
                        )}
                      </div>
                    );
                  })
                )}

                {/* Typing Indicator */}
                {typingUsers.size > 0 && (
                  <div className="flex items-center gap-2 text-xs text-gray-500 italic p-2 bg-gray-100/70 rounded-lg w-fit animate-pulse">
                    <span className="flex gap-1 items-center">
                      <span className="w-1.5 h-1.5 rounded-full bg-gray-500 animate-bounce"></span>
                      <span className="w-1.5 h-1.5 rounded-full bg-gray-500 animate-bounce [animation-delay:0.2s]"></span>
                      <span className="w-1.5 h-1.5 rounded-full bg-gray-500 animate-bounce [animation-delay:0.4s]"></span>
                    </span>
                    <span>
                      {Array.from(typingUsers.values()).join(", ")} is typing...
                    </span>
                  </div>
                )}
                <div ref={messagesEndRef} />
              </div>

              {/* Message Input Box */}
              <div className="p-3 border-t border-gray-200 bg-white">
                <div className="flex items-end gap-2 bg-gray-50 p-2 rounded-xl border border-gray-200 focus-within:border-black focus-within:ring-1 focus-within:ring-black transition-all">
                  <textarea
                    value={inputMessage}
                    onChange={handleInputChange}
                    onKeyDown={(e) => {
                      if (e.key === "Enter" && !e.shiftKey) {
                        e.preventDefault();
                        handleSendMessage();
                      }
                    }}
                    placeholder="Type your message... (Enter to send, Shift+Enter for newline)"
                    className="flex-1 bg-transparent border-0 resize-none text-sm text-gray-900 focus:outline-hidden min-h-[40px] max-h-32 p-1"
                    rows={1}
                  />
                  <button
                    onClick={handleSendMessage}
                    disabled={!inputMessage.trim() || sending}
                    className="px-4 py-2 bg-black text-white text-xs font-bold rounded-lg hover:bg-gray-800 disabled:opacity-40 transition-colors flex items-center gap-1 shadow-sm"
                  >
                    <span>Send</span>
                    <span>↑</span>
                  </button>
                </div>
              </div>
            </>
          ) : (
            <div className="flex-1 flex flex-col items-center justify-center p-8 text-center text-gray-400 space-y-4">
              <div className="w-16 h-16 rounded-full bg-gray-100 flex items-center justify-center text-2xl">
                💬
              </div>
              <div>
                <h3 className="font-bold text-gray-900 text-base mb-1">
                  Human Communication Center
                </h3>
                <p className="text-xs text-gray-500 max-w-sm">
                  Select an existing conversation on the left, start a new direct chat, or initiate a WebRTC audio/video call with peers.
                </p>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* User Search & New Chat Modal */}
      {showSearchModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 backdrop-blur-xs">
          <div className="bg-white rounded-2xl w-full max-w-md shadow-2xl border border-gray-200 overflow-hidden">
            <div className="p-4 border-b border-gray-200 flex items-center justify-between">
              <h3 className="font-bold text-gray-900 text-sm">Start a Conversation</h3>
              <button
                onClick={() => setShowSearchModal(false)}
                className="text-gray-400 hover:text-gray-600 font-bold"
              >
                ✕
              </button>
            </div>
            <div className="p-4 space-y-3">
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Search candidates by name or email..."
                className="w-full px-3.5 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-sm focus:outline-hidden focus:border-black focus:ring-1 focus:ring-black"
                autoFocus
              />

              <div className="max-h-64 overflow-y-auto divide-y divide-gray-100">
                {searching ? (
                  <div className="p-4 text-center text-gray-400 text-xs animate-pulse">
                    Searching candidates...
                  </div>
                ) : searchResults.length === 0 ? (
                  <div className="p-6 text-center text-gray-400 text-xs">
                    {searchQuery.trim().length < 2
                      ? "Type at least 2 characters to search"
                      : "No candidates found"}
                  </div>
                ) : (
                  searchResults.map((u) => (
                    <div
                      key={u.id}
                      className="p-3 flex items-center justify-between hover:bg-gray-50 rounded-xl transition-colors"
                    >
                      <div className="flex items-center gap-2.5">
                        <div className="w-8 h-8 rounded-full bg-gray-200 text-gray-700 font-bold flex items-center justify-center text-xs uppercase">
                          {u.name.charAt(0)}
                        </div>
                        <div>
                          <div className="flex items-center gap-1.5">
                            <span className="text-sm font-semibold text-gray-900">
                              {u.name}
                            </span>
                            <span
                              className={`w-2 h-2 rounded-full ${
                                u.online ? "bg-emerald-500" : "bg-gray-400"
                              }`}
                            />
                          </div>
                          <span className="text-[11px] text-gray-500">
                            {u.careerGoal || "Software Engineer"} • {u.userLevel || "Beginner"}
                          </span>
                        </div>
                      </div>
                      <button
                        onClick={() => handleStartPeerChat(u.id)}
                        className="px-3 py-1.5 bg-black text-white text-xs font-bold rounded-lg hover:bg-gray-800 transition-colors"
                      >
                        Chat
                      </button>
                    </div>
                  ))
                )}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Support Ticket Modal */}
      {showSupportModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 backdrop-blur-xs">
          <div className="bg-white rounded-2xl w-full max-w-md shadow-2xl border border-gray-200 overflow-hidden">
            <div className="p-4 border-b border-gray-200 flex items-center justify-between">
              <h3 className="font-bold text-gray-900 text-sm flex items-center gap-1.5">
                <span>🛡️</span> Open Support Ticket
              </h3>
              <button
                onClick={() => setShowSupportModal(false)}
                className="text-gray-400 hover:text-gray-600 font-bold"
              >
                ✕
              </button>
            </div>
            <form onSubmit={handleCreateSupport} className="p-4 space-y-3">
              <div>
                <label className="block text-xs font-semibold text-gray-700 mb-1">
                  Subject / Topic
                </label>
                <input
                  type="text"
                  value={supportSubject}
                  onChange={(e) => setSupportSubject(e.target.value)}
                  placeholder="e.g. Mock interview evaluation question"
                  required
                  className="w-full px-3.5 py-2 bg-gray-50 border border-gray-200 rounded-xl text-sm focus:outline-hidden focus:border-black focus:ring-1 focus:ring-black"
                />
              </div>
              <div>
                <label className="block text-xs font-semibold text-gray-700 mb-1">
                  Initial Message (Optional)
                </label>
                <textarea
                  value={supportInitialMsg}
                  onChange={(e) => setSupportInitialMsg(e.target.value)}
                  placeholder="Explain your inquiry in detail..."
                  className="w-full px-3.5 py-2 bg-gray-50 border border-gray-200 rounded-xl text-sm focus:outline-hidden focus:border-black focus:ring-1 focus:ring-black resize-none h-24"
                />
              </div>
              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setShowSupportModal(false)}
                  className="px-4 py-2 text-xs font-semibold text-gray-600 hover:text-gray-800"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={creatingSupport || !supportSubject.trim()}
                  className="px-4 py-2 bg-emerald-600 text-white text-xs font-bold rounded-xl hover:bg-emerald-700 disabled:opacity-40 transition-colors"
                >
                  {creatingSupport ? "Creating..." : "Submit Ticket"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
