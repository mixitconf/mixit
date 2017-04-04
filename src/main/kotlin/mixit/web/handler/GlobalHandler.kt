package mixit.web.handler

import mixit.model.*
import mixit.repository.UserRepository
import mixit.util.MarkdownConverter
import mixit.util.language
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse.ok
import java.util.*


@Component
class GlobalHandler(val userRepository: UserRepository,
                    val markdownConverter: MarkdownConverter) {

    fun findAboutView(req: ServerRequest) = userRepository.findByRole(Role.STAFF).collectList().then { u ->
        val users = u.map { it.toDto(req.language(), markdownConverter) }
        Collections.shuffle(users)
        ok().render("about", mapOf(Pair("staff", users), Pair("title", "about.title")))
    }

    fun mixteenView(req: ServerRequest) = ok().render("mixteen")

    fun faqView(req: ServerRequest) = ok().render("faq")

    fun comeToMixitView(req: ServerRequest) = ok().render("come")
}

