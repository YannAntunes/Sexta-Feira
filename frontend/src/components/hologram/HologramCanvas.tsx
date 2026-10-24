import React, { useEffect, useRef, useState } from 'react';
import * as THREE from 'three';
import { HologramState } from '../../types';

interface HologramCanvasProps {
  state: HologramState;
  audioLevel?: number; // 0.0 a 1.0 (amplitude em tempo real do microfone ou TTS)
  frequencyData?: Uint8Array | null;
  interactive?: boolean;
}

const STATE_COLORS: Record<HologramState, { primary: number; secondary: number; glow: string }> = {
  IDLE: { primary: 0x00f3ff, secondary: 0x0066ff, glow: 'rgba(0, 243, 255, 0.4)' },
  LISTENING: { primary: 0x00f3ff, secondary: 0x00ffaa, glow: 'rgba(0, 243, 255, 0.8)' },
  THINKING: { primary: 0x9d4edd, secondary: 0x7928ca, glow: 'rgba(157, 78, 221, 0.7)' },
  SPEAKING: { primary: 0x00ffcc, secondary: 0x0066ff, glow: 'rgba(0, 255, 204, 0.8)' },
  PROCESSING: { primary: 0xf5a623, secondary: 0xff7b00, glow: 'rgba(245, 166, 35, 0.7)' },
  ERROR: { primary: 0xff0055, secondary: 0x990022, glow: 'rgba(255, 0, 85, 0.8)' },
};

