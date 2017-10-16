package mixit.web.handler

import mixit.model.Role
import mixit.repository.UserRepository
import mixit.util.MarkdownConverter
import mixit.util.language
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse.ok
import java.util.*


@Component
class GlobalHandler(val userRepository: UserRepository, val markdownConverter: MarkdownConverter) {

    fun findAboutView(req: ServerRequest) = userRepository
            .findByRoles(listOf(Role.STAFF, Role.STAFF_IN_PAUSE))
            .collectList()
            .flatMap {
                val usersByRole = it.groupBy { it.role }

                Collections.shuffle(usersByRole[Role.STAFF])
                if(usersByRole[Role.STAFF_IN_PAUSE] != null){
                    Collections.shuffle(usersByRole[Role.STAFF_IN_PAUSE])
                }

                ok().render("about", mapOf(
                        Pair("staff", usersByRole[Role.STAFF]?.map { it.toDto(req.language(), markdownConverter) }),
                        Pair("inactiveStaff", usersByRole[Role.STAFF_IN_PAUSE]?.map { it.toDto(req.language(), markdownConverter) }),
                        Pair("title", "about.title")))
            }

    fun mixteenView(req: ServerRequest) = ok().render("mixteen", mapOf(Pair("title", "mixteen.title")))

    fun faqView(req: ServerRequest) = ok().render("faq", mapOf(Pair("title", "faq.title")))

    fun comeToMixitView(req: ServerRequest) = ok().render("come", mapOf(Pair("title", "come.title")))

    fun scheduleView(req: ServerRequest) = ok().render("schedule", mapOf(Pair("title", "schedule.title")))
}

