class MixitApp {
    bootstrap() {
        // this._initServiceWorker();
    }
}
new MixitApp().bootstrap();
/**
 * This script has to be loaded asynchronously to load images defined like this
 * <img data-src="myimage.png" class="mxt-img--lazyload">
 */
class ImagesCtrl {
    constructor() {
        const images = document.getElementsByClassName('mxt-img--lazyload');
        Array.from(images).forEach((image) => image.src = image.getAttribute('data-src'));
    }
}
window.addEventListener("load", () => new ImagesCtrl());
class TalkCtrl {
    constructor() {
        const favoriteButton = document.getElementById('favorite');
        if (favoriteButton) {
            favoriteButton.onclick = this.favoriteToggle;
        }
    }
    favoriteToggle(event) {
        // Depending on the browser the target is not the same. In Firefox this is the button and in Chrome the img
        const elt = event.target;
        const targetIsButton = elt.outerHTML.indexOf('button') >= 0;
        const talkField = document.getElementById('talkId');
        const email = document.getElementById('email');
        fetch(`/api/favorites/${email.value}/talks/${talkField.value}/toggle`, { method: 'post' })
            .then(response => response.json())
            .then((json) => {
            const imgPath = json.selected ? 'mxt-favorite.svg' : 'mxt-favorite-non.svg';
            if (targetIsButton) {
                elt.innerHTML = `<img src="/images/svg/favorites/${imgPath}" class="mxt-icon--cat__talks" id="favorite-{{id}}"/>`;
            }
            else {
                elt.src = `/images/svg/favorites/${imgPath}`;
            }
        });
        event.stopPropagation();
    }
}
window.addEventListener("load", () => new TalkCtrl());
class TalksCtrl {
    constructor() {
        const buttons = [].slice.call(document.getElementsByClassName('mxt-img--favorite'));
        for (const element of buttons) {
            element.onclick = this.favoriteToggle;
        }
    }
    favoriteToggle(event) {
        // Depending on the browser the target is not the same. In Firefox this is the button and in Chrome the img
        const elt = event.target;
        const targetIsButton = elt.outerHTML.indexOf('button') >= 0;
        const email = document.getElementById('email');
        const id = elt.id.substr(9, elt.id.length);
        fetch(`/api/favorites/${email.value}/talks/${id}/toggle`, { method: 'post' })
            .then(response => response.json())
            .then((json) => {
            const imgPath = json.selected ? 'mxt-favorite.svg' : 'mxt-favorite-non.svg';
            if (targetIsButton) {
                elt.innerHTML = `<img src="/images/svg/favorites/${imgPath}" class="mxt-icon--cat__talks" id="favorite-{{id}}"/>`;
            }
            else {
                elt.src = `/images/svg/favorites/${imgPath}`;
            }
        });
        event.stopPropagation();
    }
}
window.addEventListener("load", () => new TalksCtrl());
