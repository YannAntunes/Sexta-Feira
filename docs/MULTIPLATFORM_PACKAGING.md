# Sexta-Feira — Arquitetura de Empacotamento Multiplataforma (Desktop & Mobile)

Este documento estabelece a estratégia de engenharia, compartilhamento de código e empacotamento da **Sexta-Feira** para ambientes nativos **Desktop (Windows)** e **Mobile (Android / iOS)** sem duplicação de base de código.

---

## 1. Decisão Arquitetural: Monorepo e Código Unificado

Para garantir que a lógica de conversação, os componentes 3D holográficos, o pipeline de voz e o cliente de API sejam 100% reutilizados, adotamos a arquitetura de **Single Core + Native Adapters**:

```
[ Frontend Unificado (React 19 + TypeScript + Vite + Three.js) ]
                             │
     ┌───────────────────────┴───────────────────────┐
     ▼                                               ▼
[ Tauri 2.0 (Desktop Windows) ]          [ Capacitor 7 (Mobile Android/iOS) ]
 - WebView2 Nativo do Windows             - WebKit / Chrome WebView Nativo
 - Binário ultra-leve (< 15MB)            - APK Android & Xcode Workspace iOS
 - Consumo de RAM: ~35MB                  - Permissões nativas (RECORD_AUDIO)
 - Atalho global (Ctrl+Shift+Espaço)      - Notificações locais em background
 - Notificações Windows Action Center     - Armazenamento em Keystore/Biometria
```

### Matriz Comparativa de Tecnologias

| Critério | Tauri 2.0 + Capacitor (Escolhida) | Electron | React Native Puro | Flutter |
| :--- | :--- | :--- | :--- | :--- |
| **Reutilização de Código** | **100% da UI, Shaders 3D e Áudio** | 100% Desktop, 0% Mobile | ~60% (Requer recriar UI em primitivas nativas) | 0% (Linguagem Dart divergente) |
| **Suporte a Three.js / WebGL**| **Nativo (Desempenho total GPU)** | Nativo (Pesado) | Complexo (expo-gl bridge tem perdas) | Complexo (Shader customizado em Skia/Impeller) |
| **Tamanho do Executável** | **~12 MB no Windows / ~8 MB APK** | ~180 MB | ~35 MB | ~40 MB |
| **Consumo de Memória (RAM)**| **~35 MB a 55 MB** | ~180 MB a 280 MB | ~70 MB | ~90 MB |
| **Barge-in / Áudio Contínuo**| **Web Audio API + AnalyserNode nativo**| Nativo | Requer bridge C++/JNI de áudio | Requer plugin de canal de áudio |

---

## 2. Empacotamento Desktop (Windows via Tauri 2.0)

O Tauri compila um binário `.exe` ou instalador `.msi` para Windows utilizando o motor WebView2 já nativamente embutido no Windows 10 e 11.

### Pré-requisitos
- Rust (`rustup` / `cargo`)
- Build Tools do Visual Studio C++

### Configuração (`src-tauri/tauri.conf.json`)
```json
{
  "$schema": "https://raw.githubusercontent.com/tauri-apps/tauri/dev/tooling/cli/schema.json",
  "build": {
    "beforeDevCommand": "npm run dev",
    "beforeBuildCommand": "npm run build",
    "devPath": "http://localhost:5173",
    "distDir": "../dist"
  },
  "package": {
    "productName": "Sexta-Feira",
    "version": "1.0.0"
  },
  "tauri": {
    "allowlist": {
      "all": false,
      "notification": {
        "all": true
      },
      "globalShortcut": {
        "all": true
      }
    },
    "windows": [
      {
        "title": "Sexta-Feira",
        "width": 1280,
        "height": 820,
        "resizable": true,
        "fullscreen": false,
        "transparent": false
      }
    ]
  }
}
```

### Comandos de Build Desktop
```powershell
Set-Location "frontend"
npm run tauri build
```
O instalador é gerado em `frontend/src-tauri/target/release/bundle/msi/`.

---

## 3. Empacotamento Mobile (Android & iOS via Capacitor)

O Capacitor encapsula a pasta `frontend/dist` diretamente em uma Activity Android nativa e em um ViewController iOS, permitindo acesso direto às permissões do sistema operacional sem alterar uma única linha dos componentes React ou do Three.js.

### Pré-requisitos
- Android Studio + Android SDK (API 34+)
- Xcode (apenas para builds de iOS no macOS)

### Configuração (`capacitor.config.json`)
```json
{
  "appId": "br.com.yann.sextafeira",
  "appName": "Sexta-Feira",
  "webDir": "dist",
  "server": {
    "androidScheme": "https"
  },
  "plugins": {
    "LocalNotifications": {
      "smallIcon": "ic_stat_icon_config_sample",
      "iconColor": "#00f3ff"
    }
  }
}
```

### Permissões Android (`android/app/src/main/AndroidManifest.xml`)
```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
```

### Comandos de Build Mobile
```powershell
Set-Location "frontend"
npm run build
npx cap add android
npx cap sync
npx cap open android
```
No Android Studio, gere o APK assinado em `Build > Generate Signed Bundle / APK`.

---

## 4. Adaptador de Plataforma Unificado (`frontend/src/services/platform.ts`)

A camada `PlatformService` garante abstração automática:
1. **Ambiente Web:** Usa Web Notifications API e localStorage.
2. **Ambiente Desktop (Tauri):** Usa Windows Action Center e atalhos de hardware.
3. **Ambiente Mobile (Capacitor):** Usa LocalNotifications e controle de áudio de mídia.
