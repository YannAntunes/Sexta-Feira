import React from 'react';
import { HologramState } from '../../types';

interface VoiceVisualizerProps {
  state: HologramState;
  audioLevel: number;
  frequencyData?: Uint8Array | null;
}

export const VoiceVisualizer: React.FC<VoiceVisualizerProps> = ({
  state,
  audioLevel,
  frequencyData,
}) => {
  const barCount = 24;
  const bars = Array.from({ length: barCount }, (_, i) => {
    let height = 8;
    if (frequencyData && frequencyData.length > 0) {
      const idx = Math.floor((i / barCount) * (frequencyData.length / 2));
      const val = frequencyData[idx] / 255;
      height = Math.max(6, val * 52);
    } else if (state === 'LISTENING' || state === 'SPEAKING') {
      height = Math.max(6, Math.sin((i / barCount) * Math.PI) * audioLevel * 50);
    } else if (state === 'THINKING') {
      height = 8 + Math.sin(Date.now() * 0.01 + i) * 6;
    }
    return height;
  });

  return (
    <div className="flex items-end justify-center gap-1 h-16 px-4 py-2 pointer-events-none select-none">
      {bars.map((h, i) => (
        <div
          key={i}
          className={`w-1 rounded-full transition-all duration-75 ${
            state === 'ERROR'
              ? 'bg-rose-500 shadow-glow-red'
              : state === 'THINKING'
              ? 'bg-hologram-purple shadow-glow-purple'
              : state === 'SPEAKING'
              ? 'bg-emerald-400'
              : 'bg-hologram-cyan shadow-glow-cyan'
          }`}
          style={{ height: `${h}px`, opacity: 0.35 + (h / 52) * 0.65 }}
        />
      ))}
    </div>
  );
};
