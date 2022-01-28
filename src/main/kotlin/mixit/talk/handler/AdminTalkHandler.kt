package mixit.talk.handler

import com.fasterxml.jackson.databind.ObjectMapper
import java.time.LocalDateTime
import mixit.MixitProperties
import mixit.talk.model.Language
import mixit.talk.model.Room
import mixit.talk.model.Talk
import mixit.talk.model.TalkFormat
import mixit.talk.model.TalkFormat.TALK
import mixit.talk.model.Topic
import mixit.talk.repository.TalkRepository
import mixit.user.model.User
import mixit.user.repository.UserRepository
import mixit.util.AdminUtils.toJson
import mixit.util.AdminUtils.toLinks
import mixit.util.enumMatcher
import mixit.util.enumMatcherWithI18nKey
import mixit.util.extractFormData
import mixit.util.language
import mixit.util.seeOther
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import reactor.core.publisher.Mono

@Component
class AdminTalkHandler(
    private val talkRepository: TalkRepository,
    private val userRepository: UserRepository,
    private val properties: MixitProperties,
    private val objectMapper: ObjectMapper
) {

    companion object {
        const val TEMPLATE_LIST = "admin-talks"
        const val TEMPLATE_EDIT = "admin-talk"
        const val LAST_TALK_EVENT = "2021"
        const val LIST_URI = "/admin/talks"
    }

    fun adminTalks(req: ServerRequest, year: String): Mono<ServerResponse> {
        val talks = talkRepository
            .findByEvent(year)
            .collectList()
            .flatMap { talks ->
                userRepository
                    .findMany(talks.flatMap(Talk::speakerIds))
                    .collectMap(User::login)
                    .map { speakers ->
                        talks.map { talk ->
                            talk.toDto(req.language(), talk.speakerIds.mapNotNull { speakers[it] })
                        }
                    }
            }
        return ok().render(
            TEMPLATE_LIST,
            mapOf(
                Pair("year", year),
                Pair("talks", talks),
                Pair("title", "admin.talks.title")
            )
        )
    }

    fun createTalk(req: ServerRequest): Mono<ServerResponse> =
        this.adminTalk()

    fun editTalk(req: ServerRequest): Mono<ServerResponse> =
        talkRepository.findBySlug(req.pathVariable("slug")).flatMap(this::adminTalk)

    fun adminSaveTalk(req: ServerRequest): Mono<ServerResponse> =
        req.extractFormData().flatMap { formData ->
            val talk = Talk(
                id = formData["id"],
                event = formData["event"]!!,
                format = TalkFormat.valueOf(formData["format"]!!),
                title = formData["title"]!!,
                summary = formData["summary"]!!,
                description = formData["description"],
                topic = formData["topic"]?.let { Topic.valueOf(it).value },
                language = formData["language"]?.let { Language.valueOf(it) } ?: Language.FRENCH,
                speakerIds = formData["speakers"]!!.split(","),
                video = formData["video"],
                room = Room.valueOf(formData["room"]!!),
                addedAt = LocalDateTime.parse(formData["addedAt"]),
                start = LocalDateTime.parse(formData["start"]),
                end = LocalDateTime.parse(formData["end"]),
                photoUrls = formData["photoUrls"]?.toLinks(objectMapper) ?: emptyList()
            )
            talkRepository.save(talk).then(seeOther("${properties.baseUri}$LIST_URI"))
        }


    fun adminDeleteTalk(req: ServerRequest): Mono<ServerResponse> =
        req.extractFormData().flatMap { formData ->
            talkRepository
                .deleteOne(formData["id"]!!)
                .then(seeOther("${properties.baseUri}$LIST_URI"))
        }

    private fun adminTalk(talk: Talk = Talk(TALK, LAST_TALK_EVENT, "", "")) = ok().render(
        TEMPLATE_EDIT,
        mapOf(
            Pair("talk", talk),
            Pair("title", "admin.talk.title"),
            Pair("rooms", enumMatcherWithI18nKey(talk, "rooms") { it.room ?: Room.UNKNOWN }),
            Pair("formats", enumMatcher(talk) { it?.format ?: TALK }),
            Pair("languages", enumMatcher(talk) { it?.language ?: Language.FRENCH }),
            Pair("topics", enumMatcher(talk) { Topic.of(it?.topic ?: "other") }),
            Pair("speakers", talk.speakerIds.joinToString(separator = ",")),
            Pair("photos", talk.photoUrls.toJson(objectMapper))
        )
    )
}
