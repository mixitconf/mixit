package mixit.controller

import mixit.model.*
import mixit.repository.EventRepository
import mixit.repository.SessionRepository
import mixit.support.LazyRouterFunction
import mixit.support.MarkdownConverter
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType.APPLICATION_JSON_UTF8
import org.springframework.stereotype.Controller
import org.springframework.web.reactive.function.fromPublisher
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse.ok
import java.time.LocalDateTime


@Controller
class SessionController(val repository: SessionRepository, val eventRepository: EventRepository,
                        val markdownConverter: MarkdownConverter, @Value("\${baseUri}") val baseUri: String) : LazyRouterFunction() {

    // TODO Remove this@ArticleController when KT-15667 will be fixed
    override val routes: Routes.() -> Unit = {
        accept(TEXT_HTML).route {
            GET("/sessions/", this@SessionController::findAllView)
            GET("/{year}/sessions/", this@SessionController::findByEventView)
            GET("/session/{id}",  this@SessionController::findOneView)
            (GET("/session/{id}/") or GET("/session/{id}/{sluggifiedTitle}/")) { status(PERMANENT_REDIRECT).location(create("$baseUri${it.path().removeSuffix("/")}")).build() }
        }
        accept(APPLICATION_JSON).route {
            GET("/api/session/{login}", this@SessionController::findOne)
            GET("/api/{year}/session/", this@SessionController::findByEventId)
        }
    }

    fun findAllView(req: ServerRequest) = repository.findAll()
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
            val video: String?,
            val room: Room?,
            val start: LocalDateTime?,
            val end: LocalDateTime?
    ) {

        constructor(session: Session, markdownConverter: MarkdownConverter) : this(session.id, session.format, session.event,
                session.title, markdownConverter.toHTML(session.summary), session.speakers, session.language, session.addedAt,
                markdownConverter.toHTML(session.description), session.video, session.room, session.start, session.end)

    }
}
