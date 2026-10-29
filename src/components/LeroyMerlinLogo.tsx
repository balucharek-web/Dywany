export function LeroyMerlinLogo({ className = "h-11" }: { className?: string }) {
  return (
    <div className={`flex items-center select-none ${className}`}>
      <svg
        viewBox="0 0 170 105"
        className="h-full w-auto"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
      >
        <defs>
          {/* Path for text LEROY running up the left slope */}
          <path id="lm-left-slope" d="M 8 98 L 85 12" />
          {/* Path for text MERLIN running down the right slope */}
          <path id="lm-right-slope" d="M 85 12 L 162 98" />
        </defs>

        {/* Green Triangle (Leroy Merlin green #78be20) */}
        <polygon points="85,16 156,96 14,96" fill="#78be20" />

        {/* White House Silhouette in center */}
        <polygon points="85,42 122,72 122,96 48,96 48,72" fill="#ffffff" />

        {/* Text LEROY angled on left side */}
        <text
          fill="#111827"
          fontFamily="system-ui, -apple-system, 'Inter', 'Arial Black', sans-serif"
          fontWeight="900"
          fontSize="13"
          letterSpacing="1"
        >
          <textPath href="#lm-left-slope" startOffset="48%" textAnchor="middle">
            LEROY
          </textPath>
        </text>

        {/* Text MERLIN angled on right side */}
        <text
          fill="#111827"
          fontFamily="system-ui, -apple-system, 'Inter', 'Arial Black', sans-serif"
          fontWeight="900"
          fontSize="13"
          letterSpacing="1"
        >
          <textPath href="#lm-right-slope" startOffset="52%" textAnchor="middle">
            MERLIN
          </textPath>
        </text>
      </svg>
    </div>
  );
}
