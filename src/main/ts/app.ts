import * as $ from 'jquery';
window['jQuery'] = $;
import 'foundation-sites';

class MixitApp{
    bootstrap(){
        this._initServiceWorker();
    }

    _initServiceWorker() {
        if ('serviceWorker' in navigator && window.location.protocol === 'https:') {
            navigator.serviceWorker.register('/service-worker.js').then(registration => {
                if (typeof registration.update === 'function') {
                    registration.update();
                }
                registration.onupdatefound = () => {
                    var installingWorker = registration.installing;

                    installingWorker.onstatechange = function() {
                        switch (installingWorker.state) {
                            case 'installed':
                                if (navigator.serviceWorker.controller) {
                                    console.log('New or updated content is available.');
                                }
                                else {
                                    console.log('Content is now available offline!');
                                }
                                break;

                            case 'redundant':
                                console.error('The installing service worker became redundant.');
                                break;

                            default:
                                // Ignore
                        }
                    };
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