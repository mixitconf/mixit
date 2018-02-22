class TalksCtrl{

  constructor() {
    // const favoriteButtons = document.getElementsByClassName('mxt-img--favorite');
    // Array.from(favoriteButtons).forEach((favoriteButton: HTMLElement) => favoriteButton.onclick= this.favoriteToggle);
  }

  favoriteToggle(event) {
    const img = event.srcElement;
    const email = <HTMLInputElement> document.getElementById('email');

    fetch(`/api/favorites/${email.value}/talks/${img.id.substr(9,img.id.length)}/toggle`, {method: 'post'})
      .then(response => response.json())
      .then((json: any) => {
        const imgPath = json.selected ? 'mxt-favorite.svg' : 'mxt-favorite-non.svg';
        img.src = `/images/svg/favorites/${imgPath}`;
      });
  }

}

window.addEventListener("load", () => window['ctrl'] = new TalksCtrl());