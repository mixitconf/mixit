package mixit.controller

import org.springframework.http.MediaType
import org.springframework.http.MediaType.*
import org.springframework.stereotype.Controller
import org.springframework.web.reactive.function.fromServerSentEvents
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.RequestPredicates.accept
import org.springframework.web.reactive.function.server.ServerResponse.ok
import reactor.core.publisher.Flux
import java.time.Duration.ofMillis


@Controller
class NewsController : RouterFunction<ServerResponse> {

    override fun route(req: ServerRequest) = route(req) {
        accept(TEXT_HTML).apply {
            GET("/news") { newsView() }
        }
        accept(MediaType.TEXT_EVENT_STREAM).apply {
            GET("/news/sse") { newsSse() }
        }
    }

    fun newsView() = ok().render("news")

    fun newsSse() = ok().body(fromServerSentEvents(
            Flux.interval(ofMillis(100)).map { "Hello $it!" }
    ))

}
