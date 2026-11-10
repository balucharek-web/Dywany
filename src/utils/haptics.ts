/**
 * Web Vibration API helper for tactile haptic feedback in retail/warehouse environments.
 * Safe for all platforms (no-op if Vibration API is unsupported).
 */
export function triggerHapticFeedback(pattern: number | number[] = 35) {
  try {
    if (typeof navigator !== 'undefined' && typeof navigator.vibrate === 'function') {
      navigator.vibrate(pattern);
    }
  } catch {
    // Ignore environments where vibration is restricted or unsupported
  }
}

/** Pre-configured haptic patterns */
export const Haptics = {
  /** Light tap for button clicks or copy actions */
  tap: () => triggerHapticFeedback(25),
  /** Success pulse (double pulse) for successful barcode scan or carpet save */
  success: () => triggerHapticFeedback([40, 30, 40]),
  /** Subtle confirmation for copy to clipboard */
  copy: () => triggerHapticFeedback(30),
  /** Warning or error buzz */
  warning: () => triggerHapticFeedback([60, 40, 60]),
};
