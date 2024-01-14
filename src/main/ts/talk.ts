class TalkCtrl {
    constructor() {
        const favoriteButton = document.getElementById('favorite');
        if (favoriteButton) {
            favoriteButton.onclick = this.favoriteToggle;
        }
        const nonfavoriteButton = document.getElementById('nonfavorite');
        if (nonfavoriteButton) {
            nonfavoriteButton.onclick = this.favoriteToggle;
        }
    }

    private favoriteToggle(event:Event) {
        const talkField = <HTMLInputElement> document.getElementById('talkId');
        const email = <HTMLInputElement> document.getElementById('email');

        fetch(`/api/favorites/${email.value}/talks/${talkField.value}/toggle`, {method: 'post'})
            .then(response => response.json())
            .then((json: any) => {
                if (json.selected) {
                    document.getElementById('favorite').style.display = 'flex';
                    document.getElementById('nonfavorite').style.display = 'none';
                } else {
                    document.getElementById('favorite').style.display = 'none';
                    document.getElementById('nonfavorite').style.display = 'flex';
                }
            });
        event.stopPropagation();
    }


}

window.addEventListener("load", () => new TalkCtrl());

