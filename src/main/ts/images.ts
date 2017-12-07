/**
 * This script has to be loaded asynchronously to load images defined like this
 * <img data-src="myimage.png" class="mxt-img--lazyload">
 */
class ImagesCtrl{
  constructor() {
    const images = document.getElementsByClassName('mxt-img--lazyload');
    Array.from(images).forEach((image: HTMLImageElement) => image.src= image.getAttribute('data-src'));
  }
}

window.addEventListener("load", () => new ImagesCtrl());