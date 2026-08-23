import { apiRequest } from "./apiClient";
import { API_ENDPOINTS } from "./config";

export interface ChatMessage {
  id: number;
  conversationId: number;
  senderType: "USER" | "AI" | "ADMIN" | "SYSTEM";
  senderName: string;
  content: string;
  sequenceNumber: number;
  status: string;
  createdAt: string;
}

export interface ConversationResponse {
  id: number;
  title: string;
  conversationType: string;
  archived: boolean;
  messageCount: number;
  createdAt: string;
  updatedAt: string;
  lastMessageAt: string;
  messages?: ChatMessage[];
}

export interface ChatResponse {
  conversationId: number;
  conversationTitle: string;
  userMessage: ChatMessage;
  aiMessage: ChatMessage;
  status: string;
  provider: string;
  model: string;
  tokensUsed: number;
  latencyMs: number;
}

/**
 * Creates a new persistent conversation with the AI Career Advisor.
 */
export async function createConversation(title?: string): Promise<ConversationResponse> {
  return apiRequest<ConversationResponse>(API_ENDPOINTS.CONVERSATIONS.BASE, {
    method: "POST",
    body: JSON.stringify(title ? { title } : {}),
  });
}

/**
 * Lists all conversations belonging to the authenticated user.
 */
export async function getConversations(): Promise<ConversationResponse[]> {
  return apiRequest<ConversationResponse[]>(API_ENDPOINTS.CONVERSATIONS.BASE);
}

/**
 * Retrieves a single conversation with its message history.
 */
export async function getConversation(id: number | string): Promise<ConversationResponse> {
  return apiRequest<ConversationResponse>(API_ENDPOINTS.CONVERSATIONS.DETAIL(id));
}

/**
 * Retrieves message history for a conversation.
 */
export async function getMessages(id: number | string): Promise<ChatMessage[]> {
  return apiRequest<ChatMessage[]>(API_ENDPOINTS.CONVERSATIONS.MESSAGES(id));
}

/**
 * Sends a message in a conversation and receives the AI response.
 */
export async function sendMessage(
  conversationId: number | string,
  content: string
): Promise<ChatResponse> {
  return apiRequest<ChatResponse>(API_ENDPOINTS.CONVERSATIONS.MESSAGES(conversationId), {
    method: "POST",
    body: JSON.stringify({ content }),
  });
}

/**
 * Archives a conversation.
 */
export async function archiveConversation(
  id: number | string
): Promise<ConversationResponse> {
  return apiRequest<ConversationResponse>(API_ENDPOINTS.CONVERSATIONS.ARCHIVE(id), {
    method: "POST",
  });
}

/**
 * Deletes a conversation and its messages.
 */
export async function deleteConversation(
  id: number | string
): Promise<{ message: string }> {
  return apiRequest<{ message: string }>(API_ENDPOINTS.CONVERSATIONS.DETAIL(id), {
    method: "DELETE",
  });
}
