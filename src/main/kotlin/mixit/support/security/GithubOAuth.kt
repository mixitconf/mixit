package mixit.support.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.scribejava.apis.GitHubApi
import com.github.scribejava.core.builder.ServiceBuilder
import com.github.scribejava.core.model.OAuthRequest
import com.github.scribejava.core.model.Response
import com.github.scribejava.core.model.Verb
import com.github.scribejava.core.oauth.OAuth20Service
import org.springframework.core.env.Environment
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.server.ServerRequest
import java.net.URI
import java.util.*


/**
 * Implementation of Github OAuth, using Scribe, based on
 * <a href="https://github.com/fernandezpablo85/scribe-java/blob/master/src/test/java/org/scribe/examples/TwitterExample.java">this example</a>.
 */
@Service
class GithubOAuth(env: Environment) : DefaultOAuth(env) {

    private val GITHUB_TOKEN_ATTRIBUTE = GithubOAuth::class.java.name + "-token"

    override fun provider(): OAuthProvider = OAuthProvider.GITHUB

    override fun providerOauthUri(request: ServerRequest): URI {
        val secretState = "githubsecret${Random().nextInt(999999)}"
        request.session().block().attributes[GITHUB_TOKEN_ATTRIBUTE] = secretState
        return URI(service(secretState).getAuthorizationUrl())
    }

    override fun getOAuthId(request: ServerRequest): Optional<String> {
        val code = request.queryParam("code")
        val state = request.queryParam("state")
        val secretState = request.session().block().getAttribute<String>(GITHUB_TOKEN_ATTRIBUTE)

        if (!state.isPresent || !code.isPresent || !secretState.isPresent || !secretState.get().equals(state.get())) {
            return Optional.empty<String>()
        }

        val service = service(secretState.get())
        val accessToken = service.getAccessToken(code.get())
        val oauthRequest = OAuthRequest(Verb.GET, "https://api.github.com/user")

        service.signRequest(accessToken, oauthRequest)

        val response: Response = service.execute(oauthRequest);
        val objectMapper: ObjectMapper = Jackson2ObjectMapperBuilder.json().build()

        val json: Map<String, *> = objectMapper.readValue(response.body)
        val property = json.get("id")

        if (property == null) {
            return Optional.empty<String>()
        }
        return Optional.of(property.toString())
    }

    private fun service(secretState: String): OAuth20Service = ServiceBuilder()
            .apiKey(apiKey())
            .apiSecret(apiSecret())
            .state(secretState)
            .callback(callbackUrl())
            .build(GitHubApi.instance())

}