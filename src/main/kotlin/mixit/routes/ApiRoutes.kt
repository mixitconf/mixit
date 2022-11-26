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
            GET("/admin/favorite", favoriteHandler::findAll)
            GET("/admin/mixette", adminMixetteHandler::findAll)
            GET("/admin/ticket", ticketHandler::findAll)
            GET("/admin/lottery", lotteryHandler::findAll)
            GET("/blog", blogHandler::findAll)
            GET("/blog/{id}", blogHandler::findOne)
            GET("/event", eventHandler::findAll)
            GET("/event/{id}", eventHandler::findOne)
            GET("/favorites/{email}/talks/{id}", favoriteHandler::getFavorite)
            GET("/favorites/{email}", favoriteHandler::getFavorites)
            GET("/staff", userHandler::findStaff)
            GET("/staff/{login}", userHandler::findOneStaff)
            GET("/talk/{login}", talkHandler::findOne)
            GET("/user", userHandler::findAll)
            GET("/user/{login}", userHandler::findOne)

            GET("/{year}/event", eventHandler::findByEventID)
            GET("/{year}/speaker", userHandler::findSpeakerByEventId)
            GET("/{year}/talk", talkHandler::findByEventId)
        }
    }

    @Bean
    @Order(1)
    fun apiRouter() = router {
        (accept(APPLICATION_JSON) and "/api").nest {

            // Edition data
            "/favorites".nest {

                POST("/{email}/talks/{id}/toggle", favoriteHandler::toggleFavorite)
            }

            // Some external request needs to be secured. So for them we need in the request the email and token. Values

            // Require a token as arg
            GET("/external/token", externalHandler::sendToken)
            // Require a token and email as arg
            GET("/external/token/check", externalHandler::checkToken)
            // Needs authent and requires a token and email as arg
            GET("/external/me", externalHandler::profile)
            // Needs authent and requires a token and email as arg
            GET("/external/favorites", externalHandler::favorites)
            // Needs authent and requires a token and email as arg
            GET("/external/favorites/talks/{id}", externalHandler::favorite)
            // Needs authent and requires a token and email as arg
            POST("/external/favorites/talks/{id}/toggle", externalHandler::toggleFavorite)
        }
    }
}
