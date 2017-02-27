package mixit.controller

import mixit.model.*
import mixit.repository.EventRepository
import mixit.repository.UserRepository
import mixit.support.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus.*
import org.springframework.http.MediaType.*
import org.springframework.stereotype.Controller
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.ServerResponse.*
import toMono
import java.net.URI.*
import java.net.URLDecoder
import java.time.LocalDate
import java.util.*


@Controller
class UserController(val repository: UserRepository,
                     val eventRepository: EventRepository,
                     val markdownConverter: MarkdownConverter,
                     @Value("\${baseUri}") val baseUri: String) : RouterFunctionProvider() {

    // TODO Remove this@UserController when KT-15667 will be fixed
    override val routes: Routes = {
        accept(TEXT_HTML).route {
            (GET("/user/{login}") or GET("/speaker/{login}") or GET("/sponsor/{login}")) { findOneView(it) }
            (GET("/member/{login}") or GET("/profile/{login}") or GET("/member/sponsor/{login}") or GET("/member/member/{login}")) { status(PERMANENT_REDIRECT).location(create("$baseUri/user/${it.pathVariable("login")}")).build() }
            GET("/about/", this@UserController::findAboutView)
            GET("/about") { redirectPermanently("$baseUri/about/") }
            GET("/sponsors/", this@UserController::findSponsorsView)
        }
        accept(APPLICATION_JSON).route {
            "/api/user".route {
                GET("/", this@UserController::findAll)
                POST("/", this@UserController::create)
                GET("/{login}", this@UserController::findOne)
            }
            "/api/staff".route {
                GET("/", this@UserController::findStaff)
                GET("/{login}", this@UserController::findOneStaff)
            }
            "/api/speaker".route {
                GET("/", this@UserController::findSpeakers)
                GET("/{login}", this@UserController::findOneSpeaker)
            }
            "/api/sponsor".route {
                GET("/", this@UserController::findSponsors)
                GET("/{login}", this@UserController::findOneSponsor)
            }
            GET("/api/{event}/speaker/", this@UserController::findSpeakersByEvent)
        }
    }

    fun findOneView(req: ServerRequest) = try {
        val idLegacy = req.pathVariable("login").toLong()
        repository.findByLegacyId(idLegacy)
                .then { u -> ok().render("user", mapOf(Pair("user", u.toDto(req.language())))) }
    } catch (e:NumberFormatException) {
        repository.findOne(URLDecoder.decode(req.pathVariable("login"), "UTF-8"))
                .then { u -> ok().render("user", mapOf(Pair("user", u.toDto(req.language())))) }
    }

    fun findOne(req: ServerRequest) =
            ok().json().body(repository.findOne(req.pathVariable("login")))

    fun findAll(req: ServerRequest) =
            ok().json().body(repository.findAll())

    fun findStaff(req: ServerRequest) =
            ok().json().body(repository.findByRole(Role.STAFF))

    fun findOneStaff(req: ServerRequest) =
            ok().json().body(repository.findOneByRole(req.pathVariable("login"), Role.STAFF))

    fun findSpeakers(req: ServerRequest) =
            ok().json().body(repository.findByRole(Role.SPEAKER))

    fun findSpeakersByEvent(req: ServerRequest) =
            ok().json().body(repository.findByRoleAndEvent(Role.SPEAKER, req.pathVariable("event")))

    fun findOneSpeaker(req: ServerRequest) =
            ok().json().body(repository.findOneByRole(req.pathVariable("login"), Role.SPEAKER))

    fun findSponsors(req: ServerRequest) =
            ok().json().body(repository.findByRole(Role.SPONSOR))

    fun findOneSponsor(req: ServerRequest) =
            ok().json().body(repository.findOneByRole(req.pathVariable("login"), Role.SPONSOR))

    fun create(req: ServerRequest) =
            repository.save(req.bodyToMono<User>())
            .then { u ->
                created(create("/api/user/${u.login}")).json().body(u.toMono())
            }

    fun findAboutView(req: ServerRequest) = repository.findByRole(Role.STAFF)
            .collectList()
            .then { u ->
                val users = u.map { it.toDto(req.language()) }
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
           eventSponsoring.sponsor.toDto(language),
           eventSponsoring.subscriptionDate
    )

    private fun User.toDto(language: Language) = UserDto(
            login, firstname, lastname, email, company,
            markdownConverter.toHTML(description[language] ?: ""),
            logoUrl, events, role, links)

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