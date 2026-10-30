import { useState, useMemo } from 'react';
import { Carpet } from '../types';
import { Search, Camera, FileText, MapPin, Tag, Barcode, Calendar, Copy, Check, Sparkles, HelpCircle } from 'lucide-react';
import { formatRelativeTime } from '../utils/formatters';

interface SearchViewProps {
  carpets: Carpet[];
  searchQuery: string;
  setSearchQuery: (q: string) => void;
  onOpenScanner: () => void;
  onOpenFiszkiModal: (carpetToAdd?: Carpet) => void;
  onPrintSingleFiszka?: (carpet: Carpet) => void;
  onOpenHelp: () => void;
}

export function SearchView({
  carpets,
  searchQuery,
  setSearchQuery,
  onOpenScanner,
  onOpenFiszkiModal,
  onPrintSingleFiszka,
  onOpenHelp
}: SearchViewProps) {
  const [copiedId, setCopiedId] = useState<string | null>(null);

  // Normalize search helper (handles "1a", "1 a", "20a", etc.)
  const filteredCarpets = useMemo(() => {
    const rawQuery = searchQuery.trim().toLowerCase();
    if (!rawQuery) return [];

    const cleanQuery = rawQuery.replace(/\s+/g, '');

    return carpets.filter((c) => {
      const matchName = c.nazwa?.toLowerCase().includes(rawQuery);
      const matchRef = c.ref?.toLowerCase().includes(rawQuery);
      const matchEan = c.ean?.replace(/\s+/g, '').includes(cleanQuery);
      
      const cleanPlace = (c.miejsce || `${c.magazyn}${c.podmagazyn}`).toLowerCase().replace(/\s+/g, '');
      const matchPlace = cleanPlace.includes(cleanQuery) || cleanPlace === cleanQuery;

      const bayMatch = `magazyn${c.magazyn}`.includes(cleanQuery);

      return matchName || matchRef || matchEan || matchPlace || bayMatch;
    });
  }, [carpets, searchQuery]);

  const handleCopy = (text: string, id: string) => {
    navigator.clipboard.writeText(text);
    setCopiedId(id);
    setTimeout(() => setCopiedId(null), 1500);
  };

  const handleSearchButtonClick = () => {
    // If empty query, focus or prompt
    if (!searchQuery.trim()) {
      const input = document.getElementById('search-input-field');
      input?.focus();
    }
  };

  const handleGenerateFiszkaClick = () => {
    if (filteredCarpets.length === 1) {
      onOpenFiszkiModal(filteredCarpets[0]);
    } else {
      onOpenFiszkiModal();
    }
  };

  return (
    <div className="max-w-xl mx-auto px-4 py-4 sm:py-6 flex flex-col gap-4">
      {/* Search Input Box matching Screenshots 2 & 5 */}
      <div className="relative">
        <input
          id="search-input-field"
          type="text"
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          placeholder="Wyszukaj po: EAN, ref lub miejsce (np. 12A)"
          className="w-full px-4 py-3 text-base sm:text-lg border-2 border-gray-900 rounded-xl bg-white shadow-xs focus:ring-2 focus:ring-blue-600 focus:outline-hidden transition-all placeholder:text-gray-400 font-medium"
          autoFocus
        />
        {searchQuery && (
          <button
            onClick={() => setSearchQuery('')}
            className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-700 text-xs bg-gray-100 hover:bg-gray-200 px-2 py-1 rounded-md font-semibold"
          >
            Wyczyść
          </button>
        )}
      </div>

      {/* Action Buttons Row matching Screenshot 2 & 5: [🔍 Szukaj] [📷 Skanuj] [📝 Generuj fiszkę] */}
      <div className="grid grid-cols-3 gap-2">
        <button
          onClick={handleSearchButtonClick}
          className="flex items-center justify-center gap-1.5 py-2.5 px-3 rounded-lg bg-[#2563eb] hover:bg-[#1d4ed8] text-white font-semibold text-xs sm:text-sm shadow-xs active:scale-98 transition-transform cursor-pointer"
        >
          <Search className="w-4 h-4" />
          <span>Szukaj</span>
        </button>

        <button
          onClick={onOpenScanner}
          className="flex items-center justify-center gap-1.5 py-2.5 px-3 rounded-lg bg-[#475467] hover:bg-[#344054] text-white font-semibold text-xs sm:text-sm shadow-xs active:scale-98 transition-transform cursor-pointer"
        >
          <Camera className="w-4 h-4" />
          <span>Skanuj</span>
        </button>

        <button
          onClick={handleGenerateFiszkaClick}
          className="flex items-center justify-center gap-1.5 py-2.5 px-3 rounded-lg bg-[#475467] hover:bg-[#344054] text-white font-semibold text-xs sm:text-sm shadow-xs active:scale-98 transition-transform cursor-pointer"
        >
          <FileText className="w-4 h-4" />
          <span>Generuj fiszkę</span>
        </button>
      </div>

      {/* Search Results Display */}
      {searchQuery.trim() !== '' ? (
        <div className="flex flex-col gap-3.5 mt-2">
          {filteredCarpets.length > 0 ? (
            filteredCarpets.map((carpet) => (
              <div
                key={carpet.id}
                className="bg-white border border-gray-200 rounded-2xl p-4 sm:p-5 shadow-xs hover:border-blue-400 transition-all flex flex-col gap-3"
              >
                {/* NAZWA Block matching Screenshot 2 */}
                <div className="border-b border-gray-100 pb-2.5">
                  <span className="text-[10px] font-extrabold uppercase tracking-wider text-gray-400 block mb-0.5">
                    NAZWA
                  </span>
                  <div className="text-base sm:text-lg font-bold text-gray-900 leading-snug">
                    {carpet.nazwa}
                  </div>
                  {carpet.dodano && (
                    <div className="text-xs text-gray-400 font-medium mt-1 flex items-center gap-1">
                      <Calendar className="w-3.5 h-3.5" />
                      Dodano: {carpet.dodano}
                    </div>
                  )}
                </div>

                {/* REF Block */}
                <div className="flex items-center justify-between bg-gray-50/80 rounded-xl px-3.5 py-2">
                  <div>
                    <span className="text-[10px] font-extrabold uppercase tracking-wider text-gray-400 block">
                      REF.
                    </span>
                    <span className="text-base sm:text-lg font-bold text-gray-900 font-mono">
                      {carpet.ref}
                    </span>
                  </div>
                  <button
                    onClick={() => handleCopy(carpet.ref, `ref-${carpet.id}`)}
                    className="p-1.5 text-gray-400 hover:text-blue-600 rounded-md transition-colors"
                    title="Kopiuj numer referencyjny"
                  >
                    {copiedId === `ref-${carpet.id}` ? (
                      <Check className="w-4 h-4 text-emerald-600" />
                    ) : (
                      <Copy className="w-4 h-4" />
                    )}
                  </button>
                </div>

                {/* EAN Block */}
                <div className="flex items-center justify-between bg-gray-50/80 rounded-xl px-3.5 py-2">
                  <div>
                    <span className="text-[10px] font-extrabold uppercase tracking-wider text-gray-400 block">
                      EAN
                    </span>
                    <span className="text-base sm:text-lg font-bold text-gray-900 font-mono tracking-wider">
                      {carpet.ean}
                    </span>
                  </div>
                  <button
                    onClick={() => handleCopy(carpet.ean, `ean-${carpet.id}`)}
                    className="p-1.5 text-gray-400 hover:text-blue-600 rounded-md transition-colors"
                    title="Kopiuj kod EAN"
                  >
                    {copiedId === `ean-${carpet.id}` ? (
                      <Check className="w-4 h-4 text-emerald-600" />
                    ) : (
                      <Copy className="w-4 h-4" />
                    )}
                  </button>
                </div>

                {/* MIEJSCE Block (Highlight yellow / bold) */}
                <div className="bg-amber-50/80 border border-amber-200 rounded-xl px-3.5 py-2.5 flex items-center justify-between">
                  <div>
                    <span className="text-[10px] font-extrabold uppercase tracking-wider text-amber-800 block">
                      MIEJSCE
                    </span>
                    <span className="text-xl sm:text-2xl font-black text-gray-900 tracking-wide">
                      {carpet.miejsce || `${carpet.magazyn} ${carpet.podmagazyn}`}
                    </span>
                  </div>
                  <div className="flex items-center gap-2">
                    <button
                      onClick={() => onOpenFiszkiModal(carpet)}
                      className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-blue-600 hover:bg-blue-700 text-white font-semibold text-xs shadow-xs transition-colors cursor-pointer"
                    >
                      <FileText className="w-3.5 h-3.5" />
                      Dodaj do fiszek
                    </button>
                    {onPrintSingleFiszka && (
                      <button
                        onClick={() => onPrintSingleFiszka(carpet)}
                        className="p-1.5 rounded-lg text-gray-500 hover:text-gray-900 hover:bg-gray-100 transition-colors cursor-pointer"
                        title="Podgląd / Druk pojedynczej fiszki"
                      >
                        <Tag className="w-4 h-4" />
                      </button>
                    )}
                  </div>
                </div>

                {/* Last modification info */}
                {(carpet.updatedBy || carpet.updatedAt) && (
                  <div className="flex items-center justify-between text-[11px] text-gray-400 px-1 pt-0.5">
                    <span className="truncate">
                      Zmodyfikował: <strong className="font-semibold text-gray-600">{carpet.updatedBy || 'Brak danych'}</strong>
                    </span>
                    {carpet.updatedAt && (
                      <span className="shrink-0 ml-2 text-[10px]">
                        {formatRelativeTime(carpet.updatedAt)}
                      </span>
                    )}
                  </div>
                )}
              </div>
            ))
          ) : (
            <div className="bg-white border border-dashed border-gray-300 rounded-2xl p-8 text-center flex flex-col items-center">
              <Search className="w-10 h-10 text-gray-300 mb-2" />
              <div className="font-bold text-gray-800 text-sm">Nie znaleziono produktu</div>
              <p className="text-xs text-gray-500 mt-1 max-w-xs">
                Brak dywanu odpowiadającego zapytaniu "{searchQuery}". Sprawdź poprawność kodu EAN, numeru referencyjnego lub miejsca (np. "12A").
              </p>
            </div>
          )}
        </div>
      ) : (
        /* Empty / Ready state matching Screenshot 5 with helpful quick suggestions */
        <div className="mt-6 flex flex-col items-center justify-center text-center p-6 bg-white/70 rounded-2xl border border-gray-100 shadow-2xs">
          <div className="w-12 h-12 rounded-full bg-blue-50 text-blue-600 flex items-center justify-center mb-3">
            <Barcode className="w-6 h-6" />
          </div>
          <h3 className="font-bold text-gray-800 text-sm sm:text-base">
            System Ekspozycji Dywanów • Sklep 038 Mikołów
          </h3>
          <p className="text-xs text-gray-500 mt-1 max-w-md">
            Wpisz kod EAN, numer referencyjny produktu lub symbol miejsca (np. <span className="font-semibold text-gray-700">1A</span>, <span className="font-semibold text-gray-700">20A</span>), aby błyskawicznie zlokalizować dywan na regale ekspozycyjnym.
          </p>

          <div className="mt-4 flex flex-wrap justify-center gap-1.5">
            <span className="text-[11px] text-gray-400 self-center mr-1">Przykłady:</span>
            {['1A', '20A', '92116369', '3276007888454'].map((sample) => (
              <button
                key={sample}
                onClick={() => setSearchQuery(sample)}
                className="px-2.5 py-1 bg-gray-100 hover:bg-gray-200 text-gray-700 font-mono text-xs rounded-md transition-colors"
              >
                {sample}
              </button>
            ))}
          </div>
        </div>
      )}

      {/* Floating / Bottom Right Help Button matching Screenshot 5 */}
      <div className="fixed bottom-5 right-5 z-20">
        <button
          onClick={onOpenHelp}
          className="flex items-center gap-1.5 px-4 py-2.5 rounded-full bg-blue-600 hover:bg-blue-700 text-white font-bold text-xs sm:text-sm shadow-lg hover:shadow-xl transition-all active:scale-95"
        >
          <span>Pomoc</span>
          <HelpCircle className="w-4 h-4" />
        </button>
      </div>
    </div>
  );
}
