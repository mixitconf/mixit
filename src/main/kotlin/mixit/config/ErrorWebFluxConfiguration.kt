package mixit.config

import mixit.routes.RouteFilterUtils
import mixit.util.errors.ErrorWebFluxExceptionHandler
import org.springframework.boot.autoconfigure.web.ServerProperties
import org.springframework.boot.autoconfigure.web.WebProperties
import org.springframework.boot.web.reactive.error.ErrorAttributes
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.annotation.Order
import org.springframework.http.codec.ServerCodecConfigurer
import org.springframework.web.reactive.result.view.ViewResolver

/**
 * @author Dev-Mind <guillaume@dev-mind.fr>
 * @since 13/12/17.
 */
@Configuration
class ErrorWebFluxConfiguration(
    private val serverProperties: ServerProperties,
    private val applicationContext: ApplicationContext,
    private val webProperties: WebProperties,
    private val viewResolvers: List<ViewResolver>,
    private val serverCodecConfigurer: ServerCodecConfigurer
) {

    @Bean
    @Primary
    @Order(-1)
    fun errorWebExceptionHandler(
        errorAttributes: ErrorAttributes,
        routeFilterUtils: RouteFilterUtils
    ): ErrorWebExceptionHandler {
        val exceptionHandler = ErrorWebFluxExceptionHandler(
            errorAttributes,
            this.webProperties,
            this.serverProperties.error,
            this.applicationContext,
            routeFilterUtils
        )

        exceptionHandler.setViewResolvers(this.viewResolvers)
        exceptionHandler.setMessageWriters(this.serverCodecConfigurer.writers)
        exceptionHandler.setMessageReaders(this.serverCodecConfigurer.readers)
        return exceptionHandler
    }
}
