package mixit.support.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.scribejava.apis.TwitterApi
import com.github.scribejava.core.builder.ServiceBuilder
import com.github.scribejava.core.model.OAuth1RequestToken
import com.github.scribejava.core.model.OAuthRequest
import com.github.scribejava.core.model.Response
import com.github.scribejava.core.model.Verb
import com.github.scribejava.core.oauth.OAuth10aService
import org.springframework.core.env.Environment
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.server.ServerRequest
import java.net.URI
import java.util.*

/**
 * Implementation of Twitter OAuth, using Scribe, based on
 * <a href="https://github.com/fernandezpablo85/scribe-java/blob/master/src/test/java/org/scribe/examples/TwitterExample.java">this example</a>.
 */
@Service
class TwitterOAuth(env: Environment): DefaultOAuth(env) {

    private val TWITTER_TOKEN_ATTRIBUTE = TwitterOAuth::class.java.name + "-token"

    override fun provider(): OAuthProvider = OAuthProvider.TWITTER

    override fun providerOauthUri(request: ServerRequest): URI {
        val service = createService()
        val token = service.requestToken

        request.session().block().attributes[TWITTER_TOKEN_ATTRIBUTE] = token
        return URI(service.getAuthorizationUrl(token))
    }

    override fun getOAuthId(request: ServerRequest): Optional<String> {
        val token = request.queryParam("oauth_token")
        val verifier = request.queryParam("oauth_verifier")
        val sessionToken = request.session().block().getAttribute<OAuth1RequestToken>(TWITTER_TOKEN_ATTRIBUTE)

        if (!token.isPresent || !verifier.isPresent || !sessionToken.isPresent || !sessionToken.get().token.equals(token.get())) {
            return Optional.empty<String>()
        }

        val service = createService()
        val accessToken = service.getAccessToken(sessionToken.get(), verifier.get())
        val oauthRequest = OAuthRequest(Verb.GET, "https://api.twitter.com/1.1/account/verify_credentials.json")

        service.signRequest(accessToken, oauthRequest)

        val response: Response = service.execute(oauthRequest);
        val objectMapper: ObjectMapper = Jackson2ObjectMapperBuilder.json().build()

        val json: Map<String, *> = objectMapper.readValue(response.body)
        val property = json.get("id_str")

        if(property == null){
            return Optional.empty<String>()
        }
        return Optional.of(property.toString())
    }

    private fun createService(): OAuth10aService = ServiceBuilder()
            .apiKey(apiKey())
            .apiSecret(apiSecret())
            .callback(callbackUrl())
            .build(TwitterApi.instance())

}