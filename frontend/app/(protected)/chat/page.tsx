"use client";

import { useState, useEffect, useRef } from "react";
import {
  ConversationResponse,
  ChatMessage,
  getConversations,
  getConversation,
  createConversation,
  sendMessage,
  archiveConversation,
  deleteConversation,
} from "@/lib/chatService";
import { useAuth } from "@/context/AuthContext";

const SUGGESTIONS = [
  {
    icon: "🎯",
    title: "Assess My Readiness",
    prompt: "Analyze my verified skills and quiz scores to tell me how job-ready I am.",
  },
  {
    icon: "🗺️",
    title: "Next Learning Step",
    prompt: "What should be my next milestone on my career roadmap?",
  },
  {
    icon: "💡",
    title: "DSA Practice Focus",
    prompt: "Based on my solved problems, which Data Structures topic should I practice next?",
  },
  {
    icon: "📄",
    title: "Resume Skill Gaps",
    prompt: "What key skills are missing between my resume and my target career goal?",
  },
  {
    icon: "⚡",
    title: "Mock Interview Strategy",
    prompt: "How can I improve on my weak areas from recent technical mock interviews?",
  },
];

export default function ChatPage() {
  const { user } = useAuth();

  const [conversations, setConversations] = useState<ConversationResponse[]>([]);
  const [activeConversationId, setActiveConversationId] = useState<number | null>(null);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [inputText, setInputText] = useState("");
  const [isLoadingConversations, setIsLoadingConversations] = useState(true);
  const [isLoadingMessages, setIsLoadingMessages] = useState(false);
  const [isSending, setIsSending] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [sidebarOpen, setSidebarOpen] = useState(false);

  const messagesEndRef = useRef<HTMLDivElement>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  // Auto-scroll to bottom of messages
  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages, isSending]);

  // Load conversation list on mount
  useEffect(() => {
    loadConversations();
  }, []);

  const loadConversations = async (preferredSelectId?: number) => {
    try {
      setIsLoadingConversations(true);
      setErrorMessage(null);
      const list = await getConversations();
      setConversations(list);

      if (list.length > 0) {
        const targetId =
          preferredSelectId && list.some((c) => c.id === preferredSelectId)
            ? preferredSelectId
            : list[0].id;
        setActiveConversationId(targetId);
        loadMessages(targetId);
      } else {
        // Automatically create a first conversation if none exist
        handleCreateNewChat();
      }
    } catch (err: any) {
      setErrorMessage(err.message || "Failed to load conversations.");
    } finally {
      setIsLoadingConversations(false);
    }
  };

  const loadMessages = async (convId: number) => {
    try {
      setIsLoadingMessages(true);
      setErrorMessage(null);
      const conv = await getConversation(convId);
      setMessages(conv.messages || []);
    } catch (err: any) {
      setErrorMessage(err.message || "Failed to load messages.");
    } finally {
      setIsLoadingMessages(false);
    }
  };

  const handleSelectConversation = (id: number) => {
    if (id === activeConversationId) return;
    setActiveConversationId(id);
    setSidebarOpen(false);
    loadMessages(id);
  };

  const handleCreateNewChat = async () => {
    try {
      setIsSending(true);
      setErrorMessage(null);
      const newConv = await createConversation();
      setConversations((prev) => [newConv, ...prev]);
      setActiveConversationId(newConv.id);
      setMessages([]);
      setSidebarOpen(false);
      if (textareaRef.current) {
        textareaRef.current.focus();
      }
    } catch (err: any) {
      setErrorMessage(err.message || "Failed to create new conversation.");
    } finally {
      setIsSending(false);
    }
  };

  const handleSendMessage = async (textToSend?: string) => {
    const text = (textToSend || inputText).trim();
    if (!text || isSending) return;

    if (!activeConversationId) {
      try {
        const newConv = await createConversation();
        setConversations((prev) => [newConv, ...prev]);
        setActiveConversationId(newConv.id);
        executeSend(newConv.id, text);
      } catch (err: any) {
        setErrorMessage("Failed to initialize conversation.");
      }
      return;
    }

    executeSend(activeConversationId, text);
  };

  const executeSend = async (convId: number, text: string) => {
    // Optimistic user message preview
    const tempUserMessage: ChatMessage = {
      id: Date.now(),
      conversationId: convId,
      senderType: "USER",
      senderName: user?.name || "User",
      content: text,
      sequenceNumber: messages.length + 1,
      status: "SENT",
      createdAt: new Date().toISOString(),
    };

    setMessages((prev) => [...prev, tempUserMessage]);
    setInputText("");
    setIsSending(true);
    setErrorMessage(null);

    try {
      const response = await sendMessage(convId, text);

      // Replace optimistic message and append real AI response
      setMessages((prev) => {
        const filtered = prev.filter((m) => m.id !== tempUserMessage.id);
        return [...filtered, response.userMessage, response.aiMessage];
      });

      // Update active conversation title in sidebar if changed
      setConversations((prev) =>
        prev.map((c) =>
          c.id === convId
            ? {
                ...c,
                title: response.conversationTitle || c.title,
                lastMessageAt: new Date().toISOString(),
                updatedAt: new Date().toISOString(),
                messageCount: c.messageCount + 2,
              }
            : c
        )
      );
    } catch (err: any) {
      setErrorMessage(err.message || "Unable to receive AI response. Please try again.");
    } finally {
      setIsSending(false);
    }
  };

  const handleArchive = async (e: React.MouseEvent, convId: number) => {
    e.stopPropagation();
    try {
      await archiveConversation(convId);
      loadConversations();
    } catch (err: any) {
      setErrorMessage(err.message || "Failed to archive conversation.");
    }
  };

  const handleDelete = async (e: React.MouseEvent, convId: number) => {
    e.stopPropagation();
    if (!confirm("Are you sure you want to delete this conversation?")) return;

    try {
      await deleteConversation(convId);
      const remaining = conversations.filter((c) => c.id !== convId);
      setConversations(remaining);

      if (activeConversationId === convId) {
        if (remaining.length > 0) {
          setActiveConversationId(remaining[0].id);
          loadMessages(remaining[0].id);
        } else {
          handleCreateNewChat();
        }
      }
    } catch (err: any) {
      setErrorMessage(err.message || "Failed to delete conversation.");
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      handleSendMessage();
    }
  };

  const activeConversation = conversations.find((c) => c.id === activeConversationId);

  const formatTimestamp = (dateStr: string) => {
    try {
      const d = new Date(dateStr);
      const now = new Date();
      const diffMin = Math.floor((now.getTime() - d.getTime()) / 60000);

      if (diffMin < 1) return "Just now";
      if (diffMin < 60) return `${diffMin}m ago`;
      if (diffMin < 1440) return `${Math.floor(diffMin / 60)}h ago`;
      return d.toLocaleDateString([], { month: "short", day: "numeric" });
    } catch {
      return "";
    }
  };

  return (
    <div className="flex h-[calc(100vh-8rem)] bg-white rounded-2xl border border-gray-200 shadow-xs overflow-hidden">
      {/* Mobile Sidebar Overlay */}
      {sidebarOpen && (
        <div
          className="fixed inset-0 z-30 bg-black/50 md:hidden"
          onClick={() => setSidebarOpen(false)}
        />
      )}

      {/* Conversations Sidebar */}
      <aside
        className={`fixed inset-y-0 left-0 z-40 w-72 bg-gray-50 border-r border-gray-200 flex flex-col transition-transform duration-200 ease-in-out md:static md:translate-x-0 ${
          sidebarOpen ? "translate-x-0" : "-translate-x-full"
        }`}
      >
        {/* Sidebar Header */}
        <div className="p-4 border-b border-gray-200 flex items-center justify-between gap-2">
          <button
            onClick={handleCreateNewChat}
            disabled={isSending}
            className="flex-1 flex items-center justify-center gap-2 py-2.5 px-4 bg-blue-600 hover:bg-blue-700 text-white font-semibold text-sm rounded-xl transition shadow-xs cursor-pointer disabled:opacity-50"
          >
            <span>✨</span>
            <span>New Chat</span>
          </button>

          <button
            onClick={() => setSidebarOpen(false)}
            className="md:hidden text-gray-500 hover:text-gray-900 p-2 cursor-pointer"
          >
            ✕
          </button>
        </div>

        {/* Conversation List */}
        <div className="flex-1 overflow-y-auto p-3 space-y-1">
          {isLoadingConversations ? (
            <div className="flex items-center justify-center h-32 text-gray-400 text-xs">
              <div className="w-5 h-5 border-2 border-blue-600 border-t-transparent rounded-full animate-spin mr-2"></div>
              Loading chats...
            </div>
          ) : conversations.length === 0 ? (
            <div className="text-center py-8 text-xs text-gray-400">
              No conversations yet.
            </div>
          ) : (
            conversations.map((conv) => {
              const isActive = conv.id === activeConversationId;
              return (
                <div
                  key={conv.id}
                  onClick={() => handleSelectConversation(conv.id)}
                  className={`group relative flex items-center justify-between p-3 rounded-xl cursor-pointer transition text-left ${
                    isActive
                      ? "bg-blue-50 border border-blue-200 text-blue-900 shadow-xs"
                      : "hover:bg-gray-100 text-gray-700 border border-transparent"
                  }`}
                >
                  <div className="flex-1 min-w-0 pr-2">
                    <div className="flex items-center gap-1.5 mb-1">
                      <span className="text-sm">💬</span>
                      <p
                        className={`text-xs font-semibold truncate ${
                          isActive ? "text-blue-950 font-bold" : "text-gray-900"
                        }`}
                      >
                        {conv.title || "New AI Conversation"}
                      </p>
                    </div>
                    <div className="flex items-center gap-2 text-[11px] text-gray-400">
                      <span>{formatTimestamp(conv.lastMessageAt || conv.updatedAt)}</span>
                      {conv.messageCount > 0 && (
                        <span>• {conv.messageCount} msgs</span>
                      )}
                    </div>
                  </div>

                  {/* Actions (Delete/Archive) */}
                  <div className="opacity-0 group-hover:opacity-100 transition flex items-center gap-1">
                    <button
                      onClick={(e) => handleDelete(e, conv.id)}
                      title="Delete chat"
                      className="p-1 rounded text-gray-400 hover:text-red-600 hover:bg-red-50 transition text-xs cursor-pointer"
                    >
                      🗑️
                    </button>
                  </div>
                </div>
              );
            })
          )}
        </div>

        {/* Sidebar Footer info */}
        <div className="p-3 border-t border-gray-200 bg-white/50 text-[11px] text-gray-400 flex items-center justify-between">
          <span>Career Advisor AI v14</span>
          <span className="w-2 h-2 rounded-full bg-green-500"></span>
        </div>
      </aside>

      {/* Main Chat Workspace */}
      <div className="flex-1 flex flex-col min-w-0 bg-white">
        {/* Workspace Top Bar */}
        <div className="px-5 py-3.5 border-b border-gray-200 flex items-center justify-between bg-white/80 backdrop-blur-xs">
          <div className="flex items-center gap-3 min-w-0">
            <button
              onClick={() => setSidebarOpen(true)}
              className="md:hidden p-1.5 text-gray-600 hover:bg-gray-100 rounded-lg cursor-pointer"
              aria-label="Open chats sidebar"
            >
              ☰
            </button>

            <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-blue-600 to-indigo-700 text-white flex items-center justify-center font-bold text-base shadow-xs shrink-0">
              🤖
            </div>

            <div className="min-w-0">
              <h2 className="text-sm font-bold text-gray-900 truncate">
                {activeConversation?.title || "OneStop AI Career Advisor"}
              </h2>
              <div className="flex items-center gap-2 text-[11px] text-gray-500">
                <span className="inline-flex items-center gap-1 font-medium text-emerald-600">
                  <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse"></span>
                  Grounded in Your Profile
                </span>
                <span>•</span>
                <span>Target: {user?.careerGoal || "Software Engineering"}</span>
              </div>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={handleCreateNewChat}
              title="Start a new chat"
              className="hidden sm:inline-flex items-center gap-1 text-xs font-semibold text-gray-700 hover:text-blue-600 bg-gray-50 hover:bg-gray-100 border border-gray-200 px-3 py-1.5 rounded-lg transition cursor-pointer"
            >
              <span>+ New Chat</span>
            </button>
          </div>
        </div>

        {/* Error Banner */}
        {errorMessage && (
          <div className="bg-red-50 border-b border-red-200 px-4 py-2.5 text-xs text-red-700 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <span>⚠️</span>
              <span>{errorMessage}</span>
            </div>
            <button
              onClick={() => setErrorMessage(null)}
              className="text-red-500 hover:text-red-800 font-bold p-1 cursor-pointer"
            >
              ✕
            </button>
          </div>
        )}

        {/* Message Stream */}
        <div className="flex-1 overflow-y-auto p-4 sm:p-6 space-y-4">
          {isLoadingMessages ? (
            <div className="flex flex-col items-center justify-center h-64 text-gray-400 gap-2">
              <div className="w-6 h-6 border-2 border-blue-600 border-t-transparent rounded-full animate-spin"></div>
              <p className="text-xs">Loading conversation history...</p>
            </div>
          ) : messages.length === 0 ? (
            /* Empty State with Suggestions */
            <div className="max-w-2xl mx-auto py-8 text-center space-y-6">
              <div className="w-16 h-16 rounded-2xl bg-blue-50 border border-blue-100 text-3xl flex items-center justify-center mx-auto shadow-xs">
                🎓
              </div>

              <div>
                <h3 className="text-lg font-extrabold text-gray-900">
                  Welcome to Your AI Career Advisor
                </h3>
                <p className="text-xs text-gray-500 mt-1 max-w-md mx-auto">
                  I have full context of your verified skills, roadmap progress, solved DSA problems, mock interviews, and resume gaps. Ask me anything!
                </p>
              </div>

              {/* Suggestion Cards */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5 text-left pt-2">
                {SUGGESTIONS.map((item, idx) => (
                  <button
                    key={idx}
                    onClick={() => handleSendMessage(item.prompt)}
                    className="p-3.5 rounded-xl border border-gray-200 bg-white hover:bg-blue-50/50 hover:border-blue-300 transition shadow-2xs group cursor-pointer text-left"
                  >
                    <div className="flex items-center gap-2 font-bold text-xs text-gray-900 group-hover:text-blue-700">
                      <span>{item.icon}</span>
                      <span>{item.title}</span>
                    </div>
                    <p className="text-[11px] text-gray-500 mt-1 line-clamp-2">
                      {item.prompt}
                    </p>
                  </button>
                ))}
              </div>
            </div>
          ) : (
            /* Messages List */
            messages.map((msg, index) => {
              const isUser = msg.senderType === "USER";
              return (
                <div
                  key={msg.id || index}
                  className={`flex gap-3 ${isUser ? "justify-end" : "justify-start"}`}
                >
                  {!isUser && (
                    <div className="w-8 h-8 rounded-xl bg-gradient-to-br from-blue-600 to-indigo-700 text-white flex items-center justify-center font-bold text-sm shrink-0 shadow-xs">
                      🤖
                    </div>
                  )}

                  <div
                    className={`max-w-xl rounded-2xl px-4 py-3 text-sm leading-relaxed shadow-2xs ${
                      isUser
                        ? "bg-blue-600 text-white rounded-br-xs"
                        : "bg-gray-100 text-gray-900 border border-gray-200 rounded-bl-xs"
                    }`}
                  >
                    <div className="flex items-center justify-between gap-3 mb-1 text-[10px] opacity-75">
                      <span className="font-semibold">
                        {isUser ? "You" : "OneStop AI Advisor"}
                      </span>
                      <span>{formatTimestamp(msg.createdAt)}</span>
                    </div>

                    <div className="whitespace-pre-wrap break-words text-xs sm:text-sm">
                      {msg.content}
                    </div>
                  </div>

                  {isUser && (
                    <div className="w-8 h-8 rounded-xl bg-black text-white flex items-center justify-center font-bold text-xs shrink-0 shadow-xs">
                      {user?.name?.charAt(0).toUpperCase() || "U"}
                    </div>
                  )}
                </div>
              );
            })
          )}

          {/* Thinking Indicator */}
          {isSending && (
            <div className="flex gap-3 justify-start items-center">
              <div className="w-8 h-8 rounded-xl bg-gradient-to-br from-blue-600 to-indigo-700 text-white flex items-center justify-center font-bold text-sm shrink-0 shadow-xs">
                🤖
              </div>
              <div className="bg-gray-100 border border-gray-200 rounded-2xl rounded-bl-xs px-4 py-3 flex items-center gap-2">
                <div className="w-2 h-2 rounded-full bg-blue-600 animate-bounce"></div>
                <div className="w-2 h-2 rounded-full bg-blue-600 animate-bounce [animation-delay:0.2s]"></div>
                <div className="w-2 h-2 rounded-full bg-blue-600 animate-bounce [animation-delay:0.4s]"></div>
                <span className="text-xs text-gray-500 font-medium ml-1">
                  Synthesizing personalized advice...
                </span>
              </div>
            </div>
          )}

          <div ref={messagesEndRef} />
        </div>

        {/* Input Bar */}
        <div className="p-4 border-t border-gray-200 bg-gray-50/50">
          <div className="flex gap-2 items-end max-w-4xl mx-auto bg-white rounded-2xl border border-gray-300 p-2 focus-within:border-blue-500 focus-within:ring-2 focus-within:ring-blue-100 transition shadow-xs">
            <textarea
              ref={textareaRef}
              value={inputText}
              onChange={(e) => setInputText(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="Ask about your roadmaps, skill gaps, DSA practice, or mock interviews... (Enter to send)"
              rows={2}
              disabled={isSending}
              className="flex-1 bg-transparent border-0 resize-none text-xs sm:text-sm text-gray-900 placeholder:text-gray-400 focus:outline-hidden p-1.5"
            />

            <button
              onClick={() => handleSendMessage()}
              disabled={!inputText.trim() || isSending}
              className="px-4 py-2.5 bg-blue-600 hover:bg-blue-700 text-white text-xs font-bold rounded-xl transition shadow-xs cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed flex items-center gap-1.5"
            >
              <span>Send</span>
              <span>🚀</span>
            </button>
          </div>

          <div className="flex items-center justify-between text-[11px] text-gray-400 max-w-4xl mx-auto mt-2 px-1">
            <span>Shift + Enter for new line</span>
            <span>Career Advisor AI • Grounded in your portfolio</span>
          </div>
        </div>
      </div>
    </div>
  );
}
