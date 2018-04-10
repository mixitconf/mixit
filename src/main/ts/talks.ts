class TalksCtrl{

    favoriteToggle(event){
        // Depending on the browser the target is not the same. In Firefox this is the button and in Chrome the img
        const elt = event.target;
        const targetIsButton = event.target.outerHTML.indexOf('button') >= 0;

        const email = <HTMLInputElement> document.getElementById('email');
        const id = elt.id.substr(9,elt.id.length);

        fetch(`/api/favorites/${email.value}/talks/${id}/toggle`, {method: 'post'})
            .then(response => response.json())
            .then((json: any) => {
                const imgPath = json.selected ? 'mxt-favorite.svg' : 'mxt-favorite-non.svg';
                if(targetIsButton){
                    elt.innerHTML = `<img src="/images/svg/favorites/${imgPath}" class="mxt-icon--cat__talks" id="favorite-{{id}}"/>`;
                }
                else {
                    elt.src = `/images/svg/favorites/${imgPath}`;
                }
            });

        event.stopPropagation();
    }
}

window.addEventListener("load", () => window['ctrl'] = new TalksCtrl());