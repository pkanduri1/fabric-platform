/**
 * Device Fingerprinting Utility
 * 
 * Generates a device fingerprint for security and session tracking.
 * Creates a unique identifier based on browser and system characteristics
 * for enhanced security analysis and fraud detection.
 * 
 * @author Claude Code
 * @version 1.0
 * @since 2025-01-30
 */

interface DeviceFingerprintData {
  userAgent: string;
  language: string;
  platform: string;
  screen: {
    width: number;
    height: number;
    colorDepth: number;
  };
  timezone: string;
  hasLocalStorage: boolean;
  hasSessionStorage: boolean;
  hasCookies: boolean;
  plugins: string[];
  canvas?: string;
}

/**
 * Generates a device fingerprint based on browser characteristics
 */
export function generateDeviceFingerprint(): string {
  try {
    const fingerprintData: DeviceFingerprintData = {
      userAgent: navigator.userAgent,
      language: navigator.language,
      platform: navigator.platform,
      screen: {
        width: window.screen.width,
        height: window.screen.height,
        colorDepth: window.screen.colorDepth
      },
      timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
      hasLocalStorage: isStorageAvailable('localStorage'),
      hasSessionStorage: isStorageAvailable('sessionStorage'),
      hasCookies: navigator.cookieEnabled,
      plugins: getPluginList(),
      canvas: getCanvasFingerprint()
    };

    // Create a hash of the fingerprint data
    const fingerprintString = JSON.stringify(fingerprintData);
    return hashString(fingerprintString);

  } catch (error) {
    console.warn('Error generating device fingerprint:', error);
    // Fallback to a simple hash based on user agent and timestamp
    return hashString(navigator.userAgent + Date.now().toString());
  }
}

/**
 * Checks if a storage type is available
 */
function isStorageAvailable(storageType: 'localStorage' | 'sessionStorage'): boolean {
  try {
    const storage = window[storageType];
    const testKey = '__storage_test__';
    storage.setItem(testKey, 'test');
    storage.removeItem(testKey);
    return true;
  } catch (error) {
    return false;
  }
}

/**
 * Gets list of browser plugins
 */
function getPluginList(): string[] {
  try {
    const plugins: string[] = [];
    for (let i = 0; i < navigator.plugins.length; i++) {
      plugins.push(navigator.plugins[i].name);
    }
    return plugins.sort();
  } catch (error) {
    return [];
  }
}

/**
 * Generates a canvas fingerprint
 */
function getCanvasFingerprint(): string | undefined {
  try {
    const canvas = document.createElement('canvas');
    const ctx = canvas.getContext('2d');
    
    if (!ctx) return undefined;

    // Draw some text and shapes
    ctx.textBaseline = 'top';
    ctx.font = '14px Arial';
    ctx.fillStyle = '#f60';
    ctx.fillRect(125, 1, 62, 20);
    ctx.fillStyle = '#069';
    ctx.fillText('Fabric Platform Device Fingerprint', 2, 15);
    ctx.fillStyle = 'rgba(102, 204, 0, 0.7)';
    ctx.fillText('Device Security Check', 4, 45);

    // Get canvas data
    return canvas.toDataURL();
  } catch (error) {
    return undefined;
  }
}

/**
 * Simple hash function for strings
 */
function hashString(str: string): string {
  let hash = 0;
  if (str.length === 0) return hash.toString();
  
  for (let i = 0; i < str.length; i++) {
    const char = str.charCodeAt(i);
    hash = ((hash << 5) - hash) + char;
    hash = hash & hash; // Convert to 32-bit integer
  }
  
  return Math.abs(hash).toString(16);
}

/**
 * Gets basic device information for display
 */
export function getDeviceInfo() {
  return {
    browser: getBrowserName(),
    os: getOperatingSystem(),
    screen: `${window.screen.width}x${window.screen.height}`,
    language: navigator.language,
    timezone: Intl.DateTimeFormat().resolvedOptions().timeZone
  };
}

/**
 * Detects browser name
 */
function getBrowserName(): string {
  const userAgent = navigator.userAgent;
  
  if (userAgent.includes('Chrome') && !userAgent.includes('Edg')) return 'Chrome';
  if (userAgent.includes('Firefox')) return 'Firefox';
  if (userAgent.includes('Safari') && !userAgent.includes('Chrome')) return 'Safari';
  if (userAgent.includes('Edg')) return 'Edge';
  if (userAgent.includes('Opera')) return 'Opera';
  
  return 'Unknown';
}

/**
 * Detects operating system
 */
function getOperatingSystem(): string {
  const userAgent = navigator.userAgent;
  
  if (userAgent.includes('Windows')) return 'Windows';
  if (userAgent.includes('Mac')) return 'macOS';
  if (userAgent.includes('Linux')) return 'Linux';
  if (userAgent.includes('Android')) return 'Android';
  if (userAgent.includes('iOS')) return 'iOS';
  
  return 'Unknown';
}