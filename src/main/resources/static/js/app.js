/* Financial Manager — shared client behaviour for every page (new and legacy). */

/* Copy an element's text to the clipboard. Used by legacy pages (onclick="copyEl(this, el)")
   and by redesigned pages through [data-copy-target]. */
function copyEl(btn, el) {
    if (!el || !navigator.clipboard) return;
    navigator.clipboard.writeText(el.textContent).then(() => {
        const icon = btn.querySelector('i');
        if (icon) {
            icon.className = 'bi bi-clipboard-check';
            setTimeout(() => { icon.className = 'bi bi-clipboard'; }, 2000);
        }
        const label = btn.querySelector('[data-copied-text]');
        if (label) {
            const original = label.textContent;
            label.textContent = label.dataset.copiedText;
            setTimeout(() => { label.textContent = original; }, 2000);
        }
    });
}

(() => {
    const nav = document.querySelector('.fm-nav');

    // Legacy pages ship a viewport meta without viewport-fit=cover; add it so the
    // safe-area padding works when the app runs full-screen from the Home Screen.
    const viewport = document.querySelector('meta[name="viewport"]');
    if (viewport && !viewport.content.includes('viewport-fit')) {
        viewport.content += ', viewport-fit=cover';
    }

    // Offline support (service workers only run over HTTPS or on localhost).
    if ('serviceWorker' in navigator) {
        window.addEventListener('load', () => {
            navigator.serviceWorker.register('/sw.js').catch(() => { /* offline mode unavailable */ });
        });
    }

    // Offline banner: shown when the device has no connection or the page was served
    // from the offline copy (the service worker flips data-from-cache to "true").
    const banner = nav && nav.querySelector('.fm-offline');
    const updateBanner = () => {
        if (!banner) return;
        const fromCache = nav.dataset.fromCache === 'true';
        banner.hidden = navigator.onLine && !fromCache;
        if (!banner.hidden && fromCache && nav.dataset.rendered) {
            const when = new Date(Number(nav.dataset.rendered));
            const time = when.toLocaleString(document.documentElement.lang || undefined,
                { month: 'short', day: 'numeric', hour: 'numeric', minute: '2-digit' });
            banner.dataset.when = time;
            if (!banner.dataset.base) banner.dataset.base = banner.textContent;
            banner.textContent = banner.dataset.base + ' · ' + (banner.dataset.updated || '') + ' ' + time;
        }
    };
    updateBanner();
    window.addEventListener('online', updateBanner);
    window.addEventListener('offline', updateBanner);

    document.addEventListener('submit', (event) => {
        const form = event.target;
        // Writes never work offline: stop early with a clear message.
        if (!navigator.onLine) {
            event.preventDefault();
            alert((nav && nav.dataset.offlineForm) || 'You are offline.');
            return;
        }
        // Switching user (or logging in) wipes the offline copies of the previous user's pages.
        if (form.hasAttribute('data-clear-offline') && 'caches' in window) {
            event.preventDefault();
            form.removeAttribute('data-clear-offline');
            caches.delete('fm-pages').finally(() => form.submit());
        }
    }, true);

    // Copy buttons on redesigned pages: <button data-copy-target="#id">.
    document.addEventListener('click', (event) => {
        const btn = event.target.closest('[data-copy-target]');
        if (btn) copyEl(btn, document.querySelector(btn.dataset.copyTarget));

        const opener = event.target.closest('[data-open-sheet]');
        if (opener) {
            const sheet = document.getElementById(opener.dataset.openSheet);
            if (sheet && sheet.showModal) sheet.showModal();
        }
        const closer = event.target.closest('[data-close-sheet]');
        if (closer) closer.closest('dialog')?.close();
    });

    // Selectable groups (e.g. the Insights month bars): <button data-select="g" data-value="3">
    // shows every [data-select-panel="g"][data-value="3"] and hides the rest of that group.
    document.addEventListener('click', (event) => {
        const btn = event.target.closest('[data-select]');
        if (!btn) return;
        const group = btn.dataset.select;
        const value = btn.dataset.value;
        document.querySelectorAll(`[data-select="${group}"]`).forEach((b) =>
            b.setAttribute('aria-pressed', String(b.dataset.value === value)));
        document.querySelectorAll(`[data-select-panel="${group}"]`).forEach((p) => {
            p.hidden = p.dataset.value !== value;
        });
    });

    // Tap outside a bottom sheet closes it.
    document.querySelectorAll('dialog.sheet').forEach((sheet) => {
        sheet.addEventListener('click', (event) => {
            if (event.target === sheet) sheet.close();
        });
    });
})();
