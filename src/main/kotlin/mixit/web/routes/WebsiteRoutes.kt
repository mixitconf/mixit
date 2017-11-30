package mixit.web

import mixit.MixitProperties
import mixit.repository.EventRepository
import mixit.util.MarkdownConverter
import mixit.util.locale
import mixit.web.handler.*
import org.slf4j.LoggerFactory
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.DependsOn
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType.*
import org.springframework.web.reactive.function.server.RenderingResponse
import org.springframework.web.reactive.function.server.RouterFunctions.resources
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.router
import reactor.core.publisher.toMono
import java.util.*


@Configuration
class WebsiteRoutes(private val adminHandler: AdminHandler,
                    private val authenticationHandler: AuthenticationHandler,
                    private val blogHandler: BlogHandler,
                    private val globalHandler: GlobalHandler,
                    private val newsHandler: NewsHandler,
                    private val talkHandler: TalkHandler,
                    private val sponsorHandler: SponsorHandler,
                    private val ticketingHandler: TicketingHandler,
                    private val messageSource: MessageSource,
                    private val properties: MixitProperties,
                    private val eventRepository: EventRepository,
                    private val markdownConverter: MarkdownConverter) {

    private val logger = LoggerFactory.getLogger(WebsiteRoutes::class.java)

    companion object {
        val securedAdminUrl: List<String> = listOf("/admin")
        val securedUrl: List<String> = listOf("/schedule", "/favorites")
    }

    @Bean
    @DependsOn("databaseInitializer")
    fun websiteRouter() = router {
        GET("/blog/feed", blogHandler::feed)

        accept(TEXT_HTML).nest {
            GET("/") { sponsorHandler.viewWithSponsors("home", null, 2018, false, it) }
            GET("/about", globalHandler::findAboutView)
            GET("/news", newsHandler::newsView)
            GET("/ticketing", ticketingHandler::ticketing)
            GET("/sponsors") { sponsorHandler.viewWithSponsors("sponsors", "sponsors.title", 2018, false, it) }
            GET("/mixteen",  { sponsorHandler.viewWithSponsors("mixteen", "mixteen.title", 2018, false, it) })
            GET("/faq", globalHandler::faqView)
            GET("/come", globalHandler::comeToMixitView)
            GET("/schedule", globalHandler::scheduleView)

            // Authentication
            GET("/login", authenticationHandler::loginView)
            GET("/disconnect", authenticationHandler::logout)

            // Sponsors
            eventRepository.findAll().toIterable().map { it.year }.forEach { year ->
                GET("/sponsors/$year") { sponsorHandler.viewWithSponsors("sponsors", "sponsors.title", year, false, it) }
            }

            // Talks
            eventRepository.findAll().toIterable().map { it.year }.forEach { year ->
                GET("/$year") { talkHandler.findByEventView(year, it) }
                GET("/$year/makers") { talkHandler.findByEventView(year, it, "makers") }
                GET("/$year/aliens") { talkHandler.findByEventView(year, it, "aliens") }
                GET("/$year/tech") { talkHandler.findByEventView(year, it, "tech") }
                GET("/$year/design") { talkHandler.findByEventView(year, it, "design") }
                GET("/$year/hacktivism") { talkHandler.findByEventView(year, it, "hacktivism") }
                GET("/$year/learn") { talkHandler.findByEventView(year, it, "learn") }
                GET("/$year/medias") { talkHandler.findMediaByEventView(year, it) }
                GET("/$year/medias/{topic}") { talkHandler.findMediaTopicByEventView(year, it) }
                GET("/$year/{slug}") { talkHandler.findOneView(year, it) }
            }

            "/admin".nest {
                GET("/", adminHandler::admin)
                GET("/ticketing", adminHandler::adminTicketing)
                GET("/talks", adminHandler::adminTalks)
                DELETE("/")
                GET("/talks/edit/{slug}", adminHandler::editTalk)
                GET("/talks/create", adminHandler::createTalk)
                GET("/users", adminHandler::adminUsers)
                GET("/users/edit/{login}", adminHandler::editUser)
                GET("/users/create", adminHandler::createUser)
                GET("/events", adminHandler::adminEvents)
                GET("/events/edit/{eventId}", adminHandler::editEvent)
                GET("/events/{eventId}/sponsors/edit/{sponsorId}/{level}", adminHandler::editEventSponsoring)
                GET("/events/{eventId}/sponsors/create", adminHandler::createEventSponsoring)
                GET("/events/create", adminHandler::createEvent)
                GET("/blog", adminHandler::adminBlog)
                GET("/post/edit/{id}", adminHandler::editPost)
                GET("/post/create", adminHandler::createPost)
            }

            "/blog".nest {
                GET("/", blogHandler::findAllView)
                GET("/{slug}", blogHandler::findOneView)
            }
        }

        accept(TEXT_EVENT_STREAM).nest {
            GET("/news/sse", newsHandler::newsSse)
        }

        contentType(APPLICATION_FORM_URLENCODED).nest {
            POST("/login", authenticationHandler::login)
            POST("/signup", authenticationHandler::signUp)
            POST("/signin", authenticationHandler::signIn)

            //POST("/ticketing", ticketingHandler::submit)
            "/admin".nest {
                POST("/talks", adminHandler::adminSaveTalk)
                POST("/talks/delete", adminHandler::adminDeleteTalk)
                POST("/users", adminHandler::adminSaveUser)
                POST("/users/delete", adminHandler::adminDeleteUser)
                POST("/events", adminHandler::adminSaveEvent)
                POST("/events/{eventId}/sponsors/create", adminHandler::adminCreateEventSponsoring)
                POST("/events/{eventId}/sponsors/delete", adminHandler::adminDeleteEventSponsoring)
                POST("/events/{eventId}/sponsors", adminHandler::adminUpdateEventSponsoring)
                POST("/post", adminHandler::adminSavePost)
                POST("/post/delete", adminHandler::adminDeletePost)
            }
        }

        if (properties.baseUri != "https://mixitconf.org") {
            logger.warn("SEO disabled via robots.txt because ${properties.baseUri} baseUri is not the production one (https://mixitconf.org)")
            GET("/robots.txt") {
                ok().contentType(TEXT_PLAIN).syncBody("User-agent: *\nDisallow: /")
            }
        }
    }.filter { request, next ->
        val locale: Locale = request.locale()
        val session = request.session().block()!!
        val path = request.uri().path
        val model = generateModel(properties, path, locale, session, messageSource, markdownConverter)
        next.handle(request).flatMap { if (it is RenderingResponse) RenderingResponse.from(it).modelAttributes(model).build() else it.toMono() }
    }

    @Bean
    @DependsOn("websiteRouter")
    fun resourceRouter() = resources("/**", ClassPathResource("static/"))

}

