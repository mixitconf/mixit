package mixit.web

import mixit.MixitProperties
import mixit.model.Role
import mixit.repository.UserRepository
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpHeaders.ACCEPT_LANGUAGE
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.net.URI
import java.time.LocalDateTime
import java.util.*
import java.util.stream.Stream


@Component
class MixitWebFilter(val properties: MixitProperties, val userRepository: UserRepository) : WebFilter {

    private val redirectDoneAttribute = "redirectDone"


    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain) =
            if (exchange.request.headers.host?.hostString?.endsWith("mix-it.fr") == true) {
                val response = exchange.response
                response.statusCode = HttpStatus.PERMANENT_REDIRECT
                response.headers.location = URI("${properties.baseUri}${exchange.request.uri.path}")
                Mono.empty()
            } else if (exchange.request.uri.path == "/" &&
                    (exchange.request.headers.acceptLanguageAsLocales.firstOrNull() ?: Locale.FRENCH).language != "fr" &&
                    !isSearchEngineCrawler(exchange)) {
                val response = exchange.response
                exchange.session.flatMap {
                    if (it.attributes[redirectDoneAttribute] == true)
                        chain.filter(exchange.mutate().request(exchange.request.mutate().header(ACCEPT_LANGUAGE, "fr").build()).build())
                    else {
                        response.statusCode = HttpStatus.TEMPORARY_REDIRECT
                        response.headers.location = URI("${properties.baseUri}/en/")
                        it.attributes[redirectDoneAttribute] = true
                        it.save()
                    }
                }
            } else {
                // When a user wants to see a page in english uri path starts with 'en'
                val languageEn = exchange.request.uri.path.startsWith("/en/")
                val uriPath = if (languageEn) exchange.request.uri.path.substring(3) else exchange.request.uri.path

                // If url is securized we hav eto check the credentials information
                if (startWithSecuredUrl(uriPath)) {
                    exchange.session.flatMap {
                        if (it.attributes["username"] != null && it.attributes["token"] != null) {
                            userRepository.findByEmail(it.attributes["username"]!!.toString())
                                    .flatMap { user ->
                                        // If user is find, token must to be the good one and must be valid
                                        if (user.token.equals(it.attributes["token"]!!.toString()) && user.tokenExpiration.isAfter(LocalDateTime.now())) {
                                            if (startWithAdminSecuredUrl(uriPath) && user.role != Role.STAFF && !user.email.equals(properties.admin)){
                                                redirectForLogin(exchange, "")
                                            }
                                            else {
                                                chain.filter(exchange.mutate().request(exchange.request.mutate()
                                                        .path(uriPath)
                                                        .header(ACCEPT_LANGUAGE, if (languageEn) "en" else "fr").build())
                                                        .build())
                                            }
                                        } else {
                                            redirectForLogin(exchange, "login")
                                        }
                                    }
                                    .switchIfEmpty(redirectForLogin(exchange, "login"))
                        } else {
                            redirectForLogin(exchange, "login")
                        }
                    }
                } else {
                    chain.filter(exchange.mutate().request(exchange.request.mutate()
                            .path(uriPath)
                            .header(ACCEPT_LANGUAGE, if (languageEn) "en" else "fr").build())
                            .build())
                }
            }


    private fun redirectForLogin(exchange: ServerWebExchange, uri: String): Mono<Void> {
        val response = exchange.response
        response.statusCode = HttpStatus.TEMPORARY_REDIRECT
        response.headers.location = URI("${properties.baseUri}/${uri}")
        return Mono.empty()
    }

    private fun startWithSecuredUrl(path: String): Boolean {
        return Stream.concat(WebsiteRoutes.securedUrl.stream(), WebsiteRoutes.securedAdminUrl.stream()).anyMatch { path.startsWith(it) }
    }

    private fun startWithAdminSecuredUrl(path: String): Boolean {
        return WebsiteRoutes.securedAdminUrl.stream().anyMatch { path.startsWith(it) }
    }

    private fun isSearchEngineCrawler(exchange: ServerWebExchange): Boolean {
        val userAgent = exchange.request.headers.getFirst(HttpHeaders.USER_AGENT) ?: ""
        val bots = arrayOf("Google", "Bingbot", "Qwant", "Bingbot", "Slurp", "DuckDuckBot", "Baiduspider")
        return bots.any { userAgent.contains(it) }
    }
}

