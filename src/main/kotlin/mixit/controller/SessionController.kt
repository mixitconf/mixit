package mixit.controller

import mixit.model.*
import mixit.repository.EventRepository
import mixit.repository.SessionRepository
import mixit.support.RouterFunctionProvider
import mixit.support.MarkdownConverter
import mixit.support.json
import mixit.support.redirectPermanently
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType.*
import org.springframework.stereotype.Controller
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.ServerResponse.ok

import java.time.LocalDateTime


@Controller
class SessionController(val repository: SessionRepository, val eventRepository: EventRepository,
                        val markdownConverter: MarkdownConverter, @Value("\${baseUri}") val baseUri: String) : RouterFunctionProvider() {

    // TODO Remove this@SessionController when KT-15667 will be fixed
    override val routes: Routes = {
        accept(TEXT_HTML).route {
            GET("/2017/") { ok().render("sessions-2017") }
            GET("/2016/") { findByEventView(2016, it) }
            GET("/2015/") { findByEventView(2015, it) }
            GET("/2014/") { findByEventView(2014, it) }
            GET("/2013/") { findByEventView(2013, it) }
            GET("/2012/") { findByEventView(2012, it) }
            GET("/talk/{slug}", this@SessionController::findOneView)
            (GET("/session/{id}/") or GET("/session/{id}") or GET("/session/{id}/{sluggifiedTitle}/") or GET("/session/{id}/{sluggifiedTitle}")) { redirectOneView(it) }
        }
        (accept(APPLICATION_JSON) and "/api").route {
            GET("/talk/{login}", this@SessionController::findOne)
            GET("/{year}/talk/", this@SessionController::findByEventId)
        }
    }

    fun findByEventView(year: Int, req: ServerRequest) = repository.findByEvent(eventRepository.yearToId(year.toString()))
            .collectList()
            .then { sessions -> ok().render("sessions",  mapOf(Pair("sessions", sessions.map { SessionDto(it, markdownConverter) }), Pair("year", year))) }

    fun findOneView(req: ServerRequest) = repository.findBySlug(req.pathVariable("slug"))
            .then { session -> ok().render("session", mapOf(Pair("session", SessionDto(session, markdownConverter)))) }

    fun redirectOneView(req: ServerRequest) = repository.findOne(req.pathVariable("id")).then { s ->
        redirectPermanently("$baseUri/talk/${s.slug}")
    }

    fun findOne(req: ServerRequest) = ok().json().body(
            repository.findOne(req.pathVariable("login")))

    fun findByEventId(req: ServerRequest) = ok().json().body(
            repository.findByEvent(eventRepository.yearToId(req.pathVariable("year"))))


    class SessionDto(
            val id: String?,
            val slug: String,
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

        constructor(session: Session, markdownConverter: MarkdownConverter) : this(session.id, session.slug, session.format, session.event,
                session.title, markdownConverter.toHTML(session.summary), session.speakers, session.language, session.addedAt,
                markdownConverter.toHTML(session.description), session.video, session.room, session.start, session.end)

    }
}
