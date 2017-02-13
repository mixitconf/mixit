package mixit.controller

import mixit.model.*
import mixit.repository.EventRepository
import mixit.repository.SessionRepository
import mixit.support.MarkdownConverter
import org.springframework.http.MediaType.*
import org.springframework.stereotype.Controller
import org.springframework.web.reactive.function.fromPublisher
import org.springframework.web.reactive.function.server.RequestPredicates.accept
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.route
import java.time.LocalDateTime


@Controller
class SessionController(val repository: SessionRepository, val eventRepository: EventRepository, val markdownConverter: MarkdownConverter) : RouterFunction<ServerResponse> {

    override fun route(req: ServerRequest) = route(req) {
        accept(TEXT_HTML).apply {
            GET("/sessions/") { findAllView() }
            GET("/{year}/sessions/") { findByEventView(req) }
            GET("/session/{id}") { findOneView(req) }
        }
        accept(APPLICATION_JSON).apply {
            GET("/api/session/{login}") { findOne(req) }
            GET("/api/{year}/session/") { findByEventId(req) }
        }
    }

    fun findAllView() = repository.findAll()
            .collectList()
            .then { session -> ok().render("sessions",  mapOf(Pair("sessions", session))) }

    fun findByEventView(req: ServerRequest) = repository.findByEvent(eventRepository.yearToId(req.pathVariable("year")))
            .collectList()
            .then { session -> ok().render("sessions",  mapOf(Pair("sessions", session))) }

    fun findOneView(req: ServerRequest) = repository.findOne(req.pathVariable("id"))
            .then { session -> ok().render("session", mapOf(Pair("session", SessionDto(session, markdownConverter)))) }


    fun findOne(req: ServerRequest) = ok().contentType(APPLICATION_JSON_UTF8).body(
            fromPublisher(repository.findOne(req.pathVariable("login"))))

    fun findByEventId(req: ServerRequest) = ok().contentType(APPLICATION_JSON_UTF8).body(
            fromPublisher(repository.findByEvent(eventRepository.yearToId(req.pathVariable("year")))))


    class SessionDto(
            val id: String?,
            val format: SessionFormat,
            val event: String,
            val title: String,
            val summary: String,
            val speakers: List<User>,
            val language: Language,
            val addedAt: LocalDateTime,
            val description: String?,
            val room: Room?,
            val start: LocalDateTime?,
            val end: LocalDateTime?
    ) {

        constructor(session: Session, markdownConverter: MarkdownConverter) : this(session.id, session.format, session.event,
                session.title, markdownConverter.toHTML(session.summary), session.speakers, session.language, session.addedAt,
                markdownConverter.toHTML(session.description), session.room, session.start, session.end)

    }
}
