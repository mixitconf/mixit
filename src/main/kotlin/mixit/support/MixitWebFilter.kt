package mixit.support

import org.springframework.http.HttpHeaders
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

class MixitWebFilter : WebFilter {

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val request = exchange.request
        return if (request.uri.path.startsWith("/en/")) {
            chain.filter(exchange.mutate().request(exchange.request.mutate()
                    .path(request.uri.path.substring(3))
                    .header(HttpHeaders.ACCEPT_LANGUAGE, "en").build()).build())

        } else {
             chain.filter(exchange.mutate().request(exchange.request.mutate()
                     .header(HttpHeaders.ACCEPT_LANGUAGE, "fr").build()).build())
        }
    }
}

