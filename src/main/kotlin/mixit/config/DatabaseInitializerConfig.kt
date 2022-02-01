package mixit.config

import mixit.blog.model.BlogService
import mixit.blog.repository.PostRepository
import mixit.event.model.EventService
import mixit.event.repository.EventRepository
import mixit.favorite.repository.FavoriteRepository
import mixit.talk.repository.TalkRepository
import mixit.ticket.repository.TicketRepository
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
        talkRepository: TalkRepository,
        ticketRepository: TicketRepository,
        postRepository: PostRepository,
        favoriteRepository: FavoriteRepository
    ) = CommandLineRunner {

        userRepository.initData()
        eventRepository.initData()
        talkRepository.initData()
        postRepository.initData()
        ticketRepository.initData()
        favoriteRepository.initData()
    }

    @Bean
    fun cacheInitializer(
        eventService: EventService,
        blogService: BlogService
    ) = CommandLineRunner {
        eventService.initializeCache()
        blogService.initializeCache()
    }
}
