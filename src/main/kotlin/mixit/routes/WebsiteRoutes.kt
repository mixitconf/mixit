package mixit.routes

import mixit.MixitApplication.Companion.CURRENT_EVENT
import mixit.MixitProperties
import mixit.about.AboutHandler
import mixit.about.SearchHandler
import mixit.blog.handler.AdminPostHandler
import mixit.blog.handler.WebBlogHandler
import mixit.event.handler.AdminEventHandler
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
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.DependsOn
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED
import org.springframework.http.MediaType.TEXT_EVENT_STREAM
import org.springframework.http.MediaType.TEXT_HTML
import org.springframework.http.MediaType.TEXT_PLAIN
import org.springframework.web.reactive.function.server.RouterFunctions.resources
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.coRouter

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
    private val properties: MixitProperties,
    private val routeFilterUtils: RouteFilterUtils
) {

    private val logger = LoggerFactory.getLogger(WebsiteRoutes::class.java)

    @Bean
    fun websiteRouter() = coRouter {
        accept(TEXT_EVENT_STREAM).nest {
            GET("/mixette/dashboard/sse", mixetteHandler::mixetteRealTime)
        }

        accept(TEXT_HTML).nest {
            GET("/") { sponsorHandler.viewWithSponsors(it, Home.template) }

            // Authentication
            GET("/login", authenticationHandler::loginView)
            GET("/disconnect", authenticationHandler::logout)
            GET("/logout", authenticationHandler::logout)
            GET("/signin/{token}/{email:.*}", authenticationHandler::signInViaUrl)

            GET("/admin", adminHandler::admin)
            GET("/admin/blog", adminPostHandler::adminBlog)
            GET("/admin/post/edit/{id}", adminPostHandler::editPost)
            GET("/admin/post/create", adminPostHandler::createPost)
            GET("/admin/cache", cacheHandler::view)
            GET("/admin/users", adminUserHandler::adminUsers)
            GET("/admin/users/edit/{login}", adminUserHandler::editUser)
            GET("/admin/users/create", adminUserHandler::createUser)
            GET("/admin/mailings", mailingHandler::listMailing)
            GET("/admin/mailings/create", mailingHandler::createMailing)
            GET("/admin/mailings/edit/{id}", mailingHandler::editMailing)
            GET("/admin/mailing-lists", mailingListHandler::listMailing)
            GET("/admin/mixette-organization", adminMixetteHandler::adminOrganizationDonations)
            GET("/admin/mixette-donor", adminMixetteHandler::adminDonorDonations)
            GET("/admin/mixette-donation/edit/{id}", adminMixetteHandler::editDonation)
            GET("/admin/mixette-donation/orga", adminMixetteHandler::editOrganization)
            GET("/admin/mixette-donation/donor", adminMixetteHandler::editDonor)
            GET("/admin/mixette-donation/create", adminMixetteHandler::addDonation)
            GET("/admin/mixette-donation/create/{number}", adminMixetteHandler::addDonationForAttendee)
            GET("/admin/lottery", adminLotteryHandler::adminTicketing)
            GET("/admin/ticket", ticketHandler::ticketing)
            GET("/admin/ticket/print", ticketHandler::printTicketing)
            GET("/admin/ticket/edit/{number}", ticketHandler::editTicket)
            GET("/admin/ticket/create", ticketHandler::createTicket)
            GET("/admin/ticket/edit/{number}", ticketHandler::editTicket)

            GET("/admin/talks/edit/{id}", adminTalkHandler::editTalk)
            GET("/admin/talks") { adminTalkHandler.adminTalks(it, AdminTalkHandler.LAST_TALK_EVENT) }
            GET("/admin/talks/create", adminTalkHandler::createTalk)
            GET("/admin/talks/{year}") { adminTalkHandler.adminTalks(it, it.pathVariable("year")) }

            GET("/volunteer", adminHandler::admin)
            GET("/volunteer/mixette-organization", adminMixetteHandler::adminOrganizationDonations)
            GET("/volunteer/mixette-donor", adminMixetteHandler::adminDonorDonations)
            GET("/volunteer/mixette-donation/edit/{id}", adminMixetteHandler::editDonation)
            GET("/volunteer/mixette-donation/orga", adminMixetteHandler::editOrganization)
            GET("/volunteer/mixette-donation/donor", adminMixetteHandler::editDonor)
            GET("/volunteer/mixette-donation/create", adminMixetteHandler::addDonation)
            GET("/volunteer/mixette-donation/create/{number}", adminMixetteHandler::addDonationForAttendee)

            GET("/about") { aboutHandler.findAboutView(it, CURRENT_EVENT.toInt()) }
            GET("/accessibility", aboutHandler::accessibilityView)
            GET("/blog") { blogHandler.findAllView(it, CURRENT_EVENT.toInt()) }
            GET("/blog/{slug}", blogHandler::findOneView)
            GET("/codeofconduct", aboutHandler::codeConductView)
            GET("/code-of-conduct", aboutHandler::codeConductView)
            GET("/come", aboutHandler::comeToMixitView)
            GET("/admin/events", adminEventHandler::adminEvents)
            GET("/admin/events/edit/{eventId}", adminEventHandler::editEvent)
            GET("/admin/events/{eventId}/sponsors/edit/{sponsorId}/{level}", adminEventHandler::editEventSponsoring)
            GET("/admin/events/{eventId}/sponsors/create", adminEventHandler::createEventSponsoring)
            GET("/admin/events/{eventId}/organizations/edit/{organizationLogin}", adminEventHandler::editEventOrga)
            GET("/admin/events/{eventId}/organizations/create", adminEventHandler::createEventOrganization)
            GET("/admin/events/{eventId}/volunteers/edit/{volunteerLogin}", adminEventHandler::editEventVolunteer)
            GET("/admin/events/{eventId}/volunteers/create", adminEventHandler::createEventVolunteer)
            GET("/admin/events/{eventId}/organizers/edit/{organizerLogin}", adminEventHandler::editEventOrganizer)
            GET("/admin/events/{eventId}/organizers/create", adminEventHandler::createEventOrganizer)
            GET("/admin/events/create", adminEventHandler::createEvent)
            GET("/faq", aboutHandler::faqView)
            GET("/lottery", lotteryHandler::ticketing)
            GET("/me") { userHandler.findProfileView(it) }
            GET("/me/edit", userHandler::editProfileView)
            GET("/me/talks/edit/{slug}", talkHandler::editTalkView)
            GET("/newsletter-subscribe", authenticationHandler::newsletterView)
            GET("/newsletter-signin/{token}/{email:.*}", authenticationHandler::signInViaUrlForNewsletter)
            GET("/search") { aboutHandler.findFullTextView(it) }
            GET("/speaker", userHandler::speakerView)
            GET("/sponsors") { sponsorHandler.viewSponsors(it) }
            GET("/mixette/dashboard", mixetteHandler::mixette)
            GET("/mixteen") {
                sponsorHandler.viewWithSponsors(it, Mixteen.template, Mixteen.title, arrayOf(MIXTEEN))
            }
            GET("/schedule", talkHandler::scheduleView)
            GET("/user/{login}") { userHandler.findOneView(it) }

            (2012..CURRENT_EVENT.toInt()).forEach { year ->
                GET("/admin/$year/feedback-wall") { talkHandler.findByEventView(feedbackWall(it, year)) }
                GET("/about/$year") { aboutHandler.findAboutView(it, year) }
                GET("/blog/$year") { blogHandler.findAllView(it, year) }
                GET("/sponsors/$year") { sponsorHandler.viewSponsors(it, year) }

                // Talks
                GET("/$year") { talkHandler.findByEventView(talks(it, year)) }
                GET("/$year/favorite") { talkHandler.findByEventView(talksWithFavorites(it, year)) }
                GET("/$year/medias") { talkHandler.findByEventView(media(it, year)) }
                GET("/$year/medias/favorite") { talkHandler.findByEventView(mediaWithFavorites(it, year)) }
                Topic.values().map { it.value }.onEach { topic ->
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
            POST("/admin/mailings/preview", mailingHandler::previewMailing)
            POST("/admin/mailings", mailingHandler::saveMailing)
            POST("/admin/mailings/send", mailingHandler::sendMailing)
            POST("/admin/mailings/delete", mailingHandler::deleteMailing)
            POST("/admin/mailing-lists", mailingListHandler::generateMailinglist)
            POST("/admin/talks", adminTalkHandler::adminSaveTalk)
            POST("/admin/talks/delete", adminTalkHandler::adminDeleteTalk)
            POST("/admin/lottery/random", adminLotteryHandler::randomDraw)
            POST("/admin/lottery/random/delete", adminLotteryHandler::eraseRank)
            POST("/admin/lottery/delete", adminLotteryHandler::adminDeleteTicketing)
            POST("/admin/ticket", ticketHandler::submit)
            POST("/admin/ticket/delete", ticketHandler::adminDeleteTicket)
            POST("/admin/post", adminPostHandler::adminSavePost)
            POST("/admin/post/delete", adminPostHandler::adminDeletePost)
            POST("/admin/users", adminUserHandler::adminSaveUser)
            POST("/admin/users/delete", adminUserHandler::adminDeleteUser)
            POST("/admin/cache/{zone}/invalidate", cacheHandler::invalidate)
            POST("/admin/mixette-donation/{id}/delete", adminMixetteHandler::adminDeleteDonation)
            POST("/admin/mixette-donation", adminMixetteHandler::adminSaveDonation)
            POST("/admin/events", adminEventHandler::adminSaveEvent)
            POST("/admin/events/{eventId}/sponsors/create", adminEventHandler::adminCreateEventSponsoring)
            POST("/admin/events/{eventId}/sponsors/delete", adminEventHandler::adminDeleteEventSponsoring)
            POST("/admin/events/{eventId}/sponsors", adminEventHandler::adminUpdateEventSponsoring)
            POST("/admin/events/{eventId}/organizations/create", adminEventHandler::adminCreateEventOrganization)
            POST("/admin/events/{eventId}/organizations/delete", adminEventHandler::adminDeleteEventOrganization)
            POST("/admin/events/{eventId}/organizations", adminEventHandler::adminUpdateEventOrganization)
            POST("/admin/events/{eventId}/volunteers/create", adminEventHandler::adminCreateEventVolunteer)
            POST("/admin/events/{eventId}/volunteers/delete", adminEventHandler::adminDeleteEventVolunteer)
            POST("/admin/events/{eventId}/volunteers", adminEventHandler::adminUpdateEventVolunteer)
            POST("/admin/events/{eventId}/organizers/create", adminEventHandler::adminCreateEventOrganizer)
            POST("/admin/events/{eventId}/organizers/delete", adminEventHandler::adminDeleteEventOrganizer)
            POST("/admin/events/{eventId}/organizers", adminEventHandler::adminUpdateEventOrganizer)
            POST("/login", authenticationHandler::login)
            POST("/send-token", authenticationHandler::sendToken)
            POST("/signup", authenticationHandler::signUp)
            POST("/signin", authenticationHandler::signIn)
            POST("/newsletter-login", authenticationHandler::sendEmailForNewsletter)
            POST("/newsletter-send-token", authenticationHandler::sendTokenForNewsletter)
            POST("/newsletter-signup", authenticationHandler::signUpForNewsletter)
            POST("/newsletter-subscribe", authenticationHandler::subscribeNewsletter)
            POST("/lottery", lotteryHandler::submit)
            POST("/me", userHandler::saveProfile)
            POST("/me/talks", talkHandler::saveProfileTalk)
            POST("/search") { searchHandler.searchFullTextView(it) }
            POST("/volunteer/mixette-donation", adminMixetteHandler::adminSaveDonation)
        }

        if (properties.baseUri != "https://mixitconf.org") {
            logger.warn("SEO disabled via robots.txt because ${properties.baseUri} baseUri is not the production one (https://mixitconf.org)")
            GET("/robots.txt") {
                ServerResponse.ok().contentType(TEXT_PLAIN).bodyValueAndAwait("User-agent: *\nDisallow: /")
            }
        }
    }.filter { request, next -> routeFilterUtils.addModelToResponse(request, next) }

    @Bean
    @DependsOn("websiteRouter")
    fun resourceRouter() = resources("/**", ClassPathResource("static/"))
}
