package mixit.routes

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mixit.MixitProperties
import mixit.about.AboutHandler
import mixit.about.SearchHandler
import mixit.blog.handler.AdminPostHandler
import mixit.blog.handler.WebBlogHandler
import mixit.event.handler.AdminEventHandler
import mixit.event.handler.AdminEventHandler.Companion.CURRENT_EVENT
import mixit.event.model.Event
import mixit.event.model.SponsorshipLevel
import mixit.mailing.handler.MailingHandler
import mixit.mixette.handler.AdminMixetteHandler
import mixit.security.handler.AuthenticationHandler
import mixit.talk.handler.AdminTalkHandler
import mixit.talk.handler.TalkHandler
import mixit.ticket.handler.AdminLotteryHandler
import mixit.ticket.handler.AdminTicketHandler
import mixit.ticket.handler.LotteryHandler
import mixit.user.handler.AdminUserHandler
import mixit.user.handler.SponsorHandler
import mixit.user.handler.UserHandler
import mixit.util.AdminHandler
import mixit.util.cache.CacheHandler
import mixit.util.locale
import mixit.util.web.generateModel
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
import java.util.Locale

@Configuration
class WebsiteRoutes(
    private val adminHandler: AdminHandler,
    private val cacheHandler: CacheHandler,
    private val adminEventHandler: AdminEventHandler,
    private val adminLotteryHandler: AdminLotteryHandler,
    private val adminTalkHandler: AdminTalkHandler,
    private val adminUserHandler: AdminUserHandler,
    private val adminPostHandler: AdminPostHandler,
    private val adminMixetteHandler: AdminMixetteHandler,
    private val authenticationHandler: AuthenticationHandler,
    private val blogHandler: WebBlogHandler,
    private val aboutHandler: AboutHandler,
    private val searchHandler: SearchHandler,
    private val talkHandler: TalkHandler,
    private val sponsorHandler: SponsorHandler,
    private val lotteryHandler: LotteryHandler,
    private val ticketHandler: AdminTicketHandler,
    private val mailingHandler: MailingHandler,
    private val userHandler: UserHandler,
    private val messageSource: MessageSource,
    private val properties: MixitProperties,
    private val objectMapper: ObjectMapper
) {

    private val logger = LoggerFactory.getLogger(WebsiteRoutes::class.java)

    @Bean
    fun websiteRouter() = router {
        GET("/blog/feed", blogHandler::feed)

        accept(TEXT_HTML).nest {
            GET("/") {
                sponsorHandler.viewWithSponsors(
                    "home",
                    arrayOf(SponsorshipLevel.LANYARD, SponsorshipLevel.GOLD),
                    null,
                    CURRENT_EVENT.toInt(),
                    it
                )
            }
            GET("/about", aboutHandler::findAboutView)
            GET("/ticketing", lotteryHandler::ticketing)
            GET("/sponsors") { sponsorHandler.viewWithSponsors(CURRENT_EVENT.toInt(), it) }
            GET("/mixteen") {
                sponsorHandler.viewWithSponsors(
                    "mixteen",
                    arrayOf(SponsorshipLevel.MIXTEEN),
                    "mixteen.title",
                    CURRENT_EVENT.toInt(),
                    it
                )
            }
            GET("/faq", aboutHandler::faqView)
            GET("/come", aboutHandler::comeToMixitView)
            GET("/schedule", talkHandler::scheduleView)
            GET("/user/{login}") { userHandler.findOneView(it) }
            GET("/me") { userHandler.findProfileView(it) }
            GET("/me/edit", userHandler::editProfileView)
            GET("/me/talks/edit/{slug}", talkHandler::editTalkView)
            GET("/search") { aboutHandler.findFullTextView(it) }
            GET("/speaker", userHandler::speakerView)
            GET("/accessibility", aboutHandler::accessibilityView)
            GET("/codeofconduct", aboutHandler::codeConductView)
            GET("/blog", blogHandler::findAllView)
            GET("/blog/{slug}", blogHandler::findOneView)

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
                GET("", adminHandler::admin)
                GET("/cache", cacheHandler::view)
                GET("/lottery", adminLotteryHandler::adminTicketing)
                GET("/ticket", ticketHandler::ticketing)
                GET("/ticket/edit/{number}", ticketHandler::editTicket)
                GET("/ticket/create", ticketHandler::createTicket)
                GET("/mailings", mailingHandler::listMailing)
                GET("/mailings/create", mailingHandler::createMailing)
                GET("/mailings/edit/{id}", mailingHandler::editMailing)
                GET("/talks/edit/{slug}", adminTalkHandler::editTalk)
                GET("/talks") { adminTalkHandler.adminTalks(it, AdminTalkHandler.LAST_TALK_EVENT) }
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
                GET("/ticket/edit/{number}", ticketHandler::editTicket)
                GET("/post/create", adminPostHandler::createPost)
                GET("/mixette-organization", adminMixetteHandler::adminOrganizationDonations)
                GET("/mixette-donor", adminMixetteHandler::adminDonorDonations)
                GET("/mixette-donation/edit/{id}", adminMixetteHandler::editDonation)
                GET("/mixette-donation/orga", adminMixetteHandler::editOrga)
                GET("/mixette-donation/donor", adminMixetteHandler::editDonor)
                GET("/mixette-donation/create", adminMixetteHandler::addDonation)
                GET("/mixette-donation/create/{number}", adminMixetteHandler::addDonationForAttendee)
            }

            "/volunteer".nest {
                GET("", adminHandler::admin)
                GET("/mixette-organization", adminMixetteHandler::adminOrganizationDonations)
                GET("/mixette-donor", adminMixetteHandler::adminDonorDonations)
                GET("/mixette-donation/edit/{id}", adminMixetteHandler::editDonation)
                GET("/mixette-donation/orga", adminMixetteHandler::editOrga)
                GET("/mixette-donation/donor", adminMixetteHandler::editDonor)
                GET("/mixette-donation/create", adminMixetteHandler::addDonation)
                GET("/mixette-donation/create/{number}", adminMixetteHandler::addDonationForAttendee)
            }
        }

        contentType(APPLICATION_FORM_URLENCODED).nest {
            POST("/login", authenticationHandler::login)
            POST("/send-token", authenticationHandler::sendToken)
            POST("/signup", authenticationHandler::signUp)
            POST("/signin", authenticationHandler::signIn)
            POST("/me", userHandler::saveProfile)
            POST("/me/talks", talkHandler::saveProfileTalk)
            POST("/search") { searchHandler.searchFullTextView(it) }
            POST("/ticketing", lotteryHandler::submit)

            "/admin".nest {
                POST("/talks", adminTalkHandler::adminSaveTalk)
                POST("/talks/delete", adminTalkHandler::adminDeleteTalk)
                POST("/users", adminUserHandler::adminSaveUser)
                POST("/users/delete", adminUserHandler::adminDeleteUser)
                POST("/lottery/random", adminLotteryHandler::randomDraw)
                POST("/lottery/random/delete", adminLotteryHandler::eraseRank)
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
                POST("/ticketing/delete", adminLotteryHandler::adminDeleteTicketing)
                POST("/ticket", ticketHandler::submit)
                POST("/ticket/delete", ticketHandler::adminDeleteTicket)
                POST("/post", adminPostHandler::adminSavePost)
                POST("/post/delete", adminPostHandler::adminDeletePost)
                POST("/mailings/preview", mailingHandler::previewMailing)
                POST("/mailings", mailingHandler::saveMailing)
                POST("/mailings/send", mailingHandler::sendMailing)
                POST("/mailings/delete", mailingHandler::deleteMailing)
                POST("/mixette-donation/{id}/delete", adminMixetteHandler::adminDeleteDonation)
                POST("/mixette-donation", adminMixetteHandler::adminSaveDonation)
                POST("/cache/{zone}/invalidate", cacheHandler::invalidate)
            }

            "/volunteer".nest {
                POST("/mixette-donation", adminMixetteHandler::adminSaveDonation)
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
            val model = generateModel(properties, path, locale, session, messageSource)
            next.handle(request).flatMap { if (it is RenderingResponse) RenderingResponse.from(it).modelAttributes(model).build() else it.toMono() }
        }
    }

    @Bean
    @DependsOn("websiteRouter")
    fun resourceRouter() = resources("/**", ClassPathResource("static/"))
}
