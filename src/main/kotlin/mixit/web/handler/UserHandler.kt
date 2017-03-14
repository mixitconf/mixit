package mixit.web.handler

import mixit.model.*
import mixit.repository.EventRepository
import mixit.repository.UserRepository
import mixit.util.*
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.ServerResponse.*
import toMono
import java.net.URI.*
import java.net.URLDecoder


@Component
class UserHandler(val repository: UserRepository,
                  val eventRepository: EventRepository,
                  val markdownConverter: MarkdownConverter) {

    fun findOneView(req: ServerRequest) =
            try {
                val idLegacy = req.pathVariable("login").toLong()
                repository.findByLegacyId(idLegacy).then { u ->
                    ok().render("user", mapOf(Pair("user", u.toDto(req.language(), markdownConverter))))
                }
            } catch (e:NumberFormatException) {
                repository.findOne(URLDecoder.decode(req.pathVariable("login"), "UTF-8")).then { u ->
                    ok().render("user", mapOf(Pair("user", u.toDto(req.language(), markdownConverter))))
                }
            }

    fun findOne(req: ServerRequest) = ok().json().body(repository.findOne(req.pathVariable("login")))

    fun findAll(req: ServerRequest) = ok().json().body(repository.findAll())

    fun findStaff(req: ServerRequest) = ok().json().body(repository.findByRole(Role.STAFF))

    fun findOneStaff(req: ServerRequest) = ok().json().body(repository.findOneByRole(req.pathVariable("login"), Role.STAFF))

    fun findSpeakers(req: ServerRequest) = ok().json().body(repository.findByRole(Role.SPEAKER))

    fun findSpeakersByEvent(req: ServerRequest) =
            ok().json().body(repository.findByRoleAndEvent(Role.SPEAKER, req.pathVariable("event")))

    fun findOneSpeaker(req: ServerRequest) =
            ok().json().body(repository.findOneByRole(req.pathVariable("login"), Role.SPEAKER))

    fun findSponsors(req: ServerRequest) = ok().json().body(repository.findByRole(Role.SPONSOR))

    fun findOneSponsor(req: ServerRequest) =
            ok().json().body(repository.findOneByRole(req.pathVariable("login"), Role.SPONSOR))

    fun create(req: ServerRequest) = repository.save(req.bodyToMono<User>()).then { u ->
        created(create("/api/user/${u.login}")).json().body(u.toMono())
    }

    fun findSponsorsView(req: ServerRequest) = eventRepository.findOne("mixit17").then { events ->
        val sponsors = events.sponsors.map { it.toDto(req.language(), markdownConverter) }.groupBy { it.level }

        ok().render("sponsors", mapOf(
            Pair("sponsors-gold", sponsors[SponsorshipLevel.GOLD]),
            Pair("sponsors-silver", sponsors[SponsorshipLevel.SILVER]),
            Pair("sponsors-hosting", sponsors[SponsorshipLevel.HOSTING]),
            Pair("sponsors-lanyard", sponsors[SponsorshipLevel.LANYARD]),
            Pair("sponsors-party", sponsors[SponsorshipLevel.PARTY])
        ))
    }
}
