package mixit.web.handler.admin

import com.fasterxml.jackson.databind.ObjectMapper
import mixit.MixitProperties
import mixit.model.Language.ENGLISH
import mixit.model.Language.FRENCH
import mixit.model.Role
import mixit.model.User
import mixit.model.Users
import mixit.repository.UserRepository
import mixit.util.Cryptographer
import mixit.util.enumMatcher
import mixit.util.extractFormData
import mixit.util.seeOther
import mixit.web.handler.admin.AdminUtils.toJson
import mixit.web.handler.admin.AdminUtils.toLinks
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import reactor.core.publisher.Mono

@Component
class AdminUserHandler(
    private val userRepository: UserRepository,
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
        userRepository.findOne(req.pathVariable("login")).flatMap(this::adminUser)

    fun adminDeleteUser(req: ServerRequest): Mono<ServerResponse> =
        req.extractFormData().flatMap { formData ->
            userRepository
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
            userRepository.save(user).then(seeOther("${properties.baseUri}/admin/users"))
        }

}
