package mixit.routes

import mixit.MixitApplication.Companion.CURRENT_EVENT
import mixit.MixitProperties
import mixit.blog.handler.WebBlogHandler
import mixit.routes.Routes.GOOGLE_DRIVE_URI
import mixit.talk.handler.TalkHandler
import mixit.util.permanentRedirect
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType.TEXT_HTML
import org.springframework.web.reactive.function.server.coRouter

@Configuration
class RedirectRoutes(
    private val blogHandler: WebBlogHandler,
    private val talkHandler: TalkHandler,
    private val properties: MixitProperties
) {

    @Bean
    fun redirectRouter() = coRouter {
        accept(TEXT_HTML).nest {
            GET("/articles") { permanentRedirect("${properties.baseUri}/blog") }
            GET("/article/{id}", blogHandler::redirect)
            GET("/article/{id}/", blogHandler::redirect)

            GET("/docs/sponsor/leaflet/en") { permanentRedirect("$GOOGLE_DRIVE_URI?id=${properties.drive.en.sponsor}") }
            GET("/docs/sponsor/leaflet/fr") { permanentRedirect("$GOOGLE_DRIVE_URI?id=${properties.drive.fr.sponsor}") }
            GET("/docs/sponsor/form/en") { permanentRedirect("$GOOGLE_DRIVE_URI?id=${properties.drive.en.sponsorform}") }
            GET("/docs/sponsor/form/fr") { permanentRedirect("$GOOGLE_DRIVE_URI?id=${properties.drive.fr.sponsorform}") }
            GET("/docs/speaker/leaflet/en") { permanentRedirect("$GOOGLE_DRIVE_URI?id=${properties.drive.en.speaker}") }
            GET("/docs/speaker/leaflet/fr") { permanentRedirect("$GOOGLE_DRIVE_URI?id=${properties.drive.fr.speaker}") }
            GET("/docs/presse/leaflet/en") { permanentRedirect("$GOOGLE_DRIVE_URI?id=${properties.drive.en.press}") }
            GET("/docs/presse/leaflet/fr") { permanentRedirect("$GOOGLE_DRIVE_URI?id=${properties.drive.fr.press}") }

            (2012..CURRENT_EVENT.toInt()).forEach { year ->
                GET("/$year/") { permanentRedirect("${properties.baseUri}/$year") }
                GET("/about/$year/") { permanentRedirect("${properties.baseUri}/$year/about") }
                GET("/sponsors/$year/") { permanentRedirect("${properties.baseUri}/$year/sponsors") }
            }

            GET("/mixteen") { permanentRedirect("${properties.baseUri}/2022/mixteen-2022-born-to-code") }

            GET("/session/{id}", talkHandler::redirectFromId)
            GET("/session/{id}/", talkHandler::redirectFromId)
            GET("/session/{id}/{sluggifiedTitle}/", talkHandler::redirectFromId)
            GET("/session/{id}/{sluggifiedTitle}", talkHandler::redirectFromId)
            GET("/talk/{slug}", talkHandler::redirectFromSlug)

            (
                GET("/member/{login}")
                    or GET("/profile/{login}")
                    or GET("/member/sponsor/{login}")
                    or GET("/member/member/{login}")
                ) {
                permanentRedirect("${properties.baseUri}/user/${it.pathVariable("login")}")
            }
            GET("/sponsors/") { permanentRedirect("$${properties.baseUri}/sponsors") }

            GET("/about/") { permanentRedirect("${properties.baseUri}/about") }
            GET("/home") { permanentRedirect("${properties.baseUri}/") }
        }
    }
}
