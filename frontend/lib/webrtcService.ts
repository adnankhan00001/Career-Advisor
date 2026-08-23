import { WEBRTC_CONFIG } from './config';

export interface MediaConstraints {
  audio: boolean;
  video: boolean;
}

export type WebRtcState = 'NEW' | 'CONNECTING' | 'CONNECTED' | 'DISCONNECTED' | 'FAILED' | 'CLOSED';

export class WebRtcManager {
  private peerConnection: RTCPeerConnection | null = null;
  private localStream: MediaStream | null = null;
  private remoteStream: MediaStream | null = null;
  private onRemoteStreamCallback: ((stream: MediaStream) => void) | null = null;
  private onIceCandidateCallback: ((candidate: RTCIceCandidate) => void) | null = null;
  private onConnectionStateCallback: ((state: WebRtcState) => void) | null = null;

  public setCallbacks(callbacks: {
    onRemoteStream?: (stream: MediaStream) => void;
    onIceCandidate?: (candidate: RTCIceCandidate) => void;
    onConnectionState?: (state: WebRtcState) => void;
  }) {
    if (callbacks.onRemoteStream) this.onRemoteStreamCallback = callbacks.onRemoteStream;
    if (callbacks.onIceCandidate) this.onIceCandidateCallback = callbacks.onIceCandidate;
    if (callbacks.onConnectionState) this.onConnectionStateCallback = callbacks.onConnectionState;
  }

  public async acquireLocalMedia(constraints: MediaConstraints): Promise<MediaStream> {
    this.stopLocalMedia();

    try {
      this.localStream = await navigator.mediaDevices.getUserMedia(constraints);
      return this.localStream;
    } catch (err: any) {
      if (err.name === 'NotAllowedError' || err.name === 'PermissionDeniedError') {
        throw new Error('Microphone/Camera permission was denied.');
      } else if (err.name === 'NotFoundError' || err.name === 'DevicesNotFoundError') {
        throw new Error('No camera or microphone found on this device.');
      } else if (err.name === 'NotReadableError' || err.name === 'TrackStartError') {
        throw new Error('Camera or microphone is already in use by another application.');
      } else if (err.name === 'OverconstrainedError') {
        throw new Error('Device does not meet the required media constraints.');
      }
      throw new Error(err.message || 'Failed to acquire media devices.');
    }
  }

  public initPeerConnection(): RTCPeerConnection {
    this.cleanupPeerConnection();

    this.peerConnection = new RTCPeerConnection({
      iceServers: WEBRTC_CONFIG.ICE_SERVERS,
    });

    this.remoteStream = new MediaStream();

    this.peerConnection.onicecandidate = (event) => {
      if (event.candidate && this.onIceCandidateCallback) {
        this.onIceCandidateCallback(event.candidate);
      }
    };

    this.peerConnection.ontrack = (event) => {
      if (event.streams && event.streams[0]) {
        if (this.onRemoteStreamCallback) {
          this.onRemoteStreamCallback(event.streams[0]);
        }
      } else if (this.remoteStream) {
        this.remoteStream.addTrack(event.track);
        if (this.onRemoteStreamCallback) {
          this.onRemoteStreamCallback(this.remoteStream);
        }
      }
    };

    this.peerConnection.onconnectionstatechange = () => {
      if (!this.peerConnection) return;
      const state = this.peerConnection.connectionState.toUpperCase() as WebRtcState;
      if (this.onConnectionStateCallback) {
        this.onConnectionStateCallback(state);
      }
    };

    // Attach local stream tracks to peer connection
    if (this.localStream) {
      this.localStream.getTracks().forEach((track) => {
        if (this.localStream && this.peerConnection) {
          this.peerConnection.addTrack(track, this.localStream);
        }
      });
    }

    return this.peerConnection;
  }

  public async createOffer(): Promise<RTCSessionDescriptionInit> {
    if (!this.peerConnection) {
      this.initPeerConnection();
    }

    const offer = await this.peerConnection!.createOffer({
      offerToReceiveAudio: true,
      offerToReceiveVideo: true,
    });

    await this.peerConnection!.setLocalDescription(offer);
    return offer;
  }

  public async handleOfferAndCreateAnswer(offerSdp: string): Promise<RTCSessionDescriptionInit> {
    if (!this.peerConnection) {
      this.initPeerConnection();
    }

    await this.peerConnection!.setRemoteDescription(
      new RTCSessionDescription({ type: 'offer', sdp: offerSdp })
    );

    const answer = await this.peerConnection!.createAnswer();
    await this.peerConnection!.setLocalDescription(answer);
    return answer;
  }

  public async handleAnswer(answerSdp: string): Promise<void> {
    if (!this.peerConnection) return;
    await this.peerConnection.setRemoteDescription(
      new RTCSessionDescription({ type: 'answer', sdp: answerSdp })
    );
  }

  public async addIceCandidate(candidate: any): Promise<void> {
    if (!this.peerConnection || !candidate) return;
    try {
      await this.peerConnection.addIceCandidate(new RTCIceCandidate(candidate));
    } catch {
      // Ignore stale ICE candidates
    }
  }

  public toggleAudio(enabled: boolean): boolean {
    if (!this.localStream) return false;
    const audioTrack = this.localStream.getAudioTracks()[0];
    if (audioTrack) {
      audioTrack.enabled = enabled;
      return audioTrack.enabled;
    }
    return false;
  }

  public toggleVideo(enabled: boolean): boolean {
    if (!this.localStream) return false;
    const videoTrack = this.localStream.getVideoTracks()[0];
    if (videoTrack) {
      videoTrack.enabled = enabled;
      return videoTrack.enabled;
    }
    return false;
  }

  public getLocalStream(): MediaStream | null {
    return this.localStream;
  }

  public getRemoteStream(): MediaStream | null {
    return this.remoteStream;
  }

  public stopLocalMedia(): void {
    if (this.localStream) {
      this.localStream.getTracks().forEach((track) => track.stop());
      this.localStream = null;
    }
  }

  public cleanupPeerConnection(): void {
    if (this.peerConnection) {
      try {
        this.peerConnection.ontrack = null;
        this.peerConnection.onicecandidate = null;
        this.peerConnection.onconnectionstatechange = null;
        this.peerConnection.close();
      } catch {
        // ignore
      }
      this.peerConnection = null;
    }
  }

  public closeAll(): void {
    this.stopLocalMedia();
    this.cleanupPeerConnection();
    this.remoteStream = null;
  }
}

export const webrtcManager = new WebRtcManager();
