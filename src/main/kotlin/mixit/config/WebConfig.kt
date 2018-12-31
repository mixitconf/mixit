package mixit.config

import com.samskivert.mustache.Mustache
import mixit.util.MarkdownConverter
import mixit.util.validator.EmailValidator
import mixit.util.validator.MarkdownValidator
import mixit.util.validator.MaxLengthValidator
import mixit.util.validator.UrlValidator
import mixit.web.MixitWebFilter
import mixit.web.StringEscapers
import mixit.web.apiRouter
import mixit.web.handler.*
import mixit.web.websiteRouter
import org.springframework.core.io.ClassPathResource
import org.springframework.fu.kofu.configuration
import org.springframework.fu.kofu.web.mustache
import org.springframework.fu.kofu.web.server
import org.springframework.web.reactive.function.server.RouterFunctions


val webConfig = configuration {
    beans {
        // TODO Uncomment when possible to override beans
        //bean { Mustache.compiler().withEscaper(StringEscapers().HTML).withLoader(ref()) }
        // Use filter when spring-fu#135 will be fixed
        bean { MixitWebFilter(ref(), ref()) }
        bean<AdminHandler>()
        bean<AuthenticationHandler>()
        bean<BlogHandler>()
        bean<EventHandler>()
        bean<FavoriteHandler>()
        bean<GlobalHandler>()
        bean<NewsHandler>()
        bean<SponsorHandler>()
        bean<TalkHandler>()
        bean<TicketingHandler>()
        bean<UserHandler>()
        bean<MarkdownConverter>()
        bean<MaxLengthValidator>()
        bean<MarkdownValidator>()
        bean<UrlValidator>()
    }
    server {
        // TODO Add include variants with more parameter
        include { websiteRouter(ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref()) }
        include { apiRouter(ref(), ref(), ref(), ref(), ref(), ref()) }
        router {
            RouterFunctions.resources("/**", ClassPathResource("static/"))
        }
        codecs {
            jackson()
        }
        mustache()
    }
}
