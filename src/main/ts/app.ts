class MixitApp{
    bootstrap(){
        this._initServiceWorker();
    }

    _initServiceWorker() {
        var isLocalhost = Boolean(window.location.hostname === 'localhost' ||
            // [::1] is the IPv6 localhost address.
            window.location.hostname === '[::1]' ||
            // 127.0.0.1/8 is considered localhost for IPv4.
            window.location.hostname.match(
                /^127(?:\.(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)){3}$/
            )
        );

        if ('serviceWorker' in navigator && (window.location.protocol === 'https:' || isLocalhost)) {
            navigator.serviceWorker
                .register('/sw.js', {scope: '/'})
                .then(function (registration) {
                    console.log('ServiceWorker registration successful with scope: ', registration.scope)
                    registration.onupdatefound = function () {
                        var installingWorker = registration.installing;
                        installingWorker.onstatechange = function () {
                            switch (installingWorker.state) {
                                case 'installed':
                                    if (navigator.serviceWorker.controller) {
                                        console.log('new update available');
                                        location.reload(true);
                                    }
                                    break;

                                default:
                            }
                        }
                    }
                })
                .catch(function(e) {
                    console.error('Error during service worker registration:', e);
                });
        }
    }
}
new MixitApp().bootstrap();
