package mixit.controller

import mixit.model.*
import mixit.repository.SessionRepository
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.springframework.http.MediaType.*
import org.springframework.stereotype.Controller

import org.springframework.web.reactive.function.fromPublisher
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.RequestPredicates.*
import org.springframework.web.reactive.function.server.ServerResponse.ok
import java.time.LocalDateTime


@Controller
class SessionController(val repository: SessionRepository) : RouterFunction<ServerResponse> {

    override fun route(req: ServerRequest) = route(req) {
        accept(TEXT_HTML).apply {
            GET("/session/") { findAllView() }
            GET("/session/{id}") { findOneView(req) }
        }
        accept(APPLICATION_JSON).apply {
            GET("/api/session/{login}") { findOne(req) }
            GET("/api/{event}/session/") { findByEventId(req) }
        }
    }

    fun findAllView() = repository.findAll()
            .collectList()
            .then { session -> ok().render("sessions",  mapOf(Pair("sessions", session))) }

    fun findOneView(req: ServerRequest) = repository.findOne(req.pathVariable("id"))
            .then { session -> ok().render("session", mapOf(Pair("session", SessionDto(session)))) }


    fun findOne(req: ServerRequest) = ok().contentType(APPLICATION_JSON_UTF8).body(
            fromPublisher(repository.findOne(req.pathVariable("login"))))

    fun findByEventId(req: ServerRequest) = ok().contentType(APPLICATION_JSON_UTF8).body(
            fromPublisher(repository.findByEvent(req.pathVariable("event"))))


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

        constructor(session: Session) : this(session.id, session.format, session.event,
                session.title, session.summary, session.speakers, session.language, session.addedAt,
                session.description, session.room, session.start, session.end)

        fun toHtml(markdown: String): String {
            val parser = Parser.builder().build()
            val document = parser.parse(markdown)
            val renderer = HtmlRenderer.builder().build()
            return renderer.render(document)
        }

        val htmlSummary: String
            get() = toHtml(summary)

        val htmlDescription: String
            get() = toHtml(description ?: "")
    }
}
