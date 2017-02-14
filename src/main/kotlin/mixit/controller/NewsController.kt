package mixit.controller

import mixit.support.LazyRouterFunction
import org.springframework.http.MediaType.*
import org.springframework.stereotype.Controller
import org.springframework.web.reactive.function.fromServerSentEvents
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.RequestPredicates.accept
import org.springframework.web.reactive.function.server.ServerResponse.ok
import reactor.core.publisher.Flux
import java.time.Duration.ofMillis


@Controller
class NewsController : LazyRouterFunction() {

    // TODO Remove this@ArticleController when KT-15667 will be fixed
    override val routes: RouterDsl.() -> Unit = {
        accept(TEXT_HTML).apply {
            GET("/news", this@NewsController::newsView)
        }
        accept(TEXT_EVENT_STREAM).apply {
            GET("/news/sse", this@NewsController::newsSse)
        }
    }

    fun newsView(req: ServerRequest) = ok().render("news")

    fun newsSse(req: ServerRequest) = ok().body(fromServerSentEvents(
            Flux.interval(ofMillis(100)).map { "Hello $it!" }
    ))

}
