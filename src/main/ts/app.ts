import * as $ from 'jquery';
window['jQuery'] = $;
import 'foundation-sites';

class MixitApp{
    bootstrap(){
        this._initServiceWorker();
    }

    _initServiceWorker() {
        let isLocalhost = Boolean(window.location.hostname === 'localhost' ||
            window.location.hostname === '[::1]' ||
            window.location.hostname.match(/^127(?:\.(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)){3}$/)
        );

        if ('serviceWorker' in navigator && (window.location.protocol === 'https:' || isLocalhost)) {
            navigator.serviceWorker.register('service-worker.js').then(function(registration) {
                if (typeof registration.update === 'function') {
                    registration.update();
                }
                registration.onupdatefound = function() {
                    if (navigator.serviceWorker.controller) {
                        var installingWorker = registration.installing;

                        installingWorker.onstatechange = function() {
                            switch (installingWorker.state) {
                                case 'installed':
                                    console.error('New content is available; please refresh.');
                                    break;

                                case 'redundant':
                                    throw new Error('The installing service worker became redundant.');

                                default:
                                    // Ignore
                            }
                        };
                    }
                };
            }).catch(function(e) {
                console.error('Error during service worker registration:', e);
            });
        }
    }
}

new MixitApp().bootstrap();
$(() => {
  $(document).foundation();
});