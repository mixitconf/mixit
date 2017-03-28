package mixit.web

import mixit.MixitProperties
import mixit.web.handler.*
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Bean
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType.*
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.RouterFunctions.resources
import reactor.core.publisher.toMono


@Component
class WebsiteRoutes(val adminHandler: AdminHandler,
                    val authenticationHandler: AuthenticationHandler,
                    val blogHandler: BlogHandler,
                    val globalHandler: GlobalHandler,
                    val newsHandler: NewsHandler,
                    val talkHandler: TalkHandler,
                    val userHandler: UserHandler,
                    val sponsorHandler: SponsorHandler,
                    val ticketingHandler: TicketingHandler,
                    val messageSource: MessageSource,
                    val properties: MixitProperties) {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    fun websiteRouter() = router {
        accept(TEXT_HTML).nest {
            GET("/") { sponsorHandler.viewWithSponsors("home", null, it) }
            GET("/about", globalHandler::findAboutView)
            GET("/news", newsHandler::newsView)
            GET("/ticketing", ticketingHandler::ticketing)

            // Authentication
            GET("/login", authenticationHandler::loginView)
            GET("/logout", authenticationHandler::logout)
            GET("/oauth/{provider}", authenticationHandler::oauthCallback)

            // Talks
            GET("/2017") { talkHandler.findByEventView(2017, it) }
            GET("/2016") { talkHandler.findByEventView(2016, it) }
            GET("/2015") { talkHandler.findByEventView(2015, it) }
            GET("/2014") { talkHandler.findByEventView(2014, it) }
            GET("/2013") { talkHandler.findByEventView(2013, it) }
            GET("/2012") { talkHandler.findByEventView(2012, it) }
            GET("/talk/{slug}", talkHandler::findOneView)

            // Users
            (GET("/user/{login}") or GET("/sponsor/{login}")) { userHandler.findOneView(it) }
            GET("/sponsors") { sponsorHandler.viewWithSponsors("sponsors", "sponsors.title", it) }

            "/admin".nest {
                GET("/", adminHandler::admin)
                GET("/ticketing", adminHandler::adminTicketing)
                GET("/talks", adminHandler::adminTalks)
                DELETE("/")
                GET("/talks/edit/{slug}", adminHandler::editTalk)
                GET("/talks/create", adminHandler::createTalk)
                GET("/users", adminHandler::adminUsers)
            }

            "/blog".nest {
                GET("/", blogHandler::findAllView)
                GET("/{slug}", blogHandler::findOneView)
            }
        }

        accept(TEXT_EVENT_STREAM).nest {
            GET("/news/sse", newsHandler::newsSse)
        }

        contentType(APPLICATION_FORM_URLENCODED).nest {
            "/login".nest {
                POST("/", authenticationHandler::login)
                POST("/create", authenticationHandler::createLoginWithProvider)
                POST("/provider/add", authenticationHandler::associateProvider)
            }
            //POST("/ticketing", ticketingHandler::submit)
            "/admin".nest {
                POST("/talks", adminHandler::adminSaveTalk)
                POST("/talks/delete", adminHandler::adminDeleteTalk)
            }
        }
    }.filter { request, next ->
        val locale = request.headers().asHttpHeaders().acceptLanguageAsLocale
        val session = request.session().block()
        val path = request.uri().path
        val model = generateModel(properties.baseUri!!, path, locale, session, messageSource)
                next.handle(request).then { response -> if (response is RenderingResponse) RenderingResponse.from(response).modelAttributes(model).build() else response.toMono() }
    }

    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    fun resourceRouter() = resources("/**", ClassPathResource("static/"))

}

