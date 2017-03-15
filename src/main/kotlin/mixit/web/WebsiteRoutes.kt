package mixit.web

import mixit.util.router
import mixit.web.handler.*
import org.springframework.context.annotation.Bean
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType.*
import org.springframework.stereotype.Component


@Component
class WebsiteRoutes(val adminHandler: AdminHandler,
                    val authenticationHandler: AuthenticationHandler,
                    val blogHandler: BlogHandler,
                    val globalHandler: GlobalHandler,
                    val newsHandler: NewsHandler,
                    val talkHandler: TalkHandler,
                    val userHandler: UserHandler,
                    val ticketingHandler: TicketingHandler) {

    @Bean
    fun htmlRouter() = router {

        accept(TEXT_HTML).route {
            GET("/", globalHandler::homeView)
            GET("/about", globalHandler::findAboutView)
            GET("/news", newsHandler::newsView)
            GET("/ticketing", ticketingHandler::ticketing)

            // Authentication
            GET("/login", authenticationHandler::loginView)
            GET("/logout", authenticationHandler::logout)  // TODO Use POST

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
            GET("/sponsors", userHandler::findSponsorsView)

            "/admin".route {
                GET("/", adminHandler::admin)
                GET("/ticketing", adminHandler::adminTicketing)
            }

            "/blog".route {
                GET("/", blogHandler::findAllView)
                GET("/{slug}", blogHandler::findOneView)
            }
        }

        resources("/**", ClassPathResource("static/"))

        accept(TEXT_EVENT_STREAM).route {
            GET("/news/sse", newsHandler::newsSse)
        }

        contentType(APPLICATION_FORM_URLENCODED).route {
            POST("/login", authenticationHandler::login)
            POST("/ticketing", ticketingHandler::submit)
        }
    }

}
