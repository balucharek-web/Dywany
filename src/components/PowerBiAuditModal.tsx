import { useState, useMemo, useRef } from 'react';
import {
  X,
  ExternalLink,
  ClipboardPaste,
  Upload,
  CheckCircle2,
  AlertTriangle,
  Layers,
  Printer,
  Trash2,
  RefreshCw,
  Search,
  PlusCircle,
  HelpCircle,
  Eye,
  FileSpreadsheet
} from 'lucide-react';
import { Carpet, PowerBiStockExpoItem, STORE_NUMBER, STORE_CITY, POWER_BI_STOCK_EXPO_URL, POWER_BI_EMBED_URL } from '../types';
import {
  parsePastedPowerBiText,
  parsePowerBiFile,
  comparePowerBiWithCarpets
} from '../services/powerBiAuditService';
import { LeroyMerlinLogo } from './LeroyMerlinLogo';

interface PowerBiAuditModalProps {
  isOpen: boolean;
  onClose: () => void;
  carpets: Carpet[];
  pbiItems: PowerBiStockExpoItem[];
  pbiUpdatedAt?: string;
  pbiUpdatedBy?: string;
  onSavePbiItems: (items: PowerBiStockExpoItem[]) => Promise<void>;
  onAssignToRack: (ref: string, nazwa: string) => void;
}

