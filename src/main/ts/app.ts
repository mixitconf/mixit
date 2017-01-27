import EventLoader from './event/event'
import $ from "jquery";

let eventLoader = new EventLoader();

class MixitApp{
    bootstrap(){
        eventLoader.loadEvent('mixit16')
            .then(response => response.json())
            .then(event => console.log(event))
        $(document).foundation();
    }
}

new MixitApp().bootstrap();
