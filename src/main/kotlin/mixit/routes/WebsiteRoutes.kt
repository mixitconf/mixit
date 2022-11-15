package mixit.routes

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mixit.MixitProperties
import mixit.about.AboutHandler
import mixit.about.SearchHandler
import mixit.blog.handler.AdminPostHandler
import mixit.blog.handler.WebBlogHandler
import mixit.event.handler.AdminEventHandler
import mixit.event.model.Event
import mixit.event.model.EventService
import mixit.event.model.SponsorshipLevel.MIXTEEN
import mixit.mailing.handler.MailingHandler
import mixit.mailing.handler.MailingListHandler
import mixit.mixette.handler.AdminMixetteHandler
import mixit.mixette.handler.MixetteHandler
import mixit.routes.MustacheTemplate.Home
import mixit.routes.MustacheTemplate.Mixteen
import mixit.security.handler.AuthenticationHandler
import mixit.talk.handler.AdminTalkHandler
import mixit.talk.handler.TalkHandler
import mixit.talk.handler.TalkHandler.Companion.feedbackWall
import mixit.talk.handler.TalkHandler.Companion.media
import mixit.talk.handler.TalkHandler.Companion.mediaWithFavorites
import mixit.talk.handler.TalkHandler.Companion.talks
import mixit.talk.handler.TalkHandler.Companion.talksWithFavorites
import mixit.talk.model.Topic
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
import org.springframework.core.annotation.Order
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED
import org.springframework.http.MediaType.TEXT_EVENT_STREAM
import org.springframework.http.MediaType.TEXT_HTML
import org.springframework.http.MediaType.TEXT_PLAIN
import org.springframework.web.reactive.function.server.RenderingResponse
import org.springframework.web.reactive.function.server.RouterFunctions.resources
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.coRouter
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
    private val mailingListHandler: MailingListHandler,
    private val mixetteHandler: MixetteHandler,
    private val userHandler: UserHandler,
    private val messageSource: MessageSource,
    private val properties: MixitProperties,
    private val objectMapper: ObjectMapper,
    private val eventService: EventService
) {

    private val logger = LoggerFactory.getLogger(WebsiteRoutes::class.java)

    @Bean
    @Order(2)
    fun websiteCoRouter() =
        coRouter {
            accept(TEXT_HTML).nest {
                GET("/") { sponsorHandler.viewWithSponsors(it, Home.template) }

                "/admin".nest {
                    GET("/admin/users", adminUserHandler::adminUsers)
                    GET("/admin/users/edit/{login}", adminUserHandler::editUser)
                    GET("/admin/users/create", adminUserHandler::createUser)
                }

                GET("/about", aboutHandler::findAboutView)
                GET("/accessibility", aboutHandler::accessibilityView)
                GET("/codeofconduct", aboutHandler::codeConductView)
                GET("/code-of-conduct", aboutHandler::codeConductView)
                GET("/come", aboutHandler::comeToMixitView)
                GET("/faq", aboutHandler::faqView)
                GET("/me") { userHandler.findProfileView(it) }
                GET("/me/edit", userHandler::editProfileView)
                GET("/me/talks/edit/{slug}", talkHandler::editTalkView)
                GET("/search") { aboutHandler.findFullTextView(it) }
                GET("/speaker", userHandler::speakerView)
                GET("/sponsors") { sponsorHandler.viewSponsors(it) }
                GET("/mixteen") {
                    sponsorHandler.viewWithSponsors(it, Mixteen.template, "mixteen.title", arrayOf(MIXTEEN))
                }
                GET("/user/{login}") { userHandler.findOneView(it) }

                // TODO refactoring
                val eventsResource = ClassPathResource("data/events.json")
                val events: List<Event> = objectMapper.readValue(eventsResource.inputStream)

                events.map { it.year }.forEach { year ->
                    GET("/admin/$year/feedback-wall") { talkHandler.findByEventView(feedbackWall(it, year)) }

                    // Sponsors
                    GET("/sponsors/$year") { sponsorHandler.viewSponsors(it, year) }

                    // Talks
                    GET("/$year") { talkHandler.findByEventView(talks(it, year)) }
                    GET("/$year/favorite") { talkHandler.findByEventView(talksWithFavorites(it, year)) }
                    GET("/$year/medias") { talkHandler.findByEventView(media(it, year)) }
                    GET("/$year/medias/favorite") { talkHandler.findByEventView(mediaWithFavorites(it, year)) }
                    Topic.values()
                        .map { it.value }
                        .onEach { topic ->
                            GET("/$year/$topic") { talkHandler.findByEventView(talks(it, year, topic)) }
                            GET("/$year/$topic/favorite") {
                                talkHandler.findByEventView(talksWithFavorites(it, year, topic))
                            }
                            GET("/$year/medias/$topic") { talkHandler.findByEventView(media(it, year, topic)) }
                            GET("/$year/medias/$topic/favorite") {
                                talkHandler.findByEventView(mediaWithFavorites(it, year, topic))
                            }
                        }

                    GET("/$year/{slug}") { talkHandler.findOneView(it, year) }
                }
            }

            contentType(APPLICATION_FORM_URLENCODED).nest {
                "/admin".nest {
                    POST("/users", adminUserHandler::adminSaveUser)
                    POST("/users/delete", adminUserHandler::adminDeleteUser)
                }
                POST("/me", userHandler::saveProfile)
                POST("/me/talks", talkHandler::saveProfileTalk)
            }
        }.filter { request, next ->
            val locale: Locale = request.locale()
            val path = request.uri().path
            request.session().flatMap { session ->
                val model = generateModel(properties, path, locale, session, messageSource)
                next.handle(request).flatMap {
                    if (it is RenderingResponse) RenderingResponse.from(it).modelAttributes(model)
                        .build() else it.toMono()
                }
            }
        }

    @Bean
    @Order(1)
    fun websiteRouter() = router {
        GET("/blog/feed", blogHandler::feed)

        accept(TEXT_EVENT_STREAM).nest {
            GET("/mixette/dashboard/sse", mixetteHandler::mixetteRealTime)
        }

        accept(TEXT_HTML).nest {

            GET("/lottery", lotteryHandler::ticketing)

            GET("/schedule", talkHandler::scheduleView)

            GET("/blog", blogHandler::findAllView)
            GET("/blog/{slug}", blogHandler::findOneView)
            GET("/mixette/dashboard", mixetteHandler::mixette)

            // Authentication
            GET("/login", authenticationHandler::loginView)
            GET("/disconnect", authenticationHandler::logout)
            GET("/signin/{token}/{email:.*}", authenticationHandler::signInViaUrl)

            // Newsletter
            GET("/newsletter-subscribe", authenticationHandler::newsletterView)
            GET("/newsletter-signin/{token}/{email:.*}", authenticationHandler::signInViaUrlForNewsletter)

            "/admin".nest {
                GET("", adminHandler::admin)
                GET("/cache", cacheHandler::view)
                GET("/lottery", adminLotteryHandler::adminTicketing)
                GET("/ticket", ticketHandler::ticketing)
                GET("/ticket/print", ticketHandler::printTicketing)
                GET("/ticket/edit/{number}", ticketHandler::editTicket)
                GET("/ticket/create", ticketHandler::createTicket)
                GET("/mailings", mailingHandler::listMailing)
                GET("/mailings/create", mailingHandler::createMailing)
                GET("/mailings/edit/{id}", mailingHandler::editMailing)
                GET("/mailing-lists", mailingListHandler::listMailing)
                GET("/talks/edit/{id}", adminTalkHandler::editTalk)
                GET("/talks") { adminTalkHandler.adminTalks(it, AdminTalkHandler.LAST_TALK_EVENT) }
                GET("/talks/create", adminTalkHandler::createTalk)
                GET("/talks/{year}") { adminTalkHandler.adminTalks(it, it.pathVariable("year")) }
                GET("/events", adminEventHandler::adminEvents)
                GET("/events/edit/{eventId}", adminEventHandler::editEvent)
                GET("/events/{eventId}/sponsors/edit/{sponsorId}/{level}", adminEventHandler::editEventSponsoring)
                GET("/events/{eventId}/sponsors/create", adminEventHandler::createEventSponsoring)
                GET(
                    "/events/{eventId}/organizations/edit/{organizationLogin}",
                    adminEventHandler::editEventOrganization
                )
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
            POST("/newsletter-login", authenticationHandler::sendEmailForNewsletter)
            POST("/newsletter-send-token", authenticationHandler::sendTokenForNewsletter)
            POST("/newsletter-signup", authenticationHandler::signUpForNewsletter)
            POST("/newsletter-subscribe", authenticationHandler::subscribeNewsletter)
            POST("/search") { searchHandler.searchFullTextView(it) }
            POST("/lottery", lotteryHandler::submit)

            "/admin".nest {
                POST("/talks", adminTalkHandler::adminSaveTalk)
                POST("/talks/delete", adminTalkHandler::adminDeleteTalk)
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
                POST("/lottery/delete", adminLotteryHandler::adminDeleteTicketing)
                POST("/ticket", ticketHandler::submit)
                POST("/ticket/delete", ticketHandler::adminDeleteTicket)
                POST("/post", adminPostHandler::adminSavePost)
                POST("/post/delete", adminPostHandler::adminDeletePost)
                POST("/mailings/preview", mailingHandler::previewMailing)
                POST("/mailings", mailingHandler::saveMailing)
                POST("/mailings/send", mailingHandler::sendMailing)
                POST("/mailings/delete", mailingHandler::deleteMailing)
                POST("/mailing-lists", mailingListHandler::generateMailinglist)
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
            next.handle(request).flatMap {
                if (it is RenderingResponse) RenderingResponse.from(it).modelAttributes(model).build() else it.toMono()
            }
        }
    }

    @Bean
    @DependsOn("websiteRouter")
    fun resourceRouter() = resources("/**", ClassPathResource("static/"))
}
