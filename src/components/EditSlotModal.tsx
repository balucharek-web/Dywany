import React, { useState, useEffect } from 'react';
import { CarpetSlot } from '../types';
import { fetchLeroyProduct } from '../services/leroyMerlin';
import {
  X,
  Sparkles,
  ExternalLink,
  QrCode,
  Loader2,
  Trash2,
  Check,
  ShoppingBag,
  Tag,
  Barcode
} from 'lucide-react';

interface EditSlotModalProps {
  slot: CarpetSlot;
  onSave: (
    slotId: string,
    rackNumber: number,
    slotLetter: string,
    productName: string,
    ean: string,
    referenceNumber: string,
    eslCode: string,
    price: string,
    imageUrl: string
  ) => void;
  onClear: (slotId: string) => void;
  onClose: () => void;
  onOpenScanner: () => void;
}

export const EditSlotModal: React.FC<EditSlotModalProps> = ({
  slot,
  onSave,
  onClear,
  onClose,
  onOpenScanner
}) => {
  const [lookupCode, setLookupCode] = useState(slot.ean || slot.referenceNumber || '');
  const [productName, setProductName] = useState(slot.productName || '');
  const [price, setPrice] = useState(slot.price || '');
  const [referenceNumber, setReferenceNumber] = useState(slot.referenceNumber || '');
  const [ean, setEan] = useState(slot.ean || '');
  const [eslCode, setEslCode] = useState(slot.eslCode || `ESL-${slot.slotId.toUpperCase()}`);
  const [imageUrl, setImageUrl] = useState(slot.imageUrl || '');

  const [isSearching, setIsSearching] = useState(false);
  const [feedback, setFeedback] = useState<{ type: 'success' | 'info' | 'error'; message: string } | null>(null);

  useEffect(() => {
    // Escape key listener to close modal
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [onClose]);

  const handleLeroySearch = async (codeToUse?: string) => {
    const code = (codeToUse || lookupCode).trim();
    if (!code) {
      setFeedback({ type: 'info', message: 'Wpisz kod EAN lub numer referencyjny' });
      return;
    }

    setIsSearching(true);
    setFeedback({ type: 'info', message: 'Wyszukiwanie w bazie Leroy Merlin...' });

    try {
      const result = await fetchLeroyProduct(code);
      if (result.status === 'SUCCESS' && result.product) {
        setProductName(result.product.name);
        setPrice(result.product.price);
        if (result.product.referenceNumber) setReferenceNumber(result.product.referenceNumber);
        if (result.product.ean) setEan(result.product.ean);
        if (result.product.imageUrl) setImageUrl(result.product.imageUrl);
        setFeedback({ type: 'success', message: `✓ Pobrano dane: ${result.product.name}` });
      } else if (result.status === 'NOT_FOUND') {
        if (result.prefillRef) setReferenceNumber(result.prefillRef);
        if (result.prefillEan) setEan(result.prefillEan);
        setFeedback({
          type: 'info',
          message: result.message || 'Nie znaleziono produktu w katalogu ekspozycyjnym.'
        });
      } else {
        setFeedback({ type: 'error', message: result.message || 'Błąd wyszukiwania' });
      }
    } catch (err: any) {
      setFeedback({ type: 'error', message: err.message || 'Błąd pobierania' });
    } finally {
      setIsSearching(false);
    }
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!productName.trim()) return;

    onSave(
      slot.slotId,
      slot.rackNumber,
      slot.slotLetter,
      productName.trim(),
      ean.trim(),
      referenceNumber.trim(),
      eslCode.trim(),
      price.trim(),
      imageUrl.trim()
    );
  };

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl max-w-lg w-full p-6 shadow-2xl space-y-5 border border-slate-100 my-8">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-slate-100 pb-3">
          <div>
            <span className="px-2.5 py-1 rounded-md bg-blue-100 text-blue-800 font-extrabold text-xs">
              MIEJSCE {slot.slotId.toUpperCase()}
            </span>
            <h2 className="text-base font-bold text-slate-900 mt-1">
              {slot.occupied ? 'Edycja dywanu na stojaku' : 'Dodaj dywan na stojak'}
            </h2>
            <p className="text-xs text-slate-400">
              Stojak {slot.rackNumber} — Miejsce {slot.slotLetter.toUpperCase()}
            </p>
          </div>
          <button
            onClick={onClose}
            className="text-slate-400 hover:text-slate-600 text-lg p-1 rounded-lg hover:bg-slate-100 transition cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Leroy Merlin Scraper Section */}
        <div className="p-4 rounded-xl bg-blue-50/70 border border-blue-100 space-y-2.5">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-2 text-blue-800 font-bold text-xs">
              <Sparkles className="w-4 h-4 text-blue-600" />
              <span>Pobierz z leroymerlin.pl</span>
            </div>
            {lookupCode && (
              <a
                href={`https://www.leroymerlin.pl/szukaj.html?q=${encodeURIComponent(lookupCode.trim())}`}
                target="_blank"
                rel="noopener noreferrer"
                className="text-[11px] font-semibold text-blue-700 hover:text-blue-900 flex items-center space-x-1"
              >
                <span>Otwórz stronę</span>
                <ExternalLink className="w-3 h-3" />
              </a>
            )}
          </div>

          <p className="text-[11px] text-slate-600 leading-relaxed">
            Wpisz kod EAN lub numer referencyjny ze strony Leroy Merlin, a skrypt pobierze nazwę, cenę i specyfikację.
          </p>

          <div className="flex gap-2">
            <div className="relative flex-1">
              <input
                type="text"
                value={lookupCode}
                onChange={(e) => {
                  setLookupCode(e.target.value);
                  const trimmed = e.target.value.trim();
                  if (/^\d{7,9}$/.test(trimmed)) setReferenceNumber(trimmed);
                  if (/^\d{12,14}$/.test(trimmed)) setEan(trimmed);
                }}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') {
                    e.preventDefault();
                    handleLeroySearch();
                  }
                }}
                placeholder="np. 3276007978674, 96058791 lub 82641234"
                className="w-full px-3 py-2 pr-9 rounded-lg border border-blue-200 text-xs bg-white focus:outline-hidden focus:ring-2 focus:ring-blue-600"
              />
              <button
                type="button"
                onClick={onOpenScanner}
                title="Skanuj kod aparatem"
                className="absolute right-2 top-1/2 -translate-y-1/2 text-slate-400 hover:text-blue-600 cursor-pointer"
              >
                <QrCode className="w-4 h-4" />
              </button>
            </div>

            <button
              type="button"
              onClick={() => handleLeroySearch()}
              disabled={isSearching}
              className="px-4 py-2 rounded-lg bg-blue-700 hover:bg-blue-800 text-white font-semibold text-xs transition flex items-center space-x-1.5 cursor-pointer disabled:opacity-50"
            >
              {isSearching ? (
                <Loader2 className="w-3.5 h-3.5 animate-spin" />
              ) : (
                <span>Szukaj</span>
              )}
            </button>
          </div>

          {/* Quick chips */}
          <div className="flex flex-wrap gap-1.5 pt-1">
            <button
              type="button"
              onClick={() => {
                setLookupCode('3276007978674');
                handleLeroySearch('3276007978674');
              }}
              className="px-2 py-0.5 rounded-full bg-blue-100 hover:bg-blue-200 text-blue-800 text-[10px] font-bold cursor-pointer"
            >
              📌 Kod z plakatu (3276007978674)
            </button>
            <button
              type="button"
              onClick={() => {
                setLookupCode('96058791');
                handleLeroySearch('96058791');
              }}
              className="px-2 py-0.5 rounded-full bg-slate-100 hover:bg-slate-200 text-slate-700 text-[10px] font-semibold cursor-pointer"
            >
              Ref: 96058791
            </button>
            <button
              type="button"
              onClick={() => {
                setLookupCode('82641234');
                handleLeroySearch('82641234');
              }}
              className="px-2 py-0.5 rounded-full bg-slate-100 hover:bg-slate-200 text-slate-700 text-[10px] font-semibold cursor-pointer"
            >
              Dywan Agnella (82641234)
            </button>
            <button
              type="button"
              onClick={() => {
                setLookupCode('84512390');
                handleLeroySearch('84512390');
              }}
              className="px-2 py-0.5 rounded-full bg-slate-100 hover:bg-slate-200 text-slate-700 text-[10px] font-semibold cursor-pointer"
            >
              Dywan Canvas (84512390)
            </button>
          </div>

          {feedback && (
            <div
              className={`text-[11px] font-medium ${
                feedback.type === 'success'
                  ? 'text-emerald-700'
                  : feedback.type === 'error'
                  ? 'text-rose-600'
                  : 'text-blue-800'
              }`}
            >
              {feedback.message}
            </div>
          )}
        </div>

        {/* Carpet Fields Form */}
        <form onSubmit={handleSubmit} className="space-y-3.5 text-xs">
          <div>
            <label className="block font-semibold text-slate-700 mb-1">
              Nazwa dywanu / modelu *
            </label>
            <div className="relative">
              <ShoppingBag className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                required
                value={productName}
                onChange={(e) => setProductName(e.target.value)}
                placeholder="np. Dywan Agnella Isfahan Rubinowy 160x230 cm"
                className="w-full pl-9 pr-3 py-2 rounded-lg border border-slate-200 focus:outline-hidden focus:ring-2 focus:ring-blue-600"
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block font-semibold text-slate-700 mb-1">
                Cena (zł)
              </label>
              <input
                type="text"
                value={price}
                onChange={(e) => setPrice(e.target.value)}
                placeholder="np. 499,00 zł"
                className="w-full px-3 py-2 rounded-lg border border-slate-200 focus:outline-hidden focus:ring-2 focus:ring-blue-600"
              />
            </div>
            <div>
              <label className="block font-semibold text-slate-700 mb-1">
                Nr referencyjny
              </label>
              <div className="relative">
                <Tag className="w-3.5 h-3.5 text-slate-400 absolute left-2.5 top-1/2 -translate-y-1/2" />
                <input
                  type="text"
                  value={referenceNumber}
                  onChange={(e) => setReferenceNumber(e.target.value)}
                  placeholder="np. 82641234"
                  className="w-full pl-8 pr-3 py-2 rounded-lg border border-slate-200 focus:outline-hidden focus:ring-2 focus:ring-blue-600"
                />
              </div>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block font-semibold text-slate-700 mb-1">
                Kod kreskowy EAN
              </label>
              <div className="relative">
                <Barcode className="w-3.5 h-3.5 text-slate-400 absolute left-2.5 top-1/2 -translate-y-1/2" />
                <input
                  type="text"
                  value={ean}
                  onChange={(e) => setEan(e.target.value)}
                  placeholder="np. 5901234567890"
                  className="w-full pl-8 pr-3 py-2 rounded-lg border border-slate-200 focus:outline-hidden focus:ring-2 focus:ring-blue-600"
                />
              </div>
            </div>
            <div>
              <label className="block font-semibold text-slate-700 mb-1">
                Kod etykiety ESL *
              </label>
              <div className="relative">
                <QrCode className="w-3.5 h-3.5 text-slate-400 absolute left-2.5 top-1/2 -translate-y-1/2" />
                <input
                  type="text"
                  required
                  value={eslCode}
                  onChange={(e) => setEslCode(e.target.value)}
                  placeholder="np. ESL-1A"
                  className="w-full pl-8 pr-3 py-2 rounded-lg border border-slate-200 focus:outline-hidden focus:ring-2 focus:ring-blue-600"
                />
              </div>
            </div>
          </div>

          <div>
            <label className="block font-semibold text-slate-700 mb-1">
              URL zdjęcia (opcjonalnie)
            </label>
            <input
              type="text"
              value={imageUrl}
              onChange={(e) => setImageUrl(e.target.value)}
              placeholder="https://images.unsplash.com/..."
              className="w-full px-3 py-2 rounded-lg border border-slate-200 focus:outline-hidden focus:ring-2 focus:ring-blue-600"
            />
          </div>

          {imageUrl && (
            <div className="pt-1">
              <span className="block text-[10px] text-slate-400 mb-1">Podgląd zdjęcia:</span>
              <img
                src={imageUrl}
                alt="Podgląd dywanu"
                className="w-full h-24 object-cover rounded-lg border border-slate-200"
                onError={(e) => {
                  (e.target as HTMLElement).style.display = 'none';
                }}
              />
            </div>
          )}

          {/* Action buttons */}
          <div className="flex items-center justify-between border-t border-slate-100 pt-3">
            {slot.occupied ? (
              <button
                type="button"
                onClick={() => onClear(slot.slotId)}
                className="text-rose-600 hover:text-rose-800 font-semibold text-xs flex items-center space-x-1 p-1 cursor-pointer"
              >
                <Trash2 className="w-4 h-4" />
                <span>Zwolnij miejsce</span>
              </button>
            ) : (
              <div></div>
            )}

            <div className="flex space-x-2 ml-auto">
              <button
                type="button"
                onClick={onClose}
                className="px-4 py-2 rounded-lg bg-slate-100 hover:bg-slate-200 text-slate-600 font-semibold text-xs transition cursor-pointer"
              >
                Anuluj
              </button>
              <button
                type="submit"
                disabled={!productName.trim()}
                className="px-5 py-2 rounded-lg bg-blue-700 hover:bg-blue-800 text-white font-semibold text-xs flex items-center space-x-1.5 shadow-xs transition cursor-pointer disabled:opacity-50"
              >
                <Check className="w-4 h-4" />
                <span>Zapisz na stojaku</span>
              </button>
            </div>
          </div>
        </form>
      </div>
    </div>
  );
};
