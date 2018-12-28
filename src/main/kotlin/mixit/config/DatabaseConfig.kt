package mixit.config

import mixit.repository.*
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DatabaseConfig {

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