export const HologramCanvas: React.FC<HologramCanvasProps> = ({
  state,
  audioLevel = 0,
  interactive = true,
}) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const [webglSupported, setWebglSupported] = useState<boolean>(true);

  // Referências mutáveis para o loop de animação Three.js
  const stateRef = useRef(state);
  const audioLevelRef = useRef(audioLevel);

  useEffect(() => {
    stateRef.current = state;
  }, [state]);

  useEffect(() => {
    audioLevelRef.current = audioLevel;
  }, [audioLevel]);

  useEffect(() => {
    const container = containerRef.current;
    const canvas = canvasRef.current;
    if (!container || !canvas) return;

    // 1. Verificação de suporte a WebGL
    let renderer: THREE.WebGLRenderer;
    try {
      renderer = new THREE.WebGLRenderer({
        canvas,
        alpha: true,
        antialias: true,
        powerPreference: 'high-performance',
      });
      renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
      renderer.setSize(container.clientWidth, container.clientHeight);
    } catch {
      setWebglSupported(false);
      return;
    }

    const scene = new THREE.Scene();
    const camera = new THREE.PerspectiveCamera(
      45,
      container.clientWidth / container.clientHeight,
      0.1,
      1000
    );
    camera.position.z = 7.5;

    // Grupo mestre do holograma
    const hologramGroup = new THREE.Group();
    scene.add(hologramGroup);

    // --- 2. Camada 1: Núcleo Interno de Energia (Inner Plasma Core) ---
    const coreGeo = new THREE.IcosahedronGeometry(1.2, 3);
    const coreMat = new THREE.MeshBasicMaterial({
      color: STATE_COLORS[state].primary,
      wireframe: true,
      transparent: true,
      opacity: 0.35,
      blending: THREE.AdditiveBlending,
    });
    const coreMesh = new THREE.Mesh(coreGeo, coreMat);
    hologramGroup.add(coreMesh);

    // Guarda posições originais dos vértices para deformação de áudio/pulso
    const originalCorePositions = coreGeo.attributes.position.clone();

    // --- 3. Camada 2: Filamentos Cinéticos Orbitais (Gyroscopic Rings) ---
    const rings: THREE.LineLoop[] = [];
    const ringRadii = [1.8, 2.15, 2.5];
    const ringRotSpeeds = [
      { x: 0.008, y: 0.015, z: 0.005 },
      { x: 0.012, y: -0.009, z: 0.011 },
      { x: -0.006, y: 0.014, z: -0.008 },
    ];

    ringRadii.forEach((radius, i) => {
      const ringGeo = new THREE.BufferGeometry();
      const segments = 120;
      const points: number[] = [];
      for (let j = 0; j <= segments; j++) {
        const theta = (j / segments) * Math.PI * 2;
        points.push(Math.cos(theta) * radius, Math.sin(theta) * radius, 0);
      }
      ringGeo.setAttribute('position', new THREE.Float32BufferAttribute(points, 3));

      const ringMat = new THREE.LineBasicMaterial({
        color: STATE_COLORS[state].secondary,
        transparent: true,
        opacity: 0.55 - i * 0.1,
        blending: THREE.AdditiveBlending,
      });

      const ringMesh = new THREE.LineLoop(ringGeo, ringMat);
      ringMesh.rotation.set((i * Math.PI) / 3, (i * Math.PI) / 4, 0);
      rings.push(ringMesh);
      hologramGroup.add(ringMesh);
    });

    // --- 4. Camada 3: Nuvem de Partículas Quânticas (Particle Swarm) ---
    const particleCount = 1800;
    const particleGeo = new THREE.BufferGeometry();
    const particlePositions = new Float32Array(particleCount * 3);
    const particleSpeeds = new Float32Array(particleCount * 3);

    for (let i = 0; i < particleCount; i++) {
      const i3 = i * 3;
      // Distribuição esférica
      const u = Math.random();
      const v = Math.random();
      const theta = u * 2.0 * Math.PI;
      const phi = Math.acos(2.0 * v - 1.0);
      const r = Math.cbrt(Math.random()) * 2.6 + 0.3;

      particlePositions[i3] = r * Math.sin(phi) * Math.cos(theta);
      particlePositions[i3 + 1] = r * Math.sin(phi) * Math.sin(theta);
      particlePositions[i3 + 2] = r * Math.cos(phi);

      particleSpeeds[i3] = (Math.random() - 0.5) * 0.01;
      particleSpeeds[i3 + 1] = (Math.random() - 0.5) * 0.01;
      particleSpeeds[i3 + 2] = (Math.random() - 0.5) * 0.01;
    }

    particleGeo.setAttribute('position', new THREE.BufferAttribute(particlePositions, 3));

    const particleMat = new THREE.PointsMaterial({
      size: 0.035,
      color: STATE_COLORS[state].primary,
      transparent: true,
      opacity: 0.75,
      blending: THREE.AdditiveBlending,
    });

    const particleSystem = new THREE.Points(particleGeo, particleMat);
    hologramGroup.add(particleSystem);

    // --- 5. Camada 4: HUD Reticle Circular Externo ---
    const hudRingGeo = new THREE.RingGeometry(2.9, 2.92, 64);
    const hudRingMat = new THREE.MeshBasicMaterial({
      color: STATE_COLORS[state].primary,
      transparent: true,
      opacity: 0.25,
      side: THREE.DoubleSide,
      blending: THREE.AdditiveBlending,
    });
    const hudRing = new THREE.Mesh(hudRingGeo, hudRingMat);
    hologramGroup.add(hudRing);

    // --- 6. Interação do Mouse / Giroscópio ---
    let targetRotationX = 0;
    let targetRotationY = 0;

    const handlePointerMove = (e: MouseEvent) => {
      if (!interactive) return;
      const rect = container.getBoundingClientRect();
      const x = ((e.clientX - rect.left) / rect.width) * 2 - 1;
      const y = -(((e.clientY - rect.top) / rect.height) * 2 - 1);
      targetRotationY = x * 0.45;
      targetRotationX = y * 0.45;
    };

    window.addEventListener('mousemove', handlePointerMove);

    // --- 7. Loop de Renderização Contínuo ---
    let animationFrameId: number;
    let clock = new THREE.Clock();
    let smoothedAudio = 0;
    const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    const animate = () => {
      animationFrameId = requestAnimationFrame(animate);
      const elapsedTime = clock.getElapsedTime();
      const currentState = stateRef.current;
      const currentAudio = audioLevelRef.current;
      const theme = STATE_COLORS[currentState];

      // Filtro passa-baixas temporal (lerp 0.12) para amortecer saltos bruscos de áudio e evitar serrilhado
      smoothedAudio += (currentAudio - smoothedAudio) * 0.12;

      // Transição suave de cores
      coreMat.color.lerp(new THREE.Color(theme.primary), 0.08);
      particleMat.color.lerp(new THREE.Color(theme.primary), 0.08);
      hudRingMat.color.lerp(new THREE.Color(theme.primary), 0.08);
      rings.forEach((r, idx) => {
        (r.material as THREE.LineBasicMaterial).color.lerp(
          new THREE.Color(idx % 2 === 0 ? theme.primary : theme.secondary),
          0.08
        );
      });

      // Rotação com amortecimento suave
      hologramGroup.rotation.y += (targetRotationY - hologramGroup.rotation.y) * 0.05;
      hologramGroup.rotation.x += (targetRotationX - hologramGroup.rotation.x) * 0.05;

      // Dinâmica de acordo com o estado do sistema e sensibilidade de movimento
      let speedMult = prefersReducedMotion ? 0.2 : 1.0;
      let pulseAmp = prefersReducedMotion ? 0.02 : 0.04;

      switch (currentState) {
        case 'IDLE':
          speedMult *= 0.8;
          pulseAmp = prefersReducedMotion ? 0.01 : 0.03 + Math.sin(elapsedTime * 1.5) * 0.015;
          break;
        case 'LISTENING':
          speedMult *= 1.2;
          pulseAmp = Math.min(0.2, (0.07 + smoothedAudio * 0.4) * (prefersReducedMotion ? 0.3 : 1.0)); // Expansão reativa à voz suave
          break;
        case 'THINKING':
          speedMult *= 2.5; // Aceleração no raciocínio
          pulseAmp = (0.06 + Math.sin(elapsedTime * 6) * 0.04) * (prefersReducedMotion ? 0.3 : 1.0);
          break;
        case 'SPEAKING':
          speedMult *= 1.4;
          // Trava de amplitude com lerp orgânico: elimina glitches, jitter e deformações erráticas
          pulseAmp = Math.min(0.18, (0.06 + smoothedAudio * 0.32) * (prefersReducedMotion ? 0.3 : 1.0));
          break;
        case 'PROCESSING':
          speedMult *= 1.8;
          pulseAmp = 0.05;
          break;
        case 'ERROR':
          speedMult *= 0.4;
          pulseAmp = 0.06 + (Math.random() - 0.5) * 0.03; // Glitch estático sutil e controlado
          break;
      }

      // 1. Pulso e rotação dos anéis
      rings.forEach((ring, i) => {
        const speed = ringRotSpeeds[i];
        ring.rotation.x += speed.x * speedMult;
        ring.rotation.y += speed.y * speedMult;
        ring.rotation.z += speed.z * speedMult;

        const ringScale = 1 + pulseAmp * Math.sin(elapsedTime * 2 + i);
        ring.scale.set(ringScale, ringScale, ringScale);
      });

      // 2. Pulso e deformação harmônica suave dos vértices do núcleo
      const positions = coreGeo.attributes.position;
      const count = positions.count;
      const clampedAmp = Math.min(pulseAmp, 0.2);
      for (let i = 0; i < count; i++) {
        const ox = originalCorePositions.getX(i);
        const oy = originalCorePositions.getY(i);
        const oz = originalCorePositions.getZ(i);

        // Deformação harmônica suave e contínua sem quebra de topologia
        const wave = Math.sin(elapsedTime * 3.5 + ox * 1.8 + oy * 1.8) * clampedAmp;
        positions.setXYZ(i, ox * (1 + wave), oy * (1 + wave), oz * (1 + wave));
      }
      positions.needsUpdate = true;
      coreMesh.rotation.y += 0.01 * speedMult;
      coreMesh.rotation.x += 0.006 * speedMult;

      // 3. Nuvem de partículas com respiração
      const pPositions = particleGeo.attributes.position.array as Float32Array;
      for (let i = 0; i < particleCount; i++) {
        const i3 = i * 3;
        pPositions[i3] += particleSpeeds[i3] * speedMult;
        pPositions[i3 + 1] += particleSpeeds[i3 + 1] * speedMult;
        pPositions[i3 + 2] += particleSpeeds[i3 + 2] * speedMult;

        // Mantém dentro do raio esférico
        const distSq =
          pPositions[i3] * pPositions[i3] +
          pPositions[i3 + 1] * pPositions[i3 + 1] +
          pPositions[i3 + 2] * pPositions[i3 + 2];
        if (distSq > 9.0 || distSq < 0.2) {
          particleSpeeds[i3] *= -1;
          particleSpeeds[i3 + 1] *= -1;
          particleSpeeds[i3 + 2] *= -1;
        }
      }
      particleGeo.attributes.position.needsUpdate = true;
      particleSystem.rotation.y -= 0.003 * speedMult;

      // 4. HUD Reticle
      hudRing.rotation.z += 0.002 * speedMult;
      const hudScale = 1 + pulseAmp * 0.25;
      hudRing.scale.set(hudScale, hudScale, hudScale);

      renderer.render(scene, camera);
    };

    animate();

    // --- 8. Resize Observer ---
    const handleResize = () => {
      if (!container || !renderer) return;
      const width = container.clientWidth;
      const height = container.clientHeight;
      camera.aspect = width / height;
      camera.updateProjectionMatrix();
      renderer.setSize(width, height);
    };

    window.addEventListener('resize', handleResize);

    return () => {
      cancelAnimationFrame(animationFrameId);
      window.removeEventListener('mousemove', handlePointerMove);
      window.removeEventListener('resize', handleResize);
      renderer.dispose();
      coreGeo.dispose();
      coreMat.dispose();
      particleGeo.dispose();
      particleMat.dispose();
      hudRingGeo.dispose();
      hudRingMat.dispose();
      rings.forEach((r) => {
        r.geometry.dispose();
        (r.material as THREE.Material).dispose();
      });
    };
  }, [interactive]);

  return (
    <div
      ref={containerRef}
      className="relative w-full h-full flex items-center justify-center overflow-hidden"
    >
      {webglSupported ? (
        <canvas ref={canvasRef} className="w-full h-full pointer-events-none" />
      ) : (
        /* Fallback 2D Canvas gracioso para dispositivos sem WebGL */
        <div className="flex flex-col items-center justify-center p-6 text-center">
          <div className="w-48 h-48 rounded-full border-2 border-hologram-cyan/40 border-dashed animate-spin-slow flex items-center justify-center shadow-glow-cyan">
            <div className="w-32 h-32 rounded-full border border-hologram-cyan/60 animate-ping" />
            <div className="absolute w-16 h-16 rounded-full bg-hologram-cyan/20 backdrop-blur-sm" />
          </div>
          <span className="mt-4 font-mono text-xs text-hologram-cyan/70 tracking-widest">
            SEXTA-FEIRA // NÚCLEO HOLOGRÁFICO (2D FALLBACK)
          </span>
        </div>
      )}

      {/* Retículo de Mira & Coordenadas Táticas HUD */}
      <div className="absolute inset-0 pointer-events-none flex flex-col justify-between p-6 select-none opacity-40">
        <div className="flex justify-between items-start font-mono text-[10px] text-hologram-cyan">
          <div>
            <span>LAT. 0.002s</span> // <span>FREQ. 44.1kHz</span>
          </div>
          <div>
            <span>ORBIT: [X: 024.1 Y: 088.7]</span>
          </div>
        </div>

        <div className="flex justify-between items-end font-mono text-[10px] text-hologram-cyan">
          <div>
            <span>SYSTEM MATRIX: V3.2-HOLO</span>
          </div>
          <div>
            <span>STATE: {state}</span>
          </div>
        </div>
      </div>
    </div>
  );
};
