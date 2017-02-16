package mixit.support

import org.springframework.web.reactive.function.server.*
import reactor.core.publisher.Mono

abstract class LazyRouterFunction : RouterFunction<ServerResponse> {

	val routerFunction: RouterFunction<ServerResponse> by lazy {
		Routes().apply(routes).router()
	}

	abstract val routes: Routes.() -> Unit

	override fun route(request: ServerRequest): Mono<HandlerFunction<ServerResponse>> {
		return routerFunction.route(request)
	}
}