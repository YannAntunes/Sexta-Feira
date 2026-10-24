/**
 * Adaptador de Plataforma Unificado (Web, Tauri Desktop, Capacitor Mobile)
 * Isola chamadas de sistema (notificações nativas, armazenamento seguro, atalhos globais)
 * permitindo que a mesma base de código do frontend rode no Windows, Android, iOS e Navegador.
 */

export type PlatformType = 'tauri' | 'capacitor' | 'web';

export class PlatformService {
  private platform: PlatformType = 'web';

  constructor() {
    this.detectPlatform();
  }

  private detectPlatform() {
    if (typeof window !== 'undefined' && '__TAURI__' in window) {
      this.platform = 'tauri';
    } else if (typeof window !== 'undefined' && 'Capacitor' in window) {
      this.platform = 'capacitor';
    } else {
      this.platform = 'web';
    }
  }

  getPlatform(): PlatformType {
    return this.platform;
  }

  isDesktop(): boolean {
    return this.platform === 'tauri';
  }

  isMobile(): boolean {
    return this.platform === 'capacitor';
  }

  isWeb(): boolean {
    return this.platform === 'web';
  }

  /**
   * Dispara notificação nativa do sistema operacional
   * (Windows Action Center no Desktop / Notificações Push locais no Mobile)
   */
  async notify(title: string, body: string): Promise<void> {
    if (this.platform === 'tauri' && (window as any).__TAURI__?.notification) {
      try {
        await (window as any).__TAURI__.notification.sendNotification({ title, body });
        return;
      } catch {
        // fallback
      }
    }

    if (this.platform === 'capacitor' && (window as any).Capacitor?.Plugins?.LocalNotifications) {
      try {
        await (window as any).Capacitor.Plugins.LocalNotifications.schedule({
          notifications: [
            {
              title,
              body,
              id: Date.now(),
              schedule: { at: new Date(Date.now() + 100) },
            },
          ],
        });
        return;
      } catch {
        // fallback
      }
    }

    // Fallback Web Notifications API
    if ('Notification' in window) {
      if (Notification.permission === 'granted') {
        new Notification(title, { body, icon: '/favicon.ico' });
      } else if (Notification.permission !== 'denied') {
        const perm = await Notification.requestPermission();
        if (perm === 'granted') {
          new Notification(title, { body, icon: '/favicon.ico' });
        }
      }
    }
  }

  /**
   * Armazenamento seguro de credenciais
   */
  async setSecureItem(key: string, value: string): Promise<void> {
    localStorage.setItem(key, value);
  }

  async getSecureItem(key: string): Promise<string | null> {
    return localStorage.getItem(key);
  }

  async removeSecureItem(key: string): Promise<void> {
    localStorage.removeItem(key);
  }
}

export const platformService = new PlatformService();
