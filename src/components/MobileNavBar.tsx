import { Search, Database, Camera, FileText } from 'lucide-react';

interface MobileNavBarProps {
  currentView: 'search' | 'warehouse';
  onSelectView: (view: 'search' | 'warehouse') => void;
  onOpenScanner: () => void;
  onOpenFiszki: () => void;
  fiszkiCount?: number;
}

export function MobileNavBar({
  currentView,
  onSelectView,
  onOpenScanner,
  onOpenFiszki,
  fiszkiCount = 0
}: MobileNavBarProps) {
  return (
    <nav className="md:hidden fixed bottom-0 inset-x-0 z-30 bg-white/95 backdrop-blur-md border-t border-gray-200 shadow-lg pb-[env(safe-area-inset-bottom,0px)]">
      <div className="grid grid-cols-4 h-15">
        {/* Wyszukiwarka */}
        <button
          onClick={() => onSelectView('search')}
          className={`flex flex-col items-center justify-center gap-1 transition-colors relative cursor-pointer ${
            currentView === 'search'
              ? 'text-blue-600 font-bold'
              : 'text-gray-500 hover:text-gray-900 font-medium'
          }`}
        >
          <div className={`p-1 rounded-xl transition-all ${
            currentView === 'search' ? 'bg-blue-50' : ''
          }`}>
            <Search className="w-5 h-5" />
          </div>
          <span className="text-[11px] leading-none">Szukaj</span>
          {currentView === 'search' && (
            <span className="absolute top-0 inset-x-6 h-0.5 bg-blue-600 rounded-b-full" />
          )}
        </button>

        {/* Magazyn */}
        <button
          onClick={() => onSelectView('warehouse')}
          className={`flex flex-col items-center justify-center gap-1 transition-colors relative cursor-pointer ${
            currentView === 'warehouse'
              ? 'text-[#f04438] font-bold'
              : 'text-gray-500 hover:text-gray-900 font-medium'
          }`}
        >
          <div className={`p-1 rounded-xl transition-all ${
            currentView === 'warehouse' ? 'bg-red-50' : ''
          }`}>
            <Database className="w-5 h-5" />
          </div>
          <span className="text-[11px] leading-none">Magazyn</span>
          {currentView === 'warehouse' && (
            <span className="absolute top-0 inset-x-6 h-0.5 bg-[#f04438] rounded-b-full" />
          )}
        </button>

        {/* Skaner kamery */}
        <button
          onClick={onOpenScanner}
          className="flex flex-col items-center justify-center gap-1 text-gray-700 hover:text-blue-600 font-medium transition-colors cursor-pointer"
        >
          <div className="p-1 rounded-xl hover:bg-gray-100 transition-colors">
            <Camera className="w-5 h-5 text-gray-700" />
          </div>
          <span className="text-[11px] leading-none">Skanuj</span>
        </button>

        {/* Fiszki */}
        <button
          onClick={onOpenFiszki}
          className="flex flex-col items-center justify-center gap-1 text-gray-700 hover:text-blue-600 font-medium transition-colors relative cursor-pointer"
        >
          <div className="p-1 rounded-xl hover:bg-gray-100 transition-colors relative">
            <FileText className="w-5 h-5 text-gray-700" />
            {fiszkiCount > 0 && (
              <span className="absolute -top-1 -right-1 bg-blue-600 text-white text-[9px] font-black rounded-full w-4 h-4 flex items-center justify-center">
                {fiszkiCount}
              </span>
            )}
          </div>
          <span className="text-[11px] leading-none">Fiszki</span>
        </button>
      </div>
    </nav>
  );
}
