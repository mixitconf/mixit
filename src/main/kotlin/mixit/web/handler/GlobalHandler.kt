package mixit.web.handler

import mixit.model.*
import mixit.repository.UserRepository
import mixit.util.language
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse.ok
import java.util.*


@Component
class GlobalHandler(val userRepository: UserRepository) {

    fun findAboutView(req: ServerRequest) = userRepository.findByRole(Role.STAFF).collectList().flatMap {
        val users = it.map { it.toDto(req.language()) }
        Collections.shuffle(users)
        ok().render("about", mapOf(Pair("staff", users), Pair("title", "about.title")))
    }

    fun mixteenView(req: ServerRequest) = ok().render("mixteen", mapOf(Pair("title", "mixteen.title")))

    fun faqView(req: ServerRequest) = ok().render("faq", mapOf(Pair("title", "faq.title")))

    fun comeToMixitView(req: ServerRequest) = ok().render("come", mapOf(Pair("title", "come.title")))

    fun scheduleView(req: ServerRequest) = ok().render("schedule", mapOf(Pair("title", "schedule.title")))
}

