package mixit

import mixit.repository.EventRepository
import mixit.repository.PostRepository
import mixit.repository.TalkRepository
import mixit.repository.UserRepository
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile


/**
 * [DataInitializerConfiguration] will not be activated on Cloud Foundry when "cloud" profile is enabled
 */
@Configuration
@Profile("default")
class DataInitializerConfiguration {

    @Bean
    fun dataInitializer(userRepository: UserRepository, eventRepository: EventRepository,
                        talkRepository: TalkRepository, postRepository: PostRepository) = ApplicationRunner {
        userRepository.initData()
        eventRepository.initData()
        talkRepository.initData()
        postRepository.initData()
    }

}
