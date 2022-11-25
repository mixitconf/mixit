package mixit.favorite.handler

import mixit.favorite.model.Favorite
import mixit.favorite.repository.FavoriteRepository
import mixit.security.model.Cryptographer
import mixit.util.json
import org.springframework.stereotype.Controller
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.body
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import reactor.core.publisher.Mono

@Controller
class JsonFavoriteHandler(private val favoriteRepository: FavoriteRepository, private val cryptographer: Cryptographer) {

    suspend fun findAll(req: ServerRequest) =
        ok().json().bodyValueAndAwait(favoriteRepository.findAll())

    fun toggleFavorite(req: ServerRequest) =
        ok().json().body(
            favoriteRepository.findByEmailAndTalk(req.pathVariable("email"), req.pathVariable("id"))
                // if favorite is found we delete it
                .flatMap {
                    favoriteRepository.delete(req.pathVariable("email"), it.talkId)
                        .map { FavoriteDto(req.pathVariable("id"), false) }
                }
                // otherwise we create it
                .switchIfEmpty(
                    Mono.defer {
                        favoriteRepository.save(
                            Favorite(
                                cryptographer.encrypt(
                                    req.pathVariable("email")
                                )!!,
                                req.pathVariable("id")
                            )
                        ).map { FavoriteDto(it.talkId, true) }
                    }
                )
        )

    suspend fun getFavorite(req: ServerRequest) =
        ok().json().bodyValueAndAwait(
            favoriteRepository.coFindByEmailAndTalk(req.pathVariable("email"), req.pathVariable("id"))
                ?.let { FavoriteDto(it.talkId, true) }
                ?: FavoriteDto(req.pathVariable("id"), false)
        )

    suspend fun getFavorites(req: ServerRequest) =
        ok().json().bodyValueAndAwait(favoriteRepository.coFindByEmail(req.pathVariable("email")))
}
