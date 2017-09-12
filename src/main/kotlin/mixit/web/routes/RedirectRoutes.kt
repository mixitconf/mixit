package mixit.controller

import mixit.MixitProperties
import mixit.util.permanentRedirect
import mixit.web.handler.BlogHandler
import mixit.web.handler.TalkHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType.TEXT_HTML
import org.springframework.web.reactive.function.server.router

@Configuration
class RedirectRoutes(private val blogHandler: BlogHandler,
                     private val talkHandler: TalkHandler,
                     private val properties: MixitProperties) {

    val GOOGLE_DRIVE_URI = "https://drive.google.com/open"

    @Bean
    fun redirectRouter() = router {
        accept(TEXT_HTML).nest {
            "/articles".nest {
                GET("/") { permanentRedirect("${properties.baseUri}/blog") }
                (GET("/{id}") or GET("/{id}/")).invoke(blogHandler::redirect)
            }
            GET("/article/{id}/", blogHandler::redirect)

            GET("/docs/sponsor/leaflet/en") { permanentRedirect("$GOOGLE_DRIVE_URI?id=${properties.drive.en.sponsor}")}
            GET("/docs/sponsor/leaflet/fr") { permanentRedirect("$GOOGLE_DRIVE_URI?id=${properties.drive.fr.sponsor}")}
            GET("/docs/sponsor/form/en") { permanentRedirect("$GOOGLE_DRIVE_URI?id=${properties.drive.en.sponsorform}")}
            GET("/docs/sponsor/form/fr") { permanentRedirect("$GOOGLE_DRIVE_URI?id=${properties.drive.fr.sponsorform}")}
            GET("/docs/speaker/leaflet/en") { permanentRedirect("$GOOGLE_DRIVE_URI?id=${properties.drive.en.speaker}")}
            GET("/docs/speaker/leaflet/fr") { permanentRedirect("$GOOGLE_DRIVE_URI?id=${properties.drive.fr.speaker}")}
            GET("/docs/presse/leaflet/en") { permanentRedirect("$GOOGLE_DRIVE_URI?id=${properties.drive.en.press}")}
            GET("/docs/presse/leaflet/fr") { permanentRedirect("$GOOGLE_DRIVE_URI?id=${properties.drive.fr.press}")}

            GET("/2017/") { permanentRedirect("${properties.baseUri}/2017") }
            GET("/2016/") { permanentRedirect("${properties.baseUri}/2016") }
            GET("/2015/") { permanentRedirect("${properties.baseUri}/2015") }
            GET("/2014/") { permanentRedirect("${properties.baseUri}/2014") }
            GET("/2013/") { permanentRedirect("${properties.baseUri}/2013") }
            GET("/2012/") { permanentRedirect("${properties.baseUri}/2012") }
            (GET("/session/{id}")
                    or GET("/session/{id}/")
                    or GET("/session/{id}/{sluggifiedTitle}/")
                    or GET("/session/{id}/{sluggifiedTitle}")).invoke(talkHandler::redirectFromId)
            GET("/talk/{slug}", talkHandler::redirectFromSlug)

            (GET("/member/{login}")
                    or GET("/profile/{login}")
                    or GET("/member/sponsor/{login}")
                    or GET("/member/member/{login}")) { permanentRedirect("${properties.baseUri}/user/${it.pathVariable("login")}") }
            GET("/sponsors/") { permanentRedirect("$${properties.baseUri}/sponsors") }

            GET("/about/") { permanentRedirect("${properties.baseUri}/about") }
            GET("/home") { permanentRedirect("${properties.baseUri}/") }

        }
    }

}



