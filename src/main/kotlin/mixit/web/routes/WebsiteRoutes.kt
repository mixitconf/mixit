package mixit.web

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mixit.MixitProperties
import mixit.model.Event
import mixit.model.SponsorshipLevel
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
import org.springframework.web.reactive.function.server.ServerResponse
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
                    private val userHandler: UserHandler,
                    private val messageSource: MessageSource,
                    private val properties: MixitProperties,
                    private val objectMapper: ObjectMapper,
                    private val markdownConverter: MarkdownConverter) {

    private val logger = LoggerFactory.getLogger(WebsiteRoutes::class.java)

    companion object {
        val securedAdminUrl: List<String> = listOf("/admin", "/api/admin")
        val securedUrl: List<String> = listOf("/me")
    }

    @Bean
    fun websiteRouter() = router {
        GET("/blog/feed", blogHandler::feed)

        accept(TEXT_HTML).nest {
            GET("/") { sponsorHandler.viewWithSponsors("home", SponsorshipLevel.GOLD, null, 2019, it) }
            GET("/about", globalHandler::findAboutView)
            GET("/news", newsHandler::newsView)
            GET("/ticketing", ticketingHandler::ticketing)
            GET("/sponsors") { sponsorHandler.viewWithSponsors(2019, it) }
            GET("/mixteen") { sponsorHandler.viewWithSponsors("mixteen", SponsorshipLevel.MIXTEEN, "mixteen.title", 2019, it) }
            GET("/faq", globalHandler::faqView)
            GET("/come", globalHandler::comeToMixitView)
            GET("/schedule", globalHandler::scheduleView)
            GET("/cfp") { talkHandler.findByEventView(2019, it, false) }
            GET("/user/{login}") { userHandler.findOneView(it) }
            GET("/me") { userHandler.findProfileView(it) }
            GET("/me/edit", userHandler::editProfileView)
            GET("/me/talks/edit/{slug}", talkHandler::editTalkView)
            GET("/search") { globalHandler.findFullTextView(it) }
            GET("/speaker", globalHandler::speakerView)

            // Authentication
            GET("/login", authenticationHandler::loginView)
            GET("/disconnect", authenticationHandler::logout)
            GET("/signin/{token}/{email:.*}", authenticationHandler::signInViaUrl)

            val eventsResource = ClassPathResource("data/events.json")
            val events: List<Event> = objectMapper.readValue(eventsResource.inputStream)

            // Sponsors
            events.map { it.year }.forEach { year ->
                GET("/sponsors/$year") { sponsorHandler.viewWithSponsors(year, it) }
            }

            // Talks
            events.map { it.year }.forEach { year ->
                GET("/$year") { talkHandler.findByEventView(year, it, false) }
                GET("/$year/favorite") { talkHandler.findByEventView(year, it, true) }
                GET("/$year/makers") { talkHandler.findByEventView(year, it, false, "makers") }
                GET("/$year/makers/favorite") { talkHandler.findByEventView(year, it, true, "makers") }
                GET("/$year/aliens") { talkHandler.findByEventView(year, it, false, "aliens") }
                GET("/$year/aliens/favorite") { talkHandler.findByEventView(year, it, true, "aliens") }
                GET("/$year/tech") { talkHandler.findByEventView(year, it, false, "tech") }
                GET("/$year/tech/favorite") { talkHandler.findByEventView(year, it, true, "tech") }
                GET("/$year/design") { talkHandler.findByEventView(year, it, false, "design") }
                GET("/$year/design/favorite") { talkHandler.findByEventView(year, it, true, "design") }
                GET("/$year/hacktivism") { talkHandler.findByEventView(year, it, false, "hacktivism") }
                GET("/$year/hacktivism/favorite") { talkHandler.findByEventView(year, it, true, "hacktivism") }
                GET("/$year/learn") { talkHandler.findByEventView(year, it, false, "learn") }
                GET("/$year/learn/favorite") { talkHandler.findByEventView(year, it, true, "learn") }
                GET("/$year/ethics") { talkHandler.findByEventView(year, it, false, "ethics") }
                GET("/$year/ethics/favorite") { talkHandler.findByEventView(year, it, true, "ethics") }
                GET("/$year/lifestyle") { talkHandler.findByEventView(year, it, false, "lifestyle") }
                GET("/$year/lifestyle/favorite") { talkHandler.findByEventView(year, it, true, "lifestyle") }
                GET("/$year/team") { talkHandler.findByEventView(year, it, false, "team") }
                GET("/$year/team/favorite") { talkHandler.findByEventView(year, it, true, "team") }
                GET("/$year/medias") { talkHandler.findMediaByEventView(year, it, false) }
                GET("/$year/medias/favorite") { talkHandler.findMediaByEventView(year, it, true) }
                GET("/$year/medias/{topic}") { talkHandler.findMediaByEventView(year, it, false, it.pathVariable("topic")) }
                GET("/$year/medias/{topic}/favorite") { talkHandler.findMediaByEventView(year, it, true, it.pathVariable("topic")) }
                GET("/$year/{slug}") { talkHandler.findOneView(year, it) }
            }

            "/admin".nest {
                GET("/", adminHandler::admin)
                GET("/ticketing", adminHandler::adminTicketing)
                DELETE("/")
                GET("/talks/edit/{slug}", adminHandler::editTalk)
                GET("/talks") { adminHandler.adminTalks(it, "2019")}
                GET("/talks/create", adminHandler::createTalk)
                GET("/talks/{year}") { adminHandler.adminTalks(it, it.pathVariable("year"))}
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
            POST("/me", userHandler::saveProfile)
            POST("/me/talks", talkHandler::saveProfileTalk)
            POST("/search") { globalHandler.searchFullTextView(it) }
            POST("/ticketing", ticketingHandler::submit)

            "/admin".nest {
                POST("/talks", adminHandler::adminSaveTalk)
                POST("/talks/delete", adminHandler::adminDeleteTalk)
                POST("/users", adminHandler::adminSaveUser)
                POST("/users/delete", adminHandler::adminDeleteUser)
                POST("/events", adminHandler::adminSaveEvent)
                POST("/events/{eventId}/sponsors/create", adminHandler::adminCreateEventSponsoring)
                POST("/events/{eventId}/sponsors/delete", adminHandler::adminDeleteEventSponsoring)
                POST("/events/{eventId}/sponsors", adminHandler::adminUpdateEventSponsoring)
                POST("/ticketing/delete", adminHandler::adminDeleteTicketing)
                POST("/post", adminHandler::adminSavePost)
                POST("/post/delete", adminHandler::adminDeletePost)
            }
        }

        if (properties.baseUri != "https://mixitconf.org") {
            logger.warn("SEO disabled via robots.txt because ${properties.baseUri} baseUri is not the production one (https://mixitconf.org)")
            GET("/robots.txt") {
                ServerResponse.ok().contentType(TEXT_PLAIN).syncBody("User-agent: *\nDisallow: /")
            }
        }
    }.filter { request, next ->
        val locale: Locale = request.locale()
        val path = request.uri().path
		request.session().flatMap { session ->
			val model = generateModel(properties, path, locale, session, messageSource, markdownConverter)
			next.handle(request).flatMap { if (it is RenderingResponse) RenderingResponse.from(it).modelAttributes(model).build() else it.toMono() }
		}
    }

    @Bean
    @DependsOn("websiteRouter")
    fun resourceRouter() = resources("/**", ClassPathResource("static/"))

}

