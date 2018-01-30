class TalkCtrl{
  constructor() {
    const favoriteButton = document.getElementById('favorite');
    favoriteButton.onclick = this.favoriteToggle;
  }

  favoriteToggle(event) {
    const talkField = <HTMLInputElement> document.getElementById('talkId');
    const email = <HTMLInputElement> document.getElementById('email');
    const img = event.srcElement;
    event.stopPropagation();

    fetch(`/api/favorites/${email.value}/talks/${talkField.value}/toggle`, {method: 'post'})
      .then(response => response.json())
      .then((json:any) => {
        const imgPath = json.selected ? 'mxt-favorite.svg' : 'mxt-favorite-non.svg';
        img.src = `/images/svg/favorites/${imgPath}`;
      });
  }
}

window.addEventListener("load", () => new TalkCtrl());

