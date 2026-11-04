import { WifiOff, RefreshCw } from 'lucide-react';
import { useOnlineStatus } from '../hooks/useOnlineStatus';

export function OfflineIndicator() {
  const isOnline = useOnlineStatus();

  if (isOnline) return null;

  return (
    <div className="fixed bottom-16 sm:bottom-4 left-4 right-4 sm:left-auto sm:right-4 z-40 max-w-sm bg-gray-900/95 text-white backdrop-blur-md rounded-2xl p-3.5 shadow-2xl border border-gray-700 flex items-center justify-between gap-3 animate-in fade-in slide-in-from-bottom-3 duration-200">
      <div className="flex items-center gap-2.5">
        <div className="w-8 h-8 rounded-xl bg-amber-500/20 text-amber-400 flex items-center justify-center shrink-0">
          <WifiOff className="w-4 h-4" />
        </div>
        <div className="text-xs">
          <div className="font-bold text-amber-300">Tryb offline (Brak sieci)</div>
          <div className="text-gray-300 text-[11px] leading-tight">
            Używasz zapisanej pamięci podręcznej. Zmiany zsynchronizują się po powrocie zasięgu.
          </div>
        </div>
      </div>
      <button
        onClick={() => window.location.reload()}
        className="p-1.5 rounded-lg bg-white/10 hover:bg-white/20 text-gray-200 transition-colors shrink-0"
        title="Sprawdź połączenie"
      >
        <RefreshCw className="w-4 h-4" />
      </button>
    </div>
  );
}
