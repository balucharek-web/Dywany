import { useState, useMemo, useEffect, useRef } from 'react';
import { Carpet } from '../types';
import { X, Camera, Calendar, Check, Copy, ExternalLink, Trash2, Tag, AlertCircle, ArrowRight } from 'lucide-react';
import { BarcodeScannerModal } from './BarcodeScannerModal';

export interface FiszkaItem {
  id: string;
  ref: string;
  nazwa?: string;
  miejsce?: string;
  ean?: string;
  addedAt: number;
}

interface FiszkiBatchModalProps {
  isOpen: boolean;
  onClose: () => void;
  carpets: Carpet[];
  initialItems?: FiszkaItem[];
  onItemsChange?: (items: FiszkaItem[]) => void;
}

const KAKEMONO_URL = 'https://kakemono.gke-pllm-prod-land.priv.manawa.adeo.cloud/38/tag/';

export function FiszkiBatchModal({
  isOpen,
  onClose,
  carpets,
  initialItems = [],
  onItemsChange
}: FiszkiBatchModalProps) {
  const [inputValue, setInputValue] = useState('');
  const [selectedDate, setSelectedDate] = useState('');
  const [items, setItems] = useState<FiszkaItem[]>(initialItems);
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error' | 'info'; text: string } | null>(null);
  const [isScannerOpen, setIsScannerOpen] = useState(false);
  const [scanToast, setScanToast] = useState<string | null>(null);

  // Finishing & Countdown state
  const [isFinished, setIsFinished] = useState(false);
  const [countdown, setCountdown] = useState<number | null>(null);
  const [copiedString, setCopiedString] = useState<string>('');
  const countdownTimerRef = useRef<any>(null);

  // Sync internal items with parent if provided
  useEffect(() => {
    if (initialItems && initialItems.length > 0) {
      setItems(initialItems);
    }
  }, [initialItems]);

  const updateItems = (newItems: FiszkaItem[]) => {
    setItems(newItems);
    if (onItemsChange) {
      onItemsChange(newItems);
    }
  };

  // Available unique dates from carpets
  const availableDates = useMemo(() => {
    const dateMap = new Map<string, number>();
    carpets.forEach((c) => {
      if (c.dodano) {
        const d = c.dodano.trim();
        dateMap.set(d, (dateMap.get(d) || 0) + 1);
      }
    });

    return Array.from(dateMap.entries())
      .map(([date, count]) => ({ date, count }))
      .sort((a, b) => b.date.localeCompare(a.date));
  }, [carpets]);

  // Set default selected date if not set
  useEffect(() => {
    if (availableDates.length > 0 && !selectedDate) {
      setSelectedDate(availableDates[0].date);
    }
  }, [availableDates, selectedDate]);

  // Clear countdown timer on unmount or close
  useEffect(() => {
    return () => {
      if (countdownTimerRef.current) {
        clearInterval(countdownTimerRef.current);
      }
    };
  }, []);

  if (!isOpen) return null;

  const showFeedback = (text: string, type: 'success' | 'error' | 'info' = 'info') => {
    setFeedback({ text, type });
    setTimeout(() => {
      setFeedback(null);
    }, 3500);
  };

  /**
   * Adds reference(s) to the list, preventing exact duplicates
   */
  const addReferences = (newItems: Array<{ ref: string; nazwa?: string; miejsce?: string; ean?: string }>) => {
    if (newItems.length === 0) return 0;

    const existingRefs = new Set(items.map((i) => i.ref.trim()));
    const toAdd: FiszkaItem[] = [];

    newItems.forEach((item) => {
      const cleanRef = item.ref.trim();
      if (cleanRef && !existingRefs.has(cleanRef)) {
        existingRefs.add(cleanRef);
        toAdd.push({
          id: `${cleanRef}-${Date.now()}-${Math.random().toString(36).substring(2, 6)}`,
          ref: cleanRef,
          nazwa: item.nazwa,
          miejsce: item.miejsce,
          ean: item.ean,
          addedAt: Date.now()
        });
      }
    });

    if (toAdd.length > 0) {
      updateItems([...items, ...toAdd]);
      return toAdd.length;
    }
    return 0;
  };

  /**
   * Handle text input submission: can be a location (12A, 12), a ref (82719283), or EAN (590044...)
   */
  const handleAddInput = () => {
    const raw = inputValue.trim();
    if (!raw) {
      showFeedback('Wpisz miejsce magazynowe, referencję lub kod EAN.', 'error');
      return;
    }

    const clean = raw.replace(/\s+/g, '').toUpperCase();
    const cleanDigits = raw.replace(/\D/g, '');

    // 1. Check if matches location: e.g. "12A", "12 A", "1A"
    const matchedByPlace = carpets.filter((c) => {
      const place = (c.miejsce || `${c.magazyn}${c.podmagazyn}`).replace(/\s+/g, '').toUpperCase();
      return place === clean;
    });

    if (matchedByPlace.length > 0) {
      const added = addReferences(
        matchedByPlace.map((c) => ({
          ref: c.ref,
          nazwa: c.nazwa,
          miejsce: c.miejsce || `${c.magazyn}${c.podmagazyn}`,
          ean: c.ean
        }))
      );
      setInputValue('');
      if (added > 0) {
        showFeedback(`Dodano ${added} ref z miejsca ${raw}.`, 'success');
      } else {
        showFeedback(`Referencja z miejsca ${raw} znajduje się już na liście.`, 'info');
      }
      return;
    }

    // 2. Check if user typed just a bay number, e.g. "12" -> all carpets in bay 12
    if (/^\d{1,2}$/.test(raw)) {
      const bayNum = parseInt(raw, 10);
      const matchedBay = carpets.filter((c) => c.magazyn === bayNum);
      if (matchedBay.length > 0) {
        const added = addReferences(
          matchedBay.map((c) => ({
            ref: c.ref,
            nazwa: c.nazwa,
            miejsce: c.miejsce || `${c.magazyn}${c.podmagazyn}`,
            ean: c.ean
          }))
        );
        setInputValue('');
        if (added > 0) {
          showFeedback(`Dodano ${added} ref z regału ${bayNum}.`, 'success');
        } else {
          showFeedback(`Wszystkie dywany z regału ${bayNum} są już na liście.`, 'info');
        }
        return;
      }
    }

    // 3. Check if matches EAN in database
    if (cleanDigits.length >= 8) {
      const matchedByEan = carpets.find((c) => c.ean?.replace(/\D/g, '') === cleanDigits);
      if (matchedByEan && matchedByEan.ref) {
        const added = addReferences([
          {
            ref: matchedByEan.ref,
            nazwa: matchedByEan.nazwa,
            miejsce: matchedByEan.miejsce || `${matchedByEan.magazyn}${matchedByEan.podmagazyn}`,
            ean: matchedByEan.ean
          }
        ]);
        setInputValue('');
        if (added > 0) {
          showFeedback(`Dodano ref ${matchedByEan.ref} (${matchedByEan.nazwa || 'dywan'}).`, 'success');
        } else {
          showFeedback(`Referencja ${matchedByEan.ref} jest już na liście.`, 'info');
        }
        return;
      }

      // 4. Check if matches Ref in database
      const matchedByRef = carpets.find((c) => c.ref?.replace(/\s+/g, '') === raw);
      if (matchedByRef) {
        const added = addReferences([
          {
            ref: matchedByRef.ref,
            nazwa: matchedByRef.nazwa,
            miejsce: matchedByRef.miejsce || `${matchedByRef.magazyn}${matchedByRef.podmagazyn}`,
            ean: matchedByRef.ean
          }
        ]);
        setInputValue('');
        if (added > 0) {
          showFeedback(`Dodano ref ${matchedByRef.ref} (${matchedByRef.nazwa || 'dywan'}).`, 'success');
        } else {
          showFeedback(`Referencja ${matchedByRef.ref} jest już na liście.`, 'info');
        }
        return;
      }
    }

    // 5. If not matched in database, but is an 8-digit reference (standard Leroy Merlin REF)
    if (cleanDigits.length === 8) {
      const added = addReferences([
        {
          ref: cleanDigits,
          nazwa: 'Wprowadzona ręcznie',
          miejsce: 'Baza'
        }
      ]);
      setInputValue('');
      if (added > 0) {
        showFeedback(`Dodano referencję ${cleanDigits}.`, 'success');
      } else {
        showFeedback(`Referencja ${cleanDigits} jest już na liście.`, 'info');
      }
      return;
    }

    // 6. If plain reference code entered
    if (raw.length >= 6 && raw.length <= 12) {
      const added = addReferences([
        {
          ref: raw,
          nazwa: 'Wprowadzona referencja'
        }
      ]);
      setInputValue('');
      if (added > 0) {
        showFeedback(`Dodano referencję ${raw}.`, 'success');
      } else {
        showFeedback(`Referencja ${raw} jest już na liście.`, 'info');
      }
      return;
    }

    showFeedback(`Nie znaleziono miejsca ani produktu dla "${raw}".`, 'error');
  };

  /**
   * Handle adding all carpets from selected date
   */
  const handleAddByDate = () => {
    if (!selectedDate) {
      showFeedback('Wybierz datę z listy.', 'error');
      return;
    }

    const matchedCarpets = carpets.filter((c) => c.dodano === selectedDate && c.ref);
    if (matchedCarpets.length === 0) {
      showFeedback(`Brak dywanów z datą ${selectedDate}.`, 'error');
      return;
    }

    const added = addReferences(
      matchedCarpets.map((c) => ({
        ref: c.ref,
        nazwa: c.nazwa,
        miejsce: c.miejsce || `${c.magazyn}${c.podmagazyn}`,
        ean: c.ean
      }))
    );

    if (added > 0) {
      showFeedback(`Dodano ${added} referencji z dnia ${selectedDate}.`, 'success');
    } else {
      showFeedback(`Wszystkie referencje z dnia ${selectedDate} są już na liście.`, 'info');
    }
  };

  /**
   * Handle barcode scanned from camera
   */
  const handleScanSuccess = (code: string) => {
    const cleanDigits = code.replace(/\D/g, '');

    // Look for matching carpet by EAN or REF
    const matched = carpets.find(
      (c) => c.ean?.replace(/\D/g, '') === cleanDigits || c.ref?.replace(/\D/g, '') === cleanDigits
    );

    if (matched && matched.ref) {
      const added = addReferences([
        {
          ref: matched.ref,
          nazwa: matched.nazwa,
          miejsce: matched.miejsce || `${matched.magazyn}${matched.podmagazyn}`,
          ean: matched.ean
        }
      ]);
      if (added > 0) {
        setScanToast(`✅ Dodano ref: ${matched.ref} (${matched.miejsce || ''})`);
      } else {
        setScanToast(`ℹ️ Ref ${matched.ref} jest już na liście`);
      }
    } else if (cleanDigits.length === 8) {
      // 8-digit ref
      const added = addReferences([
        {
          ref: cleanDigits,
          nazwa: 'Zeskanowany kod REF'
        }
      ]);
      if (added > 0) {
        setScanToast(`✅ Dodano ref: ${cleanDigits}`);
      } else {
        setScanToast(`ℹ️ Ref ${cleanDigits} jest już na liście`);
      }
    } else if (cleanDigits.length === 13) {
      // EAN not in DB
      setScanToast(`⚠️ EAN ${cleanDigits} nie ma przypisanej referencji w bazie`);
    }

    setTimeout(() => {
      setScanToast(null);
    }, 2500);
  };

  const handleRemoveItem = (id: string) => {
    updateItems(items.filter((i) => i.id !== id));
  };

  const handleClearAll = () => {
    updateItems([]);
    showFeedback('Wyczyszczono listę.', 'info');
  };

  /**
   * User clicks "Zakończ"
   * 1. Copies all references to clipboard separated by commas
   * 2. Starts 5-second countdown
   * 3. Redirects to Kakemono URL in a new tab
   */
  const handleFinish = () => {
    if (items.length === 0) {
      showFeedback('Lista referencji jest pusta. Dodaj przynajmniej jedną referencję.', 'error');
      return;
    }

    const uniqueRefs = Array.from(new Set(items.map((i) => i.ref.trim()))).filter(Boolean);
    const textToCopy = uniqueRefs.join(','); // Comma separated as requested

    // Copy to clipboard
    try {
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(textToCopy);
      } else {
        const textarea = document.createElement('textarea');
        textarea.value = textToCopy;
        document.body.appendChild(textarea);
        textarea.select();
        document.execCommand('copy');
        document.body.removeChild(textarea);
      }
    } catch (err) {
      console.warn('Clipboard write error:', err);
    }

    setCopiedString(textToCopy);
    setIsFinished(true);
    setCountdown(5);

    // Start 5-second countdown
    if (countdownTimerRef.current) {
      clearInterval(countdownTimerRef.current);
    }

    let remaining = 5;
    countdownTimerRef.current = setInterval(() => {
      remaining -= 1;
      setCountdown(remaining);

      if (remaining <= 0) {
        clearInterval(countdownTimerRef.current);
        // Open Kakemono in new tab
        window.open(KAKEMONO_URL, '_blank', 'noopener,noreferrer');
      }
    }, 1000);
  };

  const handleOpenNow = () => {
    if (countdownTimerRef.current) {
      clearInterval(countdownTimerRef.current);
    }
    setCountdown(0);
    window.open(KAKEMONO_URL, '_blank', 'noopener,noreferrer');
  };

  const handleCancelCountdown = () => {
    if (countdownTimerRef.current) {
      clearInterval(countdownTimerRef.current);
    }
    setCountdown(null);
    setIsFinished(false);
  };

  return (
    <>
      <div className="fixed inset-0 z-50 bg-black/60 flex items-center justify-center p-3 sm:p-4 backdrop-blur-xs">
        <div className="bg-white rounded-2xl max-w-lg w-full shadow-2xl overflow-hidden flex flex-col max-h-[92vh] animate-in fade-in zoom-in-95 duration-150">
          {/* Header matching exact layout in Screenshot */}
          <div className="p-5 sm:p-6 pb-2">
            <div className="flex items-start justify-between">
              <div>
                <h2 className="text-xl sm:text-2xl font-black text-gray-900 tracking-tight">
                  Dodaj miejsca magazynowe
                </h2>
                <p className="text-xs sm:text-sm text-gray-700 font-medium mt-1">
                  Wpisz miejsce magazynowe (np. 1A, 20B) lub skanuj EAN w trybie fiszek.
                </p>
              </div>
              <button
                onClick={onClose}
                className="text-gray-400 hover:text-gray-700 p-1.5 rounded-lg hover:bg-gray-100 transition-colors"
                title="Zamknij"
              >
                <X className="w-5 h-5" />
              </button>
            </div>
          </div>

          {/* Feedback Banner */}
          {feedback && (
            <div
              className={`mx-5 sm:mx-6 px-3.5 py-2 rounded-xl text-xs sm:text-sm font-semibold flex items-center gap-2 ${
                feedback.type === 'success'
                  ? 'bg-emerald-50 text-emerald-800 border border-emerald-200'
                  : feedback.type === 'error'
                  ? 'bg-red-50 text-red-800 border border-red-200'
                  : 'bg-blue-50 text-blue-800 border border-blue-200'
              }`}
            >
              {feedback.type === 'success' && <Check className="w-4 h-4 text-emerald-600 shrink-0" />}
              {feedback.type === 'error' && <AlertCircle className="w-4 h-4 text-red-600 shrink-0" />}
              <span>{feedback.text}</span>
            </div>
          )}

          {/* If finished: show countdown & redirection screen */}
          {isFinished ? (
            <div className="p-6 flex flex-col items-center text-center gap-4 my-auto">
              <div className="w-16 h-16 rounded-full bg-emerald-100 flex items-center justify-center text-emerald-600 shadow-inner">
                <Check className="w-8 h-8 stroke-[3]" />
              </div>

              <div>
                <h3 className="text-lg sm:text-xl font-black text-gray-900">
                  Referencje skopiowane do schowka!
                </h3>
                <p className="text-xs sm:text-sm text-gray-600 font-medium mt-1">
                  Liczba referencji:{' '}
                  <span className="font-bold text-gray-900">{items.length}</span>
                </p>
              </div>

              {/* Copied content box */}
              <div className="w-full bg-gray-50 border border-gray-200 rounded-xl p-3 text-left">
                <div className="text-[10px] font-bold uppercase tracking-wider text-gray-500 mb-1 flex items-center justify-between">
                  <span>Skopiowana treść (oddzielona przecinkami):</span>
                  <button
                    onClick={() => {
                      navigator.clipboard.writeText(copiedString);
                      showFeedback('Skopiowano ponownie do schowka!', 'success');
                    }}
                    className="text-blue-600 hover:text-blue-800 font-bold lowercase flex items-center gap-1"
                  >
                    <Copy className="w-3 h-3" /> kopiuj
                  </button>
                </div>
                <div className="text-xs font-mono font-bold text-gray-800 break-all select-all max-h-24 overflow-y-auto">
                  {copiedString}
                </div>
              </div>

              {/* Countdown indicator */}
              <div className="w-full bg-blue-50 border border-blue-200 rounded-xl p-4 flex flex-col items-center gap-2">
                <div className="flex items-center gap-2 text-sm sm:text-base font-extrabold text-blue-900">
                  <span>Przekierowanie do Kakemono za:</span>
                  <span className="inline-flex items-center justify-center w-8 h-8 rounded-full bg-blue-600 text-white font-black text-lg shadow-sm animate-pulse">
                    {countdown !== null && countdown > 0 ? countdown : 0}
                  </span>
                  <span>sekund</span>
                </div>
                <div className="w-full bg-blue-200 h-2 rounded-full overflow-hidden mt-1">
                  <div
                    className="bg-blue-600 h-full transition-all duration-1000 ease-linear rounded-full"
                    style={{ width: `${((countdown ?? 0) / 5) * 100}%` }}
                  />
                </div>
              </div>

              {/* Action Buttons for countdown */}
              <div className="w-full flex flex-col sm:flex-row gap-2 mt-2">
                <button
                  onClick={handleOpenNow}
                  className="flex-1 py-3 px-4 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-bold text-sm flex items-center justify-center gap-2 shadow-md active:scale-98 transition-all"
                >
                  <ExternalLink className="w-4 h-4" />
                  Przejdź teraz do Kakemono
                </button>
                <button
                  onClick={handleCancelCountdown}
                  className="py-3 px-4 rounded-xl bg-gray-100 hover:bg-gray-200 text-gray-700 font-bold text-sm transition-colors"
                >
                  Anuluj
                </button>
              </div>
            </div>
          ) : (
            <div className="p-5 sm:p-6 pt-3 flex-1 overflow-y-auto flex flex-col gap-4">
              {/* Row 1: [np. 12A] [Dodaj] [📸 Skanuj EAN] */}
              <div className="flex items-center gap-2">
                <div className="flex-1">
                  <input
                    type="text"
                    value={inputValue}
                    onChange={(e) => setInputValue(e.target.value)}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter') {
                        e.preventDefault();
                        handleAddInput();
                      }
                    }}
                    placeholder="np. 12A"
                    className="w-full px-3.5 py-2.5 text-sm sm:text-base border border-gray-300 rounded-xl bg-white shadow-xs focus:ring-2 focus:ring-blue-600 focus:outline-hidden transition-all placeholder:text-gray-400 font-medium"
                    autoFocus
                  />
                </div>
                <button
                  onClick={handleAddInput}
                  className="py-2.5 px-4 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-bold text-sm shadow-xs active:scale-95 transition-all"
                >
                  Dodaj
                </button>
                <button
                  onClick={() => setIsScannerOpen(true)}
                  className="py-2.5 px-3.5 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-bold text-sm flex items-center gap-1.5 shadow-xs active:scale-95 transition-all shrink-0"
                >
                  <Camera className="w-4 h-4" />
                  <span>Skanuj EAN</span>
                </button>
              </div>

              {/* Row 2: [Wybierz datę v] [Dodaj po dacie] */}
              <div className="flex items-center gap-2">
                <div className="flex-1">
                  <select
                    value={selectedDate}
                    onChange={(e) => setSelectedDate(e.target.value)}
                    className="w-full px-3.5 py-2.5 text-sm border border-gray-300 rounded-xl bg-white shadow-xs focus:ring-2 focus:ring-blue-600 focus:outline-hidden transition-all text-gray-800 font-medium"
                  >
                    <option value="">Wybierz datę...</option>
                    {availableDates.map(({ date, count }) => (
                      <option key={date} value={date}>
                        {date} ({count} {count === 1 ? 'dywan' : count < 5 ? 'dywany' : 'dywanów'})
                      </option>
                    ))}
                  </select>
                </div>
                <button
                  onClick={handleAddByDate}
                  disabled={!selectedDate}
                  className="py-2.5 px-4 rounded-xl bg-[#475467] hover:bg-[#344054] disabled:opacity-50 text-white font-bold text-sm shadow-xs active:scale-95 transition-all shrink-0"
                >
                  Dodaj po dacie
                </button>
              </div>

              {/* Row 3: Items list container matching exact rounded container in Screenshot */}
              <div className="flex flex-col gap-2">
                <div className="flex items-center justify-between text-xs font-bold text-gray-500 uppercase tracking-wider px-1">
                  <span>Wybrane referencje ({items.length})</span>
                  {items.length > 0 && (
                    <button
                      onClick={handleClearAll}
                      className="text-red-600 hover:text-red-700 flex items-center gap-1 text-[11px] font-bold lowercase"
                    >
                      <Trash2 className="w-3 h-3" /> wyczyść listę
                    </button>
                  )}
                </div>

                <div className="border border-gray-200 rounded-xl p-3 min-h-[90px] max-h-56 overflow-y-auto bg-gray-50/70 flex flex-wrap gap-2 items-start content-start">
                  {items.length === 0 ? (
                    <div className="w-full h-full flex flex-col items-center justify-center text-center text-gray-400 py-4 text-xs font-medium">
                      <span>Brak dodanych referencji.</span>
                      <span className="text-[11px] text-gray-400 mt-0.5">
                        Wpisz miejsce (np. 12A), ref, zeskanuj EAN lub dodaj po dacie.
                      </span>
                    </div>
                  ) : (
                    items.map((item) => (
                      <div
                        key={item.id}
                        className="inline-flex items-center gap-1.5 bg-white border border-gray-300 rounded-lg px-2.5 py-1.5 text-xs shadow-xs hover:border-blue-400 transition-colors"
                      >
                        <Tag className="w-3 h-3 text-blue-600 shrink-0" />
                        <span className="font-extrabold text-gray-900">{item.ref}</span>
                        {item.miejsce && (
                          <span className="text-[10px] font-bold bg-amber-100 text-amber-900 px-1.5 py-0.5 rounded">
                            {item.miejsce}
                          </span>
                        )}
                        {item.nazwa && item.nazwa !== 'Wprowadzona ręcznie' && (
                          <span
                            className="text-[10px] text-gray-500 max-w-[110px] truncate"
                            title={item.nazwa}
                          >
                            {item.nazwa}
                          </span>
                        )}
                        <button
                          onClick={() => handleRemoveItem(item.id)}
                          className="text-gray-400 hover:text-red-600 hover:bg-gray-100 p-0.5 rounded-sm transition-colors ml-0.5"
                          title="Usuń"
                        >
                          <X className="w-3.5 h-3.5" />
                        </button>
                      </div>
                    ))
                  )}
                </div>
              </div>

              {/* Bottom Buttons matching exact look: [Zakończ] [Zamknij] */}
              <div className="grid grid-cols-2 gap-3 pt-2 mt-auto">
                <button
                  onClick={handleFinish}
                  disabled={items.length === 0}
                  className="w-full py-3 px-5 rounded-xl bg-blue-600 hover:bg-blue-700 disabled:opacity-50 text-white font-bold text-sm sm:text-base shadow-md active:scale-98 transition-all flex items-center justify-center gap-2"
                >
                  <Check className="w-4 h-4 stroke-[3]" />
                  <span>Zakończ</span>
                </button>
                <button
                  onClick={onClose}
                  className="w-full py-3 px-5 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-bold text-sm sm:text-base shadow-md active:scale-98 transition-all flex items-center justify-center"
                >
                  <span>Zamknij</span>
                </button>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Embedded Barcode Scanner for instant ESL label scanning */}
      {isScannerOpen && (
        <BarcodeScannerModal
          isOpen={isScannerOpen}
          onClose={() => setIsScannerOpen(false)}
          onScanSuccess={(code) => {
            handleScanSuccess(code);
          }}
          title="Skanuj etykiety do fiszek"
          continuousMode={true}
          toastMessage={scanToast}
        />
      )}
    </>
  );
}
