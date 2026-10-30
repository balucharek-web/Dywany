import lmLogo from '../assets/leroy-merlin-logo.png';

interface LeroyMerlinLogoProps {
  className?: string;
  alt?: string;
}

export function LeroyMerlinLogo({ className = "h-11", alt = "Leroy Merlin" }: LeroyMerlinLogoProps) {
  return (
    <img
      src={lmLogo}
      alt={alt}
      className={`w-auto object-contain select-none shrink-0 ${className}`}
      loading="eager"
      decoding="async"
    />
  );
}
