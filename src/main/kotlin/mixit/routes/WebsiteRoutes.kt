package mixit.routes

import mixit.MixitApplication.Companion.CURRENT_EVENT
import mixit.MixitProperties
import mixit.about.AboutHandler
import mixit.about.SearchHandler
import mixit.blog.handler.AdminPostHandler
import mixit.blog.handler.WebBlogHandler
import mixit.event.handler.AdminEventHandler
import mixit.event.handler.AdminEventImagesHandler
import mixit.faq.handler.AdminQuestionHandler
import mixit.favorite.handler.JsonFavoriteHandler
import mixit.features.handler.FeatureStateHandler
import mixit.feedback.handler.FeedbackHandler
import mixit.mailing.handler.MailingHandler
import mixit.mailing.handler.MailingListHandler
import mixit.mixette.handler.AdminMixetteHandler
import mixit.mixette.handler.MixetteHandler
import mixit.security.handler.AuthenticationHandler
import mixit.talk.handler.AdminTalkHandler
import mixit.talk.handler.TalkHandler
import mixit.talk.handler.TalkViewConfig.Companion.imageAlbum
import mixit.talk.handler.TalkViewConfig.Companion.images
import mixit.talk.handler.TalkViewConfig.Companion.media
import mixit.talk.handler.TalkViewConfig.Companion.mediaWithFavorites
import mixit.talk.handler.TalkViewConfig.Companion.mixette
import mixit.talk.handler.TalkViewConfig.Companion.mixitonair
import mixit.talk.handler.TalkViewConfig.Companion.speakers
import mixit.talk.handler.TalkViewConfig.Companion.talks
import mixit.talk.handler.TalkViewConfig.Companion.talksWithFavorites
import mixit.talk.handler.TalkViewConfig.Companion.video
import mixit.talk.model.Topic
import mixit.ticket.handler.AdminLotteryHandler
import mixit.ticket.handler.AdminTicketHandler
import mixit.ticket.handler.LotteryHandler
import mixit.ticket.handler.TicketHandler
import mixit.user.handler.AdminUserHandler
import mixit.user.handler.SponsorHandler
import mixit.user.handler.UserHandler
import mixit.util.AdminHandler
import mixit.util.cache.CacheHandler
import mixit.util.mustache.MustacheTemplate.Home
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
    private val adminEventImagesHandler: AdminEventImagesHandler,
    private val adminLotteryHandler: AdminLotteryHandler,
    private val adminTalkHandler: AdminTalkHandler,
    private val adminUserHandler: AdminUserHandler,
    private val adminPostHandler: AdminPostHandler,
    private val adminMixetteHandler: AdminMixetteHandler,
    private val adminFaqHandler: AdminQuestionHandler,
    private val authenticationHandler: AuthenticationHandler,
    private val blogHandler: WebBlogHandler,
    private val aboutHandler: AboutHandler,
    private val searchHandler: SearchHandler,
    private val talkHandler: TalkHandler,
    private val sponsorHandler: SponsorHandler,
    private val lotteryHandler: LotteryHandler,
    private val adminTicketHandler: AdminTicketHandler,
    private val mailingHandler: MailingHandler,
    private val mailingListHandler: MailingListHandler,
    private val mixetteHandler: MixetteHandler,
    private val userHandler: UserHandler,
    private val favoriteHandler: JsonFavoriteHandler,
    private val ticketHandler: TicketHandler,
    private val properties: MixitProperties,
    private val routeFilterUtils: RouteFilterUtils,
    private val featureStateHandler: FeatureStateHandler,
    private val faqHandler: mixit.faq.handler.FaqHandler,
    private val feedbackHandler: FeedbackHandler,
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
            GET("/disconnect", authenticationHandler::logout)
            GET("/login", authenticationHandler::loginView)
            GET("/logout", authenticationHandler::logout)
            GET("/signin/{token}/{email:.*}", authenticationHandler::signInViaUrl)

            GET("/admin/faq", adminFaqHandler::viewFaqAdmin)
            GET("/admin/faq/sections/{sectionId}/questions/create", adminFaqHandler::createFaqQuestionAdmin)
            GET("/admin/faq/sections/{sectionId}/questions/{questionId}", adminFaqHandler::viewFaqQuestionAdmin)
            GET("/admin/faq/sections/create", adminFaqHandler::createFaqSectionAdmin)
            GET("/admin/faq/sections/{sectionId}", adminFaqHandler::viewFaqSectionAdmin)
            GET("/admin", adminHandler::admin)
            GET("/admin/blog", adminPostHandler::adminBlog)
            GET("/admin/cache", cacheHandler::view)
            GET("/admin/events", adminEventHandler::adminEvents)
            GET("/admin/events/create", adminEventHandler::createEvent)
            GET("/admin/events/edit/{eventId}", adminEventHandler::editEvent)
            GET("/admin/events/images", adminEventImagesHandler::adminEventImages)
            GET("/admin/events/images/create", adminEventImagesHandler::createEventImages)
            GET("/admin/events/images/edit/{id}", adminEventImagesHandler::editEventImages)
            GET("/admin/events/images/{event}/sections/create", adminEventImagesHandler::createEventImagesSection)
            GET("/admin/events/images/{event}/sections/edit/{sectionId}", adminEventImagesHandler::editEventImagesSection)
            GET("/admin/events/images/{event}/sections/{sectionId}/images/create", adminEventImagesHandler::createEventImagesSectionImage)
            GET("/admin/events/images/{event}/sections/{sectionId}/images/edit", adminEventImagesHandler::editEventImagesSectionImage)
            GET("/admin/events/{eventId}/organizations/create", adminEventHandler::createEventOrganization)
            GET("/admin/events/{eventId}/organizations/edit/{organizationLogin}", adminEventHandler::editEventOrga)
            GET("/admin/events/{eventId}/organizers/create", adminEventHandler::createEventOrganizer)
            GET("/admin/events/{eventId}/organizers/edit/{organizerLogin}", adminEventHandler::editEventOrganizer)
            GET("/admin/events/{eventId}/sponsors/create", adminEventHandler::createEventSponsoring)
            GET("/admin/events/{eventId}/sponsors/edit/{sponsorId}/{level}", adminEventHandler::editEventSponsoring)
            GET("/admin/events/{eventId}/volunteers/create", adminEventHandler::createEventVolunteer)
            GET("/admin/events/{eventId}/volunteers/edit/{volunteerLogin}", adminEventHandler::editEventVolunteer)
            GET("/admin/features", featureStateHandler::list)
            GET("/admin/feedbacks/{year}", feedbackHandler::admin)
            GET("/admin/feedbacks/{year}/all", feedbackHandler::seeAll)
            GET("/admin/lottery", adminLotteryHandler::adminTicketing)
            GET("/admin/mailing-lists", mailingListHandler::listMailing)
            GET("/admin/mailings", mailingHandler::listMailing)
            GET("/admin/mailings/create", mailingHandler::createMailing)
            GET("/admin/mailings/edit/{id}", mailingHandler::editMailing)
            GET("/admin/mixette-donation/create", adminMixetteHandler::addDonation)
            GET("/admin/mixette-donation/create/{number}", adminMixetteHandler::addDonationForAttendee)
            GET("/admin/mixette-donation/donor", adminMixetteHandler::editDonor)
            GET("/admin/mixette-donation/edit/{id}", adminMixetteHandler::editDonation)
            GET("/admin/mixette-donation/orga", adminMixetteHandler::editOrganization)
            GET("/admin/mixette-donor", adminMixetteHandler::adminDonorDonations)
            GET("/admin/mixette-organization", adminMixetteHandler::adminOrganizationDonations)
            GET("/admin/post/create", adminPostHandler::createPost)
            GET("/admin/post/edit/{id}", adminPostHandler::editPost)
            GET("/admin/talks") { adminTalkHandler.adminTalks(it, CURRENT_EVENT) }
            GET("/admin/talks/create/{year}", adminTalkHandler::createTalk)
            GET("/admin/talks/edit/{id}", adminTalkHandler::editTalk)
            GET("/admin/talk/feedback/{talkId}") { feedbackHandler.findMyFeedbacks(it, admin = true) }
            GET("/admin/talks/{year}") { adminTalkHandler.adminTalks(it, it.pathVariable("year")) }
            GET("/admin/ticket", adminTicketHandler::ticketing)
            GET("/admin/ticket/create", adminTicketHandler::createTicket)
            GET("/admin/ticket/edit/{number}", adminTicketHandler::editTicket)
            GET("/admin/ticket/edit/{number}", adminTicketHandler::editTicket)
            GET("/admin/ticket/print", adminTicketHandler::printTicketing)
            GET("/admin/users", adminUserHandler::adminUsers)
            GET("/admin/users-newsletter", adminUserHandler::adminUserNewsLetters)
            GET("/admin/users/create", adminUserHandler::createUser)
            GET("/admin/users/edit/{login}", adminUserHandler::editUser)

            GET("/volunteer", adminHandler::admin)
            GET("/volunteer/mixette-donation/create", adminMixetteHandler::addDonation)
            GET("/volunteer/mixette-donation/create/{number}", adminMixetteHandler::addDonationForAttendee)
            GET("/volunteer/mixette-donation/donor", adminMixetteHandler::editDonor)
            GET("/volunteer/mixette-donation/edit/{id}", adminMixetteHandler::editDonation)
            GET("/volunteer/mixette-donation/orga", adminMixetteHandler::editOrganization)
            GET("/volunteer/mixette-donor", adminMixetteHandler::adminDonorDonations)
            GET("/volunteer/mixette-organization", adminMixetteHandler::adminOrganizationDonations)

            GET("/about") { aboutHandler.findAboutView(it, CURRENT_EVENT.toInt()) }
            GET("/accessibility", aboutHandler::accessibilityView)
            GET("/blog") { blogHandler.findAllView(it, CURRENT_EVENT.toInt()) }
            GET("/blog/feed", blogHandler::feed)
            GET("/blog/{slug}", blogHandler::findOneView)
            GET("/code-of-conduct", aboutHandler::codeConductView)
            GET("/codeofconduct", aboutHandler::codeConductView)
            GET("/faq", faqHandler::faqView)
            GET("/lottery", lotteryHandler::ticketing)
            GET("/me") { userHandler.findProfileView(it) }
            GET("/me/feedback/{talkId}") { feedbackHandler.findMyFeedbacks(it) }
            GET("/me/edit", userHandler::editProfileView)
            GET("/me/talks/edit/{slug}", talkHandler::editTalkView)
            GET("/mixette/dashboard", mixetteHandler::mixette)
            GET("/newsletter-signin/{token}/{email:.*}", authenticationHandler::signInViaUrlForNewsletter)
            GET("/newsletter-subscribe", authenticationHandler::newsletterView)
            GET("/schedule", talkHandler::scheduleView)
            GET("/search") { aboutHandler.findFullTextView(it) }
            GET("/speaker", userHandler::speakerView)
            GET("/sponsor/{login}") { userHandler.findOneView(it, UserHandler.ViewMode.ViewSponsor) }
            GET("/sponsors") { sponsorHandler.viewSponsors(it) }
            GET("/ticket/{number}") { ticketHandler.findOneView(it) }
            GET("/user/{login}") { userHandler.findOneView(it) }
            GET("/venue", aboutHandler::comeToMixitView)

            (2012..CURRENT_EVENT.toInt()).forEach { year ->
                GET("/$year") { talkHandler.findByEventView(talks(it, year)) }
                GET("/$year/about") { aboutHandler.findAboutView(it, year) }
                GET("/$year/favorites") { talkHandler.findByEventView(talksWithFavorites(it, year)) }
                GET("/$year/medias/video") { talkHandler.findByEventView(video(it, year)) }
                GET("/$year/medias") { talkHandler.findByEventView(media(it, year)) }
                GET("/$year/medias/favorites") { talkHandler.findByEventView(talksWithFavorites(it, year)) }
                GET("/$year/medias/images") { talkHandler.findByEventView(images(it, year)) }
                GET("/$year/medias/images/{album}") { talkHandler.findByEventView(imageAlbum(it, year)) }
                GET("/$year/sponsors") { sponsorHandler.viewSponsors(it, year) }
                GET("/$year/speakers") { talkHandler.findByEventView(speakers(it, year)) }
                GET("/$year/mixette") { talkHandler.findByEventView(mixette(it, year)) }
                GET("/$year/mixit-on-air") { talkHandler.findByEventView(mixitonair(it, year)) }
                GET("/blog/$year") { blogHandler.findAllView(it, year) }
                Topic.entries.map { it.value }.onEach { topic ->
                    GET("/$year/$topic") { talkHandler.findByEventView(talks(it, year, topic)) }
                    GET("/$year/$topic/favorites") {
                        talkHandler.findByEventView(talksWithFavorites(it, year, topic))
                    }
                    GET("/$year/medias/$topic") { talkHandler.findByEventView(media(it, year, topic)) }
                    GET("/$year/medias/$topic/favorites") {
                        talkHandler.findByEventView(mediaWithFavorites(it, year, topic))
                    }
                }

                GET("/$year/{slug}") { talkHandler.findOneView(it, year) }
                POST("/$year/{slug}/feedback/comment") {
                    feedbackHandler.comment(it, year)
                }
            }
        }

        contentType(APPLICATION_FORM_URLENCODED).nest {
            POST("/admin/cache/{zone}/invalidate", cacheHandler::invalidate)
            POST("/admin/events", adminEventHandler::adminSaveEvent)
            POST("/admin/events/images", adminEventImagesHandler::adminSaveEventImages)
            POST("/admin/events/images/delete", adminEventImagesHandler::adminDeleteEventImages)
            POST("/admin/events/images/{eventId}/sections/create", adminEventImagesHandler::adminCreateEventImagesSection)
            POST("/admin/events/images/{eventId}/sections/delete", adminEventImagesHandler::adminDeleteEventImagesSection)
            POST("/admin/events/images/{eventId}/sections/{sectionId}/images", adminEventImagesHandler::adminUpdateEventImagesSectionImage)
            POST("/admin/events/images/{eventId}/sections/{sectionId}/images/create", adminEventImagesHandler::adminCreateEventImagesSectionImage)
            POST("/admin/events/images/{eventId}/sections/{sectionId}/images/delete", adminEventImagesHandler::adminDeleteEventImagesSectionImage)
            POST("/admin/events/{eventId}/organizations", adminEventHandler::adminUpdateEventOrganization)
            POST("/admin/events/{eventId}/organizations/create", adminEventHandler::adminCreateEventOrganization)
            POST("/admin/events/{eventId}/organizations/delete", adminEventHandler::adminDeleteEventOrganization)
            POST("/admin/events/{eventId}/organizers", adminEventHandler::adminUpdateEventOrganizer)
            POST("/admin/events/{eventId}/organizers/create", adminEventHandler::adminCreateEventOrganizer)
            POST("/admin/events/{eventId}/organizers/delete", adminEventHandler::adminDeleteEventOrganizer)
            POST("/admin/events/{eventId}/sponsors", adminEventHandler::adminUpdateEventSponsoring)
            POST("/admin/events/{eventId}/sponsors/create", adminEventHandler::adminCreateEventSponsoring)
            POST("/admin/events/{eventId}/sponsors/delete", adminEventHandler::adminDeleteEventSponsoring)
            POST("/admin/events/{eventId}/volunteers", adminEventHandler::adminUpdateEventVolunteer)
            POST("/admin/events/{eventId}/volunteers/create", adminEventHandler::adminCreateEventVolunteer)
            POST("/admin/events/{eventId}/volunteers/delete", adminEventHandler::adminDeleteEventVolunteer)
            POST("/admin/faq/sections", adminFaqHandler::saveFaqSectionAdmin)
            POST("/admin/faq/sections/{sectionId}/delete", adminFaqHandler::deleteFaqSectionAdmin)
            POST("/admin/faq/sections/{sectionId}/questions", adminFaqHandler::saveFaqQuestionAdmin)
            POST("/admin/faq/sections/{sectionId}/questions/{questionId}/delete", adminFaqHandler::deleteFaqQuestionAdmin)
            POST("/admin/feedbacks/{id}/reject", feedbackHandler::reject)
            POST("/admin/feedbacks/{id}/approve", feedbackHandler::approve)
            POST("/admin/features", featureStateHandler::save)
            POST("/admin/lottery/delete", adminLotteryHandler::adminDeleteTicketing)
            POST("/admin/lottery/random", adminLotteryHandler::randomDraw)
            POST("/admin/lottery/random/delete", adminLotteryHandler::eraseRank)
            POST("/admin/mailing-lists", mailingListHandler::generateMailinglist)
            POST("/admin/mailings", mailingHandler::saveMailing)
            POST("/admin/mailings/delete", mailingHandler::deleteMailing)
            POST("/admin/mailings/preview", mailingHandler::previewMailing)
            POST("/admin/mailings/send", mailingHandler::sendMailing)
            POST("/admin/mixette-donation", adminMixetteHandler::adminSaveDonation)
            POST("/admin/mixette-donation/{id}/delete", adminMixetteHandler::adminDeleteDonation)
            POST("/admin/post", adminPostHandler::adminSavePost)
            POST("/admin/post/delete", adminPostHandler::adminDeletePost)
            POST("/admin/talks", adminTalkHandler::adminSaveTalk)
            POST("/admin/talks/synchronize", adminTalkHandler::adminSynchronize)
            POST("/admin/talks/delete", adminTalkHandler::adminDeleteTalk)
            POST("/admin/ticket", adminTicketHandler::submit)
            POST("/admin/ticket/delete", adminTicketHandler::adminDeleteTicket)
            POST("/admin/ticket/import/speakers", adminTicketHandler::adminImportSpeakers)
            POST("/admin/ticket/import/sponsors", adminTicketHandler::adminImportSponsors)
            POST("/admin/ticket/import/staff", adminTicketHandler::adminImportStaff)
            POST("/admin/ticket/import/volunteers", adminTicketHandler::adminImportVolunteers)
            POST("/admin/ticket/search", adminTicketHandler::ticketing)
            POST("/admin/users", adminUserHandler::adminSaveUser)
            POST("/admin/users/delete", adminUserHandler::adminDeleteUser)
            POST("/admin/users/search", adminUserHandler::adminUsers)
            POST("/admin/users-newsletter/search", adminUserHandler::adminUserNewsLetters)
            POST("/favorites/{email}/talks/{id}/toggle", favoriteHandler::toggleFavorite)
            POST("/login", authenticationHandler::login)
            POST("/lottery", lotteryHandler::submit)
            POST("/me", userHandler::saveProfile)
            POST("/me/talks", talkHandler::saveProfileTalk)
            POST("/newsletter-login", authenticationHandler::sendEmailForNewsletter)
            POST("/newsletter-send-token", authenticationHandler::sendTokenForNewsletter)
            POST("/newsletter-signup", authenticationHandler::signUpForNewsletter)
            POST("/newsletter-subscribe", authenticationHandler::subscribeNewsletter)
            POST("/search") { searchHandler.searchFullTextView(it) }
            POST("/send-token", authenticationHandler::sendToken)
            POST("/signin", authenticationHandler::signIn)
            POST("/signup", authenticationHandler::signUp)
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
