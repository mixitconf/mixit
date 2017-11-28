function loadFavorite() {
  const talkField = <HTMLInputElement> document.getElementById('talkId');
  const emailField = <HTMLInputElement> document.getElementById('email');
  if (talkField && emailField) {
    const favoriteButton = document.getElementById('favorite');

    fetch(`/api/favorites/${emailField.value}/talks/${talkField.value}`, {method: 'get'})
      .then(response => response.json())
      .then(json => {
        const img = json.selected ? 'mxt-favorite.svg' : 'mxt-favorite-non.svg';
        favoriteButton.innerHTML = `<img src="/images/svg/favorites/${img}" class="mxt-icon--cat__talks"/>`;
        favoriteButton.onclick = favoriteToggle;
        favoriteButton.style.visibility = 'visible';
      });
  }
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

window.addEventListener("load", loadFavorite);
