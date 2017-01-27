import EventLoader from './event/event'
import * as $ from 'jquery';
window['jQuery'] = $;
import 'foundation-sites';

let eventLoader = new EventLoader();

class MixitApp{
    bootstrap(){
        eventLoader.loadEvent('mixit16')
            .then(response => response.json())
            .then(event => console.log(event));
    }
}

new MixitApp().bootstrap();
$(() => {
  $(document).foundation();
});