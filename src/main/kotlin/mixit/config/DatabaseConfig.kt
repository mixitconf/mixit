package mixit.config

import mixit.repository.EventRepository
import mixit.repository.FavoriteRepository
import mixit.repository.PostRepository
import mixit.repository.TalkRepository
import mixit.repository.TicketRepository
import mixit.repository.UserRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DatabaseConfig{

    @Bean
    fun databaseInitializer(userRepository: UserRepository,
                            eventRepository: EventRepository,
                            talkRepository: TalkRepository,
                            ticketRepository: TicketRepository,
                            postRepository: PostRepository,
                            favoriteRepository: FavoriteRepository) = CommandLineRunner {

        userRepository.initData()
        eventRepository.initData()
        talkRepository.initData()
        postRepository.initData()
        ticketRepository.initData()
        favoriteRepository.initData()
    }

}