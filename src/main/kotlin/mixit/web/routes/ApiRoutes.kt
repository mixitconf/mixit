package mixit.web

import mixit.web.handler.*
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.web.reactive.function.server.router


@Configuration
class ApiRoutes(private val blogHandler: BlogHandler,
                private val eventHandler: EventHandler,
                private val talkHandler: TalkHandler,
                private val favoriteHandler: FavoriteHandler,
                private val ticketingHandler: TicketingHandler,
                private val userHandler: UserHandler) {

    @Bean
    fun apiRouter() = router {
        (accept(APPLICATION_JSON) and "/api").nest {
            "/blog".nest {
                GET("/", blogHandler::findAll)
                GET("/{id}", blogHandler::findOne)
            }

            "/event".nest {
                GET("/", eventHandler::findAll)
                GET("/{id}", eventHandler::findOne)
            }

            "/admin".nest {
                GET("/ticket", ticketingHandler::findAll)
                GET("/ticket/random", ticketingHandler::randomDraw)
                GET("/favorite", favoriteHandler::findAll)
                GET("/{year}/talk", talkHandler::findAdminByEventId)
            }

            // Talks
            GET("/talk/{login}", talkHandler::findOne)
            GET("/{year}/talk", talkHandler::findByEventId)

            GET("/favorites/{email}/talks/{id}", favoriteHandler::getFavorite)
            GET("/favorites/{email}", favoriteHandler::getFavorites)
            POST("/favorites/{email}/talks/{id}/toggle", favoriteHandler::toggleFavorite)

            // users
            "/user".nest {
                GET("/", userHandler::findAll)
                POST("/", userHandler::create)
                GET("/{login}", userHandler::findOne)
            }
            "/staff".nest {
                GET("/", userHandler::findStaff)
                GET("/{login}", userHandler::findOneStaff)
            }
        }
    }
}
