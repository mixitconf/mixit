package mixit.util

import org.springframework.web.reactive.function.server.*

abstract class RouterFunctionProvider : () -> RouterFunction<ServerResponse> {

	override fun invoke(): RouterFunction<ServerResponse> = RouterDsl().apply(routes).router()

	abstract val routes: Routes

}