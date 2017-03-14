package mixit.web

import mixit.util.router
import mixit.web.handler.*
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType
import org.springframework.stereotype.Component


@Component
class ApiRoutes(val blogHandler: BlogHandler,
                 val eventHandler: EventHandler,
                 val talkHandler: TalkHandler,
                 val userHandler: UserHandler) {

    @Bean
    fun apiRouter() = router {
        (accept(MediaType.APPLICATION_JSON) and "/api").route {
            "/blog".route {
                GET("/", blogHandler::findAll)
                GET("/{id}", blogHandler::findOne)
            }

            "/event".route {
                GET("/", eventHandler::findAll)
                GET("/{login}", eventHandler::findOne)
            }

            // Talks
            GET("/talk/{login}", talkHandler::findOne)
            GET("/{year}/talk", talkHandler::findByEventId)

            // users
            "/user".route {
                GET("/", userHandler::findAll)
                POST("/", userHandler::create)
                GET("/{login}", userHandler::findOne)
            }
            "/staff".route {
                GET("/", userHandler::findStaff)
                GET("/{login}", userHandler::findOneStaff)
            }
            "/speaker".route {
                GET("/", userHandler::findSpeakers)
                GET("/{login}", userHandler::findOneSpeaker)
            }
            "/sponsor".route {
                GET("/", userHandler::findSponsors)
                GET("/{login}", userHandler::findOneSponsor)
            }
            GET("/{event}/speaker/", userHandler::findSpeakersByEvent)
        }
    }
}
