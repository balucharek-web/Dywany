document.getElementById('popupSearch').addEventListener('keydown', (e) => {
  if (e.key === 'Enter') {
    const q = encodeURIComponent(e.target.value.trim());
    if (q) {
      window.open(`https://balucharek-web.github.io/Dywany/?search=${q}`, '_blank');
    }
  }
});
