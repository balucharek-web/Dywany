import { useEffect, useRef, useState } from 'react';
import {
  X,
  Camera,
  Upload,
  Flashlight,
  Sparkles,
  CheckCircle2,
  AlertCircle,
  RotateCcw,
  Loader2,
  Tag,
  Hash,
  Barcode
} from 'lucide-react';
import { scanEslLabel, fileToBase64Compressed, EslScanResult } from '../services/eslScannerService';

interface EslScannerModalProps {
  isOpen: boolean;
  onClose: () => void;
  onApplyData: (result: EslScanResult) => void;
}

export function EslScannerModal({ isOpen, onClose, onApplyData }: EslScannerModalProps) {
  const [cameraActive, setCameraActive] = useState(false);
  const [cameraLoading, setCameraLoading] = useState(true);
  const [cameraError, setCameraError] = useState<string | null>(null);
  const [torchOn, setTorchOn] = useState(false);
  const [hasTorch, setHasTorch] = useState(false);
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [analyzedImage, setAnalyzedImage] = useState<string | null>(null);
  const [scanResult, setScanResult] = useState<EslScanResult | null>(null);
  const [scanError, setScanError] = useState<string | null>(null);

  const videoRef = useRef<HTMLVideoElement | null>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const galleryInputRef = useRef<HTMLInputElement | null>(null);
  const cameraInputRef = useRef<HTMLInputElement | null>(null);

  // Start Camera with multi-tier fallback for constraints
  const startCamera = async () => {
    setCameraLoading(true);
    setCameraError(null);
    try {
      if (streamRef.current) {
        streamRef.current.getTracks().forEach((t) => t.stop());
        streamRef.current = null;
      }

      let stream: MediaStream | null = null;

      // Tier 1: High quality environment camera
      try {
        stream = await navigator.mediaDevices.getUserMedia({
          video: {
            facingMode: { ideal: 'environment' },
            width: { ideal: 1920 },
            height: { ideal: 1080 }
          },
          audio: false
        });
      } catch (e1) {
        // Tier 2: Basic environment facing camera
        try {
          stream = await navigator.mediaDevices.getUserMedia({
            video: { facingMode: 'environment' },
            audio: false
          });
        } catch (e2) {
          // Tier 3: Any video camera available
          stream = await navigator.mediaDevices.getUserMedia({
            video: true,
            audio: false
          });
        }
      }

      if (!stream) {
        throw new Error('Nie udało się uzyskać strumienia wideo.');
      }

      streamRef.current = stream;

      if (videoRef.current) {
        videoRef.current.srcObject = stream;
        videoRef.current.onloadedmetadata = () => {
          videoRef.current?.play().catch((playErr) => {
            console.warn('Video play prevented or delayed:', playErr);
          });
        };
        // also call play directly in case loadedmetadata already fired
        try {
          await videoRef.current.play();
        } catch (_) {}
      }

      setCameraActive(true);
      setCameraLoading(false);

      // Check flashlight support
      const track = stream.getVideoTracks()[0];
      if (track) {
        const capabilities = (track.getCapabilities?.() as any) || {};
        setHasTorch(Boolean(capabilities.torch));
      }
    } catch (err: unknown) {
      console.warn('Camera access issue:', err);
      setCameraLoading(false);
      setCameraActive(false);
      setCameraError(
        'Brak dostępu do kamery w przeglądarce. Użyj przycisku „Zrób zdjęcie aparatem” lub „Wybierz z galerii”.'
      );
    }
  };

  // Stop Camera
  const stopCamera = () => {
    if (streamRef.current) {
      streamRef.current.getTracks().forEach((track) => track.stop());
      streamRef.current = null;
    }
    if (videoRef.current) {
      videoRef.current.srcObject = null;
    }
    setCameraActive(false);
    setCameraLoading(false);
    setTorchOn(false);
  };

  // Toggle Torch
  const toggleTorch = async () => {
    if (!streamRef.current) return;
    const track = streamRef.current.getVideoTracks()[0];
    if (!track) return;
    try {
      const nextTorch = !torchOn;
      await (track as any).applyConstraints({
        advanced: [{ torch: nextTorch }]
      });
      setTorchOn(nextTorch);
    } catch (e) {
      console.warn('Torch toggle failed:', e);
    }
  };

  useEffect(() => {
    if (isOpen && !analyzedImage && !scanResult) {
      const timer = setTimeout(() => {
        startCamera();
      }, 50);
      return () => {
        clearTimeout(timer);
        stopCamera();
      };
    } else {
      stopCamera();
    }
    return () => {
      stopCamera();
    };
  }, [isOpen]);

  // Capture frame from video feed
  const handleCaptureFrame = async () => {
    if (!videoRef.current) return;
    try {
      const video = videoRef.current;
      const vw = video.videoWidth || 1280;
      const vh = video.videoHeight || 720;
      const canvas = document.createElement('canvas');
      canvas.width = vw;
      canvas.height = vh;
      const ctx = canvas.getContext('2d');
      if (!ctx) return;

      ctx.drawImage(video, 0, 0, vw, vh);
      const dataUrl = canvas.toDataURL('image/jpeg', 0.90);
      stopCamera();
      setAnalyzedImage(dataUrl);
      processImage(dataUrl, 'image/jpeg');
    } catch (err: unknown) {
      setScanError('Błąd podczas przechwytywania klatki z kamery.');
    }
  };

  // Handle file or native camera photo selection
  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    stopCamera();
    try {
      setIsAnalyzing(true);
      setScanError(null);
      const { base64, mimeType } = await fileToBase64Compressed(file);
      setAnalyzedImage(base64);
      await processImage(base64, mimeType);
    } catch (err: unknown) {
      setScanError(err instanceof Error ? err.message : 'Błąd wczytywania zdjęcia');
      setIsAnalyzing(false);
    } finally {
      if (galleryInputRef.current) galleryInputRef.current.value = '';
      if (cameraInputRef.current) cameraInputRef.current.value = '';
    }
  };

  // Run OCR analysis via backend API
  const processImage = async (base64: string, mimeType: string) => {
    setIsAnalyzing(true);
    setScanError(null);
    try {
      const result = await scanEslLabel(base64, mimeType);
      setScanResult(result);
    } catch (err: unknown) {
      console.error('Scan error:', err);
      setScanError(
        err instanceof Error
          ? err.message
          : 'Nie udało się poprawnie odczytać etykiety ESL. Upewnij się, że tekst i cyfry są ostre i czytelne.'
      );
    } finally {
      setIsAnalyzing(false);
    }
  };

  // Reset to scan again
  const handleResetScan = () => {
    setAnalyzedImage(null);
    setScanResult(null);
    setScanError(null);
    setIsAnalyzing(false);
    startCamera();
  };

  // Apply data to form and close
  const handleConfirmApply = () => {
    if (!scanResult) return;
    onApplyData(scanResult);
    onClose();
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-60 bg-black/80 flex items-center justify-center p-3 sm:p-4 backdrop-blur-sm animate-in fade-in">
      {/* Hidden file inputs: ONE for pure gallery, ONE for direct camera */}
      <input
        ref={galleryInputRef}
        type="file"
        accept="image/*"
        onChange={handleFileChange}
        className="hidden"
      />
      <input
        ref={cameraInputRef}
        type="file"
        accept="image/*"
        capture="environment"
        onChange={handleFileChange}
        className="hidden"
      />

      <div className="bg-white rounded-2xl max-w-lg w-full shadow-2xl overflow-hidden flex flex-col max-h-[92vh]">
        {/* Header */}
        <div className="flex items-center justify-between px-5 py-3.5 border-b border-gray-200 bg-linear-to-r from-emerald-50 via-teal-50 to-blue-50">
          <div className="flex items-center gap-2 font-bold text-gray-900 text-sm sm:text-base">
            <div className="w-7 h-7 rounded-lg bg-emerald-600 text-white flex items-center justify-center shadow-xs">
              <Sparkles className="w-4 h-4" />
            </div>
            <span>Odczytaj etykietę cenową ESL (AI)</span>
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded-lg text-gray-400 hover:text-gray-700 hover:bg-gray-200 transition-colors cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content body */}
        <div className="p-4 sm:p-5 overflow-y-auto flex-1 flex flex-col gap-3.5">
          {/* Instructions banner */}
          <div className="text-xs text-gray-600 bg-gray-50 border border-gray-200 rounded-xl p-3 flex items-start gap-2.5">
            <Sparkles className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" />
            <div>
              <p className="font-semibold text-gray-800">
                Skieruj aparat na cenówkę elektroniczną lub wgraj jej zdjęcie.
              </p>
              <p className="text-[11px] text-gray-500 mt-0.5">
                AI automatycznie wyodrębni nazwę dywanu, numer referencyjny REF LM oraz kod kreskowy EAN.
              </p>
            </div>
          </div>

          {/* Camera View or Captured Photo Container */}
          <div className="relative rounded-2xl overflow-hidden bg-black aspect-4/3 flex items-center justify-center shadow-inner">
            {/* VIDEO IS ALWAYS MOUNTED IN DOM SO videoRef.current IS NEVER NULL */}
            <video
              ref={videoRef}
              playsInline
              muted
              autoPlay
              className={`w-full h-full object-cover transition-opacity duration-300 ${
                cameraActive && !analyzedImage ? 'opacity-100' : 'opacity-0 absolute pointer-events-none'
              }`}
            />

            {/* ESL Viewfinder Overlay on active video */}
            {cameraActive && !analyzedImage && (
              <>
                <div className="absolute inset-0 pointer-events-none flex flex-col items-center justify-center p-6">
                  <div className="w-full max-w-[340px] aspect-21/10 rounded-xl border-2 border-dashed border-emerald-400 bg-emerald-500/10 shadow-[0_0_0_9999px_rgba(0,0,0,0.5)] flex flex-col justify-between p-2">
                    <div className="flex justify-between items-center text-[10px] font-bold text-emerald-300 uppercase tracking-wider">
                      <span>Etykieta ESL</span>
                      <span>Dywany 038</span>
                    </div>
                    <div className="text-center text-[11px] font-bold text-white drop-shadow-md">
                      Umieść cenówkę w ramce
                    </div>
                    <div className="flex justify-between items-center text-[9px] text-emerald-200">
                      <span>REF & Nazwa</span>
                      <span>Kod EAN</span>
                    </div>
                  </div>
                </div>

                {/* Torch Button */}
                {hasTorch && (
                  <button
                    type="button"
                    onClick={toggleTorch}
                    className={`absolute top-3 right-3 p-2.5 rounded-full backdrop-blur-md transition-all shadow-md cursor-pointer ${
                      torchOn ? 'bg-amber-400 text-black' : 'bg-black/50 text-white hover:bg-black/70'
                    }`}
                    title={torchOn ? 'Wyłącz latarkę' : 'Włącz latarkę'}
                  >
                    <Flashlight className="w-5 h-5" />
                  </button>
                )}
              </>
            )}

            {/* Captured or Uploaded Image Preview */}
            {analyzedImage && (
              <img
                src={analyzedImage}
                alt="Etykieta ESL"
                className="w-full h-full object-contain"
              />
            )}

            {/* Loading / analyzing spinner */}
            {isAnalyzing && (
              <div className="absolute inset-0 bg-black/60 flex flex-col items-center justify-center gap-3 backdrop-blur-2xs z-20">
                <div className="w-12 h-12 rounded-full border-4 border-emerald-400 border-t-transparent animate-spin" />
                <span className="text-xs font-black text-white px-3 py-1.5 bg-black/70 rounded-full tracking-wide shadow-lg">
                  AI odczytuje dane z etykiety...
                </span>
              </div>
            )}

            {/* Camera Loading State */}
            {cameraLoading && !analyzedImage && (
              <div className="absolute inset-0 flex flex-col items-center justify-center gap-2 text-white bg-black/80 z-10">
                <Loader2 className="w-8 h-8 animate-spin text-emerald-400" />
                <span className="text-xs font-medium text-gray-300">Uruchamianie aparatu...</span>
              </div>
            )}

            {/* Camera Inactive / Error UI */}
            {!cameraActive && !cameraLoading && !analyzedImage && (
              <div className="flex flex-col items-center gap-2 p-6 text-center text-gray-400 z-10">
                <Camera className="w-10 h-10 text-gray-500 mb-1" />
                <span className="text-xs text-gray-300 font-medium max-w-xs">
                  {cameraError || 'Kamera nie jest aktywna'}
                </span>
                <div className="flex items-center gap-2 mt-2">
                  <button
                    type="button"
                    onClick={startCamera}
                    className="px-3.5 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-xs font-bold transition-colors cursor-pointer"
                  >
                    Włącz kamerę ponownie
                  </button>
                </div>
              </div>
            )}
          </div>

          {/* Action buttons (Capture, Take native photo, or Pick from Gallery) */}
          {!scanResult && (
            <div className="flex flex-col gap-2">
              {/* Primary live camera snapshot button */}
              {cameraActive && !analyzedImage && (
                <button
                  type="button"
                  onClick={handleCaptureFrame}
                  className="w-full py-3 px-4 bg-emerald-600 hover:bg-emerald-700 active:scale-98 text-white rounded-xl text-sm font-bold flex items-center justify-center gap-2 shadow-md transition-all cursor-pointer"
                >
                  <Camera className="w-5 h-5" />
                  <span>Zrób zdjęcie i odczytaj</span>
                </button>
              )}

              {/* Two clear buttons: Gallery vs System Camera */}
              <div className="grid grid-cols-2 gap-2">
                <button
                  type="button"
                  onClick={() => galleryInputRef.current?.click()}
                  disabled={isAnalyzing}
                  className="py-2.5 px-3 border border-gray-300 hover:border-emerald-500 bg-white hover:bg-emerald-50/50 text-gray-800 rounded-xl text-xs font-bold flex items-center justify-center gap-1.5 transition-all cursor-pointer shadow-2xs"
                >
                  <Upload className="w-4 h-4 text-blue-600 shrink-0" />
                  <span>Wybierz z galerii</span>
                </button>

                <button
                  type="button"
                  onClick={() => cameraInputRef.current?.click()}
                  disabled={isAnalyzing}
                  className="py-2.5 px-3 border border-gray-300 hover:border-emerald-500 bg-white hover:bg-emerald-50/50 text-gray-800 rounded-xl text-xs font-bold flex items-center justify-center gap-1.5 transition-all cursor-pointer shadow-2xs"
                >
                  <Camera className="w-4 h-4 text-emerald-600 shrink-0" />
                  <span>Aparat systemowy</span>
                </button>
              </div>
            </div>
          )}

          {/* Error Message */}
          {scanError && (
            <div className="bg-red-50 border border-red-200 rounded-xl p-3 flex flex-col gap-2 animate-in fade-in">
              <div className="flex items-start gap-2 text-xs text-red-700">
                <AlertCircle className="w-4 h-4 text-red-600 shrink-0 mt-0.5" />
                <span>{scanError}</span>
              </div>
              <button
                type="button"
                onClick={handleResetScan}
                className="self-start text-xs font-bold text-red-700 hover:underline flex items-center gap-1 cursor-pointer pt-1"
              >
                <RotateCcw className="w-3.5 h-3.5" />
                Spróbuj ponownie
              </button>
            </div>
          )}

          {/* Success Scan Result Display */}
          {scanResult && (
            <div className="bg-emerald-50/80 border-2 border-emerald-300 rounded-2xl p-4 flex flex-col gap-3 animate-in fade-in zoom-in-98 shadow-sm">
              <div className="flex items-center justify-between border-b border-emerald-200 pb-2">
                <div className="flex items-center gap-1.5 text-xs font-black text-emerald-950 uppercase tracking-wider">
                  <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                  Odczytano dane z etykiety ESL
                </div>
                {scanResult.cena && (
                  <span className="px-2 py-0.5 bg-emerald-200 text-emerald-900 rounded-md text-xs font-extrabold">
                    {scanResult.cena} zł
                  </span>
                )}
              </div>

              <div className="flex flex-col gap-2 text-xs">
                {/* Nazwa */}
                <div className="bg-white p-2.5 rounded-xl border border-emerald-200 flex flex-col">
                  <span className="text-[10px] font-bold text-gray-400 uppercase flex items-center gap-1">
                    <Tag className="w-3 h-3 text-emerald-600" />
                    Nazwa dywanu i wymiary
                  </span>
                  <span className="font-extrabold text-gray-900 text-sm mt-0.5">
                    {scanResult.nazwa || 'Nie odczytano'}
                  </span>
                </div>

                {/* REF & EAN grid */}
                <div className="grid grid-cols-2 gap-2">
                  <div className="bg-white p-2.5 rounded-xl border border-emerald-200 flex flex-col">
                    <span className="text-[10px] font-bold text-gray-400 uppercase flex items-center gap-1">
                      <Hash className="w-3 h-3 text-blue-600" />
                      REF. LM (8 cyfr)
                    </span>
                    <span className="font-mono font-bold text-gray-900 text-sm mt-0.5">
                      {scanResult.ref || '-'}
                    </span>
                  </div>

                  <div className="bg-white p-2.5 rounded-xl border border-emerald-200 flex flex-col">
                    <span className="text-[10px] font-bold text-gray-400 uppercase flex items-center gap-1">
                      <Barcode className="w-3 h-3 text-purple-600" />
                      Kod kreskowy EAN
                    </span>
                    <span className="font-mono font-bold text-gray-900 text-sm mt-0.5">
                      {scanResult.ean || '-'}
                    </span>
                  </div>
                </div>
              </div>

              {/* Action Buttons */}
              <div className="flex gap-2 pt-1">
                <button
                  type="button"
                  onClick={handleResetScan}
                  className="py-2.5 px-3 rounded-xl bg-white border border-gray-300 text-gray-700 font-bold text-xs hover:bg-gray-50 transition-colors cursor-pointer flex items-center justify-center gap-1.5"
                >
                  <RotateCcw className="w-3.5 h-3.5" />
                  Zeskanuj inną
                </button>

                <button
                  type="button"
                  onClick={handleConfirmApply}
                  className="flex-1 py-2.5 px-4 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-extrabold text-xs sm:text-sm transition-all shadow-md cursor-pointer flex items-center justify-center gap-1.5 active:scale-98"
                >
                  <CheckCircle2 className="w-4 h-4" />
                  Wstaw do formularza
                </button>
              </div>
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="px-5 py-3 border-t border-gray-200 bg-gray-50 flex justify-between items-center">
          <span className="text-[11px] text-gray-500 font-medium">
            AI Gemini 3.8 Flash • Leroy Merlin ESL OCR
          </span>
          <button
            onClick={onClose}
            className="px-4 py-1.5 bg-gray-200 hover:bg-gray-300 text-gray-700 rounded-lg text-xs font-bold transition-colors cursor-pointer"
          >
            Zamknij
          </button>
        </div>
      </div>
    </div>
  );
}
