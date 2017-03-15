package mixit.controller

import mixit.MixitProperties
import mixit.repository.PostRepository
import mixit.repository.TalkRepository
import mixit.util.language
import mixit.util.permanentRedirect
import mixit.util.router
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType.TEXT_HTML
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest

@Component
class RedirectRoutes(val postRepository: PostRepository,
                     val talkRepository: TalkRepository,
                     val mixitProperties: MixitProperties) {

    val GOOGLE_DRIVE_URI = "https://drive.google.com/open"

    @Bean
    fun redirectRouter() = router {
        accept(TEXT_HTML).route {
            "/articles".route {
                GET("/") { permanentRedirect("${mixitProperties.baseUri}/blog") }
                (GET("/{id}") or GET("/{id}/")) { redirectOneArticleView(it) }
            }
            GET("/article/{id}/", this@RedirectRoutes::redirectOneArticleView)

            GET("/docs/sponsor/leaflet/en") { permanentRedirect("$GOOGLE_DRIVE_URI?id=${mixitProperties.drive.en.sponsor}")}
            GET("/docs/sponsor/leaflet/fr") { permanentRedirect("$GOOGLE_DRIVE_URI?id=${mixitProperties.drive.fr.sponsor}")}
            GET("/docs/sponsor/form/en") { permanentRedirect("$GOOGLE_DRIVE_URI?id=${mixitProperties.drive.en.sponsorform}")}
            GET("/docs/sponsor/form/fr") { permanentRedirect("$GOOGLE_DRIVE_URI?id=${mixitProperties.drive.fr.sponsorform}")}
            GET("/docs/speaker/leaflet/en") { permanentRedirect("$GOOGLE_DRIVE_URI?id=${mixitProperties.drive.en.speaker}")}
            GET("/docs/speaker/leaflet/fr") { permanentRedirect("$GOOGLE_DRIVE_URI?id=${mixitProperties.drive.fr.speaker}")}
            GET("/docs/presse/leaflet/en") { permanentRedirect("$GOOGLE_DRIVE_URI?id=${mixitProperties.drive.en.press}")}
            GET("/docs/presse/leaflet/fr") { permanentRedirect("$GOOGLE_DRIVE_URI?id=${mixitProperties.drive.fr.press}")}

            GET("/2017/") { permanentRedirect("${mixitProperties.baseUri}/2017") }
            GET("/2016/") { permanentRedirect("${mixitProperties.baseUri}/2016") }
            GET("/2015/") { permanentRedirect("${mixitProperties.baseUri}/2015") }
            GET("/2014/") { permanentRedirect("${mixitProperties.baseUri}/2014") }
            GET("/2013/") { permanentRedirect("${mixitProperties.baseUri}/2013") }
            GET("/2012/") { permanentRedirect("${mixitProperties.baseUri}/2012") }
            (GET("/session/{id}")
                    or GET("/session/{id}/")
                    or GET("/session/{id}/{sluggifiedTitle}/")
                    or GET("/session/{id}/{sluggifiedTitle}")) { redirectOneSessionView(it) }

            (GET("/member/{login}")
                    or GET("/profile/{login}")
                    or GET("/member/sponsor/{login}")
                    or GET("/member/member/{login}")) { permanentRedirect("${mixitProperties.baseUri}/user/${it.pathVariable("login")}") }
            GET("/sponsors/") { permanentRedirect("$${mixitProperties.baseUri}/sponsors") }

            GET("/about/") { permanentRedirect("${mixitProperties.baseUri}/about") }
            GET("/home") { permanentRedirect("${mixitProperties.baseUri}/") }

        }
    }

    fun redirectOneArticleView(req: ServerRequest) = postRepository.findOne(req.pathVariable("id")).then { a ->
        permanentRedirect("${mixitProperties.baseUri}/blog/${a.slug[req.language()]}")
    }

    fun redirectOneSessionView(req: ServerRequest) = talkRepository.findOne(req.pathVariable("id")).then { s ->
        permanentRedirect("${mixitProperties.baseUri}/talk/${s.slug}")
    }

}



