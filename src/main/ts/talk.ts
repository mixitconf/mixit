class TalkCtrl {
    constructor() {
        const favoriteButton = document.getElementById('favorite');
        favoriteButton.onclick = this.favoriteToggle;
    }

    favoriteToggle(event) {
        // Depending on the browser the target is not the same. In Firefox this is the button and in Chrome the img
        const elt = event.target;
        const targetIsButton = event.target.outerHTML.indexOf('button') >= 0;

        const talkField = <HTMLInputElement> document.getElementById('talkId');
        const email = <HTMLInputElement> document.getElementById('email');

        fetch(`/api/favorites/${email.value}/talks/${talkField.value}/toggle`, {method: 'post'})
            .then(response => response.json())
            .then((json: any) => {
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

