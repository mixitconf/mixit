package mixit.controller

import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType.*
import org.springframework.stereotype.Controller
import org.springframework.web.reactive.function.fromServerSentEvents
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.ServerResponse.ok
import reactor.core.publisher.Flux
import java.time.Duration.ofMillis


@Controller
class NewsController {

    @Bean
    fun newsRouter() = mixit.util.router {
        "/news".route {
            (accept(TEXT_HTML) and GET("/")) { newsView(it) }
            (accept(TEXT_EVENT_STREAM) and GET("/sse")) { newsSse(it) }
        }
    }

    fun newsView(req: ServerRequest) = ok().render("news")

    fun newsSse(req: ServerRequest) = ok().body(fromServerSentEvents(Flux.interval(ofMillis(100)).map { "Hello $it!" }))

}
