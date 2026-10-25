/* Financial Manager — shared client behaviour for every page. */

/* Copy an element's text to the clipboard ([data-copy-target] buttons). The button, or an
   element inside it, with data-copied-text briefly shows that text. */
function copyEl(btn, el) {
    if (!el || !navigator.clipboard) return;
    navigator.clipboard.writeText(el.textContent).then(() => {
        const label = btn.matches('[data-copied-text]') ? btn : btn.querySelector('[data-copied-text]');
        if (label) {
            const original = label.textContent;
            label.textContent = label.dataset.copiedText;
            setTimeout(() => { label.textContent = original; }, 2000);
        }
    });
}

(() => {
    const nav = document.querySelector('.fm-nav');

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
        // <form data-confirm="…"> or <button data-confirm="…">: ask before submitting.
        const question = (event.submitter && event.submitter.dataset.confirm) || form.dataset.confirm;
        if (question && !window.confirm(question)) {
            event.preventDefault();
            return;
        }
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
            if (sheet && sheet.showModal) {
                fillSheet(sheet, opener);
                sheet.showModal();
            }
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

    // An opener can prefill the sheet's form: data-action="/url" sets the form action and
    // data-f-monthly-payment="12" sets the field named "monthlyPayment".
    function fillSheet(sheet, opener) {
        const form = sheet.querySelector('form');
        if (!form) return;
        if (opener.dataset.action) form.action = opener.dataset.action;
        // A second submit button (e.g. Delete) posting elsewhere: data-alt-action on the opener.
        const alt = form.querySelector('[data-alt-submit]');
        if (alt) {
            alt.hidden = !opener.dataset.altAction;
            if (opener.dataset.altAction) alt.formAction = opener.dataset.altAction;
        }
        Object.keys(opener.dataset).forEach((key) => {
            if (!/^f[A-Z]/.test(key)) return;
            const name = key.charAt(1).toLowerCase() + key.slice(2);
            const field = form.elements.namedItem(name);
            if (field) field.value = opener.dataset[key];
        });
        // Extra forms inside the sheet (e.g. Cancel / Delete): <form data-action-from="cancelAction">
        // posts to the opener's data-cancel-action; hidden when the opener has none.
        sheet.querySelectorAll('form[data-action-from]').forEach((f) => {
            const url = opener.dataset[f.dataset.actionFrom];
            f.hidden = !url;
            if (url) f.action = url;
        });
        sheet.querySelectorAll('[data-sheet-extra]').forEach((el) => {
            el.hidden = el.dataset.sheetExtra !== (opener.dataset.mode || '');
        });
    }

    // Tap outside a bottom sheet closes it.
    document.querySelectorAll('dialog.sheet').forEach((sheet) => {
        sheet.addEventListener('click', (event) => {
            if (event.target === sheet) sheet.close();
        });
    });
})();