export function PowerBiAuditModal({
  isOpen,
  onClose,
  carpets,
  pbiItems,
  pbiUpdatedAt,
  pbiUpdatedBy,
  onSavePbiItems,
  onAssignToRack
}: PowerBiAuditModalProps) {
  const [activeTab, setActiveTab] = useState<'missing' | 'matched' | 'excess' | 'all' | 'embed'>('missing');
  const [pasteInput, setPasteInput] = useState('');
  const [isPastingOpen, setIsPastingOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [isProcessing, setIsProcessing] = useState(false);
  const [feedbackMsg, setFeedbackMsg] = useState<{ type: 'success' | 'error'; text: string } | null>(null);
  const fileInputRef = useRef<HTMLInputElement | null>(null);

  // Compute comparison results
  const comparison = useMemo(() => {
    return comparePowerBiWithCarpets(pbiItems, carpets);
  }, [pbiItems, carpets]);

  // Filter items based on active tab & search query
  const filteredList = useMemo(() => {
    const q = searchQuery.toLowerCase().trim();

    if (activeTab === 'missing') {
      return comparison.missingOnShelf.filter(
        (i) => i.ref.toLowerCase().includes(q) || i.opis.toLowerCase().includes(q)
      );
    }
    if (activeTab === 'matched') {
      return comparison.matched.filter(
        (m) =>
          m.pbi.ref.toLowerCase().includes(q) ||
          m.pbi.opis.toLowerCase().includes(q) ||
          m.carpet.miejsce.toLowerCase().includes(q)
      );
    }
    if (activeTab === 'excess') {
      return comparison.excessOnShelf.filter(
        (c) => c.ref.toLowerCase().includes(q) || c.nazwa.toLowerCase().includes(q) || c.miejsce.toLowerCase().includes(q)
      );
    }
    if (activeTab === 'all') {
      return comparison.pbiItems.filter(
        (i) => i.ref.toLowerCase().includes(q) || i.opis.toLowerCase().includes(q)
      );
    }
    return [];
  }, [activeTab, comparison, searchQuery]);

  if (!isOpen) return null;

  // Handle Paste parsing
  const handleProcessPastedText = async () => {
    if (!pasteInput.trim()) return;
    setIsProcessing(true);
    setFeedbackMsg(null);

    try {
      const parsed = parsePastedPowerBiText(pasteInput);
      if (parsed.length === 0) {
        throw new Error('Nie udało się rozpoznać wierszy tabeli Power BI. Upewnij się, że skopiowano dane ze schowka.');
      }
      await onSavePbiItems(parsed);
      setPasteInput('');
      setIsPastingOpen(false);
      setFeedbackMsg({
        type: 'success',
        text: `Zapisano ${parsed.length} pozycji z Power BI do bazy sklepu.`
      });
      setActiveTab('missing');
    } catch (err: unknown) {
      setFeedbackMsg({
        type: 'error',
        text: err instanceof Error ? err.message : String(err)
      });
    } finally {
      setIsProcessing(false);
    }
  };

  // Handle File Upload (XLSX / CSV)
  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setIsProcessing(true);
    setFeedbackMsg(null);

    try {
      const buffer = await file.arrayBuffer();
      const parsed = parsePowerBiFile(buffer);
      if (parsed.length === 0) {
        throw new Error('Wybrany plik nie zawiera poprawnych rekordów ze Stock Expo Power BI.');
      }
      await onSavePbiItems(parsed);
      setFeedbackMsg({
        type: 'success',
        text: `Wczytano ${parsed.length} pozycji z pliku ${file.name}.`
      });
      setActiveTab('missing');
    } catch (err: unknown) {
      setFeedbackMsg({
        type: 'error',
        text: err instanceof Error ? err.message : String(err)
      });
    } finally {
      setIsProcessing(false);
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    }
  };

  // Print missing rugs report
  const handlePrintMissing = () => {
    window.print();
  };

  return (
    <div className="pbi-modal-backdrop fixed inset-0 z-50 bg-black/60 backdrop-blur-xs flex items-center justify-center p-2 sm:p-4 overflow-hidden animate-in fade-in">
      {/* Hidden file input */}
      <input
        type="file"
        ref={fileInputRef}
        onChange={handleFileUpload}
        accept=".xlsx,.xls,.csv"
        className="hidden"
      />

      <div className="pbi-modal-container bg-white rounded-3xl sm:rounded-2xl w-full max-w-5xl h-[94vh] flex flex-col shadow-2xl overflow-hidden border border-gray-200">
        {/* =========================================================================
            1. ON-SCREEN MODAL UI (HIDDEN ON PRINT via pbi-screen-only)
           ========================================================================= */}
        <div className="pbi-screen-only flex flex-col h-full overflow-hidden">
          {/* Header */}
          <div className="px-4 py-3.5 sm:px-6 sm:py-4 border-b border-gray-200 flex flex-wrap items-center justify-between gap-3 bg-gray-50/80 shrink-0">
            <div className="flex items-center gap-3">
              <LeroyMerlinLogo className="h-9 sm:h-10" />
              <div>
                <div className="flex items-center gap-2">
                  <h2 className="text-base sm:text-lg font-black text-gray-900 tracking-tight">
                    Audyt Stock Expo (Power BI)
                  </h2>
                  <span className="px-2 py-0.5 text-[11px] font-extrabold bg-blue-100 text-blue-700 rounded-md">
                    Sklep {STORE_NUMBER} {STORE_CITY}
                  </span>
                </div>
                <p className="text-xs text-gray-500 font-medium hidden sm:block">
                  Porównanie oficjalnego stanu ekspozycji Power BI z fizycznymi regałami w sklepie
                </p>
              </div>
            </div>

            <div className="flex items-center gap-2">
              {/* Open direct in Power BI button */}
              <a
                href={POWER_BI_STOCK_EXPO_URL}
                target="_blank"
                rel="noopener noreferrer"
                className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-amber-500 hover:bg-amber-600 text-gray-950 font-black text-xs shadow-xs transition-all cursor-pointer"
                title="Otwórz raport bezpośrednio w Power BI (w nowej karcie z Twoim logowaniem służbowym)"
              >
                <ExternalLink className="w-3.5 h-3.5" />
                <span className="hidden xs:inline">Otwórz w Power BI</span>
              </a>

              {/* Print button */}
              <button
                onClick={handlePrintMissing}
                className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-gray-900 hover:bg-black text-white font-bold text-xs transition-all shadow-xs cursor-pointer active:scale-95"
                title="Wydrukuj listę brakujących dywanów do powieszenia (A4)"
              >
                <Printer className="w-3.5 h-3.5 text-amber-400" />
                <span>Drukuj listę ({comparison.missingOnShelf.length})</span>
              </button>

              {/* Close button */}
              <button
                onClick={onClose}
                className="p-1.5 rounded-xl text-gray-400 hover:text-gray-700 hover:bg-gray-100 transition-colors cursor-pointer"
              >
                <X className="w-5 h-5" />
              </button>
            </div>
          </div>

        {/* Top summary card */}
        <div className="p-3 sm:p-5 border-b border-gray-100 bg-white shrink-0 flex flex-col gap-3">
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 sm:gap-3">
            {/* Rate */}
            <div className="p-3 rounded-2xl bg-blue-50 border border-blue-100 flex flex-col justify-between">
              <span className="text-[10px] font-black text-blue-700 uppercase tracking-wider">
                Zgodność ekspozycji
              </span>
              <div className="flex items-baseline gap-1 mt-1">
                <span className="text-xl sm:text-2xl font-black text-blue-900">
                  {comparison.totalPbi > 0 ? `${comparison.complianceRate}%` : 'Brak danych'}
                </span>
                {comparison.totalPbi > 0 && (
                  <span className="text-xs text-blue-700 font-semibold">
                    ({comparison.matched.length}/{comparison.totalPbi})
                  </span>
                )}
              </div>
            </div>

            {/* Missing */}
            <div
              onClick={() => setActiveTab('missing')}
              className={`p-3 rounded-2xl border transition-all cursor-pointer ${
                activeTab === 'missing'
                  ? 'bg-red-50 border-red-300 ring-2 ring-red-400'
                  : 'bg-red-50/60 border-red-100 hover:bg-red-50'
              }`}
            >
              <span className="text-[10px] font-black text-red-700 uppercase tracking-wider flex items-center justify-between">
                <span>Braki do powieszenia</span>
                <span className="w-2 h-2 rounded-full bg-red-500 animate-pulse" />
              </span>
              <div className="text-xl sm:text-2xl font-black text-red-600 mt-1">
                {comparison.missingOnShelf.length} szt.
              </div>
            </div>

            {/* Matched */}
            <div
              onClick={() => setActiveTab('matched')}
              className={`p-3 rounded-2xl border transition-all cursor-pointer ${
                activeTab === 'matched'
                  ? 'bg-emerald-50 border-emerald-300 ring-2 ring-emerald-400'
                  : 'bg-emerald-50/60 border-emerald-100 hover:bg-emerald-50'
              }`}
            >
              <span className="text-[10px] font-black text-emerald-700 uppercase tracking-wider">
                Na regałach (OK)
              </span>
              <div className="text-xl sm:text-2xl font-black text-emerald-700 mt-1">
                {comparison.matched.length} szt.
              </div>
            </div>

            {/* Excess */}
            <div
              onClick={() => setActiveTab('excess')}
              className={`p-3 rounded-2xl border transition-all cursor-pointer ${
                activeTab === 'excess'
                  ? 'bg-amber-50 border-amber-300 ring-2 ring-amber-400'
                  : 'bg-amber-50/60 border-amber-100 hover:bg-amber-50'
              }`}
            >
              <span className="text-[10px] font-black text-amber-800 uppercase tracking-wider">
                Poza raportem expo
              </span>
              <div className="text-xl sm:text-2xl font-black text-amber-700 mt-1">
                {comparison.excessOnShelf.length} szt.
              </div>
            </div>
          </div>

          {/* Sync status info & action buttons */}
          <div className="flex flex-wrap items-center justify-between gap-2 pt-1 text-xs">
            <div className="text-gray-500 flex items-center gap-1.5 flex-wrap">
              {pbiUpdatedAt ? (
                <>
                  <span className="w-2 h-2 rounded-full bg-emerald-500 shrink-0" />
                  <span>
                    Baza Power BI: <strong>{comparison.totalPbi} pozycji</strong> • Zaktualizowano:{' '}
                    {new Date(pbiUpdatedAt).toLocaleDateString('pl-PL')}{' '}
                    {new Date(pbiUpdatedAt).toLocaleTimeString('pl-PL', { hour: '2-digit', minute: '2-digit' })}
                    {pbiUpdatedBy ? ` przez ${pbiUpdatedBy.split('@')[0]}` : ''}
                  </span>
                </>
              ) : (
                <>
                  <span className="w-2 h-2 rounded-full bg-amber-400 shrink-0" />
                  <span>Brak wgranych danych Power BI dla Sklepu {STORE_NUMBER}. Wklej tabelę lub wgraj plik poniżej.</span>
                </>
              )}
            </div>

            <div className="flex items-center gap-2">
              {/* Toggle paste input */}
              <button
                onClick={() => setIsPastingOpen(!isPastingOpen)}
                className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-bold transition-all shadow-2xs cursor-pointer active:scale-95"
              >
                <ClipboardPaste className="w-3.5 h-3.5" />
                <span>{isPastingOpen ? 'Ukryj pole wklejania' : 'Wklej tabelę (Ctrl+V)'}</span>
              </button>

              {/* Upload file */}
              <button
                onClick={() => fileInputRef.current?.click()}
                disabled={isProcessing}
                className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-gray-100 hover:bg-gray-200 text-gray-800 font-bold transition-all cursor-pointer active:scale-95"
              >
                <FileSpreadsheet className="w-3.5 h-3.5 text-emerald-600" />
                <span>Wgraj Excel / CSV</span>
              </button>

              {/* Clear data button if items exist */}
              {pbiItems.length > 0 && (
                <button
                  onClick={async () => {
                    if (window.confirm('Czy na pewno chcesz wyczyścić wgrane dane Power BI dla tego sklepu?')) {
                      await onSavePbiItems([]);
                    }
                  }}
                  className="p-1.5 rounded-xl text-gray-400 hover:text-red-600 hover:bg-red-50 transition-colors"
                  title="Wyczyść dane Power BI"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              )}
            </div>
          </div>

          {/* Feedback banner */}
          {feedbackMsg && (
            <div
              className={`px-3 py-2 rounded-xl text-xs font-bold flex items-center justify-between ${
                feedbackMsg.type === 'success'
                  ? 'bg-emerald-50 text-emerald-900 border border-emerald-200'
                  : 'bg-red-50 text-red-900 border border-red-200'
              }`}
            >
              <span>{feedbackMsg.text}</span>
              <button onClick={() => setFeedbackMsg(null)} className="cursor-pointer text-gray-400 hover:text-gray-700">
                <X className="w-3.5 h-3.5" />
              </button>
            </div>
          )}

          {/* Paste Input Container */}
          {isPastingOpen && (
            <div className="p-3 bg-gray-50 rounded-2xl border border-gray-200 flex flex-col gap-2 animate-in fade-in">
              <div className="flex flex-col gap-1">
                <span className="text-xs font-bold text-gray-800 flex items-center gap-1.5">
                  <ClipboardPaste className="w-4 h-4 text-blue-600" />
                  Wklej skopiowaną tabelę lub wiersze z Power BI:
                </span>
                <div className="p-2 rounded-xl bg-blue-50/70 border border-blue-200 text-[11px] text-blue-900 flex flex-col gap-1 leading-relaxed">
                  <p><strong>Jak pobrać dane z Power BI:</strong></p>
                  <ul className="list-disc list-inside space-y-0.5 text-blue-800">
                    <li><strong>Sposób 1 (Zalecany):</strong> W prawym górnym rogu tabeli w Power BI kliknij <strong>trzy kropki (...)</strong> ➔ <strong>Eksportuj dane</strong> ➔ wybierz format Excel/CSV i kliknij przycisk <em>„Wgraj Excel / CSV”</em> obok.</li>
                    <li><strong>Sposób 2 (Zaznaczenie myszką):</strong> Zaznacz wiersze tabeli myszką (od góry do dołu), wciśnij <strong>Ctrl + C</strong> na klawiaturze i wklej poniżej (<strong>Ctrl + V</strong>).</li>
                    <li><strong>Sposób 3 (Pokaż jako tabelę):</strong> W menu kontekstowym kliknij <strong>„Pokaż jako tabelę”</strong> (tryb pełnoekranowy), zaznacz dane i wciśnij <strong>Ctrl + C</strong>.</li>
                  </ul>
                </div>
              </div>
              <textarea
                value={pasteInput}
                onChange={(e) => setPasteInput(e.target.value)}
                placeholder="Wklej tutaj zawartość skopiowaną z Power BI (lub zaznaczony tekst z tabeli)..."
                rows={4}
                className="w-full text-xs font-mono p-2.5 rounded-xl border border-gray-300 bg-white focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
              <div className="flex items-center justify-between">
                <span className="text-[11px] text-gray-500">
                  {pasteInput.trim() ? `Gotowe do przetworzenia ~${pasteInput.trim().split('\n').length} wierszy` : ''}
                </span>
                <div className="flex items-center gap-2">
                  <button
                    onClick={() => {
                      setPasteInput('');
                      setIsPastingOpen(false);
                    }}
                    className="px-3 py-1.5 text-xs text-gray-600 hover:bg-gray-200 rounded-lg"
                  >
                    Anuluj
                  </button>
                  <button
                    onClick={handleProcessPastedText}
                    disabled={isProcessing || !pasteInput.trim()}
                    className="px-4 py-1.5 text-xs font-bold text-white bg-blue-600 hover:bg-blue-700 rounded-xl shadow-xs transition-all disabled:opacity-50 cursor-pointer flex items-center gap-1.5"
                  >
                    {isProcessing ? <RefreshCw className="w-3.5 h-3.5 animate-spin" /> : null}
                    <span>Przetwórz i porównaj z regałami</span>
                  </button>
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Tab Navigation & Search */}
        <div className="px-4 py-2 border-b border-gray-200 bg-gray-50/50 flex flex-wrap items-center justify-between gap-2 shrink-0">
          <div className="flex items-center gap-1 overflow-x-auto">
            <button
              onClick={() => setActiveTab('missing')}
              className={`px-3 py-1.5 rounded-xl font-bold text-xs flex items-center gap-1.5 transition-all cursor-pointer whitespace-nowrap ${
                activeTab === 'missing'
                  ? 'bg-red-600 text-white shadow-xs'
                  : 'bg-white text-gray-700 hover:bg-gray-100 border border-gray-200'
              }`}
            >
              <span>Braki do powieszenia</span>
              <span className="px-1.5 py-0.2 rounded-full text-[10px] bg-white/20 font-mono">
                {comparison.missingOnShelf.length}
              </span>
            </button>

            <button
              onClick={() => setActiveTab('matched')}
              className={`px-3 py-1.5 rounded-xl font-bold text-xs flex items-center gap-1.5 transition-all cursor-pointer whitespace-nowrap ${
                activeTab === 'matched'
                  ? 'bg-emerald-600 text-white shadow-xs'
                  : 'bg-white text-gray-700 hover:bg-gray-100 border border-gray-200'
              }`}
            >
              <span>Zgodne (na regale)</span>
              <span className="px-1.5 py-0.2 rounded-full text-[10px] bg-white/20 font-mono">
                {comparison.matched.length}
              </span>
            </button>

            <button
              onClick={() => setActiveTab('excess')}
              className={`px-3 py-1.5 rounded-xl font-bold text-xs flex items-center gap-1.5 transition-all cursor-pointer whitespace-nowrap ${
                activeTab === 'excess'
                  ? 'bg-amber-600 text-white shadow-xs'
                  : 'bg-white text-gray-700 hover:bg-gray-100 border border-gray-200'
              }`}
            >
              <span>Poza raportem</span>
              <span className="px-1.5 py-0.2 rounded-full text-[10px] bg-white/20 font-mono">
                {comparison.excessOnShelf.length}
              </span>
            </button>

            <button
              onClick={() => setActiveTab('all')}
              className={`px-3 py-1.5 rounded-xl font-bold text-xs flex items-center gap-1.5 transition-all cursor-pointer whitespace-nowrap ${
                activeTab === 'all'
                  ? 'bg-gray-800 text-white shadow-xs'
                  : 'bg-white text-gray-700 hover:bg-gray-100 border border-gray-200'
              }`}
            >
              <span>Wszystkie Power BI</span>
              <span className="px-1.5 py-0.2 rounded-full text-[10px] bg-white/20 font-mono">
                {comparison.totalPbi}
              </span>
            </button>

            <button
              onClick={() => setActiveTab('embed')}
              className={`px-3 py-1.5 rounded-xl font-bold text-xs flex items-center gap-1.5 transition-all cursor-pointer whitespace-nowrap ${
                activeTab === 'embed'
                  ? 'bg-purple-600 text-white shadow-xs'
                  : 'bg-white text-gray-700 hover:bg-gray-100 border border-gray-200'
              }`}
            >
              <Eye className="w-3.5 h-3.5" />
              <span>Podgląd Power BI na żywo</span>
            </button>
          </div>

          {/* Filter Search */}
          {activeTab !== 'embed' && (
            <div className="relative w-full sm:w-56">
              <Search className="w-3.5 h-3.5 text-gray-400 absolute left-2.5 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Filtruj REF lub nazwę..."
                className="w-full pl-8 pr-3 py-1.5 text-xs rounded-xl border border-gray-300 bg-white focus:outline-none focus:ring-1 focus:ring-blue-500"
              />
            </div>
          )}
        </div>

        {/* List Content Area */}
        <div className="flex-1 overflow-y-auto p-3 sm:p-5">
          {activeTab === 'embed' ? (
            /* Live Power BI Embed View */
            <div className="w-full h-full flex flex-col gap-2">
              <div className="p-3 bg-blue-50 border border-blue-200 rounded-xl text-xs text-blue-950 flex items-center justify-between">
                <span>
                  Raport Power BI z Twoim kontem służbowym. Jeśli z powodu polityki bezpieczeństwa ADEO ramka pozostanie pusta,
                  skorzystaj z przycisku po prawej:
                </span>
                <a
                  href={POWER_BI_STOCK_EXPO_URL}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="px-3 py-1.5 rounded-lg bg-blue-600 hover:bg-blue-700 text-white font-bold shrink-0 ml-2 flex items-center gap-1"
                >
                  <ExternalLink className="w-3 h-3" />
                  <span>Otwórz w Power BI</span>
                </a>
              </div>
              <iframe
                title="Stock Expo Power BI"
                src={POWER_BI_EMBED_URL}
                className="w-full flex-1 rounded-2xl border border-gray-200 shadow-inner bg-white"
                allowFullScreen
              />
            </div>
          ) : filteredList.length === 0 ? (
            /* Empty state */
            <div className="h-full flex flex-col items-center justify-center text-center p-8">
              {pbiItems.length === 0 ? (
                <>
                  <div className="w-14 h-14 rounded-2xl bg-blue-100 text-blue-600 flex items-center justify-center mb-3">
                    <ClipboardPaste className="w-7 h-7" />
                  </div>
                  <h3 className="text-base font-bold text-gray-900 mb-1">Brak danych Power BI dla tego sklepu</h3>
                  <p className="text-xs text-gray-500 max-w-md mb-4">
                    Skopiuj tabelę z raportu Power BI lub pobierz plik Excel (ikona <strong>... ➔ Eksportuj dane</strong> w Power BI)
                    i kliknij przycisk „Wklej tabelę” u góry.
                  </p>
                  <button
                    onClick={() => setIsPastingOpen(true)}
                    className="px-4 py-2 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-bold text-xs shadow-md"
                  >
                    Wklej dane ze schowka
                  </button>
                </>
              ) : (
                <>
                  <CheckCircle2 className="w-12 h-12 text-emerald-500 mb-2" />
                  <h3 className="text-base font-bold text-gray-900 mb-1">
                    {activeTab === 'missing'
                      ? 'Wszystkie pozycje z Power BI wiszą na regałach!'
                      : 'Brak pozycji w tej kategorii'}
                  </h3>
                  <p className="text-xs text-gray-500">
                    {activeTab === 'missing'
                      ? 'Pełna 100% zgodność ekspozycji z centralnym raportem Stock Expo.'
                      : 'Zmień kryteria wyszukiwania lub wybierz inną zakładkę.'}
                  </p>
                </>
              )}
            </div>
          ) : (
            /* Table of items */
            <div className="flex flex-col gap-2">
              {activeTab === 'missing' && (
                <div className="mb-2 p-2.5 bg-red-50 border border-red-200 rounded-xl text-xs text-red-900 flex flex-wrap items-center justify-between gap-2">
                  <div className="flex items-center gap-2 max-w-xl">
                    <AlertTriangle className="w-4 h-4 text-red-600 shrink-0" />
                    <span>
                      Poniższe dywany figurują w Power BI jako <strong>wpisane na ekspozycję (stock = 1,00)</strong>, lecz nie są przypisane do żadnego regału w sklepie.
                      Kliknij <strong>„Zawieś na regale”</strong>, aby natychmiast przypisać im miejsce.
                    </span>
                  </div>
                  {comparison.missingOnShelf.length > 0 && (
                    <button
                      onClick={handlePrintMissing}
                      className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-red-600 hover:bg-red-700 text-white font-bold text-xs shadow-xs transition-all cursor-pointer active:scale-95 shrink-0"
                    >
                      <Printer className="w-3.5 h-3.5" />
                      <span>Drukuj listę braków A4 ({comparison.missingOnShelf.length})</span>
                    </button>
                  )}
                </div>
              )}

              <div className="grid grid-cols-1 gap-2">
                {activeTab === 'matched' ? (
                  // Render matched items
                  (filteredList as Array<{ pbi: PowerBiStockExpoItem; carpet: Carpet }>).map((item, idx) => (
                    <div
                      key={`matched-${item.pbi.ref}-${idx}`}
                      className="p-3 rounded-2xl border border-emerald-200 bg-emerald-50/40 hover:bg-emerald-50 flex items-center justify-between gap-3 transition-colors"
                    >
                      <div className="flex items-center gap-3">
                        <div className="w-10 h-10 rounded-xl bg-emerald-600 text-white font-black text-xs flex items-center justify-center shrink-0">
                          {item.carpet.miejsce}
                        </div>
                        <div>
                          <div className="text-sm font-bold text-gray-900">{item.pbi.opis}</div>
                          <div className="text-xs text-gray-500 flex items-center gap-3 mt-0.5">
                            <span>
                              REF: <strong className="font-mono text-gray-700">{item.pbi.ref}</strong>
                            </span>
                            {item.carpet.ean && (
                              <span>
                                EAN: <span className="font-mono">{item.carpet.ean}</span>
                              </span>
                            )}
                          </div>
                        </div>
                      </div>

                      <div className="flex items-center gap-2">
                        <span className="px-2.5 py-1 rounded-full text-xs font-extrabold bg-emerald-100 text-emerald-800">
                          Zgodny (Regał {item.carpet.miejsce})
                        </span>
                      </div>
                    </div>
                  ))
                ) : activeTab === 'excess' ? (
                  // Render excess items (on shelf but not in PBI)
                  (filteredList as Carpet[]).map((c, idx) => (
                    <div
                      key={`excess-${c.id}-${idx}`}
                      className="p-3 rounded-2xl border border-amber-200 bg-amber-50/40 hover:bg-amber-50 flex items-center justify-between gap-3 transition-colors"
                    >
                      <div className="flex items-center gap-3">
                        <div className="w-10 h-10 rounded-xl bg-amber-500 text-gray-950 font-black text-xs flex items-center justify-center shrink-0">
                          {c.miejsce}
                        </div>
                        <div>
                          <div className="text-sm font-bold text-gray-900">{c.nazwa}</div>
                          <div className="text-xs text-gray-500 flex items-center gap-3 mt-0.5">
                            <span>
                              REF: <strong className="font-mono text-gray-700">{c.ref || 'Brak'}</strong>
                            </span>
                            {c.ean && (
                              <span>
                                EAN: <span className="font-mono">{c.ean}</span>
                              </span>
                            )}
                          </div>
                        </div>
                      </div>

                      <span className="px-2.5 py-1 rounded-full text-xs font-bold bg-amber-100 text-amber-900">
                        Wisi w sklepie (Brak w expo PBI)
                      </span>
                    </div>
                  ))
                ) : (
                  // Render missing or all Power BI items
                  (filteredList as PowerBiStockExpoItem[]).map((item, idx) => {
                    const isMissing = comparison.missingOnShelf.some((m) => m.ref === item.ref);
                    return (
                      <div
                        key={`pbi-${item.ref}-${idx}`}
                        className={`p-3 rounded-2xl border flex items-center justify-between gap-3 transition-colors ${
                          isMissing
                            ? 'border-red-200 bg-red-50/40 hover:bg-red-50'
                            : 'border-gray-200 bg-white hover:bg-gray-50'
                        }`}
                      >
                        <div className="flex items-center gap-3">
                          <div
                            className={`w-10 h-10 rounded-xl font-black text-xs flex items-center justify-center shrink-0 ${
                              isMissing ? 'bg-red-600 text-white' : 'bg-emerald-600 text-white'
                            }`}
                          >
                            {isMissing ? 'BRAK' : 'OK'}
                          </div>
                          <div>
                            <div className="text-sm font-bold text-gray-900">{item.opis}</div>
                            <div className="text-xs text-gray-500 flex items-center gap-3 mt-0.5">
                              <span>
                                REF: <strong className="font-mono text-gray-800">{item.ref}</strong>
                              </span>
                              <span>
                                Stock expo: <strong>{item.stock.toFixed(2)}</strong>
                              </span>
                              {item.sklep && <span>Sklep: {item.sklep}</span>}
                            </div>
                          </div>
                        </div>

                        <div className="flex items-center gap-2">
                          {isMissing ? (
                            <button
                              onClick={() => {
                                onClose();
                                onAssignToRack(item.ref, item.opis);
                              }}
                              className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-red-600 hover:bg-red-700 text-white font-bold text-xs shadow-xs transition-all cursor-pointer active:scale-95"
                            >
                              <PlusCircle className="w-3.5 h-3.5" />
                              <span>Zawieś na regale</span>
                            </button>
                          ) : (
                            <span className="px-2.5 py-1 rounded-full text-xs font-extrabold bg-emerald-100 text-emerald-800">
                              Na regale
                            </span>
                          )}
                        </div>
                      </div>
                    );
                  })
                )}
              </div>
            </div>
          )}
        </div>
        </div>

        {/* =========================================================================
            2. PRINTABLE DOCUMENT (ONLY VISIBLE ON PRINT)
            A4 sheet for store team: Missing carpets to hang on the racks
           ========================================================================= */}
        <div id="pbi-audit-printable">
          {/* Header */}
          <div className="flex items-center justify-between border-b-2 border-black pb-3 mb-3">
            <div className="flex items-center gap-3">
              <LeroyMerlinLogo className="h-9" />
              <div>
                <h1 className="text-base font-black text-black uppercase tracking-tight">
                  Audyt Stock Expo Power BI — Lista braków do powieszenia
                </h1>
                <div className="text-[11px] text-gray-700 font-bold">
                  LEROY MERLIN • Sklep {STORE_NUMBER} {STORE_CITY} • Dział 12 Dywany (Poddział 05)
                </div>
              </div>
            </div>

            <div className="text-right text-[11px] leading-tight">
              <div>Wydruk: <strong className="font-mono">{new Date().toLocaleDateString('pl-PL')} {new Date().toLocaleTimeString('pl-PL', { hour: '2-digit', minute: '2-digit' })}</strong></div>
              <div>Raport Power BI: <strong>{comparison.totalPbi} pozycji</strong></div>
              <div>Na regale: <strong>{comparison.matched.length}</strong> ({comparison.complianceRate}%) | Braki: <strong className="text-red-700 font-black">{comparison.missingOnShelf.length}</strong></div>
            </div>
          </div>

          {/* Table */}
          {comparison.missingOnShelf.length === 0 ? (
            <div className="py-8 text-center border border-black p-4">
              <div className="text-sm font-black text-black">Wszystkie dywany z raportu Power BI wiszą na regałach! (Pokrycie 100%)</div>
              <div className="text-xs text-gray-600 mt-1">Brak pozycji do powieszenia.</div>
            </div>
          ) : (
            <table className="w-full border-collapse border border-black text-[11px]">
              <thead>
                <tr className="bg-gray-100 border-b-2 border-black text-black uppercase text-[10px] tracking-wider font-black">
                  <th className="p-1.5 border border-black w-[35px] text-center">LP</th>
                  <th className="p-1.5 border border-black w-[95px] text-center">REF LM</th>
                  <th className="p-1.5 border border-black">Nazwa / Opis dywanu (Power BI)</th>
                  <th className="p-1.5 border border-black w-[60px] text-center">Stock</th>
                  <th className="p-1.5 border border-black w-[130px] text-center">Zawieś na regale</th>
                  <th className="p-1.5 border border-black w-[110px] text-center">Powieszono [ ✓ ]</th>
                </tr>
              </thead>
              <tbody>
                {comparison.missingOnShelf.map((item, idx) => (
                  <tr key={item.ref} className="pbi-print-row border-b border-gray-400">
                    <td className="p-1.5 border border-black text-center font-bold font-mono text-[10px] bg-gray-50">
                      {idx + 1}
                    </td>
                    <td className="p-1.5 border border-black text-center font-black font-mono text-xs">
                      {item.ref}
                    </td>
                    <td className="p-1.5 border border-black font-bold text-black text-[11px] leading-tight">
                      {item.opis}
                    </td>
                    <td className="p-1.5 border border-black text-center font-mono font-bold text-xs">
                      {item.stock.toFixed(2).replace('.', ',')}
                    </td>
                    <td className="p-1.5 border border-black text-center text-[10px] text-gray-500 font-mono">
                      Regał: ____ Slot: [A/B]
                    </td>
                    <td className="p-1.5 border border-black text-center">
                      <div className="inline-block w-4 h-4 border-2 border-black rounded-xs"></div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}

          {/* Footer instructions */}
          <div className="mt-3 pt-2 border-t border-gray-400 flex items-center justify-between text-[10px] text-gray-600">
            <div>
              Instrukcja: Odszukaj dywan w strefie zaplecza/dostaw, zawieś na wolnym ramieniu ekspozytora, a następnie dodaj w aplikacji.
            </div>
            <div className="font-bold">
              Strona wydruku A4 • System Zarządzania Ekspozycją Dywanów
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
