package mixit

import mixit.repository.*
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
class DatabaseInitializer(private val userRepository: UserRepository,
                          private val eventRepository: EventRepository,
                          private val talkRepository: TalkRepository,
                          private val ticketRepository: TicketRepository,
                          private val postRepository: PostRepository) {

    @PostConstruct
    fun init() {
        userRepository.initData()
        eventRepository.initData()
        talkRepository.initData()
        postRepository.initData()
        ticketRepository.initData()
    }
}