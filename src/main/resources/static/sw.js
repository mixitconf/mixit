importScripts('workbox-sw.js');

if (workbox) {
    workbox.core.setCacheNameDetails({
        prefix: 'mixit',
        suffix: 'v2'
    });

    workbox.precaching.precacheAndRoute([]);

    workbox.routing.registerRoute(
        new RegExp('https://fonts.(?:googleapis|gstatic).com/(.*)'),
        workbox.strategies.cacheFirst({
            cacheName: 'googleapis',
            networkTimeoutSeconds: 3,
            plugins: [
                new workbox.expiration.Plugin({
                    maxEntries: 30
                })
            ]
        })
    );

    workbox.routing.registerRoute(
        /\.(?:png|gif|jpg|jpeg|svg|webp)$/,
        workbox.strategies.cacheFirst({
            cacheName: 'images',
            networkTimeoutSeconds: 3,
            plugins: [
                new workbox.expiration.Plugin({
                    maxEntries: 60,
                    maxAgeSeconds: 6 * 60 * 60
                })
            ]
        })
    );

    // use a stale while revalidate for CSS and JavaScript files that aren't precached.
    workbox.routing.registerRoute(/\.(?:js|css)$/, workbox.strategies.staleWhileRevalidate({
            cacheName: 'static-resources',
            networkTimeoutSeconds: 3,
            plugins: [
                new workbox.expiration.Plugin({
                    maxEntries: 60,
                    maxAgeSeconds: 6 * 60 * 60
                })
            ]
        })
    );

    workbox.routing.registerRoute(/.*$/, workbox.strategies.staleWhileRevalidate({
            cacheName: 'html-resources',
            networkTimeoutSeconds: 3,
            plugins: [
                new workbox.expiration.Plugin({
                    maxEntries: 80,
                    maxAgeSeconds: 6 * 60 * 60
                }),
                new workbox.cacheableResponse.Plugin({
                    headers: {
                        'Content-Type': 'text/html;charset=UTF-8'
                    }
                })
            ]
        })
    );
}
else{
    console.error('Error on workbox initialization');
}