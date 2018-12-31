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
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType.*
import org.springframework.web.reactive.function.server.RenderingResponse
import org.springframework.web.reactive.function.server.router
import reactor.core.publisher.toMono
import java.util.*


fun websiteRouter(adminHandler: AdminHandler,
                  authenticationHandler: AuthenticationHandler,
                  blogHandler: BlogHandler,
                  globalHandler: GlobalHandler,
                  newsHandler: NewsHandler,
                  talkHandler: TalkHandler,
                  sponsorHandler: SponsorHandler,
                  ticketingHandler: TicketingHandler,
                  userHandler: UserHandler,
                  messageSource: MessageSource,
                  properties: MixitProperties,
                  objectMapper: ObjectMapper,
                  markdownConverter: MarkdownConverter) = router {

    val logger = LoggerFactory.getLogger("websiteRouter")
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
            GET("/$year/hacktivism/favorite/favorite") { talkHandler.findByEventView(year, it, true, "hacktivism") }
            GET("/$year/learn") { talkHandler.findByEventView(year, it, false, "learn") }
            GET("/$year/learn/favorite") { talkHandler.findByEventView(year, it, true, "learn") }
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
        // POST("/ticketing", ticketingHandler::submit)

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
            ok().contentType(TEXT_PLAIN).syncBody("User-agent: *\nDisallow: /")
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


