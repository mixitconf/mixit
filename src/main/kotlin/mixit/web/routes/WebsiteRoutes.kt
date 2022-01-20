package mixit.web.routes

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.util.Locale
import mixit.MixitProperties
import mixit.model.Event
import mixit.model.SponsorshipLevel
import mixit.util.MarkdownConverter
import mixit.util.locale
import mixit.web.generateModel
import mixit.web.handler.admin.AdminUserHandler
import mixit.web.handler.security.AuthenticationHandler
import mixit.web.handler.GlobalHandler
import mixit.web.handler.admin.AdminEventHandler
import mixit.web.handler.admin.AdminPostHandler
import mixit.web.handler.admin.AdminTalkHandler
import mixit.web.handler.admin.AdminTicketingHandler
import mixit.web.handler.admin.AdminUtils
import mixit.web.handler.blog.BlogHandler
import mixit.web.handler.mailing.MailingHandler
import mixit.web.handler.user.SponsorHandler
import mixit.web.handler.user.TalkHandler
import mixit.web.handler.ticketing.TicketingHandler
import mixit.web.handler.user.UserHandler
import org.slf4j.LoggerFactory
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.DependsOn
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED
import org.springframework.http.MediaType.TEXT_HTML
import org.springframework.http.MediaType.TEXT_PLAIN
import org.springframework.web.reactive.function.server.RenderingResponse
import org.springframework.web.reactive.function.server.RouterFunctions.resources
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.router
import reactor.kotlin.core.publisher.toMono

