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
      const data = await humanChatService.getAdminInbox();
      setInbox(data);
      if (data.length > 0 && activeConvId === null) {
        setActiveConvId(data[0].id);
      }
    } catch {
      // Error handling
    } finally {
      setLoading(false);
    }
  }, [activeConvId]);

  useEffect(() => {
    if (user?.role === "ADMIN") {
      loadInbox();
    }
  }, [user, loadInbox]);

  // Connect to STOMP WebSocket
  useEffect(() => {
    wsService.connect();
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
      } catch {
        // Error handling
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
    } catch {
      // Error handling
    } finally {
      setSending(false);
    }
  };

  if (user?.role !== "ADMIN") {
    return (
      <div className="p-8 text-center bg-white rounded-2xl border border-gray-200 shadow-sm">
        <h2 className="text-lg font-bold text-red-600 mb-2">Access Denied</h2>
        <p className="text-sm text-gray-500">
          Only platform administrators can access the support inbox.
        </p>
      </div>
    );
  }

  return (
    <div className="h-[calc(100vh-8.5rem)] flex flex-col bg-white rounded-2xl border border-gray-200 overflow-hidden shadow-sm relative">
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
          <div className="p-4 border-b border-gray-200 bg-white flex items-center justify-between">
            <h2 className="font-bold text-gray-900 text-lg flex items-center gap-2">
              <span>🛡️</span> Support Inbox
            </h2>
            <span className="px-2 py-0.5 text-xs font-bold bg-emerald-100 text-emerald-800 rounded-full">
              {inbox.length} Tickets
            </span>
          </div>

          <div className="flex-1 overflow-y-auto divide-y divide-gray-100">
            {loading ? (
              <div className="p-8 text-center text-gray-400 text-sm animate-pulse">
                Loading support tickets...
              </div>
            ) : inbox.length === 0 ? (
              <div className="p-8 text-center space-y-3">
                <div className="w-12 h-12 rounded-full bg-gray-100 flex items-center justify-center mx-auto text-xl">
                  🛡️
                </div>
                <p className="text-sm font-semibold text-gray-700">No support tickets</p>
                <p className="text-xs text-gray-500">
                  All candidate support inquiries are currently resolved.
                </p>
              </div>
            ) : (
              inbox.map((c) => {
                const isSelected = c.id === activeConvId;
                const candidate = c.participants?.find((p) => p.role !== "ADMIN");

                return (
                  <button
                    key={c.id}
                    onClick={() => setActiveConvId(c.id)}
                    className={`w-full text-left p-4 transition-colors flex items-start gap-3 relative ${
                      isSelected
                        ? "bg-white border-l-4 border-emerald-600 shadow-xs"
                        : "hover:bg-gray-100/70"
                    }`}
                  >
                    <div className="w-10 h-10 rounded-full bg-emerald-100 text-emerald-800 font-bold flex items-center justify-center text-sm border border-emerald-200 flex-shrink-0">
                      {candidate?.userName?.charAt(0) || "U"}
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center justify-between mb-0.5">
                        <h4 className="text-sm font-semibold text-gray-900 truncate">
                          {c.title}
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
                      <p className="text-xs text-gray-500 truncate">
                        Candidate: {candidate?.userName || "User"}
                      </p>
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
              {/* Header with Calling Controls */}
              <div className="p-4 border-b border-gray-200 flex items-center justify-between bg-white/90 backdrop-blur-xs">
                <div>
                  <h3 className="font-bold text-gray-900 text-sm flex items-center gap-2">
                    {activeConv.title}
                    <span className="px-2 py-0.5 text-[10px] font-semibold bg-emerald-100 text-emerald-800 rounded-full">
                      Support Ticket #{activeConv.id}
                    </span>
                  </h3>
                  <p className="text-xs text-gray-500 mt-0.5">
                    Candidate:{" "}
                    {activeConv.participants?.find((p) => p.role !== "ADMIN")?.userName ||
                      "User"}
                  </p>
                </div>

                <div className="flex items-center gap-2">
                  <button
                    onClick={() => handleStartCall("AUDIO")}
                    className="px-3 py-1.5 text-xs font-semibold bg-gray-100 text-gray-800 hover:bg-gray-200 rounded-xl transition-colors flex items-center gap-1.5 border border-gray-200 shadow-xs"
                    title="Start Voice Call with Candidate"
                  >
                    <span>📞</span> Audio
                  </button>
                  <button
                    onClick={() => handleStartCall("VIDEO")}
                    className="px-3 py-1.5 text-xs font-semibold bg-black text-white hover:bg-gray-800 rounded-xl transition-colors flex items-center gap-1.5 shadow-xs"
                    title="Start Video Call with Candidate"
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
                  <div className="text-center py-12 text-gray-400">
                    <p className="text-sm font-semibold">No messages yet</p>
                  </div>
                ) : (
                  messages.map((m) => {
                    const isMe = m.senderName === user?.name || m.senderType === "ADMIN";
                    return (
                      <div
                        key={m.id}
                        className={`flex flex-col ${
                          isMe ? "items-end" : "items-start"
                        }`}
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
                          className={`max-w-[75%] rounded-2xl px-4 py-2.5 text-sm shadow-xs whitespace-pre-wrap leading-relaxed ${
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

              {/* Reply Box */}
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
                    placeholder="Reply to candidate support ticket..."
                    className="flex-1 bg-transparent border-0 resize-none text-sm text-gray-900 focus:outline-hidden min-h-[40px] max-h-32 p-1"
                    rows={1}
                  />
                  <button
                    onClick={handleSendMessage}
                    disabled={!inputMessage.trim() || sending}
                    className="px-4 py-2 bg-emerald-600 text-white text-xs font-bold rounded-lg hover:bg-emerald-700 disabled:opacity-40 transition-colors flex items-center gap-1 shadow-sm"
                  >
                    <span>Reply</span>
                    <span>↑</span>
                  </button>
                </div>
              </div>
            </>
          ) : (
            <div className="flex-1 flex flex-col items-center justify-center p-8 text-center text-gray-400 space-y-4">
              <div className="w-16 h-16 rounded-full bg-gray-100 flex items-center justify-center text-2xl">
                🛡️
              </div>
              <p className="text-sm font-semibold text-gray-700">Support Ticket Console</p>
              <p className="text-xs text-gray-500 max-w-sm">
                Select a support ticket from the inbox to reply to candidates or initiate voice/video support.
              </p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
