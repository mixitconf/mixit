package mixit.support

import org.springframework.http.HttpHeaders.ACCEPT_LANGUAGE
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain

class MixitWebFilter : WebFilter {

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain) =
        if (exchange.request.uri.path.startsWith("/en/"))
            chain.filter(exchange.mutate().request(exchange.request.mutate()
                    .path(exchange.request.uri.path.substring(3))
                    .header(ACCEPT_LANGUAGE, "en").build()).build())
        else chain.filter(exchange.mutate()
                .request(exchange.request.mutate().header(ACCEPT_LANGUAGE, "fr").build()).build())

}

