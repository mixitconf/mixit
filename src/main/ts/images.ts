/**
 * This script has to be loaded asynchronously to load images defined like this
 * <img data-src="myimage.png" class="mxt-img--lazyload">
 */
class ImagesCtrl {
    constructor() {
        setTimeout(
            () => {
                const images = document.getElementsByClassName('mxt-img--lazyload');
                Array
                    .from(images)
                    .forEach((image) => (image as HTMLImageElement).src = image.getAttribute('data-src'))
            },
            100);
    }
}

window.addEventListener("load", () => new ImagesCtrl());
