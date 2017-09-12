package mixit.web

import mixit.web.handler.*
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType.*
import org.springframework.web.reactive.function.server.router


@Configuration
class ApiRoutes(private val blogHandler: BlogHandler,
                private val eventHandler: EventHandler,
                private val talkHandler: TalkHandler,
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

            // Talks
            GET("/talk/{login}", talkHandler::findOne)
            GET("/{year}/talk", talkHandler::findByEventId)

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
