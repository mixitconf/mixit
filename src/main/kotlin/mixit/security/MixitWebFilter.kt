package mixit.security

import com.google.common.annotations.VisibleForTesting
import kotlinx.coroutines.runBlocking
import mixit.MixitProperties
import mixit.routes.Routes
import mixit.security.model.Credential
import mixit.talk.model.Language
import mixit.ticket.handler.TicketHandler
import mixit.user.model.Role
import mixit.user.model.User
import mixit.user.model.hasValidToken
import mixit.user.repository.UserRepository
import mixit.util.decodeFromBase64
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpHeaders.CONTENT_LANGUAGE
import org.springframework.http.HttpStatus
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import org.springframework.web.server.WebSession
import reactor.core.publisher.Mono
import java.net.URI
import java.util.Locale

@Component
class MixitWebFilter(
    val properties: MixitProperties,
    val userRepository: UserRepository,
    val ticketHandler: TicketHandler
) : WebFilter {

    companion object {
        const val AUTHENT_COOKIE = "XSRF-TOKEN"
        val BOTS = arrayOf("Google", "Bingbot", "Qwant", "Bingbot", "Slurp", "DuckDuckBot", "Baiduspider")
        val WEB_RESSOURCE_EXTENSIONS =
            arrayOf(".css", ".js", ".svg", ".jpg", ".png", ".webp", ".webapp", ".pdf", ".icns", ".ico", ".html")

        const val SESSION_ROLE_KEY = "role"
        const val SESSION_EMAIL_KEY = "email"
        const val SESSION_TOKEN_KEY = "token"
        const val SESSION_LOGIN_KEY = "login"
    }

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> =
        // People who used the old URL are directly redirected
        if (isACallOnOldUrl(exchange)) {
            redirect(exchange, exchange.request.uri.path, HttpStatus.PERMANENT_REDIRECT)
        }
        // For those who arrive at our home page with another language than french, we need to load the site in english
        else if (isAHomePageCallFromForeignLanguage(exchange)) {
            redirect(exchange, "/${Language.ENGLISH.toLanguageTag()}/")
        }
        // For web resource we don't need to know if the user is connected or not
        else if (isWebResource(exchange.request.uri.path)) {
            chain.filter(exchange)
        }
        // For other calls we have to check credentials
        else {
            exchange.session.flatMap {
                runBlocking {
                    filterAndCheckCredentials(
                        exchange,
                        chain,
                        it,
                        readCredentialsFromCookie(exchange.request)
                    )
                }
            }
        }

    /**
     * In this method we try to read user credentials in request cookies
     */
    suspend fun filterAndCheckCredentials(
        exchange: ServerWebExchange,
        chain: WebFilterChain,
        session: WebSession,
        credential: Credential?
    ): Mono<Void> =
        credential
            ?.let { cred ->
                // If session contains credentials we check data
                val user = userRepository.findByNonEncryptedEmail(cred.email) ?: User()
                // We have to see if the token is the good one anf if it is yet valid
                if (user.hasValidToken(cred.token)) {
                    // If user is found we need to restore infos in session
                    session.attributes.let {
                        it[SESSION_ROLE_KEY] = user.role
                        it[SESSION_EMAIL_KEY] = cred.email
                        it[SESSION_TOKEN_KEY] = cred.token
                        it[SESSION_LOGIN_KEY] = user.login
                        filterWithCredential(exchange, chain, user)
                    }
                } else {
                    filterWithCredential(exchange, chain)
                }
            } ?: run {
            // If credentials are not read
            filterWithCredential(exchange, chain)
        }

    private suspend fun filterWithCredential(
        exchange: ServerWebExchange,
        chain: WebFilterChain,
        user: User? = null
    ): Mono<Void> {
        // When a user wants to see a page in english uri path starts with 'en'
        val initUriPath = exchange.request.uri.rawPath
        val languageEn = initUriPath.startsWith("/en/")
        val uriPath = if (languageEn || initUriPath.startsWith("/fr/")) initUriPath.substring(3) else initUriPath

        val req =
            exchange.request.mutate().path(uriPath).header(CONTENT_LANGUAGE, if (languageEn) "en" else "fr").build()

        // If url is securized we have to check the credentials information
        return if (isASecuredUrl(uriPath)) {
            if (isAMixetteQRCodeUrl(uriPath)) {
                // If user is a volunteer or a staff member we continue the route
                if (user == null || !listOf(Role.VOLUNTEER, Role.STAFF).contains(user.role)) {
                    val url = ticketHandler.viewTicket(uriPath)
                    redirect(exchange, url)
                } else {
                    chain.filter(exchange.mutate().request(req).build())
                }
            } else if (user == null) {
                redirect(exchange, "/login")
            } else if (isAnAdminUrl(uriPath) && user.role != Role.STAFF) {
                // If admin page we see if user is a staff member
                redirect(exchange, "/")
            } else if (isAVolunteerUrl(uriPath) && !listOf(Role.VOLUNTEER, Role.STAFF).contains(user.role)) {
                // If volunteer page we have 2 cases.l p
                // The first one is the  see if user is a staff member
                redirect(exchange, "/")
            } else {
                chain.filter(exchange.mutate().request(req).build())
            }
        } else {
            chain.filter(exchange.mutate().request(req).build())
        }
    }

    @VisibleForTesting
    fun readCredentialsFromCookie(request: ServerHttpRequest): Credential? =
        runCatching {
            (request.cookies)[AUTHENT_COOKIE]?.first()?.value?.decodeFromBase64()?.split(":")
                ?.let { if (it.size != 2) null else Credential(it[0], it[1]) }
        }
            .getOrNull()

    @VisibleForTesting
    fun isACallOnOldUrl(exchange: ServerWebExchange): Boolean =
        exchange.request.headers.host?.hostString?.endsWith("mix-it.fr") == true

    @VisibleForTesting
    fun isAHomePageCallFromForeignLanguage(exchange: ServerWebExchange): Boolean = exchange.request.uri.path == "/" &&
        (
        exchange.request.headers.acceptLanguageAsLocales.firstOrNull()
            ?: Locale.FRENCH
        ).language != Language.FRENCH.toLanguageTag() &&
        !isSearchEngineCrawler(exchange.request)

    @VisibleForTesting
    fun isWebResource(initUriPath: String) = WEB_RESSOURCE_EXTENSIONS.any { initUriPath.endsWith(it) }

    @VisibleForTesting
    fun isSearchEngineCrawler(request: ServerHttpRequest) = BOTS.any {
        (request.headers.getFirst(HttpHeaders.USER_AGENT) ?: "").contains(it)
    }

    @VisibleForTesting
    fun isASecuredUrl(path: String) =
        (Routes.securedUrl + Routes.securedAdminUrl + Routes.securedVolunteerUrl).any { path.startsWith(it) }

    @VisibleForTesting
    fun isAnAdminUrl(path: String) =
        Routes.securedAdminUrl.stream().anyMatch { path.startsWith(it) }

    fun isAVolunteerUrl(path: String) =
        Routes.securedVolunteerUrl.stream().anyMatch { path.startsWith(it) }

    fun isAMixetteQRCodeUrl(path: String) =
        path.startsWith(Routes.mixetteQRCode)

    private fun redirect(
        exchange: ServerWebExchange,
        uri: String,
        statusCode: HttpStatus = HttpStatus.TEMPORARY_REDIRECT
    ): Mono<Void> =
        exchange.response.let {
            it.statusCode = statusCode
            it.headers.location = URI("${properties.baseUri}$uri")
            Mono.empty()
        }
}
