package mixit.about

import mixit.util.json
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.body
import reactor.core.publisher.Mono

@Component
class ServiceDiscoveryHandler(private val client: DiscoveryClient) {

    fun serviceInstancesByApplicationName(req: ServerRequest): Mono<ServerResponse> =
        ok().json().body(Mono.justOrEmpty(client.getInstances(req.pathVariable("application"))))
}
