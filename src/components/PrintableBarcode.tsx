import { useEffect, useRef } from 'react';
import JsBarcode from 'jsbarcode';

interface PrintableBarcodeProps {
  value?: string;
  className?: string;
  width?: number;
  height?: number;
  fontSize?: number;
}

export function PrintableBarcode({
  value,
  className = 'max-w-[140px] h-[36px]',
  width = 1.3,
  height = 30,
  fontSize = 10
}: PrintableBarcodeProps) {
  const svgRef = useRef<SVGSVGElement | null>(null);

  useEffect(() => {
    if (!value || !svgRef.current) return;
    try {
      const clean = value.replace(/\D/g, '');
      if (clean.length === 13 || clean.length === 12 || clean.length === 8) {
        JsBarcode(svgRef.current, clean, {
          format: clean.length === 13 ? 'EAN13' : 'CODE128',
          width,
          height,
          displayValue: true,
          fontSize,
          margin: 0,
          textMargin: 1
        });
      } else {
        JsBarcode(svgRef.current, value, {
          format: 'CODE128',
          width: Math.max(1.0, width - 0.2),
          height,
          displayValue: true,
          fontSize,
          margin: 0,
          textMargin: 1
        });
      }
    } catch (e) {
      console.warn('Barcode print error:', e);
    }
  }, [value, width, height, fontSize]);

  if (!value || value.trim() === '') {
    return <span className="text-[10px] text-gray-400 italic print:text-gray-500">—</span>;
  }

  return (
    <div className="flex flex-col items-center justify-center">
      <svg ref={svgRef} className={className} />
    </div>
  );
}
