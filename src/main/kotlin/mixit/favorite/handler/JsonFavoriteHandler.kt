package mixit.favorite.handler

import kotlinx.coroutines.reactor.awaitSingle
import mixit.favorite.model.Favorite
import mixit.favorite.repository.FavoriteRepository
import mixit.security.model.Cryptographer
import mixit.util.json
import org.springframework.stereotype.Controller
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.bodyValueAndAwait

@Controller
class JsonFavoriteHandler(
    private val favoriteRepository: FavoriteRepository,
    private val cryptographer: Cryptographer
) {

    suspend fun findAll(req: ServerRequest): ServerResponse =
        ok().json().bodyValueAndAwait(favoriteRepository.findAll())

    suspend fun toggleFavorite(req: ServerRequest): ServerResponse {
        val favorite = favoriteRepository.findByEmailAndTalk(req.pathVariable("email"), req.pathVariable("id"))
        // if favorite is not here we create it
        val response = if (favorite == null) {
            favoriteRepository
                .save(
                    Favorite(
                        cryptographer.encrypt(req.pathVariable("email"))!!,
                        req.pathVariable("id")
                    )
                )
                .map { FavoriteDto(it.talkId, true) }
                .awaitSingle()
        } else {
            favoriteRepository.delete(req.pathVariable("email"), favorite.talkId)
                .map { FavoriteDto(req.pathVariable("id"), false) }
                .awaitSingle()
        }
        return ok().json().bodyValueAndAwait(response)
    }

    suspend fun getFavorite(req: ServerRequest) =
        ok().json().bodyValueAndAwait(
            favoriteRepository.findByEmailAndTalk(req.pathVariable("email"), req.pathVariable("id"))
                ?.let { FavoriteDto(it.talkId, true) }
                ?: FavoriteDto(req.pathVariable("id"), false)
        )

    suspend fun getFavorites(req: ServerRequest) =
        ok().json().bodyValueAndAwait(favoriteRepository.coFindByEmail(req.pathVariable("email")))
}
