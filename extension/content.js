// Content Script injected on leroymerlin.pl
(function() {
  function getProductData() {
    const titleElem = document.querySelector('h1') || document.querySelector('[data-qa="product-title"]');
    const title = titleElem ? titleElem.innerText.trim() : document.title.replace(' - Leroy Merlin', '').trim();

    // Check if this is a carpet/rug page
    const isCarpet = /dywan|chodnik|wykładzina/i.test(title) || /dywan/i.test(window.location.href);

    // Extract reference number
    let ref = '';
    const refMatch = document.body.innerText.match(/(?:Nr ref\.|Numer referencyjny|Ref:)\s*[:.]?\s*(\d{7,10})/i);
    if (refMatch) ref = refMatch[1];

    // Extract EAN
    let ean = '';
    const eanMatch = document.body.innerText.match(/(?:Kod EAN|EAN)\s*[:.]?\s*(\d{12,14})/i);
    if (eanMatch) ean = eanMatch[1];

    // Price
    let price = '';
    const priceElem = document.querySelector('[data-qa="product-price"]') || document.querySelector('.price');
    if (priceElem) price = priceElem.innerText.trim();

    return { title, ref, ean, price, isCarpet };
  }

  function injectBanner() {
    if (document.getElementById('dywanmag-floating-btn')) return;

    const data = getProductData();
    if (!data.ref && !data.ean && !data.isCarpet) return;

    const btn = document.createElement('div');
    btn.id = 'dywanmag-floating-btn';
    btn.style.cssText = `
      position: fixed;
      bottom: 24px;
      right: 24px;
      z-index: 999999;
      background: #1d4ed8;
      color: white;
      padding: 12px 18px;
      border-radius: 50px;
      box-shadow: 0 10px 25px rgba(0,0,0,0.3);
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
      font-size: 13px;
      font-weight: bold;
      display: flex;
      align-items: center;
      gap: 10px;
      cursor: pointer;
      transition: transform 0.2s, background 0.2s;
    `;
    btn.innerHTML = `
      <span style="font-size: 18px;">📌</span>
      <span>DywanMag: Przypisz do stojaka</span>
    `;

    btn.onmouseover = () => btn.style.transform = 'scale(1.05)';
    btn.onmouseout = () => btn.style.transform = 'scale(1)';

    btn.onclick = () => {
      const code = data.ref || data.ean || '';
      const url = `https://balucharek-web.github.io/Dywany/?search=${encodeURIComponent(code)}`;
      window.open(url, '_blank');
    };

    document.body.appendChild(btn);
  }

  window.addEventListener('DOMContentLoaded', injectBanner);
  setTimeout(injectBanner, 2000);
})();
