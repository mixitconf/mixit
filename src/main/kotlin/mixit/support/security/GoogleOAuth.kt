package mixit.support.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.scribejava.apis.GoogleApi20
import com.github.scribejava.core.builder.ServiceBuilder
import com.github.scribejava.core.oauth.OAuth20Service
import mixit.MixitProperties
import mixit.model.User
import mixit.util.md5
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.server.ServerRequest
import java.net.URI
import java.util.*


/**
 * Implementation of Google OAuth, using Scribe, based on
 * <a href="https://github.com/fernandezpablo85/scribe-java/blob/master/src/test/java/org/scribe/examples/TwitterExample.java">this example</a>.
 */
@Service
class GoogleOAuth(mixitProperties: MixitProperties): DefaultOAuth(mixitProperties) {

    override fun apiKey(): String = mixitProperties.oauth.google.apiKey!!

    override fun apiSecret(): String = mixitProperties.oauth.google.clientSecret!!

    override fun provider(): OAuthProvider = OAuthProvider.GOOGLE

    override fun providerOauthUri(request: ServerRequest, user: User): URI {
        val additionalParams = HashMap<String, String>()
        additionalParams.put("login_hint", "email")

        return URI(service("googlesecret${user.email.md5()}").getAuthorizationUrl(additionalParams))
    }

    override fun getOAuthId(request: ServerRequest, user: User): Optional<String> {
        val objectMapper: ObjectMapper = Jackson2ObjectMapperBuilder.json().build()

        val code = request.queryParam("code")
        val state = request.queryParam("state")
        val secretState = "googlesecret${user.email.md5()}"

        if (!state.isPresent || !code.isPresent || !secretState.equals(state.get())) {
            return Optional.empty<String>()
        }

        var accessToken = service(secretState).getAccessToken(code.get())

        // The accessToken contains the JWT token
        val json: Map<String, String> = objectMapper.readValue(accessToken.rawResponse)
        val jwt = json.get("id_token")

        // We need to decode the JWT token to read the user id
        val json2 = Base64.getDecoder().decode(jwt!!.split(".").get(1))
        val userInformation: Map<String, String> = objectMapper.readValue(json2)


        if(userInformation.get("sub") == null){
            return Optional.empty<String>()
        }
        return Optional.of(userInformation.get("sub")!!)
    }

    private fun service(secretState: String): OAuth20Service = ServiceBuilder()
            .apiKey(apiKey())
            .apiSecret(apiSecret())
            .state(secretState)
            .scope("openid")
            .callback(callbackUrl())
            .build(GoogleApi20.instance())



}