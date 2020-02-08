package mixit.web

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import mixit.MixitProperties
import mixit.model.Credential
import mixit.repository.UserRepository
import mixit.util.encodeToBase64
import mixit.web.MixitWebFilter.Companion.AUTENT_COOKIE
import mixit.web.MixitWebFilter.Companion.BOTS
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpCookie
import org.springframework.http.HttpHeaders
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.server.ServerWebExchange
import java.net.InetSocketAddress
import java.net.URI
import java.util.*


@ExtendWith(MockKExtension::class)
class MixitWebFilterTest() {
    @RelaxedMockK
    lateinit var request: ServerHttpRequest

    @RelaxedMockK
    lateinit var exchange: ServerWebExchange

    @RelaxedMockK
    lateinit var properties: MixitProperties

    @RelaxedMockK
    lateinit var userRepository: UserRepository

    @InjectMockKs
    lateinit var filter: MixitWebFilter

    lateinit var headers: HttpHeaders

    @BeforeEach
    fun init() {
        headers = HttpHeaders()
    }

    private fun mockExchangeRequest() {
        every { request.headers } returns headers
        every { exchange.request } returns request
    }

    @Test
    fun `should find if user try to call an old URL or not`() {
        mockExchangeRequest()

        // If no host is defined
        assertThat(filter.isACallOnOldUrl(exchange)).isFalse()

        headers.setHost(InetSocketAddress("http://mix-it.fr", 8080))
        assertThat(filter.isACallOnOldUrl(exchange)).isTrue()

        headers.setHost(InetSocketAddress("https://mixitconf.org", 8080))
        assertThat(filter.isACallOnOldUrl(exchange)).isFalse()
    }

    @Test
    fun `a foreign user should be redirected on english version when he tries to open homepage`() {
        mockExchangeRequest()

        // By default the response is false
        assertThat(filter.isAHomePageCallFromForeignLanguage(exchange)).isFalse()

        // If user come on home page with no language french will be the default
        every { request.uri } returns URI("https", "mixitconf.org", "/", "")
        assertThat(filter.isAHomePageCallFromForeignLanguage(exchange)).isFalse()

        // !if language is not french user has to be redirected
        headers.acceptLanguageAsLocales = arrayListOf(Locale.ENGLISH)
        assertThat(filter.isAHomePageCallFromForeignLanguage(exchange)).isTrue()

        // But if it's a robot we don't want a redirection
        headers.set(HttpHeaders.USER_AGENT, "DuckDuckBot")
        assertThat(filter.isAHomePageCallFromForeignLanguage(exchange)).isFalse()
    }

    @Test
    fun `should read credential from cookies`() {
        val cookies: MultiValueMap<String, HttpCookie> = LinkedMultiValueMap()
        every { request.cookies } returns cookies

        // When request has no cookie credentials are null
        assertThat(filter.readCredentialsFromCookie(request)).isNull()

        // When cookie value is null credentials are null
        cookies.put(AUTENT_COOKIE, null)
        assertThat(filter.readCredentialsFromCookie(request)).isNull()

        // When cookie value is invalid credentials are null
        cookies.put(AUTENT_COOKIE, listOf(HttpCookie(AUTENT_COOKIE, "invalid")))
        assertThat(filter.readCredentialsFromCookie(request)).isNull()

        // When cookie value is valid we have a credential
        cookies.put(AUTENT_COOKIE, listOf(HttpCookie(AUTENT_COOKIE, "email:token".encodeToBase64())))
        assertThat(filter.readCredentialsFromCookie(request)).isEqualTo(Credential("email", "token"))
    }

    @Test
    fun `should detect if resource is an authorized static web resource`() {
        assertThat(filter.isWebResource("/myfile.css")).isTrue()
        assertThat(filter.isWebResource("/myfile.js")).isTrue()
        assertThat(filter.isWebResource("/myfile.svg")).isTrue()
        assertThat(filter.isWebResource("/myfile.jpg")).isTrue()
        assertThat(filter.isWebResource("/myfile.png")).isTrue()
        assertThat(filter.isWebResource("/myfile.webp")).isTrue()
        assertThat(filter.isWebResource("/myfile.webapp")).isTrue()
        assertThat(filter.isWebResource("/myfile.icns")).isTrue()
        assertThat(filter.isWebResource("/myfile.ico")).isTrue()
        assertThat(filter.isWebResource("/myfile.html")).isTrue()

        assertThat(filter.isWebResource("/myfile.xls")).isFalse()
        assertThat(filter.isWebResource("/myfile.cgi")).isFalse()
        assertThat(filter.isWebResource("/api/users")).isFalse()
        assertThat(filter.isWebResource("/talks")).isFalse()
    }

    @Test
    fun `should detect if a robot is the caller`() {
        mockExchangeRequest()

        assertThat(filter.isSearchEngineCrawler(request)).isFalse()

        headers.set(HttpHeaders.USER_AGENT, null)
        assertThat(filter.isSearchEngineCrawler(request)).isFalse()

        BOTS.forEach {
            headers.set(HttpHeaders.USER_AGENT, it)
            assertThat(filter.isSearchEngineCrawler(request)).isTrue()
        }

        headers.set(HttpHeaders.USER_AGENT, "unknown")
        assertThat(filter.isSearchEngineCrawler(request)).isFalse()
    }

}