package mixit.web.handler

import org.springframework.http.MediaType.*
import org.springframework.stereotype.Controller
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.ServerResponse.ok
import reactor.core.publisher.Flux
import java.time.Duration.ofMillis


@Controller
class NewsHandler {

    fun newsView(req: ServerRequest) = ok().render("news")

    fun newsSse(req: ServerRequest) = ok()
            .contentType(TEXT_EVENT_STREAM)
            .body(Flux.interval(ofMillis(100)).map { "Hello $it!" })

}
