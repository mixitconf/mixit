package mixit

import mixit.repository.EventRepository
import mixit.repository.PostRepository
import mixit.repository.TalkRepository
import mixit.repository.UserRepository
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
class DatabaseInitializer(val userRepository: UserRepository,
                          val eventRepository: EventRepository,
                          val talkRepository: TalkRepository,
                          val postRepository: PostRepository) {

    @PostConstruct
    fun init() {
        userRepository.initData()
        eventRepository.initData()
        talkRepository.initData()
        postRepository.initData()
    }
}