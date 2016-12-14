package mixit.support

import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import org.springframework.http.server.reactive.ServerHttpRequestDecorator
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.util.*

class LocaleWebFilter : WebFilter {

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val request = exchange.request
        return if (request.uri.path.startsWith("/en/")) {
            exchange.attributes.put("locale", Locale.ENGLISH)
            chain.filter(exchange.mutate().request(PathSubstringServerHttpRequestDecorator(request, 3)).build())
        } else {
            exchange.attributes.put("locale", Locale.FRENCH)
            chain.filter(exchange)
        }
    }

    internal class PathSubstringServerHttpRequestDecorator(delegate: ServerHttpRequest, val index: Int) : ServerHttpRequestDecorator(delegate) {

        override fun getURI(): URI {
            val uri = super.getURI()
            return UriComponentsBuilder.fromUri(uri).replacePath(uri.path.substring(index)).build().toUri()
        }
    }
}

