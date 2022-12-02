package mixit.util.errors

import mixit.routes.RouteFilterUtils
import org.springframework.boot.autoconfigure.web.ErrorProperties
import org.springframework.boot.autoconfigure.web.WebProperties
import org.springframework.boot.autoconfigure.web.reactive.error.DefaultErrorWebExceptionHandler
import org.springframework.boot.web.reactive.error.ErrorAttributes
import org.springframework.context.ApplicationContext
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.ServerResponse

class ErrorWebFluxExceptionHandler(
    errorAttributes: ErrorAttributes,
    webProperties: WebProperties,
    errorProperties: ErrorProperties,
    applicationContext: ApplicationContext,
    private val routeFilterUtils: RouteFilterUtils
) :
    DefaultErrorWebExceptionHandler(errorAttributes, webProperties.resources, errorProperties, applicationContext) {

    /**
     * Override routing function to complete generated model
     */
    override fun getRoutingFunction(errorAttributes: ErrorAttributes): RouterFunction<ServerResponse> {
        return super.getRoutingFunction(errorAttributes)
            .filter { request, next -> routeFilterUtils.addModelToResponse(request, next) }
    }
}
