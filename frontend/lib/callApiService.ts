import { apiRequest } from './apiClient';
import { API_ENDPOINTS } from './config';

export type CallType = 'AUDIO' | 'VIDEO';
export type CallStatus = 'RINGING' | 'ACCEPTED' | 'REJECTED' | 'MISSED' | 'CANCELLED' | 'ENDED' | 'FAILED' | 'EXPIRED';
export type EndReason = 'USER_ENDED' | 'REMOTE_ENDED' | 'REJECTED' | 'TIMEOUT' | 'NETWORK_FAILURE' | 'PERMISSION_DENIED' | 'BUSY' | 'UNKNOWN';

export interface CallSession {
  id: number;
  conversationId: number;
  callerId: number;
  callerName: string;
  receiverId: number;
  receiverName: string;
  callType: CallType;
  status: CallStatus;
  startedAt: string;
  answeredAt?: string;
  endedAt?: string;
  durationSeconds: number;
  endReason?: EndReason;
  createdAt: string;
}

export interface WebRtcSignal {
  type: string;
  callId?: number;
  conversationId?: number;
  senderId?: number;
  senderName?: string;
  targetUserId?: number;
  callType?: CallType;
  sdp?: string;
  candidate?: any;
  endReason?: EndReason;
  durationSeconds?: number;
  timestamp?: string;
}

export const callApiService = {
  async initiateCall(conversationId: number, callType: CallType): Promise<CallSession> {
    return apiRequest<CallSession>(API_ENDPOINTS.CALLS.BASE, {
      method: 'POST',
      body: JSON.stringify({ conversationId, callType }),
    });
  },

  async getCall(id: number | string): Promise<CallSession> {
    return apiRequest<CallSession>(API_ENDPOINTS.CALLS.DETAIL(id));
  },

  async acceptCall(id: number | string): Promise<CallSession> {
    return apiRequest<CallSession>(API_ENDPOINTS.CALLS.ACCEPT(id), {
      method: 'POST',
    });
  },

  async rejectCall(id: number | string, reason?: EndReason): Promise<CallSession> {
    return apiRequest<CallSession>(API_ENDPOINTS.CALLS.REJECT(id), {
      method: 'POST',
      body: JSON.stringify({ reason: reason || 'REJECTED' }),
    });
  },

  async cancelCall(id: number | string): Promise<CallSession> {
    return apiRequest<CallSession>(API_ENDPOINTS.CALLS.CANCEL(id), {
      method: 'POST',
    });
  },

  async endCall(id: number | string, reason?: EndReason): Promise<CallSession> {
    return apiRequest<CallSession>(API_ENDPOINTS.CALLS.END(id), {
      method: 'POST',
      body: JSON.stringify({ reason: reason || 'USER_ENDED' }),
    });
  },

  async sendSignal(id: number | string, signal: WebRtcSignal): Promise<void> {
    try {
      await apiRequest(API_ENDPOINTS.CALLS.SIGNAL(id), {
        method: 'POST',
        body: JSON.stringify(signal),
      });
    } catch {
      // Non-blocking
    }
  },

  async getCallHistory(): Promise<CallSession[]> {
    return apiRequest<CallSession[]>(API_ENDPOINTS.CALLS.HISTORY);
  },

  async getActiveCalls(): Promise<CallSession[]> {
    return apiRequest<CallSession[]>(API_ENDPOINTS.CALLS.ACTIVE);
  },
};
