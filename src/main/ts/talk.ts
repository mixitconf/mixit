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
        const feedbackBtn = document.getElementsByClassName('mxt-feedback-btn');
        if (feedbackBtn) {
            Array.from(feedbackBtn)
                .forEach(elt => (elt as HTMLElement).onclick = this.voteForTalk);

        }
        const newComment = document.getElementById('mxt-feedback-comment--new')
        if (newComment) {
            newComment.onclick = this.fillComment;
        }
        const cancelComment = document.getElementById('mxt-feedback-comment--cancel')
        if (cancelComment) {
            cancelComment.onclick = this.cancelComment;
        }
    }

    private fillComment() {
        document.getElementById('mxt-feedback-comment--new').style.display = 'none';
        document.getElementById('mxt-feedback-comment--container').style.display = 'flex';
    }

    private cancelComment() {
        document.getElementById('mxt-feedback-comment--new').style.display = 'flex';
        document.getElementById('mxt-feedback-comment--container').style.display = 'none';
    }

    private favoriteToggle(event: Event) {
        const talkField = <HTMLInputElement>document.getElementById('talkId');
        const email = <HTMLInputElement>document.getElementById('email');

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


    private voteForTalk(event: Event) {
        const elt = event.target as HTMLElement;
        const feedback = elt.dataset['feedback'];
        const talkId = elt.dataset['talk'];
        const email = elt.dataset['email'];

        fetch(`/api/feedback/${email}/talks/${talkId}/vote/${feedback}`, {method: 'post'})
            .then(response => response.json())
            .then((json: any) => {
                const vote = document.getElementById(`mxt-feedback-${feedback}--vote`);
                vote.innerHTML = json.count;
                const container = document.getElementById(`mxt-feedback-${feedback}--container`);
                if (json.selectedByCurrentUser && !container.classList.contains('mxt-feedback-selected')) {
                    container.classList.add('mxt-feedback-selected');
                }
                if (!json.selectedByCurrentUser && container.classList.contains('mxt-feedback-selected')) {
                    container.classList.remove('mxt-feedback-selected');
                }
            });

        event.stopPropagation();
    }
}

window.addEventListener("load", () => new TalkCtrl());

