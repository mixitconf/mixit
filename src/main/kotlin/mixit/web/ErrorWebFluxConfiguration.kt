package mixit.web

import mixit.MixitProperties
import mixit.util.MarkdownConverter
import mixit.web.handler.ErrorWebFluxExceptionHandler
import org.springframework.boot.autoconfigure.web.ResourceProperties
import org.springframework.boot.autoconfigure.web.ServerProperties
import org.springframework.boot.web.reactive.error.ErrorAttributes
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler
import org.springframework.context.ApplicationContext
import org.springframework.context.MessageSource
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
class ErrorWebFluxConfiguration(private val serverProperties: ServerProperties,
                                private val applicationContext: ApplicationContext,
                                private val resourceProperties: ResourceProperties,
                                private val viewResolvers: List<ViewResolver>,
                                private val serverCodecConfigurer: ServerCodecConfigurer) {


    @Bean
    @Primary
    @Order(-1)
    fun errorWebExceptionHandler(errorAttributes: ErrorAttributes, messageSource: MessageSource, properties: MixitProperties, markdownConverter: MarkdownConverter): ErrorWebExceptionHandler {
        val exceptionHandler = ErrorWebFluxExceptionHandler(
                errorAttributes,
                this.resourceProperties,
                this.serverProperties.getError(),
                this.applicationContext,
                messageSource,
                properties,
                markdownConverter)

        exceptionHandler.setViewResolvers(this.viewResolvers)
        exceptionHandler.setMessageWriters(this.serverCodecConfigurer.getWriters())
        exceptionHandler.setMessageReaders(this.serverCodecConfigurer.getReaders())
        return exceptionHandler
    }
}