package mixit.support.security

import mixit.MixitProperties
import mixit.model.User
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.server.ServerRequest
import java.net.URI
import java.util.*

/**
 * The supported OAuth providers
 */
enum class OAuthProvider {
    GITHUB,
    GOOGLE,
    TWITTER
}

/**
 * The common interface of the OAuth authentications. Since it's a standard, a different implementation is
 * necessary for every provider.
 */
interface OAuth {
    /**
     * Gets the provider of this OAuth service
     */
    fun provider(): OAuthProvider

    /**
     * Gets the service used to call remote provider
     */
    fun providerOauthUri(request: ServerRequest, user: User): URI

    /**
     * Gets the oauth ID from the callback request sent by the OAuth provider.

     * @return the OAuth ID, or empty if the user refused to authenticate
     * @throws BadRequestException if the state or token saved in the first step is invalid
     */
    fun getOAuthId(request: ServerRequest, user: User): Optional<String>

    /**
     * Key to access to the provider
     */
    fun apiKey(): String

    /**
     * Secret code to access to the provider
     */
    fun apiSecret(): String
}

/**
 * Base class for OAuth2 implementations.
 */
abstract class DefaultOAuth(val mixitProperties: MixitProperties) : OAuth {

    /**
     * Returns the URL call by the provider after the authentication
     */
    protected fun callbackUrl(): String = "${mixitProperties.baseUri}/oauth/${provider().name.toLowerCase()}"

}


/**
 * Factory for OAuth implementations
 */
@Service
class OAuthFactory(val twitterOAuth: TwitterOAuth, val googleOAuth: GoogleOAuth, val githubOAuth: GithubOAuth) {

    companion object Factory {
        fun getOAuthProvider(provider: String): OAuthProvider {
            val oAuthProvider = OAuthProvider.values()
                    .filter { v -> v.name.toLowerCase().equals(provider.toLowerCase()) }
                    .firstOrNull()

            if (oAuthProvider == null) {
                throw IllegalArgumentException("Unknown provider")
            }
            return oAuthProvider
        }
    }

    fun create(provider: String): OAuth {
        if (Factory.getOAuthProvider(provider) == OAuthProvider.GOOGLE) {
            return googleOAuth
        }
        if (Factory.getOAuthProvider(provider) == OAuthProvider.GITHUB) {
            return githubOAuth
        }
        return twitterOAuth
    }
}