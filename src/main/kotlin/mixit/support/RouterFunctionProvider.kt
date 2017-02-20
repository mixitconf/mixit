package mixit.support

import org.springframework.web.reactive.function.server.*

abstract class RouterFunctionProvider : () -> RouterFunction<ServerResponse> {

	override fun invoke(): RouterFunction<ServerResponse> = Routes().apply(routes).router()

	abstract val routes: Routes.() -> Unit

}