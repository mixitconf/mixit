package mixit.favorite.handler

import kotlinx.coroutines.reactor.awaitSingle
import mixit.favorite.model.Favorite
import mixit.favorite.repository.FavoriteRepository
import mixit.security.model.Cryptographer
import mixit.talk.handler.TalkHandler
import mixit.talk.model.TalkService
import org.springframework.stereotype.Controller
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.json

@Controller
class JsonFavoriteHandler(
    private val favoriteRepository: FavoriteRepository,
    private val cryptographer: Cryptographer,
    private val talkHandler: TalkHandler,
    private val talkService: TalkService,
) {

    suspend fun findAll(req: ServerRequest): ServerResponse =
        ok().json().bodyValueAndAwait(favoriteRepository.findAll())

    suspend fun toggleFavorite(req: ServerRequest): ServerResponse {
        val email = req.pathVariable("email")
        val talkId = req.pathVariable("id")
        val favorite = favoriteRepository.findByEmailAndTalk(email, talkId)
        // if favorite is not here we create it
        val response = if (favorite == null) {
            favoriteRepository
                .save(
                    Favorite(
                        cryptographer.encrypt(email)!!,
                        talkId
                    )
                )
                .map { FavoriteDto(it.talkId, true) }
                .awaitSingle()
        } else {
            favoriteRepository.delete(email, favorite.talkId)
                .map { FavoriteDto(talkId, false) }
                .awaitSingle()
        }
        return talkHandler.findOneView(talkService.findOneOrNull(talkId)!!, req, 2024)
    }

    suspend fun getFavorite(req: ServerRequest) =
        ok().json().bodyValueAndAwait(
            favoriteRepository.findByEmailAndTalk(req.pathVariable("email"), req.pathVariable("id"))
                ?.let { FavoriteDto(it.talkId, true) }
                ?: FavoriteDto(req.pathVariable("id"), false)
        )

    suspend fun getFavorites(req: ServerRequest) =
        ok().json().bodyValueAndAwait(favoriteRepository.findByEmail(req.pathVariable("email")))
}
