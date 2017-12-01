function initFavoriteButton() {
  const favoriteButton = document.getElementById('favorite');
  favoriteButton.onclick = favoriteToggle;
}

function favoriteToggle() {
  const talkField = <HTMLInputElement> document.getElementById('talkId');
  const email = <HTMLInputElement> document.getElementById('email');
  const favoriteButton = document.getElementById('favorite');

  fetch(`/api/favorites/${email.value}/talks/${talkField.value}/toggle`, {method: 'post'})
    .then(response => response.json())
    .then(json => {
      const img = json.selected ? 'mxt-favorite.svg' : 'mxt-favorite-non.svg';
      favoriteButton.innerHTML = `<img src="/images/svg/favorites/${img}" class="mxt-icon--cat__talks"/>`;
    });
}

window.addEventListener("load", initFavoriteButton);
