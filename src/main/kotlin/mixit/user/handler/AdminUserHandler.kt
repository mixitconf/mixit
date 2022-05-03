package mixit.user.handler

import com.fasterxml.jackson.databind.ObjectMapper
import mixit.MixitProperties
import mixit.security.model.Cryptographer
import mixit.talk.model.Language.ENGLISH
import mixit.talk.model.Language.FRENCH
import mixit.user.model.PhotoShape
import mixit.user.model.Role
import mixit.user.model.User
import mixit.user.model.UserService
import mixit.user.model.Users
import mixit.user.repository.UserRepository
import mixit.util.AdminUtils.toJson
import mixit.util.AdminUtils.toLinks
import mixit.util.encodeToMd5
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
    private val userRepository: UserRepository,
    private val userService: UserService,
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
                    userRepository.findAll()
                        .sort(Comparator.comparing(User::lastname).thenComparing(Comparator.comparing(User::firstname)))
                ),
                Pair("title", "admin.users.title")
            )
        )

    fun createUser(req: ServerRequest): Mono<ServerResponse> =
        this.adminUser()

    fun editUser(req: ServerRequest): Mono<ServerResponse> =
        userRepository.findOne(req.pathVariable("login"))
            .map { it.copy(photoUrl = if (it.emailHash != null && it.photoUrl == Users.DEFAULT_IMG_URL) null else it.photoUrl) }
            .flatMap(this::adminUser)

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
            Pair("links", user.links.toJson(objectMapper)),
            Pair("photoShapes", enumMatcher(user) { user.photoShape ?: PhotoShape.Square }),
        )
    )

    fun adminSaveUser(req: ServerRequest): Mono<ServerResponse> =
        req.extractFormData().flatMap { formData ->
            userRepository.findOne(formData["login"]!!)
                .map { it }
                .switchIfEmpty(Mono.just(User()))
                .flatMap { oldUser ->
                    val user = oldUser.copy(
                        login = formData["login"]!!,
                        firstname = formData["firstname"]!!,
                        lastname = formData["lastname"]!!,
                        email = formData["email"]?.let { cryptographer.encrypt(it) },
                        emailHash = if (formData["photoUrl"] == null) formData["emailHash"] ?: formData["email"]?.encodeToMd5() else null,
                        photoUrl = formData["photoUrl"]?.let { sanitizeImage(it) },
                        photoShape = formData["photoShape"]?.let { PhotoShape.valueOf(it) },
                        company = formData["company"],
                        description = mapOf(
                            Pair(FRENCH, formData["description-fr"] ?: formData["description-en"] ?: ""),
                            Pair(ENGLISH, formData["description-en"] ?: formData["description-fr"] ?: "")
                        ),
                        role = Role.valueOf(formData["role"]!!),
                        links = formData["links"]!!.toLinks(objectMapper),
                        legacyId = formData["legacyId"]?.toLong(),
                        newsletterSubscriber = formData["newsletterSubscriber"]?.toBoolean() ?: false
                    )
                    userService.save(user)
                        .then(seeOther("${properties.baseUri}/admin/users"))
                }
        }

    private fun sanitizeImage(photoUrl: String): String =
        if (logoType(photoUrl) == null) {
            Users.DEFAULT_IMG_URL
        } else {
            if (photoUrl.startsWith("images")) "/$photoUrl" else photoUrl
        }
}
