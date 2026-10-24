/**
 * VoiceProvider — Pipeline de Voz Híbrido, VAD e Barge-in
 * Suporta Web Speech API (zero latência local) com fallback para TTS/STT via API.
 * Captura dados de frequência via AnalyserNode para alimentar o núcleo holográfico 3D.
 */

export interface VoiceCallback {
  onTranscript: (text: string, isFinal: boolean) => void;
  onAudioLevel: (level: number, frequencyData: Uint8Array) => void;
  onStateChange: (state: 'IDLE' | 'LISTENING' | 'SPEAKING') => void;
  onError: (error: string) => void;
}

export class VoiceService {
  private recognition: any = null;
  private audioContext: AudioContext | null = null;
  private analyser: AnalyserNode | null = null;
  private micStream: MediaStream | null = null;
  private animFrameId: number | null = null;
  private isListening = false;
  private isSpeaking = false;
  private callbacks: VoiceCallback | null = null;

  constructor() {
    this.initSpeechRecognition();
  }

  private initSpeechRecognition() {
    const SpeechRecognition =
      (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;

    if (SpeechRecognition) {
      this.recognition = new SpeechRecognition();
      this.recognition.continuous = true;
      this.recognition.interimResults = true;
      this.recognition.lang = 'pt-BR';

      this.recognition.onresult = (event: any) => {
        let interimTranscript = '';
        let finalTranscript = '';

        for (let i = event.resultIndex; i < event.results.length; ++i) {
          if (event.results[i].isFinal) {
            finalTranscript += event.results[i][0].transcript;
          } else {
            interimTranscript += event.results[i][0].transcript;
          }
        }

        if (finalTranscript && this.callbacks) {
          this.callbacks.onTranscript(finalTranscript.trim(), true);
        } else if (interimTranscript && this.callbacks) {
          this.callbacks.onTranscript(interimTranscript.trim(), false);
        }
      };

      this.recognition.onerror = (event: any) => {
        if (event.error !== 'no-speech' && this.callbacks) {
          this.callbacks.onError(`Erro no microfone: ${event.error}`);
        }
      };

      this.recognition.onend = () => {
        if (this.isListening) {
          try {
            this.recognition.start();
          } catch {
            // ignorar
          }
        }
      };
    }
  }

  setCallbacks(cb: VoiceCallback) {
    this.callbacks = cb;
  }

  async startListening(): Promise<boolean> {
    // Interrompe qualquer fala em andamento (Barge-in automático)
    this.stopSpeaking();

    try {
      this.micStream = await navigator.mediaDevices.getUserMedia({
        audio: {
          echoCancellation: true,
          noiseSuppression: true,
          autoGainControl: true,
        },
      });

      this.audioContext = new (window.AudioContext || (window as any).webkitAudioContext)();
      const source = this.audioContext.createMediaStreamSource(this.micStream);
      this.analyser = this.audioContext.createAnalyser();
      this.analyser.fftSize = 64;
      source.connect(this.analyser);

      this.isListening = true;
      if (this.recognition) {
        try {
          this.recognition.start();
        } catch {
          // pode já estar rodando
        }
      }

      this.startAudioLoop();
      this.callbacks?.onStateChange('LISTENING');
      return true;
    } catch (err: any) {
      this.callbacks?.onError('Permissão de microfone negada ou dispositivo não disponível.');
      return false;
    }
  }

  stopListening() {
    this.isListening = false;
    if (this.recognition) {
      try {
        this.recognition.stop();
      } catch {
        // ignorar
      }
    }

    if (this.micStream) {
      this.micStream.getTracks().forEach((track) => track.stop());
      this.micStream = null;
    }

    if (this.animFrameId) {
      cancelAnimationFrame(this.animFrameId);
      this.animFrameId = null;
    }

    if (this.audioContext) {
      this.audioContext.close().catch(() => {});
      this.audioContext = null;
    }

    this.callbacks?.onStateChange('IDLE');
  }

  private startAudioLoop() {
    if (!this.analyser) return;

    const dataArray = new Uint8Array(this.analyser.frequencyBinCount);

    const checkAudio = () => {
      if (!this.isListening && !this.isSpeaking) return;

      this.analyser?.getByteFrequencyData(dataArray);

      // Calcula RMS de amplitude
      let sum = 0;
      for (let i = 0; i < dataArray.length; i++) {
        sum += dataArray[i];
      }
      const average = sum / dataArray.length;
      const level = Math.min(1.0, average / 128.0);

      this.callbacks?.onAudioLevel(level, dataArray);
      this.animFrameId = requestAnimationFrame(checkAudio);
    };

    checkAudio();
  }

  speak(text: string, onEnd?: () => void) {
    if (!('speechSynthesis' in window)) {
      onEnd?.();
      return;
    }

    // Interrompe fala anterior imediatamente (Barge-in)
    this.stopSpeaking();

    // Remove emojis e caracteres que o sintetizador lê de forma estranha
    const cleanText = text.replace(/[\u{1F600}-\u{1F64F}\u{1F300}-\u{1F5FF}\u{1F680}-\u{1F6FF}\u{2600}-\u{26FF}\u{2700}-\u{27BF}]/gu, '');

    const utterance = new SpeechSynthesisUtterance(cleanText);
    utterance.lang = 'pt-BR';
    utterance.rate = 1.05;
    utterance.pitch = 1.0;

    // Seleciona voz brasileira mais natural se disponível
    const voices = window.speechSynthesis.getVoices();
    const ptVoice = voices.find((v) => v.lang.includes('pt-BR') || v.lang.includes('pt'));
    if (ptVoice) utterance.voice = ptVoice;

    this.isSpeaking = true;
    this.callbacks?.onStateChange('SPEAKING');

    // Simula áudio level do TTS
    let ttsSimInterval: any = null;
    ttsSimInterval = setInterval(() => {
      if (!this.isSpeaking) {
        clearInterval(ttsSimInterval);
        return;
      }
      const simLevel = 0.3 + Math.random() * 0.5;
      const simData = new Uint8Array(32);
      for (let i = 0; i < simData.length; i++) {
        simData[i] = Math.floor(Math.random() * 200 * simLevel);
      }
      this.callbacks?.onAudioLevel(simLevel, simData);
    }, 80);

    utterance.onend = () => {
      this.isSpeaking = false;
      clearInterval(ttsSimInterval);
      this.callbacks?.onStateChange('IDLE');
      onEnd?.();
    };

    utterance.onerror = () => {
      this.isSpeaking = false;
      clearInterval(ttsSimInterval);
      this.callbacks?.onStateChange('IDLE');
      onEnd?.();
    };

    window.speechSynthesis.speak(utterance);
  }

  stopSpeaking() {
    if ('speechSynthesis' in window) {
      window.speechSynthesis.cancel();
    }
    this.isSpeaking = false;
    this.callbacks?.onStateChange('IDLE');
  }
}

export const voiceService = new VoiceService();
