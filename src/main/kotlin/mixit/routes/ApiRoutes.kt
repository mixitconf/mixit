package mixit.routes

import mixit.blog.handler.JsonBlogHandler
import mixit.event.handler.JsonEventHandler
import mixit.favorite.handler.JsonFavoriteHandler
import mixit.mixette.handler.AdminMixetteHandler
import mixit.security.handler.ExternalHandler
import mixit.talk.handler.TalkJsonHandler
import mixit.ticket.handler.AdminTicketHandler
import mixit.ticket.handler.LotteryHandler
import mixit.user.handler.UserJsonHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.web.reactive.function.server.coRouter
import org.springframework.web.reactive.function.server.router

@Configuration
class ApiRoutes(
    private val blogHandler: JsonBlogHandler,
    private val eventHandler: JsonEventHandler,
    private val talkHandler: TalkJsonHandler,
    private val favoriteHandler: JsonFavoriteHandler,
    private val lotteryHandler: LotteryHandler,
    private val externalHandler: ExternalHandler,
    private val userHandler: UserJsonHandler,
    private val ticketHandler: AdminTicketHandler,
    private val adminMixetteHandler: AdminMixetteHandler
) {

    @Bean
    @Order(2)
    fun apiCoRouter() = coRouter {
        (accept(APPLICATION_JSON) and "/api").nest {
            "/admin".nest {
                GET("/{year}/talk", talkHandler::findAdminByEventId)
            }
            GET("/staff", userHandler::findStaff)
            GET("/staff/{login}", userHandler::findOneStaff)
            GET("/talk/{login}", talkHandler::findOne)
            GET("/user", userHandler::findAll)
            GET("/user/{login}", userHandler::findOne)

            GET("/{year}/speaker", userHandler::findSpeakerByEventId)
            GET("/{year}/talk", talkHandler::findByEventId)
        }
    }

    @Bean
    @Order(1)
    fun apiRouter() = router {
        (accept(APPLICATION_JSON) and "/api").nest {
            "/blog".nest {
                GET("", blogHandler::findAll)
                GET("/{id}", blogHandler::findOne)
            }

            "/event".nest {
                GET("", eventHandler::findAll)
                GET("/{id}", eventHandler::findOne)
            }

            "/admin".nest {
                GET("/mixette", adminMixetteHandler::findAll)
                GET("/ticket", ticketHandler::findAll)
                GET("/lottery", lotteryHandler::findAll)
                GET("/favorite", favoriteHandler::findAll)
            }

            // Edition data

            GET("/{year}/event", eventHandler::findByEventID)

            "/favorites".nest {
                GET("/{email}/talks/{id}", favoriteHandler::getFavorite)
                GET("/{email}", favoriteHandler::getFavorites)
                POST("/{email}/talks/{id}/toggle", favoriteHandler::toggleFavorite)
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
