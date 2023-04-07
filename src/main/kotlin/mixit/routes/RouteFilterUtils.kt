package mixit.routes

import com.samskivert.mustache.Mustache
import kotlinx.coroutines.runBlocking
import mixit.MixitProperties
import mixit.features.model.Feature
import mixit.features.repository.FeatureStateRepository
import mixit.user.model.Role
import mixit.util.locale
import mixit.util.localePrefix
import mixit.util.toHTML
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.HandlerFunction
import org.springframework.web.reactive.function.server.RenderingResponse
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.server.WebSession
import org.springframework.web.util.UriUtils
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.util.Locale

@Component
class RouteFilterUtils(
    private val messageSource: MessageSource,
    private val properties: MixitProperties,
    private val featureStateRepository: FeatureStateRepository,
) {

    fun addModelToResponse(request: ServerRequest, next: HandlerFunction<ServerResponse>): Mono<ServerResponse> =
        request.session().flatMap { session ->
            val model = generateModel(request.uri().path, request.locale(), session)
            next.handle(request).flatMap {
                if (it is RenderingResponse) RenderingResponse.from(it).modelAttributes(model).build() else it.toMono()
            }
        }

    private fun generateModel(
        path: String,
        locale: Locale,
        session: WebSession,
    ) = mutableMapOf<String, Any>().apply {

        this.putAll(generateModelForExernalCall(locale))

        val email = session.getAttribute<String>("email")
        val role = session.getAttribute<Role>("role")
        email?.let {
            this["email"] = it
            if ((role != null && role == Role.STAFF)) this["admin"] = true
            if ((role != null && role == Role.VOLUNTEER)) this["volunteer"] = true
            this["connected"] = true
        }

        this["switchLangUrl"] = switchLangUrl(path, locale)
        this["uri"] = "${properties.baseUri}$path"
        this["markdown"] = Mustache.Lambda { frag, out -> out.write(frag.execute().toHTML()) }
        this["hasFeatureLottery"] = properties.feature.lottery
        this["hasFeatureLotteryResult"] = properties.feature.lotteryResult
        this["hasFeatureMixette"] = properties.feature.mixette
        this["hasFeatureProfileWithMessages"] = properties.feature.profilemsg
        this["hasMixitOnAirOnHomePage"] = featureStateRepository.findFeature(Feature.MixitOnAirOnHomePage).map { it.active }
    }.toMap()

    fun generateModelForExernalCall(
        locale: Locale
    ) = mutableMapOf<String, Any>().apply {
        this["locale"] = locale.toString()
        this["localePrefix"] = localePrefix(locale)
        this["en"] = locale.language == "en"
        this["fr"] = locale.language == "fr"
        this["baseUri"] = properties.baseUri
        this["i18n"] = Mustache.Lambda { frag, out ->
            val tokens = frag.execute().split("|")
            out.write(
                messageSource.getMessage(
                    tokens[0],
                    tokens.slice(IntRange(1, tokens.size - 1)).toTypedArray(),
                    locale
                )
            )
        }
        this["urlEncode"] =
            Mustache.Lambda { frag, out -> out.write(UriUtils.encodePathSegment(frag.execute(), "UTF-8")) }
    }

    private fun switchLangUrl(path: String, locale: Locale): String {
        if (locale.language == "en" && (path == "/" || path == "/en/" || path == "/en")) {
            return "/fr/"
        }
        return if (locale.language == "en") path else "/en$path"
    }
}
