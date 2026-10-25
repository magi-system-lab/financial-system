/* Financial Manager service worker: offline copies of the pages you open.
 *
 * - Pages under /ui: network first; every successful load is saved, and when the
 *   network is unreachable the saved copy is shown with an "offline" banner.
 * - Static files (/css, /js, /img, manifest): served from cache, refreshed in the background.
 * - Only GET requests are handled. Forms (payments, imports, …) always go to the server.
 */
const STATIC_CACHE = 'fm-static-v1';
const PAGES_CACHE = 'fm-pages';
const MAX_PAGES = 60;
const NETWORK_TIMEOUT_MS = 6000;
const PRECACHE = ['/offline.html', '/manifest.webmanifest', '/img/icons.svg', '/img/icon-192.png'];

self.addEventListener('install', (event) => {
    event.waitUntil(caches.open(STATIC_CACHE).then((cache) => cache.addAll(PRECACHE)).then(() => self.skipWaiting()));
});

self.addEventListener('activate', (event) => {
    event.waitUntil(
        caches.keys()
            .then((keys) => Promise.all(keys
                .filter((key) => key !== STATIC_CACHE && key !== PAGES_CACHE)
                .map((key) => caches.delete(key))))
            .then(() => self.clients.claim())
    );
});

self.addEventListener('fetch', (event) => {
    const request = event.request;
    if (request.method !== 'GET') return;
    const url = new URL(request.url);
    if (url.origin !== self.location.origin) return;

    if (request.mode === 'navigate') {
        if (url.pathname === '/ui' || url.pathname.startsWith('/ui/')) {
            event.respondWith(networkFirst(event));
        }
        return;
    }
    if (/^\/(css|js|img)\//.test(url.pathname) || url.pathname.startsWith('/manifest')) {
        event.respondWith(staleWhileRevalidate(event));
    }
});

async function networkFirst(event) {
    const request = event.request;
    const network = fetch(request).then(async (response) => {
        // Redirects come back as "opaqueredirect" and are never stored.
        if (response.ok && response.type === 'basic') {
            const cache = await caches.open(PAGES_CACHE);
            await cache.put(request, response.clone());
            trimPages(cache);
        }
        return response;
    });
    event.waitUntil(network.catch(() => undefined));

    const timeout = new Promise((resolve) => setTimeout(resolve, NETWORK_TIMEOUT_MS));
    try {
        const first = await Promise.race([network, timeout.then(() => offlineCopy(request))]);
        if (first) return first;
        return await network;
    } catch (e) {
        return (await offlineCopy(request)) || (await caches.match('/offline.html')) || Response.error();
    }
}

async function offlineCopy(request) {
    const cache = await caches.open(PAGES_CACHE);
    const cached = await cache.match(request);
    if (!cached) return undefined;
    // Mark the page so app.js shows the "offline · last updated" banner.
    const html = (await cached.text()).replace('data-from-cache="false"', 'data-from-cache="true"');
    const headers = new Headers(cached.headers);
    headers.delete('Content-Length');
    return new Response(html, { status: 200, headers });
}

async function trimPages(cache) {
    const keys = await cache.keys();
    for (let i = 0; i < keys.length - MAX_PAGES; i++) await cache.delete(keys[i]);
}

async function staleWhileRevalidate(event) {
    const cache = await caches.open(STATIC_CACHE);
    const cached = await cache.match(event.request);
    const refresh = fetch(event.request).then((response) => {
        if (response.ok) cache.put(event.request, response.clone());
        return response;
    });
    if (cached) {
        event.waitUntil(refresh.catch(() => undefined));
        return cached;
    }
    return refresh;
}
