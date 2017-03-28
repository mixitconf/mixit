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

    fun adminTicketing(req: ServerRequest) = ticketRepository.findAll().collectList().then { t ->
        ok().render("admin-ticketing", mapOf(Pair("tickets", t), Pair("title", "admin.ticketing.title")))
    }

    fun adminTalks(req: ServerRequest) = talkRepository.findByEvent("mixit17").collectList().then { talks ->
        userRepository.findMany(talks.flatMap(Talk::speakerIds)).collectMap(User::login).then { speakers ->
            ok().render("admin-talks", mapOf(Pair("talks", talks.map { it.toDto(it.speakerIds.mapNotNull { speakers[it] }, markdownConverter) }), Pair("title", "admin.talks.title")))
        }
    }

    fun adminUsers(req: ServerRequest) = userRepository.findAll().collectList().then { users ->
        ok().render("admin-users", mapOf(Pair("users", users), Pair("title", "admin.users.title")))
    }

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
                    Triple(AMPHI1, AMPHI1.name, AMPHI1 == talk.room),
                    Triple(AMPHI2, AMPHI2.name, AMPHI2 == talk.room),
                    Triple(ROOM1, ROOM1.name, ROOM1 == talk.room),
                    Triple(ROOM2, ROOM2.name, ROOM2 == talk.room),
                    Triple(ROOM3, ROOM3.name, ROOM3 == talk.room),
                    Triple(ROOM4, ROOM4.name, ROOM4 == talk.room),
                    Triple(ROOM5, ROOM5.name, ROOM5 == talk.room),
                    Triple(ROOM6, ROOM6.name, ROOM6 == talk.room),
                    Triple(ROOM7, ROOM7.name, ROOM7 == talk.room),
                    Triple(UNKNOWN, UNKNOWN.name, UNKNOWN == talk.room)
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


