package mixit.web.handler

import mixit.MixitProperties
import mixit.repository.WorkedAdventureRepository
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyExtractors.toFormData
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import reactor.core.publisher.Mono


@Component
class WorkAdventureHandler(private val workedAdventureRepository: WorkedAdventureRepository,
                           private val mixitProperties: MixitProperties) {

    private fun displayWorkedAdventureLoginView(error: String? = null, ticket: String? = null) = ok().render(
        "work-adventure-login", mapOf(
            Pair("title", "work-adventure.title"),
            Pair("error", error),
            Pair("ticket", ticket)
        )
    )

    private fun displayWorkedAdventurePage(token: String) = ok().render(
        "work-adventure-world", mapOf(
            Pair("title", "work-adventure.title"),
            Pair("token", token)
        )
    )

    fun loginView(req: ServerRequest): Mono<ServerResponse> = displayWorkedAdventureLoginView()

    fun connect(req: ServerRequest): Mono<ServerResponse> = req
        .body(toFormData())
        .flatMap {
            val ticketNumber = it.toSingleValueMap()["ticket"]
                ?: return@flatMap displayWorkedAdventureLoginView("work-adventure.error.ticket-required")

            workedAdventureRepository
                .findOne(ticketNumber)
                .flatMap { workAdventure ->
                    req.session().flatMap { session ->
                        session.attributes["work-adventure-token"] = workAdventure.token
                        return@flatMap openVimeoView(req)
                    }

                }
                .switchIfEmpty(displayWorkedAdventureLoginView("work-adventure.error.ticket-invalid", ticketNumber))
        }

    fun logout(req: ServerRequest): Mono<ServerResponse> = req.session().flatMap { session ->
        session.attributes.apply {
            remove("work-adventure-token")
        }
        return@flatMap displayWorkedAdventureLoginView()
    }

    fun openWorkAdventureView(req: ServerRequest): Mono<ServerResponse> =
        req.session().flatMap { session ->
            val token =
                session.getAttribute<String>("work-adventure-token") ?: return@flatMap displayWorkedAdventureLoginView()
            return@flatMap displayWorkedAdventurePage(token)
        }

    fun openVimeoView(req: ServerRequest): Mono<ServerResponse> =
        req.session().flatMap { session ->
            val token =
                session.getAttribute<String>("work-adventure-token") ?: return@flatMap displayWorkedAdventureLoginView()
            return@flatMap  ok().render(
                "vimeo-2021", mapOf(
                    Pair("title", "work-adventure.title"),
                    Pair("vimeoFluxUri", mixitProperties.vimeoFluxUri),
                    Pair("vimeoTchatUri", mixitProperties.vimeoTchatUri)
                )
            )
        }
}

