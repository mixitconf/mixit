package mixit.user.handler

import com.fasterxml.jackson.databind.ObjectMapper
import mixit.MixitProperties
import mixit.blog.model.BlogService
import mixit.event.model.EventService
import mixit.security.model.Cryptographer
import mixit.talk.model.Language.ENGLISH
import mixit.talk.model.Language.FRENCH
import mixit.talk.model.TalkService
import mixit.user.model.Role
import mixit.user.model.User
import mixit.user.model.UserService
import mixit.user.model.Users
import mixit.util.AdminUtils.toJson
import mixit.util.AdminUtils.toLinks
import mixit.util.enumMatcher
import mixit.util.extractFormData
import mixit.util.seeOther
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import reactor.core.publisher.Mono

@Component
class AdminUserHandler(
    private val userService: UserService,
    private val blogService: BlogService,
    private val talkService: TalkService,
    private val eventService: EventService,
    private val properties: MixitProperties,
    private val objectMapper: ObjectMapper,
    private val cryptographer: Cryptographer
) {

    companion object {
        const val TEMPLATE_LIST = "admin-users"
        const val TEMPLATE_EDIT = "admin-user"
        const val LIST_URI = "/admin/users"
    }

    fun adminUsers(req: ServerRequest) =
        ok().render(
            TEMPLATE_LIST,
            mapOf(
                Pair(
                    "users",
                    userService.findAll()
                        .map { it.toUser() }
                        .sort(Comparator.comparing(User::lastname).thenComparing(Comparator.comparing(User::firstname)))
                ),
                Pair("title", "admin.users.title")
            )
        )

    fun createUser(req: ServerRequest): Mono<ServerResponse> =
        this.adminUser()

    fun editUser(req: ServerRequest): Mono<ServerResponse> =
        userService.findOne(req.pathVariable("login")).flatMap { adminUser(it.toUser()) }

    fun adminDeleteUser(req: ServerRequest): Mono<ServerResponse> =
        req.extractFormData().flatMap { formData ->
            userService
                .deleteOne(formData["login"]!!)
                .then(seeOther("${properties.baseUri}$LIST_URI"))
        }

    private fun adminUser(user: User = User("", "", "", "")) = ok().render(
        TEMPLATE_EDIT,
        mapOf(
            Pair("user", user),
            Pair("usermail", cryptographer.decrypt(user.email)),
            Pair("description-fr", user.description[FRENCH]),
            Pair("description-en", user.description[ENGLISH]),
            Pair("roles", enumMatcher(user) { user.role }),
            Pair("links", user.links.toJson(objectMapper))

        )
    )

    fun adminSaveUser(req: ServerRequest): Mono<ServerResponse> =
        req.extractFormData().flatMap { formData ->
            val user = User(
                login = formData["login"]!!,
                firstname = formData["firstname"]!!,
                lastname = formData["lastname"]!!,
                email = formData["email"]?.let { cryptographer.encrypt(it) },
                emailHash = formData["emailHash"],
                photoUrl = formData["photoUrl"] ?: formData["emailHash"] ?: Users.DEFAULT_IMG_URL,
                company = formData["company"],
                description = mapOf(
                    Pair(FRENCH, formData["description-fr"]!!),
                    Pair(ENGLISH, formData["description-en"]!!)
                ),
                role = Role.valueOf(formData["role"]!!),
                links = formData["links"]!!.toLinks(objectMapper),
                legacyId = formData["legacyId"]?.toLong()
            )
            userService.save(user)
                .doOnSuccess {
                    // We need to refresh event or blog or talk cache
                    // TODO we don't need to invalidate all zones
                    blogService.invalidateCache()
                    eventService.invalidateCache()
                    talkService.invalidateCache()
                }
                .then(seeOther("${properties.baseUri}/admin/users"))
        }

}