@Configuration
class WebsiteRoutes(
    private val adminEventHandler: AdminEventHandler,
    private val adminTicketingHandler: AdminTicketingHandler,
    private val adminTalkHandler: AdminTalkHandler,
    private val adminUserHandler: AdminUserHandler,
    private val adminPostHandler: AdminPostHandler,
    private val authenticationHandler: AuthenticationHandler,
    private val blogHandler: BlogHandler,
    private val globalHandler: GlobalHandler,
    private val talkHandler: TalkHandler,
    private val sponsorHandler: SponsorHandler,
    private val ticketingHandler: TicketingHandler,
    private val mailingHandler: MailingHandler,
    private val userHandler: UserHandler,
    private val messageSource: MessageSource,
    private val properties: MixitProperties,
    private val objectMapper: ObjectMapper,
    private val markdownConverter: MarkdownConverter
) {

    private val logger = LoggerFactory.getLogger(WebsiteRoutes::class.java)

    @Bean
    fun websiteRouter() = router {
        GET("/blog/feed", blogHandler::feed)

        accept(TEXT_HTML).nest {
            GET("/") { sponsorHandler.viewWithSponsors("home", arrayOf(SponsorshipLevel.LANYARD, SponsorshipLevel.GOLD), null, 2022, it) }
            GET("/about", globalHandler::findAboutView)
            GET("/ticketing", ticketingHandler::ticketing)
            GET("/sponsors") { sponsorHandler.viewWithSponsors(2022, it) }
            GET("/mixteen") { sponsorHandler.viewWithSponsors("mixteen", arrayOf(SponsorshipLevel.MIXTEEN), "mixteen.title", 2021, it) }
            GET("/faq", globalHandler::faqView)
            GET("/come", globalHandler::comeToMixitView)
            GET("/schedule", globalHandler::scheduleView)
            GET("/cfp") { talkHandler.findByEventView(2021, it, false) }
            GET("/user/{login}") { userHandler.findOneView(it) }
            GET("/me") { userHandler.findProfileView(it) }
            GET("/me/edit", userHandler::editProfileView)
            GET("/me/talks/edit/{slug}", talkHandler::editTalkView)
            GET("/search") { globalHandler.findFullTextView(it) }
            GET("/speaker", globalHandler::speakerView)
            GET("/accessibility", globalHandler::accessibilityView)
            GET("/codeofconduct", globalHandler::codeConductView)

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
                GET("/", AdminUtils::admin)
                GET("/ticketing", adminTicketingHandler::adminTicketing)
                GET("/mailings", mailingHandler::listMailing)
                GET("/mailings/create", mailingHandler::createMailing)
                GET("/mailings/edit/{id}", mailingHandler::editMailing)
                DELETE("/")
                GET("/talks/edit/{slug}", adminTalkHandler::editTalk)
                GET("/talks") { adminTalkHandler.adminTalks(it, "2021") }
                GET("/talks/create", adminTalkHandler::createTalk)
                GET("/talks/{year}") { adminTalkHandler.adminTalks(it, it.pathVariable("year")) }
                GET("/users", adminUserHandler::adminUsers)
                GET("/users/edit/{login}", adminUserHandler::editUser)
                GET("/users/create", adminUserHandler::createUser)
                GET("/events", adminEventHandler::adminEvents)
                GET("/events/edit/{eventId}", adminEventHandler::editEvent)
                GET("/events/{eventId}/sponsors/edit/{sponsorId}/{level}", adminEventHandler::editEventSponsoring)
                GET("/events/{eventId}/sponsors/create", adminEventHandler::createEventSponsoring)
                GET("/events/{eventId}/organizations/edit/{organizationLogin}", adminEventHandler::editEventOrganization)
                GET("/events/{eventId}/organizations/create", adminEventHandler::createEventOrganization)
                GET("/events/{eventId}/volunteers/edit/{organizationLogin}", adminEventHandler::editEventVolunteer)
                GET("/events/{eventId}/volunteers/create", adminEventHandler::createEventVolunteer)
                GET("/events/create", adminEventHandler::createEvent)
                GET("/blog", adminPostHandler::adminBlog)
                GET("/post/edit/{id}", adminPostHandler::editPost)
                GET("/post/create", adminPostHandler::createPost)
            }

            "/blog".nest {
                GET("/", blogHandler::findAllView)
                GET("/{slug}", blogHandler::findOneView)
            }
        }

        contentType(APPLICATION_FORM_URLENCODED).nest {
            POST("/login", authenticationHandler::login)
            POST("/send-token", authenticationHandler::sendToken)
            POST("/signup", authenticationHandler::signUp)
            POST("/signin", authenticationHandler::signIn)
            POST("/me", userHandler::saveProfile)
            POST("/me/talks", talkHandler::saveProfileTalk)
            POST("/search") { globalHandler.searchFullTextView(it) }
            POST("/ticketing", ticketingHandler::submit)

            "/admin".nest {
                POST("/talks", adminTalkHandler::adminSaveTalk)
                POST("/talks/delete", adminTalkHandler::adminDeleteTalk)
                POST("/users", adminUserHandler::adminSaveUser)
                POST("/users/delete", adminUserHandler::adminDeleteUser)
                POST("/events", adminEventHandler::adminSaveEvent)
                POST("/events/{eventId}/sponsors/create", adminEventHandler::adminCreateEventSponsoring)
                POST("/events/{eventId}/sponsors/delete", adminEventHandler::adminDeleteEventSponsoring)
                POST("/events/{eventId}/sponsors", adminEventHandler::adminUpdateEventSponsoring)
                POST("/events/{eventId}/organizations/create", adminEventHandler::adminCreateEventOrganization)
                POST("/events/{eventId}/organizations/delete", adminEventHandler::adminDeleteEventOrganization)
                POST("/events/{eventId}/organizations", adminEventHandler::adminUpdateEventOrganization)
                POST("/events/{eventId}/volunteers/create", adminEventHandler::adminCreateEventVolunteer)
                POST("/events/{eventId}/volunteers/delete", adminEventHandler::adminDeleteEventVolunteer)
                POST("/events/{eventId}/volunteers", adminEventHandler::adminUpdateEventVolunteer)
                POST("/ticketing/delete", adminTicketingHandler::adminDeleteTicketing)
                POST("/post", adminPostHandler::adminSavePost)
                POST("/post/delete", adminPostHandler::adminDeletePost)
                POST("/mailings/preview", mailingHandler::previewMailing)
                POST("/mailings", mailingHandler::saveMailing)
                POST("/mailings/send", mailingHandler::sendMailing)
                POST("/mailings/delete", mailingHandler::deleteMailing)
            }
        }

        if (properties.baseUri != "https://mixitconf.org") {
            logger.warn("SEO disabled via robots.txt because ${properties.baseUri} baseUri is not the production one (https://mixitconf.org)")
            GET("/robots.txt") {
                ServerResponse.ok().contentType(TEXT_PLAIN).bodyValue("User-agent: *\nDisallow: /")
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
