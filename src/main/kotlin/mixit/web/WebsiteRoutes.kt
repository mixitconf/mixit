package mixit.web

import mixit.web.handler.*
import org.springframework.context.annotation.Bean
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType.*
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.RouterFunctions.resources
import org.springframework.web.reactive.function.server.router


@Component
class WebsiteRoutes(val adminHandler: AdminHandler,
                    val authenticationHandler: AuthenticationHandler,
                    val blogHandler: BlogHandler,
                    val globalHandler: GlobalHandler,
                    val newsHandler: NewsHandler,
                    val talkHandler: TalkHandler,
                    val userHandler: UserHandler,
                    val sponsorHandler: SponsorHandler,
                    val ticketingHandler: TicketingHandler) {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    fun websiteRouter() = router {
        accept(TEXT_HTML).nest {
            GET("/") { sponsorHandler.viewWithSponsors("home", it) }
            GET("/about", globalHandler::findAboutView)
            GET("/news", newsHandler::newsView)
            //GET("/ticketing", ticketingHandler::ticketing)

            // Authentication
            GET("/login", authenticationHandler::loginView)

            // Talks
            GET("/2017", talkHandler::talks2017)
            GET("/2016") { talkHandler.findByEventView(2016, it) }
            GET("/2015") { talkHandler.findByEventView(2015, it) }
            GET("/2014") { talkHandler.findByEventView(2014, it) }
            GET("/2013") { talkHandler.findByEventView(2013, it) }
            GET("/2012") { talkHandler.findByEventView(2012, it) }
            GET("/talk/{slug}", talkHandler::findOneView)

            // Users
            (GET("/user/{login}")
                    or GET("/speaker/{login}")
                    or GET("/sponsor/{login}")) { userHandler.findOneView(it) }
            GET("/sponsors") { sponsorHandler.viewWithSponsors("sponsors", it) }

            "/admin".nest {
                GET("/", adminHandler::admin)
                GET("/ticketing", adminHandler::adminTicketing)
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
            POST("/login", authenticationHandler::login)
            //POST("/ticketing", ticketingHandler::submit)
        }
    }

    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    fun resourceRouter() = resources("/**", ClassPathResource("static/"))

}
