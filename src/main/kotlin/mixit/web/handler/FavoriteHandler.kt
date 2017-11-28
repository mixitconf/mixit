package mixit.web.handler

import mixit.model.Favorite
import mixit.repository.FavoriteRepository
import mixit.util.Cryptographer
import mixit.util.json
import org.springframework.stereotype.Controller
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.body
import reactor.core.publisher.toMono

@Controller
class FavoriteHandler(private val favoriteRepository: FavoriteRepository, private val cryptographer: Cryptographer) {

    fun toggleFavorite(req: ServerRequest) = ok().json().body(
            favoriteRepository.findByTalkAndEmail(req.pathVariable("email"), req.pathVariable("id"))
                    // if favorite is found we delete it
                    .flatMap { favoriteRepository.delete(req.pathVariable("email"), it.talkId).map { FavoriteDto(req.pathVariable("id"), false) } }
                    // otherwise we create it
                    .switchIfEmpty(favoriteRepository.save(
                            Favorite(cryptographer.encrypt(
                                    req.pathVariable("email"))!!,
                                    req.pathVariable("id"))).map { FavoriteDto(it.talkId, true) })
    )


    fun getFavorite(req: ServerRequest) = ok().json().body(
            favoriteRepository.findByTalkAndEmail(req.pathVariable("email"), req.pathVariable("id"))
                    .flatMap { FavoriteDto(it.talkId, true).toMono() }
                    .switchIfEmpty(FavoriteDto(req.pathVariable("id"), false).toMono())
    )

    fun getFavorites(req: ServerRequest) = ok().json().body(favoriteRepository.findByEmail(req.pathVariable("email")))
}

class FavoriteDto(
        val talkId: String,
        val selected: Boolean
)
