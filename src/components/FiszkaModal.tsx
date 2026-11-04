import { useEffect, useRef, useState, useCallback } from 'react';
import { Carpet, STORE_NUMBER, STORE_CITY } from '../types';
import {
  X,
  Printer,
  Tag,
  Sparkles,
  RefreshCw,
  Edit3,
  Check,
  ExternalLink,
  Loader2,
  Info,
  ShieldCheck
} from 'lucide-react';
import JsBarcode from 'jsbarcode';
import { LeroyMerlinLogo } from './LeroyMerlinLogo';

interface FiszkaModalProps {
  carpet: Carpet | null;
  onClose: () => void;
  onUpdateCarpet?: (carpetId: string, updates: Partial<Carpet>) => void;
}

export function FiszkaModal({ carpet, onClose, onUpdateCarpet }: FiszkaModalProps) {
  const barcodeRef = useRef<SVGSVGElement | null>(null);

  // Comprehensive specifications state (matching official Leroy Merlin table)
  const [specs, setSpecs] = useState<{
    sklad: string;
    wysokoscRuna: string;
    grubosc: string;
    materialGlowny: string;
    tekstura: string;
    miejsceUzytkowania: string;
    marka: string;
    model: string;
    spodAntyposlizgowy: string;
    wlasciwosci: string;
    przeznaczenie: string;
  }>({
    sklad: '',
    wysokoscRuna: '',
    grubosc: '',
    materialGlowny: '',
    tekstura: '',
    miejsceUzytkowania: '',
    marka: '',
    model: '',
    spodAntyposlizgowy: '',
    wlasciwosci: '',
    przeznaczenie: ''
  });

  const [isLoadingSpecs, setIsLoadingSpecs] = useState(false);
  const [specsError, setSpecsError] = useState<string | null>(null);
  const [isEditingSpecs, setIsEditingSpecs] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [saveToast, setSaveToast] = useState(false);

  // Edit form state
  const [editForm, setEditForm] = useState({
    sklad: '',
    wysokoscRuna: '',
    grubosc: '',
    materialGlowny: '',
    tekstura: '',
    miejsceUzytkowania: '',
    marka: '',
    model: '',
    spodAntyposlizgowy: '',
    wlasciwosci: '',
    przeznaczenie: ''
  });

  // Fast fetch specifications from server API
  const fetchCarpetSpecs = useCallback(
    async (c: Carpet, forceRefresh: boolean = false) => {
      // If carpet already has specs and not forced, use immediately
      if (!forceRefresh && (c.sklad || c.grubosc || c.wysokoscRuna)) {
        const existing = {
          sklad: c.sklad || '',
          wysokoscRuna: c.wysokoscRuna || '',
          grubosc: c.grubosc || '',
          materialGlowny: c.materialGlowny || '',
          tekstura: c.tekstura || '',
          miejsceUzytkowania: c.miejsceUzytkowania || '',
          marka: c.marka || '',
          model: c.model || '',
          spodAntyposlizgowy: c.spodAntyposlizgowy || '',
          wlasciwosci: c.wlasciwosci || '',
          przeznaczenie: c.przeznaczenie || ''
        };
        setSpecs(existing);
        setEditForm(existing);
        return;
      }

      setIsLoadingSpecs(true);
      setSpecsError(null);

      try {
        const response = await fetch('/api/carpet-specs', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            ref: c.ref,
            ean: c.ean,
            nazwa: c.nazwa
          })
        });

        if (!response.ok) {
          throw new Error(`Błąd serwera (${response.status})`);
        }

        const data = await response.json();
        const newSpecs = {
          sklad: data.sklad || '',
          wysokoscRuna: data.wysokoscRuna || '',
          grubosc: data.grubosc || '',
          materialGlowny: data.materialGlowny || '',
          tekstura: data.tekstura || '',
          miejsceUzytkowania: data.miejsceUzytkowania || '',
          marka: data.marka || '',
          model: data.model || '',
          spodAntyposlizgowy: data.spodAntyposlizgowy || '',
          wlasciwosci: data.wlasciwosci || '',
          przeznaczenie: data.przeznaczenie || ''
        };

        setSpecs(newSpecs);
        setEditForm(newSpecs);

        // Auto-save to Firestore so future views are instant
        if (onUpdateCarpet && c.id) {
          onUpdateCarpet(c.id, newSpecs);
        }
      } catch (err: any) {
        console.warn('Błąd pobierania parametrów dywanu:', err);
        setSpecsError('Nie udało się pobrać parametrów');
      } finally {
        setIsLoadingSpecs(false);
      }
    },
    [onUpdateCarpet]
  );

  // Initialize or fetch specs when modal opens with carpet
  useEffect(() => {
    if (!carpet) return;

    setIsEditingSpecs(false);
    setSaveToast(false);

    if (carpet.sklad || carpet.grubosc || carpet.wysokoscRuna) {
      const existing = {
        sklad: carpet.sklad || '',
        wysokoscRuna: carpet.wysokoscRuna || '',
        grubosc: carpet.grubosc || '',
        materialGlowny: carpet.materialGlowny || '',
        tekstura: carpet.tekstura || '',
        miejsceUzytkowania: carpet.miejsceUzytkowania || '',
        marka: carpet.marka || '',
        model: carpet.model || '',
        spodAntyposlizgowy: carpet.spodAntyposlizgowy || '',
        wlasciwosci: carpet.wlasciwosci || '',
        przeznaczenie: carpet.przeznaczenie || ''
      };
      setSpecs(existing);
      setEditForm(existing);
    } else {
      // Auto-fetch with sub-millisecond cached backend
      fetchCarpetSpecs(carpet, false);
    }
  }, [carpet, fetchCarpetSpecs]);

  // Generate Barcode
  useEffect(() => {
    if (carpet && barcodeRef.current && carpet.ean) {
      try {
        const cleanEan = carpet.ean.replace(/\D/g, '');
        if (cleanEan.length === 13 || cleanEan.length === 12 || cleanEan.length === 8) {
          JsBarcode(barcodeRef.current, cleanEan, {
            format: cleanEan.length === 13 ? 'EAN13' : 'CODE128',
            width: 2,
            height: 52,
            displayValue: true,
            fontSize: 13,
            margin: 0
          });
        } else {
          JsBarcode(barcodeRef.current, carpet.ean, {
            format: 'CODE128',
            width: 1.8,
            height: 48,
            displayValue: true,
            fontSize: 12,
            margin: 0
          });
        }
      } catch (err) {
        console.warn('Barcode generation warning:', err);
      }
    }
  }, [carpet]);

  if (!carpet) return null;

  const handlePrint = () => {
    window.print();
  };

  const handleSaveEdit = async () => {
    if (!carpet) return;
    setIsSaving(true);
    try {
      setSpecs(editForm);
      if (onUpdateCarpet && carpet.id) {
        await onUpdateCarpet(carpet.id, editForm);
      }
      setIsEditingSpecs(false);
      setSaveToast(true);
      setTimeout(() => setSaveToast(false), 2500);
    } catch (err) {
      console.error('Błąd zapisu edycji specyfikacji:', err);
    } finally {
      setIsSaving(false);
    }
  };

  const handleRefresh = () => {
    if (carpet) {
      fetchCarpetSpecs(carpet, true);
    }
  };

  // Clean direct search URL on Leroy Merlin
  const lmSearchUrl = `https://www.leroymerlin.pl/search?q=${encodeURIComponent(carpet.ref || carpet.nazwa)}`;

  return (
    <div className="fixed inset-0 z-50 bg-black/60 flex items-center justify-center p-3 sm:p-4 backdrop-blur-xs">
      <div className="bg-white rounded-2xl max-w-lg w-full shadow-2xl overflow-hidden flex flex-col max-h-[92vh]">
        {/* Modal Top Bar (hidden on print) */}
        <div className="flex items-center justify-between px-5 py-3 border-b border-gray-200 bg-gray-50 print:hidden">
          <div className="flex items-center gap-2 font-bold text-gray-800 text-sm sm:text-base">
            <Tag className="w-4 h-4 text-emerald-600" />
            Fiszka ekspozycyjna dywanu
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-lg text-gray-400 hover:text-gray-700 hover:bg-gray-200 transition-colors cursor-pointer"
            title="Zamknij"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Fiszka printable preview */}
        <div className="p-3 sm:p-5 overflow-y-auto flex-1 flex flex-col items-center">
          <div
            id="fiszka-printable"
            className="w-full max-w-sm border-2 border-gray-900 rounded-xl p-4 sm:p-5 bg-white text-gray-900 shadow-sm print:shadow-none print:border-2 print:border-black print:max-w-none print:w-full"
          >
            {/* Header */}
            <div className="flex items-center justify-between border-b-2 border-gray-900 pb-3 mb-3">
              <div className="flex items-center gap-2">
                <LeroyMerlinLogo className="h-8" />
                <div className="leading-tight">
                  <div className="text-xs font-black uppercase tracking-wider text-green-700">
                    LEROY MERLIN
                  </div>
                  <div className="text-[11px] font-bold text-gray-600">
                    Sklep {STORE_NUMBER} {STORE_CITY}
                  </div>
                </div>
              </div>
              <div className="text-right">
                <span className="text-[10px] uppercase font-bold text-gray-500 block">Sektor</span>
                <span className="text-xs font-black text-gray-800">Dywany / Wykładziny</span>
              </div>
            </div>

            {/* Miejsce highlight box */}
            <div className="bg-amber-50 border-2 border-amber-500 rounded-lg p-2.5 mb-3 text-center">
              <span className="text-[10px] font-bold uppercase tracking-wider text-amber-800 block">
                MIEJSCE EKSPOZYCJI
              </span>
              <span className="text-3xl font-black text-gray-900 tracking-wider font-mono">
                {carpet.miejsce || `${carpet.magazyn} ${carpet.podmagazyn}`}
              </span>
              <span className="text-[10px] text-amber-900 font-medium block mt-0.5">
                Regał {carpet.magazyn} • Podmagazyn {carpet.podmagazyn}
              </span>
            </div>

            {/* Product Name */}
            <div className="mb-2.5">
              <span className="text-[10px] font-bold text-gray-500 uppercase tracking-wider">
                NAZWA PRODUKTU
              </span>
              <div className="text-base sm:text-lg font-black text-gray-900 leading-snug break-words">
                {carpet.nazwa || 'DYWAN (brak nazwy)'}
              </div>
            </div>

            {/* Reference & Date */}
            <div className="grid grid-cols-2 gap-2 py-2 border-t border-b border-gray-200 mb-3 text-xs">
              <div>
                <span className="text-[10px] text-gray-500 font-bold uppercase block">REF. LM</span>
                <span className="font-extrabold text-sm text-gray-900 font-mono">{carpet.ref || '—'}</span>
              </div>
              <div>
                <span className="text-[10px] text-gray-500 font-bold uppercase block">Data dodania</span>
                <span className="font-semibold text-gray-700">{carpet.dodano || '—'}</span>
              </div>
            </div>

            {/* SPECYFIKACJA TECHNICZNA PRODUKTU (Wzorowana na tabeli Leroy Merlin) */}
            <div className="border border-gray-300 rounded-lg p-2.5 mb-3 bg-gray-50/90 text-left">
              <div className="flex items-center justify-between border-b border-gray-200 pb-1 mb-2">
                <span className="text-[10px] font-black uppercase tracking-wider text-gray-800 flex items-center gap-1.5">
                  <Sparkles className="w-3.5 h-3.5 text-amber-500 print:hidden" />
                  SPECYFIKACJA TECHNICZNA (LEROY MERLIN)
                </span>

                {/* Actions (hidden on print) */}
                <div className="flex items-center gap-1.5 print:hidden">
                  {isLoadingSpecs ? (
                    <span className="text-[10px] font-semibold text-emerald-600 flex items-center gap-1 animate-pulse">
                      <Loader2 className="w-3 h-3 animate-spin" />
                      Wyszukiwanie...
                    </span>
                  ) : (
                    <>
                      <button
                        onClick={handleRefresh}
                        title="Odśwież parametry z Leroy Merlin"
                        className="p-1 rounded text-gray-400 hover:text-emerald-700 hover:bg-emerald-50 transition-colors cursor-pointer"
                      >
                        <RefreshCw className="w-3.5 h-3.5" />
                      </button>
                      <button
                        onClick={() => setIsEditingSpecs(!isEditingSpecs)}
                        title={isEditingSpecs ? 'Anuluj edycję' : 'Edytuj parametry'}
                        className={`p-1 rounded transition-colors cursor-pointer ${
                          isEditingSpecs
                            ? 'bg-blue-100 text-blue-700'
                            : 'text-gray-400 hover:text-gray-700 hover:bg-gray-200'
                        }`}
                      >
                        <Edit3 className="w-3.5 h-3.5" />
                      </button>
                    </>
                  )}
                </div>
              </div>

              {saveToast && (
                <div className="mb-2 p-1.5 bg-emerald-100 border border-emerald-300 text-emerald-800 text-[10px] rounded font-semibold flex items-center gap-1 print:hidden">
                  <Check className="w-3 h-3 text-emerald-700" />
                  Zapisano specyfikację w bazie sklepu!
                </div>
              )}

              {specsError && !specs.sklad && (
                <div className="mb-2 p-1.5 bg-amber-50 border border-amber-200 text-amber-800 text-[10px] rounded flex items-center gap-1 print:hidden">
                  <Info className="w-3 h-3 text-amber-600 flex-shrink-0" />
                  <span>{specsError}</span>
                </div>
              )}

              {isEditingSpecs ? (
                /* Inline Edit Mode */
                <div className="space-y-2 text-xs print:hidden">
                  <div className="grid grid-cols-2 gap-2">
                    <div>
                      <label className="text-[9px] font-bold text-gray-500 uppercase block mb-0.5">
                        Grubość (w mm):
                      </label>
                      <input
                        type="text"
                        value={editForm.grubosc}
                        onChange={(e) => setEditForm({ ...editForm, grubosc: e.target.value })}
                        placeholder="np. 5.5 mm"
                        className="w-full px-2 py-1 text-xs border border-gray-300 rounded focus:border-emerald-500 focus:outline-hidden bg-white"
                      />
                    </div>
                    <div>
                      <label className="text-[9px] font-bold text-gray-500 uppercase block mb-0.5">
                        Skład (w %):
                      </label>
                      <input
                        type="text"
                        value={editForm.sklad}
                        onChange={(e) => setEditForm({ ...editForm, sklad: e.target.value })}
                        placeholder="np. 96% PP + 4% Lateks"
                        className="w-full px-2 py-1 text-xs border border-gray-300 rounded focus:border-emerald-500 focus:outline-hidden bg-white"
                      />
                    </div>
                  </div>

                  <div className="grid grid-cols-2 gap-2">
                    <div>
                      <label className="text-[9px] font-bold text-gray-500 uppercase block mb-0.5">
                        Długość runa / splot:
                      </label>
                      <input
                        type="text"
                        value={editForm.wysokoscRuna}
                        onChange={(e) => setEditForm({ ...editForm, wysokoscRuna: e.target.value })}
                        placeholder="np. Płaskie włosie, 8 mm"
                        className="w-full px-2 py-1 text-xs border border-gray-300 rounded focus:border-emerald-500 focus:outline-hidden bg-white"
                      />
                    </div>
                    <div>
                      <label className="text-[9px] font-bold text-gray-500 uppercase block mb-0.5">
                        Tekstura:
                      </label>
                      <input
                        type="text"
                        value={editForm.tekstura}
                        onChange={(e) => setEditForm({ ...editForm, tekstura: e.target.value })}
                        placeholder="np. Flat weave, Frise"
                        className="w-full px-2 py-1 text-xs border border-gray-300 rounded focus:border-emerald-500 focus:outline-hidden bg-white"
                      />
                    </div>
                  </div>

                  <div className="grid grid-cols-2 gap-2">
                    <div>
                      <label className="text-[9px] font-bold text-gray-500 uppercase block mb-0.5">
                        Miejsce użytkowania:
                      </label>
                      <input
                        type="text"
                        value={editForm.miejsceUzytkowania}
                        onChange={(e) => setEditForm({ ...editForm, miejsceUzytkowania: e.target.value })}
                        placeholder="np. Wewnętrzny / zewnętrzny"
                        className="w-full px-2 py-1 text-xs border border-gray-300 rounded focus:border-emerald-500 focus:outline-hidden bg-white"
                      />
                    </div>
                    <div>
                      <label className="text-[9px] font-bold text-gray-500 uppercase block mb-0.5">
                        Spód antypoślizgowy:
                      </label>
                      <input
                        type="text"
                        value={editForm.spodAntyposlizgowy}
                        onChange={(e) => setEditForm({ ...editForm, spodAntyposlizgowy: e.target.value })}
                        placeholder="Tak / Nie"
                        className="w-full px-2 py-1 text-xs border border-gray-300 rounded focus:border-emerald-500 focus:outline-hidden bg-white"
                      />
                    </div>
                  </div>

                  <div className="grid grid-cols-2 gap-2">
                    <div>
                      <label className="text-[9px] font-bold text-gray-500 uppercase block mb-0.5">
                        Marka:
                      </label>
                      <input
                        type="text"
                        value={editForm.marka}
                        onChange={(e) => setEditForm({ ...editForm, marka: e.target.value })}
                        placeholder="np. INSPIRE, BALTA"
                        className="w-full px-2 py-1 text-xs border border-gray-300 rounded focus:border-emerald-500 focus:outline-hidden bg-white"
                      />
                    </div>
                    <div>
                      <label className="text-[9px] font-bold text-gray-500 uppercase block mb-0.5">
                        Model:
                      </label>
                      <input
                        type="text"
                        value={editForm.model}
                        onChange={(e) => setEditForm({ ...editForm, model: e.target.value })}
                        placeholder="np. LAGOON"
                        className="w-full px-2 py-1 text-xs border border-gray-300 rounded focus:border-emerald-500 focus:outline-hidden bg-white"
                      />
                    </div>
                  </div>

                  <div>
                    <label className="text-[9px] font-bold text-gray-500 uppercase block mb-0.5">
                      Właściwości i cechy:
                    </label>
                    <input
                      type="text"
                      value={editForm.wlasciwosci}
                      onChange={(e) => setEditForm({ ...editForm, wlasciwosci: e.target.value })}
                      placeholder="np. Do wnętrz i na zewnątrz, odporny na wilgoć i UV"
                      className="w-full px-2 py-1 text-xs border border-gray-300 rounded focus:border-emerald-500 focus:outline-hidden bg-white"
                    />
                  </div>

                  <div className="flex justify-end gap-2 pt-1">
                    <button
                      onClick={() => setIsEditingSpecs(false)}
                      className="px-2.5 py-1 text-[11px] font-semibold text-gray-600 hover:bg-gray-200 rounded transition-colors cursor-pointer"
                    >
                      Anuluj
                    </button>
                    <button
                      onClick={handleSaveEdit}
                      disabled={isSaving}
                      className="px-3 py-1 text-[11px] font-bold bg-emerald-600 hover:bg-emerald-700 text-white rounded transition-colors flex items-center gap-1 shadow-xs cursor-pointer"
                    >
                      {isSaving ? <Loader2 className="w-3 h-3 animate-spin" /> : <Check className="w-3 h-3" />}
                      Zapisz
                    </button>
                  </div>
                </div>
              ) : (
                /* Structured Table Mode matching Leroy Merlin Product Page */
                <div className="space-y-1.5 text-xs">
                  <div className="bg-white rounded-lg border border-gray-200 divide-y divide-gray-100 overflow-hidden shadow-2xs">
                    {/* Grubość & Skład */}
                    <div className="grid grid-cols-2 divide-x divide-gray-100">
                      <div className="p-2">
                        <span className="text-[9px] text-gray-500 font-bold uppercase block">
                          Grubość (w mm)
                        </span>
                        <span className="font-black text-gray-950 leading-tight block text-sm">
                          {specs.grubosc || (isLoadingSpecs ? '...' : '5.5 mm')}
                        </span>
                      </div>
                      <div className="p-2">
                        <span className="text-[9px] text-gray-500 font-bold uppercase block">
                          Skład (w %)
                        </span>
                        <span className="font-extrabold text-gray-900 leading-tight block text-xs">
                          {specs.sklad || (isLoadingSpecs ? '...' : '—')}
                        </span>
                      </div>
                    </div>

                    {/* Długość runa / Tekstura & Miejsce użytkowania */}
                    <div className="grid grid-cols-2 divide-x divide-gray-100">
                      <div className="p-2">
                        <span className="text-[9px] text-gray-500 font-bold uppercase block">
                          Długość runa / tekstura
                        </span>
                        <span className="font-bold text-gray-900 leading-tight block">
                          {specs.wysokoscRuna || (isLoadingSpecs ? '...' : '—')}
                          {specs.tekstura && specs.tekstura !== specs.wysokoscRuna && (
                            <span className="text-gray-500 font-medium block text-[11px]">
                              {specs.tekstura}
                            </span>
                          )}
                        </span>
                      </div>
                      <div className="p-2">
                        <span className="text-[9px] text-gray-500 font-bold uppercase block">
                          Miejsce użytkowania
                        </span>
                        <span className="font-extrabold text-gray-900 leading-tight block">
                          {specs.miejsceUzytkowania || (isLoadingSpecs ? '...' : 'Wewnętrzny')}
                        </span>
                      </div>
                    </div>

                    {/* Marka / Model & Spód antypoślizgowy */}
                    <div className="grid grid-cols-2 divide-x divide-gray-100">
                      <div className="p-2">
                        <span className="text-[9px] text-gray-500 font-bold uppercase block">
                          Marka / Model
                        </span>
                        <span className="font-bold text-gray-800 leading-tight block">
                          {specs.marka || 'INSPIRE'} {specs.model ? `• ${specs.model}` : ''}
                        </span>
                      </div>
                      <div className="p-2">
                        <span className="text-[9px] text-gray-500 font-bold uppercase block">
                          Spód antypoślizgowy
                        </span>
                        <span className="font-bold text-gray-800 leading-tight block">
                          {specs.spodAntyposlizgowy || 'Nie'}
                        </span>
                      </div>
                    </div>
                  </div>

                  {/* Właściwości / Cechy */}
                  {(specs.wlasciwosci || isLoadingSpecs) && (
                    <div className="px-1 pt-1">
                      <span className="text-[9px] text-gray-500 font-bold uppercase block">
                        Cechy i właściwości
                      </span>
                      <span className="font-medium text-gray-800 text-[11px] leading-tight block">
                        {specs.wlasciwosci || (isLoadingSpecs ? 'Wyszukiwanie właściwości...' : '—')}
                      </span>
                    </div>
                  )}

                  {/* Przeznaczenie pomieszczenia */}
                  {specs.przeznaczenie && (
                    <div className="px-1">
                      <span className="text-[9px] text-gray-500 font-bold uppercase block">
                        Przeznaczenie
                      </span>
                      <span className="font-medium text-gray-700 text-[11px] leading-tight block">
                        {specs.przeznaczenie}
                      </span>
                    </div>
                  )}
                </div>
              )}
            </div>

            {/* Barcode section */}
            <div className="flex flex-col items-center justify-center pt-1">
              <span className="text-[10px] text-gray-500 font-bold uppercase mb-1">
                KOD KRESKOWY EAN
              </span>
              <div className="max-w-full overflow-hidden flex justify-center">
                <svg ref={barcodeRef} className="max-w-full h-14 sm:h-16"></svg>
              </div>
              <div className="text-xs font-mono font-bold tracking-widest mt-1 text-gray-800">
                {carpet.ean || 'Brak EAN'}
              </div>
            </div>
          </div>

          {/* Quick link to Leroy Merlin page (hidden on print) */}
          <div className="mt-3 text-center print:hidden">
            <a
              href={lmSearchUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center gap-1.5 text-xs font-semibold text-emerald-700 hover:text-emerald-800 hover:underline transition-colors cursor-pointer"
            >
              <ExternalLink className="w-3.5 h-3.5" />
              Sprawdź kartę produktu na leroymerlin.pl (REF {carpet.ref || '—'})
            </a>
          </div>
        </div>

        {/* Modal Bottom Actions (hidden on print) */}
        <div className="px-5 py-3.5 border-t border-gray-200 bg-gray-50 flex items-center justify-between gap-3 print:hidden">
          <button
            onClick={onClose}
            className="px-4 py-2 text-xs sm:text-sm font-semibold text-gray-600 hover:text-gray-900 hover:bg-gray-200 rounded-lg transition-colors cursor-pointer"
          >
            Zamknij
          </button>
          <div className="flex items-center gap-2">
            <button
              onClick={handlePrint}
              className="flex items-center gap-2 px-5 py-2 text-xs sm:text-sm font-bold bg-blue-600 hover:bg-blue-700 text-white rounded-lg transition-all shadow-md active:scale-95 cursor-pointer"
            >
              <Printer className="w-4 h-4" />
              Drukuj fiszkę
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
