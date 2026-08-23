import { apiRequest } from './apiClient';
import { API_ENDPOINTS } from './config';
import { ChatMessage, ConversationResponse } from './chatService';

export interface UserSearchItem {
  id: number;
  name: string;
  careerGoal?: string;
  userLevel?: string;
  role: 'USER' | 'ADMIN';
  online: boolean;
}

export interface ConversationParticipant {
  id: number;
  userId: number;
  userName: string;
  userEmail?: string;
  role: 'CREATOR' | 'MEMBER' | 'ADMIN';
  online: boolean;
  joinedAt: string;
  lastReadAt?: string;
}

export interface HumanConversation extends ConversationResponse {
  unreadCount?: number;
  participants?: ConversationParticipant[];
}

export interface RealTimeEvent {
  type: 'MESSAGE_SENT' | 'MESSAGE_DELIVERED' | 'MESSAGE_READ' | 'TYPING_STARTED' | 'TYPING_STOPPED' | 'USER_ONLINE' | 'USER_OFFLINE';
  conversationId?: number;
  messageId?: number;
  senderId?: number;
  senderName?: string;
  senderRole?: string;
  content?: string;
  status?: string;
  sequenceNumber?: number;
  timestamp?: string;
}

export const humanChatService = {
  async searchUsers(query: string): Promise<UserSearchItem[]> {
    if (!query || query.trim().length < 2) return [];
    return apiRequest<UserSearchItem[]>(`${API_ENDPOINTS.USERS.SEARCH}?q=${encodeURIComponent(query.trim())}`);
  },

  async startUserConversation(targetUserId: number, initialMessage?: string): Promise<HumanConversation> {
    return apiRequest<HumanConversation>(API_ENDPOINTS.CONVERSATIONS.USER, {
      method: 'POST',
      body: JSON.stringify({ targetUserId, initialMessage }),
    });
  },

  async startAdminConversation(subject: string, initialMessage?: string): Promise<HumanConversation> {
    return apiRequest<HumanConversation>(API_ENDPOINTS.CONVERSATIONS.ADMIN, {
      method: 'POST',
      body: JSON.stringify({ subject, initialMessage }),
    });
  },

  async getHumanConversations(): Promise<HumanConversation[]> {
    return apiRequest<HumanConversation[]>(API_ENDPOINTS.CONVERSATIONS.HUMAN_LIST);
  },

  async getHumanConversation(id: number | string): Promise<HumanConversation> {
    return apiRequest<HumanConversation>(API_ENDPOINTS.CONVERSATIONS.HUMAN_DETAIL(id));
  },

  async sendHumanMessage(id: number | string, content: string): Promise<ChatMessage> {
    return apiRequest<ChatMessage>(API_ENDPOINTS.CONVERSATIONS.HUMAN_MESSAGES(id), {
      method: 'POST',
      body: JSON.stringify({ content }),
    });
  },

  async markAsRead(id: number | string): Promise<void> {
    try {
      await apiRequest(API_ENDPOINTS.CONVERSATIONS.READ(id), {
        method: 'POST',
      });
    } catch {
      // Non-blocking
    }
  },

  async sendTyping(id: number | string, typing: boolean): Promise<void> {
    try {
      await apiRequest(API_ENDPOINTS.CONVERSATIONS.TYPING(id), {
        method: 'POST',
        body: JSON.stringify({ typing }),
      });
    } catch {
      // Non-blocking
    }
  },

  async getAdminInbox(): Promise<HumanConversation[]> {
    return apiRequest<HumanConversation[]>(API_ENDPOINTS.CONVERSATIONS.ADMIN_INBOX);
  },
};
