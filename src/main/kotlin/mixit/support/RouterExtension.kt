package mixit.support

import org.springframework.core.io.Resource
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.RequestPredicates.*
import org.springframework.web.reactive.function.server.RouterFunctions.route
import reactor.core.publisher.Mono
import java.lang.StringBuilder


operator fun String.invoke(init: Path.() -> Unit): Path {
    return path(this, init)
}

fun path(url: String, init: Path.() -> Unit): Path {
    val path = Path(url)
    path.init()
    return path
}

class Path(val path: String) {

    val children = mutableListOf<Path>()
    val routes = mutableListOf<RouterFunction<ServerResponse>>()

    operator fun String.invoke(init: Path.() -> Unit) {
        path(this, init)
    }

    fun path(path: String, init: Path.() -> Unit) {
        val innerPath = Path(this.path + path)
        innerPath.init()
        children += innerPath
    }

    fun GET(f: () -> HandlerFunction<ServerResponse>) {
        routes += route(GET(path), f())
    }

    fun HEAD(f: () -> HandlerFunction<ServerResponse>) {
        routes += route(HEAD(path), f())
    }

    fun POST(f: () -> HandlerFunction<ServerResponse>) {
        routes += route(POST(path), f())
    }

    fun PUT(f: () -> HandlerFunction<ServerResponse>) {
        routes += route(PUT(path), f())
    }

    fun PATCH(f: () -> HandlerFunction<ServerResponse>) {
        routes += route(PATCH(path), f())
    }

    fun DELETE(f: () -> HandlerFunction<ServerResponse>) {
        routes += route(DELETE(path), f())
    }

    fun OPTIONS(f: () -> HandlerFunction<ServerResponse>) {
        routes += route(OPTIONS(path), f())
    }

    fun route(predicate: RequestPredicate, f: () -> HandlerFunction<ServerResponse>) {
        routes += route(path(path).and(predicate), f())
    }

    fun resources(location: Resource) {
        routes +=  RouterFunctions.resources(path, location)
    }

    fun router(): RouterFunction<ServerResponse> {
        return routes().reduce(RouterFunction<*>::and) as RouterFunction<ServerResponse>
    }

    operator fun invoke(request: ServerRequest): Mono<HandlerFunction<ServerResponse>> {
        return router().route(request)
    }

    private fun routes(): List<RouterFunction<ServerResponse>> {
        val allRoutes = mutableListOf<RouterFunction<ServerResponse>>()
        allRoutes += routes
        for (child in children) {
            allRoutes += child.routes()
        }
        return allRoutes
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("$path\n")
        for (child in children) {
            child.toString(sb)
        }
        return sb.toString()
    }

    private fun toString(sb: StringBuilder) {
        sb.append("$path\n")
        for (child in children) {
            child.toString(sb)
        }
    }
}
