import { useEffect } from 'react';

interface KeyboardShortcutsOptions {
  onSearchFocus: () => void;
  onToggleView: () => void;
  onEscape?: () => void;
  isAnyModalOpen?: boolean;
}

/**
 * Global PC keyboard shortcuts for instant warehouse productivity:
 * - Ctrl+K or / : Focus search bar
 * - Ctrl+M : Toggle between Search and Warehouse views
 * - Escape : Close open modal
 */
export function useKeyboardShortcuts({
  onSearchFocus,
  onToggleView,
  onEscape,
  isAnyModalOpen
}: KeyboardShortcutsOptions) {
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      // 1. ESC to close modal
      if (e.key === 'Escape') {
        if (isAnyModalOpen && onEscape) {
          e.preventDefault();
          onEscape();
          return;
        }
      }

      // Check if target is an active editable field
      const target = e.target as HTMLElement | null;
      const isInput =
        target &&
        (target.tagName === 'INPUT' ||
          target.tagName === 'TEXTAREA' ||
          target.isContentEditable);

      // 2. Ctrl+K (or Cmd+K) to focus search
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
        e.preventDefault();
        onSearchFocus();
        return;
      }

      // 3. / to focus search (when not already inside an input)
      if (e.key === '/' && !isInput && !e.ctrlKey && !e.metaKey && !e.altKey) {
        e.preventDefault();
        onSearchFocus();
        return;
      }

      // 4. Ctrl+M (or Cmd+M) to toggle view between Search and Warehouse
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'm') {
        e.preventDefault();
        onToggleView();
        return;
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [onSearchFocus, onToggleView, onEscape, isAnyModalOpen]);
}
