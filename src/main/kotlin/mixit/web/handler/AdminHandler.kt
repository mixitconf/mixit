package mixit.web.handler

import mixit.MixitProperties
import mixit.model.*
import mixit.model.Language.*
import mixit.model.Room.*
import mixit.model.TalkFormat.*
import mixit.repository.TalkRepository
import mixit.repository.TicketRepository
import mixit.repository.UserRepository
import mixit.util.MarkdownConverter
import mixit.util.language
import mixit.util.seeOther
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.*
import reactor.core.publisher.Mono
import java.time.LocalDateTime


@Component
class AdminHandler(val ticketRepository: TicketRepository,
                   val talkRepository: TalkRepository,
                   val userRepository: UserRepository,
                   val markdownConverter: MarkdownConverter,
                   val properties: MixitProperties) {

    fun admin(req: ServerRequest) = ok().render("admin", mapOf(Pair("title", "admin.title")))

    fun adminTicketing(req: ServerRequest) = ok().render("admin-ticketing", mapOf(
            Pair("tickets", ticketRepository.findAll()),
            Pair("title", "admin.ticketing.title")
    ))

    fun adminTalks(req: ServerRequest) = ok().render("admin-talks", mapOf(
            Pair("talks", talkRepository
                    .findByEvent("mixit17")
                    .collectList()
                    .then { talks -> userRepository
                            .findMany(talks.flatMap(Talk::speakerIds))
                            .collectMap(User::login)
                            .map { speakers -> talks.map { it.toDto(req.language(), it.speakerIds.mapNotNull { speakers[it] }, markdownConverter) } }
                    }),
            Pair("title", "admin.talks.title")
    ))


    fun adminUsers(req: ServerRequest) = ok().render("admin-users", mapOf(Pair("users", userRepository.findAll()), Pair("title", "admin.users.title")))


    fun createTalk(req: ServerRequest) : Mono<ServerResponse> = this.adminTalk()

    fun editTalk(req: ServerRequest) : Mono<ServerResponse> = talkRepository.findBySlug(req.pathVariable("slug")).then(this::adminTalk)

    fun adminSaveTalk(req: ServerRequest) : Mono<ServerResponse> {
        return req.body(BodyExtractors.toFormData()).then { data ->
            val formData = data.toSingleValueMap()
            val talk = Talk(
                    id = formData["id"],
                    event = formData["event"]!!,
                    format = TalkFormat.valueOf(formData["format"]!!),
                    title = formData["title"]!!,
                    summary = formData["summary"]!!,
                    description = formData["description"],
                    language = Language.valueOf(formData["language"]!!),
                    speakerIds = formData["speakers"]!!.split(","),
                    video = formData["video"],
                    room = Room.valueOf(formData["room"]!!),
                    addedAt = LocalDateTime.parse(formData["addedAt"]),
                    start = LocalDateTime.parse(formData["start"]),
                    end = LocalDateTime.parse(formData["end"])
            )
            talkRepository.save(talk).then { _ -> seeOther("${properties.baseUri}/admin/talks") }
        }
    }

    fun adminDeleteTalk(req: ServerRequest) : Mono<ServerResponse> =
            req.body(BodyExtractors.toFormData()).then { data ->
                val formData = data.toSingleValueMap()
                talkRepository
                        .deleteOne(formData["id"]!!)
                        .then{ _ -> seeOther("${properties.baseUri}/admin/talks") }
            }


    private fun adminTalk(talk: Talk = Talk(TALK, "mixit17", "", "")) = ok().render("admin-talk", mapOf(
            Pair("talk", talk),
            Pair("title", "admin.talk.title"),
            Pair("rooms", listOf(
                    Triple(AMPHI1, "rooms.${AMPHI1.name.toLowerCase()}", AMPHI1 == talk.room),
                    Triple(AMPHI2, "rooms.${AMPHI2.name.toLowerCase()}", AMPHI2 == talk.room),
                    Triple(ROOM1, "rooms.${ROOM1.name.toLowerCase()}", ROOM1 == talk.room),
                    Triple(ROOM2, "rooms.${ROOM2.name.toLowerCase()}", ROOM2 == talk.room),
                    Triple(ROOM3, "rooms.${ROOM3.name.toLowerCase()}", ROOM3 == talk.room),
                    Triple(ROOM4, "rooms.${ROOM4.name.toLowerCase()}", ROOM4 == talk.room),
                    Triple(ROOM5, "rooms.${ROOM5.name.toLowerCase()}", ROOM5 == talk.room),
                    Triple(ROOM6, "rooms.${ROOM6.name.toLowerCase()}", ROOM6 == talk.room),
                    Triple(ROOM7, "rooms.${ROOM7.name.toLowerCase()}", ROOM7 == talk.room),
                    Triple(UNKNOWN, "rooms.${UNKNOWN.name.toLowerCase()}", UNKNOWN == talk.room)
            )),
            Pair("formats", listOf(
                    Pair(TALK, TALK == talk.format),
                    Pair(LIGHTNING_TALK, LIGHTNING_TALK == talk.format),
                    Pair(WORKSHOP, WORKSHOP == talk.format),
                    Pair(RANDOM, RANDOM == talk.format),
                    Pair(KEYNOTE, KEYNOTE == talk.format)
            )),
            Pair("languages", listOf(
                    Pair(ENGLISH, ENGLISH == talk.language),
                    Pair(FRENCH, FRENCH == talk.language)
            )),
            Pair("speakers", talk.speakerIds.joinToString(separator = ","))

    ))


}


