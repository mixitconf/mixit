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
                private val externalHandler: ExternalHandler,
                private val userHandler: UserHandler,
                private val workAdventureHandler: WorkAdventureHandler) {

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

            GET("/wa", workAdventureHandler::findAll)
            GET("/wac", workAdventureHandler::findAllCrypted)

            // Edition data
            GET("/{year}/talk", talkHandler::findByEventId)
            GET("/{year}/speaker", userHandler::findSpeakerByEventId)
            GET("/{year}/event", eventHandler::findByEventID)

            GET("/talk/{login}", talkHandler::findOne)

            "/favorites".nest {
                GET("/{email}/talks/{id}", favoriteHandler::getFavorite)
                GET("/{email}", favoriteHandler::getFavorites)
                POST("/{email}/talks/{id}/toggle", favoriteHandler::toggleFavorite)
            }

            "/user".nest {
                GET("/", userHandler::findAll)
                GET("/{login}", userHandler::findOne)
            }
            "/staff".nest {
                GET("/", userHandler::findStaff)
                GET("/{login}", userHandler::findOneStaff)
            }

            // Some external request needs to be secured. So for them we need in the request the email and token. Values
            "/external".nest {
                // Require a token as arg
                GET("/token", externalHandler::sendToken)
                // Require a token and email as arg
                GET("/token/check", externalHandler::checkToken)
                // Needs authent and requires a token and email as arg
                GET("/me", externalHandler::profile)
                // Needs authent and requires a token and email as arg
                GET("/favorites", externalHandler::favorites)
                // Needs authent and requires a token and email as arg
                GET("/favorites/talks/{id}", externalHandler::favorite)
                // Needs authent and requires a token and email as arg
                POST("/favorites/talks/{id}/toggle", externalHandler::toggleFavorite)
            }
        }
    }
}
