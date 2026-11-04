import { useState, useMemo } from 'react';
import { Carpet } from '../types';
import { Search, Camera, FileText, MapPin, Tag, Barcode, Calendar, Copy, Check, Sparkles, HelpCircle, Database, CheckCircle2, ArrowLeftRight, ExternalLink } from 'lucide-react';
import { formatRelativeTime, getLeroyMerlinSearchUrl } from '../utils/formatters';
import { searchCatalog } from '../services/catalogService';

interface SearchViewProps {
  carpets: Carpet[];
  searchQuery: string;
  setSearchQuery: (q: string) => void;
  onOpenScanner: () => void;
  onOpenFiszkiModal: (carpetToAdd?: Carpet) => void;
  onPrintSingleFiszka?: (carpet: Carpet) => void;
  onOpenHelp: () => void;
  isAdmin?: boolean;
  onMoveCarpet?: (carpet: Carpet) => void;
}

export function SearchView({
  carpets,
  searchQuery,
  setSearchQuery,
  onOpenScanner,
  onOpenFiszkiModal,
  onPrintSingleFiszka,
  onOpenHelp,
  isAdmin,
  onMoveCarpet
}: SearchViewProps) {
  const [copiedId, setCopiedId] = useState<string | null>(null);

  // Precise search logic:
  // - "1a" / "21 b" -> finds ONLY that specific rack & slot (not 11a, 111a)
  // - "11" -> finds both 11a and 11b of rack 11 (not 111a, 110a)
  // - "92116369" / EAN -> finds exact matching product
  const filteredCarpets = useMemo(() => {
    const rawQuery = searchQuery.trim().toLowerCase();
    if (!rawQuery) return [];

    const cleanQuery = rawQuery.replace(/\s+/g, '');

    // 1. Sprawdź czy to zapytanie o konkretne miejsce z literą (np. "1a", "1 a", "11b", "21 B", "120a")
    const placeWithLetterMatch = rawQuery.match(/^(\d+)\s*[-/]?\s*([a-zA-Z])$/i);
    if (placeWithLetterMatch) {
      const bayNum = parseInt(placeWithLetterMatch[1], 10);
      const subBay = placeWithLetterMatch[2].toUpperCase();

      // Szukamy DOKŁADNIE tego miejsca lub dokładnego kodu REF/EAN
      const exactMatches = carpets.filter((c) => {
        const isExactBay = c.magazyn === bayNum;
        const isExactSub = (c.podmagazyn || '').trim().toUpperCase() === subBay;
        const cleanPlace = (c.miejsce || `${c.magazyn}${c.podmagazyn}`).toLowerCase().replace(/\s+/g, '');
        const isExactPlace = (isExactBay && isExactSub) || cleanPlace === cleanQuery;

        const isExactRef = c.ref?.trim().toLowerCase() === rawQuery;
        const isExactEan = c.ean?.replace(/\s+/g, '') === cleanQuery;

        return isExactPlace || isExactRef || isExactEan;
      });

      if (exactMatches.length > 0) {
        return exactMatches.sort((a, b) => (a.podmagazyn || '').localeCompare(b.podmagazyn || ''));
      }
    }

    // 2. Sprawdź czy to zapytanie o sam numer (np. "11", "1", "21" lub numer REF / EAN)
    const isPureNumber = /^\d+$/.test(cleanQuery);
    if (isPureNumber) {
      const num = parseInt(cleanQuery, 10);

      // Sprawdź dokładne dopasowanie:
      // a) Dokładny numer regału magazynu (np. wpisanie 11 daje dokładnie 11A i 11B, NIE 111A ani 110A!)
      // b) Dokładny kod referencji REF
      // c) Dokładny kod kreskowy EAN
      const exactMatches = carpets.filter((c) => {
        const isExactBay = c.magazyn === num;
        const isExactRef = c.ref?.trim().toLowerCase() === rawQuery;
        const isExactEan = c.ean?.replace(/\s+/g, '') === cleanQuery;
        return isExactBay || isExactRef || isExactEan;
      });

      if (exactMatches.length > 0) {
        return exactMatches.sort((a, b) => {
          // Dokładny REF/EAN ma priorytet na samej górze
          const aIsCode = a.ref?.trim().toLowerCase() === rawQuery || a.ean?.replace(/\s+/g, '') === cleanQuery;
          const bIsCode = b.ref?.trim().toLowerCase() === rawQuery || b.ean?.replace(/\s+/g, '') === cleanQuery;
          if (aIsCode && !bIsCode) return -1;
          if (!aIsCode && bIsCode) return 1;
          // Następnie sortuj podmagazyny A przed B
          return (a.podmagazyn || '').localeCompare(b.podmagazyn || '');
        });
      }

      // Jeśli nie ma dokładnego regału ani dokładnego kodu (np. wpisano częściowy 4-8 cyfrowy kod):
      const partialCodeMatches = carpets.filter((c) => {
        const cleanRef = (c.ref || '').toLowerCase().replace(/\s+/g, '');
        const cleanEan = (c.ean || '').replace(/\s+/g, '');
        return cleanRef.includes(cleanQuery) || cleanEan.includes(cleanQuery);
      });

      if (partialCodeMatches.length > 0) {
        return partialCodeMatches.sort((a, b) => {
          const aStarts = (a.ref || '').startsWith(cleanQuery) || (a.ean || '').startsWith(cleanQuery);
          const bStarts = (b.ref || '').startsWith(cleanQuery) || (b.ean || '').startsWith(cleanQuery);
          if (aStarts && !bStarts) return -1;
          if (!aStarts && bStarts) return 1;
          return a.magazyn - b.magazyn;
        });
      }
    }

    // 3. Sprawdź czy zapytanie dokładnie pasuje do REF, EAN lub pełnego symbolu miejsca
    const exactMatches = carpets.filter((c) => {
      const isExactRef = c.ref?.trim().toLowerCase() === rawQuery;
      const isExactEan = c.ean?.replace(/\s+/g, '') === cleanQuery;
      const cleanPlace = (c.miejsce || `${c.magazyn}${c.podmagazyn}`).toLowerCase().replace(/\s+/g, '');
      const isExactPlace = cleanPlace === cleanQuery;
      return isExactRef || isExactEan || isExactPlace;
    });

    if (exactMatches.length > 0) {
      return exactMatches;
    }

    // 4. Wyszukiwanie tekstowe po nazwie dywanu lub częściowym REF/EAN
    const terms = rawQuery.split(/\s+/).filter(Boolean);

    return carpets
      .filter((c) => {
        const nameLower = (c.nazwa || '').toLowerCase();
        const refLower = (c.ref || '').toLowerCase();
        const eanClean = (c.ean || '').replace(/\s+/g, '');

        // Search in all technical specifications (sklad, grubosc, tekstura, przeznaczenie, itp.)
        const specsText = [
          c.sklad,
          c.wysokoscRuna,
          c.grubosc,
          c.materialGlowny,
          c.tekstura,
          c.miejsceUzytkowania,
          c.marka,
          c.model,
          c.spodAntyposlizgowy,
          c.wlasciwosci,
          c.przeznaczenie
        ].filter(Boolean).join(' ').toLowerCase();

        // Wszystkie wpisane słowa muszą wystąpić w nazwie, ref, ean lub parametrach
        return terms.every((term) => {
          return (
            nameLower.includes(term) ||
            refLower.includes(term) ||
            eanClean.includes(term) ||
            specsText.includes(term)
          );
        });
      })
      .sort((a, b) => {
        const aStarts = (a.nazwa || '').toLowerCase().startsWith(rawQuery);
        const bStarts = (b.nazwa || '').toLowerCase().startsWith(rawQuery);
        if (aStarts && !bStarts) return -1;
        if (!aStarts && bStarts) return 1;
        return a.magazyn - b.magazyn;
      });
  }, [carpets, searchQuery]);

  // Catalog memory fallback matches (items remembered in store master catalog but currently off-shelf)
  const catalogMatches = useMemo(() => {
    if (filteredCarpets.length > 0 || !searchQuery.trim()) return [];
    return searchCatalog(searchQuery, 4);
  }, [filteredCarpets, searchQuery]);

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
    <div className="max-w-4xl lg:max-w-5xl mx-auto px-3 sm:px-6 py-4 sm:py-6 flex flex-col gap-4 pb-12 sm:pb-8">
      {/* Search Input Box matching Screenshots 2 & 5 */}
      <div className="relative">
        <input
          id="search-input-field"
          type="text"
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          placeholder="Wyszukaj po: EAN, ref lub miejsce (np. 12A)"
          className="w-full pl-4 pr-20 py-3 sm:py-3.5 text-base sm:text-lg border-2 border-gray-900 rounded-2xl bg-white shadow-xs focus:ring-2 focus:ring-blue-600 focus:outline-hidden transition-all placeholder:text-gray-400 font-medium"
          autoFocus
        />
        {searchQuery && (
          <button
            onClick={() => setSearchQuery('')}
            className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-700 text-xs bg-gray-100 hover:bg-gray-200 px-2.5 py-1.5 rounded-lg font-semibold transition-colors cursor-pointer"
          >
            Wyczyść
          </button>
        )}
      </div>

      {/* Action Buttons Row: [🔍 Szukaj] [📷 Skanuj] [📝 Generuj fiszkę] */}
      <div className="grid grid-cols-3 gap-2 sm:gap-3">
        <button
          onClick={handleSearchButtonClick}
          className="flex items-center justify-center gap-1.5 py-3 px-3 rounded-xl bg-[#2563eb] hover:bg-[#1d4ed8] text-white font-bold text-xs sm:text-sm shadow-xs active:scale-98 transition-transform cursor-pointer"
        >
          <Search className="w-4 h-4" />
          <span>Szukaj</span>
        </button>

        <button
          onClick={onOpenScanner}
          className="flex items-center justify-center gap-1.5 py-3 px-3 rounded-xl bg-[#475467] hover:bg-[#344054] text-white font-bold text-xs sm:text-sm shadow-xs active:scale-98 transition-transform cursor-pointer"
        >
          <Camera className="w-4 h-4" />
          <span>Skanuj</span>
        </button>

        <button
          onClick={handleGenerateFiszkaClick}
          className="flex items-center justify-center gap-1.5 py-3 px-3 rounded-xl bg-[#475467] hover:bg-[#344054] text-white font-bold text-xs sm:text-sm shadow-xs active:scale-98 transition-transform cursor-pointer"
        >
          <FileText className="w-4 h-4" />
          <span>Generuj fiszkę</span>
        </button>
      </div>

      {/* Szybkie filtry parametrów technicznych (Quick Specs Filters) */}
      <div className="flex items-center gap-1.5 overflow-x-auto pb-1 pt-0.5 no-scrollbar text-xs">
        <span className="text-[11px] font-bold text-gray-500 uppercase shrink-0 mr-1 flex items-center gap-1">
          <Sparkles className="w-3.5 h-3.5 text-amber-500" /> Szybkie:
        </span>
        {[
          { label: 'Wszystkie', value: '' },
          { label: '🌿 Na taras / Zewnętrzny', value: 'zewnętrzny' },
          { label: '🧵 Płaski splot', value: 'płasko' },
          { label: '✨ Shaggy', value: 'shaggy' },
          { label: '🛡️ Antypoślizgowy', value: 'antypoślizgowy' },
          { label: 'Inspire Lagoon', value: 'lagoon' },
          { label: 'Inspire Blida', value: 'blida' },
          { label: 'Inspire Amba', value: 'amba' },
          { label: 'Balta Sparkles', value: 'sparkles' }
        ].map((chip) => {
          const isActive =
            chip.value === ''
              ? searchQuery === ''
              : searchQuery.toLowerCase().includes(chip.value.toLowerCase());
          return (
            <button
              key={chip.label}
              onClick={() => setSearchQuery(chip.value)}
              className={`px-3 py-1.5 rounded-full font-bold transition-all shrink-0 cursor-pointer text-xs border ${
                isActive
                  ? 'bg-blue-600 text-white border-blue-600 shadow-2xs'
                  : 'bg-white hover:bg-gray-100 text-gray-700 border-gray-300'
              }`}
            >
              {chip.label}
            </button>
          );
        })}
      </div>

      {/* Search Results Display */}
      {searchQuery.trim() !== '' ? (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-3.5 mt-2">
          {filteredCarpets.length > 0 ? (
            filteredCarpets.map((carpet) => (
              <div
                key={carpet.id}
                className="bg-white border border-gray-200 rounded-2xl p-4 sm:p-5 shadow-xs hover:border-blue-400 transition-all flex flex-col gap-3"
              >
                {/* NAZWA Block */}
                <div className="border-b border-gray-100 pb-2.5">
                  <span className="text-xs font-black uppercase tracking-wider text-gray-500 block mb-1">
                    NAZWA DYWANU
                  </span>
                  <a
                    href={getLeroyMerlinSearchUrl(carpet)}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="group inline-flex items-center gap-1.5 text-lg sm:text-xl font-black text-gray-900 hover:text-blue-600 transition-colors cursor-pointer leading-snug tracking-tight"
                    title={`Otwórz w Leroy Merlin (REF: ${carpet.ref || carpet.nazwa})`}
                  >
                    <span className="group-hover:underline">{carpet.nazwa}</span>
                    <ExternalLink className="w-4 h-4 text-gray-400 group-hover:text-blue-600 shrink-0 transition-colors" />
                  </a>
                  {carpet.dodano && (
                    <div className="text-xs text-gray-500 font-medium mt-1.5 flex items-center gap-1.5">
                      <Calendar className="w-3.5 h-3.5 text-gray-400" />
                      Dodano: {carpet.dodano}
                    </div>
                  )}
                </div>

                {/* REF Block */}
                <div className="flex items-center justify-between bg-gray-50 hover:bg-gray-100/80 rounded-xl px-4 py-2.5 border border-gray-200/80 transition-colors">
                  <div>
                    <span className="text-xs font-black uppercase tracking-wider text-gray-500 block mb-0.5">
                      REF. LM
                    </span>
                    <span className="text-lg sm:text-xl font-black text-gray-900 font-mono tracking-wide">
                      {carpet.ref}
                    </span>
                  </div>
                  <button
                    onClick={() => handleCopy(carpet.ref, `ref-${carpet.id}`)}
                    className="p-2 text-gray-400 hover:text-blue-600 rounded-lg hover:bg-white transition-colors"
                    title="Kopiuj numer referencyjny"
                  >
                    {copiedId === `ref-${carpet.id}` ? (
                      <Check className="w-5 h-5 text-emerald-600" />
                    ) : (
                      <Copy className="w-5 h-5" />
                    )}
                  </button>
                </div>

                {/* EAN Block */}
                <div className="flex items-center justify-between bg-gray-50 hover:bg-gray-100/80 rounded-xl px-4 py-2.5 border border-gray-200/80 transition-colors">
                  <div>
                    <span className="text-xs font-black uppercase tracking-wider text-gray-500 block mb-0.5">
                      KOD EAN
                    </span>
                    <span className="text-lg sm:text-xl font-black text-gray-900 font-mono tracking-wider">
                      {carpet.ean}
                    </span>
                  </div>
                  <button
                    onClick={() => handleCopy(carpet.ean, `ean-${carpet.id}`)}
                    className="p-2 text-gray-400 hover:text-blue-600 rounded-lg hover:bg-white transition-colors"
                    title="Kopiuj kod EAN"
                  >
                    {copiedId === `ean-${carpet.id}` ? (
                      <Check className="w-5 h-5 text-emerald-600" />
                    ) : (
                      <Copy className="w-5 h-5" />
                    )}
                  </button>
                </div>

                {/* Parametry techniczne Leroy Merlin (jeśli pobrane lub dopasowane) */}
                {(carpet.sklad || carpet.grubosc || carpet.wysokoscRuna || carpet.miejsceUzytkowania || carpet.tekstura || carpet.marka) && (
                  <div className="bg-emerald-50/70 border border-emerald-200/90 rounded-xl p-2.5 text-xs flex flex-col gap-1.5 shadow-2xs">
                    <div className="flex items-center justify-between">
                      <span className="text-[10px] font-black uppercase tracking-wider text-emerald-900 flex items-center gap-1">
                        <Sparkles className="w-3 h-3 text-amber-600" />
                        SPECYFIKACJA LEROY MERLIN
                      </span>
                      {carpet.grubosc && (
                        <span className="font-black text-gray-950 bg-white border border-emerald-300 px-1.5 py-0.5 rounded text-[11px] shadow-2xs font-mono">
                          Grubość: {carpet.grubosc}
                        </span>
                      )}
                    </div>
                    <div className="flex flex-wrap items-center gap-1.5 text-gray-800 text-[11px]">
                      {carpet.sklad && (
                        <span className="font-bold bg-white/90 px-2 py-0.5 rounded border border-emerald-200 text-gray-900">
                          {carpet.sklad}
                        </span>
                      )}
                      {(carpet.wysokoscRuna || carpet.tekstura) && (
                        <span className="bg-white/90 px-2 py-0.5 rounded border border-emerald-200 text-gray-700 font-medium">
                          {carpet.wysokoscRuna || carpet.tekstura}
                        </span>
                      )}
                      {carpet.miejsceUzytkowania && (
                        <span className="font-extrabold text-emerald-800 bg-emerald-100/90 px-2 py-0.5 rounded border border-emerald-300">
                          {carpet.miejsceUzytkowania}
                        </span>
                      )}
                      {carpet.marka && (
                        <span className="text-gray-500 font-semibold text-[10px]">
                          {carpet.marka} {carpet.model ? `• ${carpet.model}` : ''}
                        </span>
                      )}
                    </div>
                  </div>
                )}

                {/* MIEJSCE Block (Highlight yellow / bold) */}
                <div className="bg-amber-50/90 border-2 border-amber-300 rounded-xl px-4 py-3 flex items-center justify-between shadow-2xs">
                  <div>
                    <span className="text-xs font-black uppercase tracking-wider text-amber-900 block mb-0.5">
                      MIEJSCE NA REGALE
                    </span>
                    <span className="text-2xl sm:text-3xl font-black text-gray-950 tracking-wider font-mono">
                      {carpet.miejsce || `${carpet.magazyn} ${carpet.podmagazyn}`}
                    </span>
                  </div>
                  <div className="flex items-center gap-1.5 flex-wrap justify-end">
                    {isAdmin && onMoveCarpet && (
                      <button
                        onClick={() => onMoveCarpet(carpet)}
                        className="flex items-center gap-1 px-2.5 py-1.5 rounded-lg bg-indigo-50 hover:bg-indigo-100 text-indigo-700 font-bold text-xs border border-indigo-200 shadow-2xs transition-colors cursor-pointer"
                        title="Przenieś dywan na inny regał lub zamień miejscami (Swap)"
                      >
                        <ArrowLeftRight className="w-3.5 h-3.5" />
                        <span>Przenieś</span>
                      </button>
                    )}
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
          ) : catalogMatches.length > 0 ? (
            <div className="flex flex-col gap-3">
              <div className="p-3 bg-emerald-50 border border-emerald-200 rounded-2xl flex items-center gap-2 text-xs text-emerald-900 shadow-2xs">
                <Database className="w-4 h-4 text-emerald-600 shrink-0" />
                <span>
                  Znaleziono w <strong>pamięci sklepu</strong> (produkt zdjęty z ekspozycji lub bez przypisanego regału):
                </span>
              </div>

              {catalogMatches.map((catItem) => (
                <div
                  key={catItem.id}
                  className="bg-white border-2 border-emerald-200 rounded-2xl p-4 sm:p-5 shadow-xs flex flex-col gap-2.5"
                >
                  <div className="flex items-start justify-between gap-2 border-b border-gray-100 pb-2">
                    <div>
                      <span className="text-[10px] font-black uppercase tracking-wider text-emerald-800 bg-emerald-100 px-2 py-0.5 rounded-md inline-block mb-1">
                        Pamięć sklepu (100% zgodności)
                      </span>
                      <a
                        href={getLeroyMerlinSearchUrl(catItem)}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="group inline-flex items-center gap-1.5 text-lg sm:text-xl font-black text-gray-900 hover:text-emerald-700 transition-colors cursor-pointer leading-snug"
                        title={`Otwórz w Leroy Merlin (REF: ${catItem.ref || catItem.nazwa})`}
                      >
                        <span className="group-hover:underline">{catItem.nazwa}</span>
                        <ExternalLink className="w-4 h-4 text-gray-400 group-hover:text-emerald-700 shrink-0 transition-colors" />
                      </a>
                    </div>
                  </div>

                  <div className="grid grid-cols-2 gap-2 text-xs">
                    <div className="bg-gray-50 p-3 rounded-xl border border-gray-200">
                      <span className="text-[11px] font-black uppercase tracking-wider text-gray-500 block mb-0.5">
                        REF. LM
                      </span>
                      <span className="font-mono font-black text-base sm:text-lg text-gray-900 tracking-wide">
                        {catItem.ref || 'Brak'}
                      </span>
                    </div>

                    <div className="bg-gray-50 p-3 rounded-xl border border-gray-200">
                      <span className="text-[11px] font-black uppercase tracking-wider text-gray-500 block mb-0.5">
                        KOD EAN
                      </span>
                      <span className="font-mono font-black text-base sm:text-lg text-gray-900 tracking-wider truncate block">
                        {catItem.ean || 'Brak'}
                      </span>
                    </div>
                  </div>

                  <div className="text-[11px] text-gray-500 flex items-center gap-1.5 pt-1">
                    <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600 shrink-0" />
                    <span>
                      Gdy znowu dodasz ten dywan na dowolny regał, zeskanowanie kodu natychmiast uzupełni jego nazwę.
                    </span>
                  </div>
                </div>
              ))}
            </div>
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
          className="flex items-center gap-1.5 px-4 py-2.5 rounded-full bg-blue-600 hover:bg-blue-700 text-white font-bold text-xs sm:text-sm shadow-lg hover:shadow-xl transition-all active:scale-95 cursor-pointer"
        >
          <span>Pomoc</span>
          <HelpCircle className="w-4 h-4" />
        </button>
      </div>
    </div>
  );
}
