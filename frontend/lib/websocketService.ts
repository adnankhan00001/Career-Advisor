import { WS_BASE_URL } from './config';
import { RealTimeEvent } from './humanChatService';

type EventCallback = (event: any) => void;

class WebSocketService {
  private socket: WebSocket | null = null;
  private connected = false;
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 10;
  private reconnectTimer: any = null;
  private subscriptions = new Map<string, Set<EventCallback>>();
  private subIdCounter = 1;
  private destinationSubIds = new Map<string, string>();

  public connect(): void {
    if (typeof window === 'undefined') return;
    if (this.socket && (this.socket.readyState === WebSocket.OPEN || this.socket.readyState === WebSocket.CONNECTING)) {
      return;
    }

    const token = localStorage.getItem('token');
    if (!token) {
      return;
    }

    try {
      // Create native WebSocket connection to /ws endpoint
      const wsUrl = WS_BASE_URL.replace(/^http/, 'ws');
      this.socket = new WebSocket(wsUrl);

      this.socket.onopen = () => {
        // Send STOMP CONNECT frame with JWT in headers
        const connectFrame = 
          "CONNECT\n" +
          "accept-version:1.2,1.1,1.0\n" +
          "heart-beat:10000,10000\n" +
          `Authorization:Bearer ${token}\n` +
          `token:${token}\n` +
          "\n\0";
        this.socket?.send(connectFrame);
      };

      this.socket.onmessage = (event) => {
        this.handleStompMessage(event.data);
      };

      this.socket.onclose = () => {
        this.connected = false;
        this.scheduleReconnect();
      };

      this.socket.onerror = () => {
        this.connected = false;
      };
    } catch {
      this.scheduleReconnect();
    }
  }

  private scheduleReconnect(): void {
    if (this.reconnectTimer) clearTimeout(this.reconnectTimer);
    if (this.reconnectAttempts >= this.maxReconnectAttempts) return;

    const delay = Math.min(1000 * Math.pow(1.5, this.reconnectAttempts), 15000);
    this.reconnectAttempts++;
    this.reconnectTimer = setTimeout(() => {
      this.connect();
    }, delay);
  }

  private handleStompMessage(raw: string): void {
    if (!raw || typeof raw !== 'string') return;

    // Handle heartbeats
    if (raw === '\n' || raw === '\r\n') return;

    const frames = raw.split('\0');
    for (const frame of frames) {
      if (!frame.trim()) continue;

      const lines = frame.split('\n');
      const command = lines[0]?.trim();

      if (command === 'CONNECTED') {
        this.connected = true;
        this.reconnectAttempts = 0;
        // Resubscribe active destinations
        for (const destination of this.subscriptions.keys()) {
          this.sendSubscribeFrame(destination);
        }
      } else if (command === 'MESSAGE') {
        let destination = '';
        let bodyIndex = -1;

        for (let i = 1; i < lines.length; i++) {
          const line = lines[i];
          if (line.startsWith('destination:')) {
            destination = line.substring('destination:'.length).trim();
          } else if (line.trim() === '' && bodyIndex === -1) {
            bodyIndex = i + 1;
            break;
          }
        }

        if (bodyIndex !== -1 && destination) {
          const body = lines.slice(bodyIndex).join('\n').trim();
          try {
            const eventPayload: RealTimeEvent = JSON.parse(body);
            const callbacks = this.subscriptions.get(destination);
            if (callbacks) {
              callbacks.forEach(cb => {
                try { cb(eventPayload); } catch { /* ignore */ }
              });
            }
          } catch {
            // Ignore parse errors
          }
        }
      }
    }
  }

  private sendSubscribeFrame(destination: string): void {
    if (!this.connected || !this.socket || this.socket.readyState !== WebSocket.OPEN) return;

    let subId = this.destinationSubIds.get(destination);
    if (!subId) {
      subId = `sub-${this.subIdCounter++}`;
      this.destinationSubIds.set(destination, subId);
    }

    const subFrame =
      "SUBSCRIBE\n" +
      `id:${subId}\n` +
      `destination:${destination}\n` +
      "\n\0";
    this.socket.send(subFrame);
  }

  public subscribe(destination: string, callback: EventCallback): () => void {
    if (!this.subscriptions.has(destination)) {
      this.subscriptions.set(destination, new Set());
      if (this.connected) {
        this.sendSubscribeFrame(destination);
      }
    }

    this.subscriptions.get(destination)!.add(callback);

    if (!this.connected) {
      this.connect();
    }

    return () => {
      const set = this.subscriptions.get(destination);
      if (set) {
        set.delete(callback);
        if (set.size === 0) {
          this.subscriptions.delete(destination);
          const subId = this.destinationSubIds.get(destination);
          if (subId && this.connected && this.socket?.readyState === WebSocket.OPEN) {
            const unsubFrame = `UNSUBSCRIBE\nid:${subId}\n\n\0`;
            this.socket.send(unsubFrame);
            this.destinationSubIds.delete(destination);
          }
        }
      }
    };
  }

  public isConnected(): boolean {
    return this.connected;
  }

  public disconnect(): void {
    if (this.reconnectTimer) clearTimeout(this.reconnectTimer);
    if (this.socket) {
      try {
        if (this.connected) {
          this.socket.send("DISCONNECT\n\n\0");
        }
        this.socket.close();
      } catch {
        // ignore
      }
      this.socket = null;
    }
    this.connected = false;
    this.subscriptions.clear();
    this.destinationSubIds.clear();
  }
}

export const wsService = new WebSocketService();
