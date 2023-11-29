package mixit.config

import kotlinx.coroutines.runBlocking
import mixit.blog.model.BlogService
import mixit.blog.repository.PostRepository
import mixit.event.model.EventImagesService
import mixit.event.model.EventService
import mixit.event.repository.EventImagesRepository
import mixit.event.repository.EventRepository
import mixit.faq.model.QuestionSectionService
import mixit.faq.repository.QuestionSectionRepository
import mixit.favorite.repository.FavoriteRepository
import mixit.features.repository.FeatureStateRepository
import mixit.mixette.repository.MixetteDonationRepository
import mixit.talk.model.TalkService
import mixit.talk.repository.TalkRepository
import mixit.ticket.repository.LotteryRepository
import mixit.ticket.repository.TicketRepository
import mixit.user.model.UserService
import mixit.user.repository.UserRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DatabaseInitializerConfig {

    @Bean
    fun databaseInitializer(
        userRepository: UserRepository,
        eventRepository: EventRepository,
        eventImagesRepository: EventImagesRepository,
        talkRepository: TalkRepository,
        lotteryRepository: LotteryRepository,
        postRepository: PostRepository,
        favoriteRepository: FavoriteRepository,
        ticketRepository: TicketRepository,
        mixetteDonationRepository: MixetteDonationRepository,
        featureStateRepository: FeatureStateRepository,
        questionSectionRepository: QuestionSectionRepository,
        eventService: EventService,
        eventImagesService: EventImagesService,
        blogService: BlogService,
        talkService: TalkService,
        userService: UserService,
        questionSectionService: QuestionSectionService
    ) = CommandLineRunner {

        userRepository.initData()
        eventRepository.initData()
        eventImagesRepository.initData()
        talkRepository.initData()
        postRepository.initData()
        lotteryRepository.initData()
        favoriteRepository.initData()
        ticketRepository.initData()
        mixetteDonationRepository.initData()
        featureStateRepository.initData()
        questionSectionRepository.initData()

        runBlocking {
            userService.initializeCache()
            eventService.initializeCache()
            blogService.initializeCache()
            talkService.initializeCache()
            eventImagesService.initializeCache()
            questionSectionService.initializeCache()
        }
    }
}
