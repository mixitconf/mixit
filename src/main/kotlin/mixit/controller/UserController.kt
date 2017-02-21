package mixit.controller

import mixit.model.*
import mixit.repository.EventRepository
import mixit.repository.UserRepository
import mixit.support.RouterFunctionProvider
import mixit.support.MarkdownConverter
import mixit.support.language
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus.*
import org.springframework.http.MediaType.*
import org.springframework.stereotype.Controller
import org.springframework.web.reactive.function.BodyInserters.fromObject
import org.springframework.web.reactive.function.fromPublisher
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.RequestPredicates.accept
import org.springframework.web.reactive.function.server.RequestPredicates.*
import org.springframework.web.reactive.function.server.ServerResponse.*
import java.net.URI.*
import java.net.URLDecoder
import java.time.LocalDate
import java.util.*


@Controller
class UserController(val repository: UserRepository, val eventRepository: EventRepository, val markdownConverter: MarkdownConverter,
                     @Value("\${baseUri}") val baseUri: String) : RouterFunctionProvider() {

    // TODO Remove this@UserController when KT-15667 will be fixed
    override val routes: Routes.() -> Unit = {
        accept(TEXT_HTML).route {
            (GET("/user/{login}") or GET("/speaker/{login}") or GET("/sponsor/{login}")) { findOneView(it) }
            (GET("/member/{login}") or GET("/profile/{login}") or GET("/member/sponsor/{login}") or GET("/member/member/{login}")) { status(PERMANENT_REDIRECT).location(create("$baseUri/user/${it.pathVariable("login")}")).build() }
            GET("/about/", this@UserController::findAboutView)
            GET("/about") { status(PERMANENT_REDIRECT).location(create("$baseUri/about/")).build() }
            GET("/sponsors/", this@UserController::findSponsorsView)
        }
        accept(APPLICATION_JSON).route {
            GET("/api/user/", this@UserController::findAll)
            POST("/api/user/", this@UserController::create)
            GET("/api/user/{login}", this@UserController::findOne)
            GET("/api/staff/", this@UserController::findStaff)
            GET("/api/staff/{login}", this@UserController::findOneStaff)
            GET("/api/speaker/", this@UserController::findSpeakers)
            GET("/api/speaker/{login}", this@UserController::findOneSpeaker)
            GET("/api/sponsor/", this@UserController::findSponsors)
            GET("/api/sponsor/{login}", this@UserController::findOneSponsor)
            GET("/api/{event}/speaker/", this@UserController::findSpeakersByEvent)
        }
    }

    fun findOneView(req: ServerRequest) = try {
        val idLegacy = req.pathVariable("login").toLong()
        repository.findByLegacyId(idLegacy)
                .then { u -> ok().render("user", mapOf(Pair("user", toUserDto(u, req.language())))) }
    } catch (e:NumberFormatException) {
        repository.findOne(URLDecoder.decode(req.pathVariable("login"), "UTF-8"))
                .then { u -> ok().render("user", mapOf(Pair("user", toUserDto(u, req.language())))) }
    }

    fun findOne(req: ServerRequest) = ok().contentType(APPLICATION_JSON_UTF8).body(
                fromPublisher(repository.findOne(req.pathVariable("login"))))

    fun findAll(req: ServerRequest) = ok().contentType(APPLICATION_JSON_UTF8).body(
            fromPublisher(repository.findAll()))

    fun findStaff(req: ServerRequest) = ok().contentType(APPLICATION_JSON_UTF8).body(
            fromPublisher(repository.findByRole(Role.STAFF)))

    fun findOneStaff(req: ServerRequest) = ok().contentType(APPLICATION_JSON_UTF8).body(
                fromPublisher(repository.findOneByRole(req.pathVariable("login"), Role.STAFF)))

    fun findSpeakers(req: ServerRequest) = ok().contentType(APPLICATION_JSON_UTF8).body(
            fromPublisher(repository.findByRole(Role.SPEAKER)))

    fun findSpeakersByEvent(req: ServerRequest) = ok().contentType(APPLICATION_JSON_UTF8).body(
            fromPublisher(repository.findByRoleAndEvent(Role.SPEAKER, req.pathVariable("event"))))

    fun findOneSpeaker(req: ServerRequest) = ok().contentType(APPLICATION_JSON_UTF8).body(
                fromPublisher(repository.findOneByRole(req.pathVariable("login"), Role.SPEAKER)))

    fun findSponsors(req: ServerRequest) = ok().contentType(APPLICATION_JSON_UTF8).body(
            fromPublisher(repository.findByRole(Role.SPONSOR)))

    fun findOneSponsor(req: ServerRequest) = ok().contentType(APPLICATION_JSON_UTF8).body(
                fromPublisher(repository.findOneByRole(req.pathVariable("login"), Role.SPONSOR)))

    fun create(req: ServerRequest) = repository.save(req.bodyToMono<User>())
            .then { u -> created(create("/api/user/${u.login}"))
                .contentType(APPLICATION_JSON_UTF8)
                .body(fromObject(u))
            }

    fun findAboutView(req: ServerRequest) = repository.findByRole(Role.STAFF)
            .collectList()
            .then { u ->
                val users = u.map { toUserDto(it, req.language()) }
                Collections.shuffle(users)
                ok().render("about",  mapOf(Pair("staff", users)))
            }

    fun findSponsorsView(req: ServerRequest) = eventRepository.findOne("mixit17").then { events ->
        val sponsors = events.sponsors.map { toEventSponsoringDto(it, req.language()) }.groupBy { it.level }

        ok().render("sponsors", mapOf(
            Pair("sponsors-gold", sponsors[SponsorshipLevel.GOLD]),
            Pair("sponsors-silver", sponsors[SponsorshipLevel.SILVER]),
            Pair("sponsors-hosting", sponsors[SponsorshipLevel.HOSTING]),
            Pair("sponsors-lanyard", sponsors[SponsorshipLevel.LANYARD]),
            Pair("sponsors-party", sponsors[SponsorshipLevel.PARTY])
        ))
    }

    private fun toEventSponsoringDto(eventSponsoring: EventSponsoring, language: Language) = EventSponsoringDto(
           eventSponsoring.level,
           toUserDto(eventSponsoring.sponsor, language),
           eventSponsoring.subscriptionDate
    )

    private fun toUserDto(user: User, language: Language) = UserDto(
            user.login,
            user.firstname,
            user.lastname,
            user.email,
            user.company,
            markdownConverter.toHTML(user.description[language] ?: ""),
            user.logoUrl,
            user.events,
            user.role,
            user.links)

    class EventSponsoringDto(
        val level: SponsorshipLevel,
        val sponsor: UserDto,
        val subscriptionDate: LocalDate = LocalDate.now()
)

    class UserDto(
        val login: String,
        val firstname: String,
        val lastname: String,
        var email: String,
        var company: String? = null,
        var description: String,
        var logoUrl: String? = null,
        val events: List<String>,
        val role: Role,
        var links: List<Link>)
}