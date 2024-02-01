package mixit.talk.handler

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import mixit.MixitProperties
import mixit.util.mustache.MustacheI18n
import mixit.util.mustache.MustacheI18n.FORMATS
import mixit.util.mustache.MustacheI18n.LANGUAGES
import mixit.util.mustache.MustacheI18n.PHOTOS
import mixit.util.mustache.MustacheI18n.ROOMS
import mixit.util.mustache.MustacheI18n.SPEAKERS
import mixit.util.mustache.MustacheI18n.TALKS
import mixit.util.mustache.MustacheI18n.TITLE
import mixit.util.mustache.MustacheI18n.TOPICS
import mixit.util.mustache.MustacheI18n.YEAR
import mixit.util.mustache.MustacheTemplate.AdminTalk
import mixit.util.mustache.MustacheTemplate.AdminTalks
import mixit.talk.model.Language
import mixit.talk.model.Room
import mixit.talk.model.Talk
import mixit.talk.model.TalkFormat
import mixit.talk.model.TalkFormat.TALK
import mixit.talk.model.TalkService
import mixit.talk.model.Topic
import mixit.util.AdminUtils.toJson
import mixit.util.AdminUtils.toLinks
import mixit.util.YearSelector
import mixit.util.enumMatcher
import mixit.util.enumMatcherWithI18nKey
import mixit.util.errors.NotFoundException
import mixit.util.extractFormData
import mixit.util.language
import mixit.util.seeOther
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.renderAndAwait
import java.time.LocalDateTime
import mixit.MixitApplication
import mixit.talk.spi.CfpSynchronizer
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.json

@Component
class AdminTalkHandler(
    private val service: TalkService,
    private val properties: MixitProperties,
    private val objectMapper: ObjectMapper,
    private val synchronizer: CfpSynchronizer
) {

    companion object {
        const val LAST_TALK_EVENT = "2023"
        const val LIST_URI = "/admin/talks"
    }

    suspend fun adminTalks(req: ServerRequest, year: String): ServerResponse {
        val talks = service.findByEvent(year).map { it.toDto(req.language()) }
        val params = mapOf(
            TITLE to AdminTalks.title,
            YEAR to year,
            "isCurrent" to (year == MixitApplication.CURRENT_EVENT),
            MustacheI18n.YEAR_SELECTOR to YearSelector.create(year.toInt(), "admin/talks"),
            TALKS to talks
        )
        return ok().render(AdminTalks.template, params).awaitSingle()
    }

    suspend fun createTalk(req: ServerRequest): ServerResponse =
        this.adminTalk(Talk(TALK, req.pathVariable("year"), "", ""))

    suspend fun editTalk(req: ServerRequest): ServerResponse =
        adminTalk(service.findOneOrNull(req.pathVariable("id"))?.toTalk() ?: throw NotFoundException())

    suspend fun adminSaveTalk(req: ServerRequest): ServerResponse {
        val formData = req.extractFormData()
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
            video2 = formData["video2"],
            room = Room.valueOf(formData["room"]!!),
            addedAt = LocalDateTime.parse(formData["addedAt"]),
            start = LocalDateTime.parse(formData["start"]),
            end = LocalDateTime.parse(formData["end"]),
            photoUrls = formData["photoUrls"]?.toLinks(objectMapper) ?: emptyList()
        )
        service.save(talk).awaitSingleOrNull()
        return seeOther("${properties.baseUri}$LIST_URI")
    }

    suspend fun adminDeleteTalk(req: ServerRequest): ServerResponse {
        val formData = req.extractFormData()
        service.deleteOne(formData["id"]!!).awaitSingleOrNull()
        return seeOther("${properties.baseUri}$LIST_URI")
    }

    private suspend fun adminTalk(talk: Talk): ServerResponse {
        val params = mapOf(
            TITLE to AdminTalk.title,
            MustacheI18n.TALK to talk,
            LANGUAGES to enumMatcher(talk) { it?.language ?: Language.FRENCH },
            ROOMS to enumMatcherWithI18nKey(talk, "rooms") { it.room ?: Room.UNKNOWN },
            SPEAKERS to talk.speakerIds.joinToString(separator = ","),
            PHOTOS to talk.photoUrls.toJson(objectMapper),
            FORMATS to enumMatcher(talk) { it?.format ?: TALK },
            TOPICS to enumMatcher(talk) { Topic.of(it?.topic ?: "other") }
        )
        return ok().renderAndAwait(AdminTalk.template, params)
    }

    suspend fun adminSynchronize(req: ServerRequest): ServerResponse {
        synchronizer.synchronize()
        return adminTalks(req, MixitApplication.CURRENT_EVENT)
    }
}
