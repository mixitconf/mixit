package mixit.user.handler

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import mixit.MixitProperties
import mixit.util.mustache.MustacheI18n.TITLE
import mixit.util.mustache.MustacheI18n.USER
import mixit.util.mustache.MustacheI18n.USERS
import mixit.util.mustache.MustacheTemplate.AdminUser
import mixit.util.mustache.MustacheTemplate.AdminUserNewsLetter
import mixit.util.mustache.MustacheTemplate.AdminUsers
import mixit.security.model.Cryptographer
import mixit.talk.model.Language.ENGLISH
import mixit.talk.model.Language.FRENCH
import mixit.user.model.PhotoShape
import mixit.user.model.Role
import mixit.user.model.User
import mixit.user.model.UserService
import mixit.user.model.Users.DEFAULT_IMG_URL
import mixit.user.repository.UserRepository
import mixit.util.AdminUtils.toJson
import mixit.util.AdminUtils.toLinks
import mixit.util.encodeToMd5
import mixit.util.enumMatcher
import mixit.util.extractFormData
import org.springframework.web.reactive.function.server.json
import mixit.util.seeOther
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.renderAndAwait

@Component
class AdminUserHandler(
    private val userRepository: UserRepository,
    private val userService: UserService,
    private val properties: MixitProperties,
    private val objectMapper: ObjectMapper,
    private val cryptographer: Cryptographer
) {

    companion object {
        const val LIST_URI = "/admin/users"
    }

    suspend fun adminUsers(req: ServerRequest): ServerResponse =
        req.extractFormData().let { formData ->
            val criteria = formData["criteria"]
            ok().renderAndAwait(
                AdminUsers.template,
                mapOf(
                    USERS to userRepository.findAll()
                        .filter { it.filterOn(criteria, cryptographer) }
                        .sortedWith(compareBy<User> { it.lastname.lowercase() }.thenBy { it.firstname.lowercase() }),
                    TITLE to AdminUsers.title
                )
            )
        }

    suspend fun adminUserNewsLetters(req: ServerRequest): ServerResponse =
        req.extractFormData().let { formData ->
            val criteria = formData["criteria"]
            ok().renderAndAwait(
                AdminUserNewsLetter.template,
                mapOf(
                    USERS to userRepository
                        .findAll()
                        .filter { it.filterOn(criteria, cryptographer) }
                        .filter { it.newsletterSubscriber }
                        .map { it.copy(email = cryptographer.decrypt(it.email)) }
                        .sortedWith(compareBy<User> { it.lastname }.thenBy { it.firstname }),
                    TITLE to AdminUserNewsLetter.title
                )
            )
        }

    suspend fun adminUserNewsLetterCsv(req: ServerRequest): ServerResponse =
        ok().contentType(MediaType(MediaType.TEXT_PLAIN, Charsets.UTF_8)).bodyValueAndAwait(
            (
                listOf("firstname;lastname;email") +
                    userRepository
                        .findAll()
                        .filter { it.newsletterSubscriber }
                        .map { it.copy(email = cryptographer.decrypt(it.email)) }
                        .sortedWith(compareBy<User> { it.lastname }.thenBy { it.firstname })
                        .map { "${it.firstname};${it.lastname};${it.email}" }
                ).joinToString("\n")

        )

    suspend fun createUser(req: ServerRequest): ServerResponse = this.adminUser()

    suspend fun editUser(req: ServerRequest): ServerResponse {
        val existingUser = userRepository.findOneOrNull(req.pathVariable("login"))!!
        val updatedUser = existingUser.copy(
            photoUrl = if (existingUser.emailHash != null && existingUser.photoUrl == DEFAULT_IMG_URL) null else existingUser.photoUrl
        )
        return this.adminUser(updatedUser)
    }

    suspend fun adminDeleteUser(req: ServerRequest): ServerResponse {
        val formData = req.extractFormData()
        userService.deleteOne(formData["login"]!!).awaitSingleOrNull()
        return seeOther("${properties.baseUri}$LIST_URI")
    }

    private suspend fun adminUser(user: User = User.empty()): ServerResponse =
        ok().renderAndAwait(
            AdminUser.template,
            mapOf(
                TITLE to AdminUser.title,
                USER to user,
                "usermail" to cryptographer.decrypt(user.email),
                "description-fr" to user.description[FRENCH],
                "description-en" to user.description[ENGLISH],
                "roles" to enumMatcher(user) { user.role },
                "links" to user.links.toJson(objectMapper),
                "photoShapes" to enumMatcher(user) { user.photoShape ?: PhotoShape.Square },
            )
        )

    suspend fun adminSaveUser(req: ServerRequest): ServerResponse {
        val formData = req.extractFormData()
        val existingUser = userRepository.findOneOrNull(formData["login"]!!) ?: User()
        val updatedUser = existingUser.copy(
            login = formData["login"]!!,
            firstname = formData["firstname"]!!,
            lastname = formData["lastname"]!!,
            email = formData["email"]?.let { cryptographer.encrypt(it) },
            emailHash = if (formData["photoUrl"] == null) formData["email"]?.encodeToMd5() else null,
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
        userService.save(updatedUser).awaitSingle()
        return seeOther("${properties.baseUri}/admin/users")
    }

    private fun sanitizeImage(photoUrl: String): String = if (logoType(photoUrl) == null) {
        DEFAULT_IMG_URL
    } else {
        if (photoUrl.startsWith("images")) "/$photoUrl" else photoUrl
    }

    suspend fun findAll(req: ServerRequest): ServerResponse =
        userRepository
            .findAll()
            .let { users ->
                ok().json().bodyValueAndAwait(users.map { it.copy(email = cryptographer.decrypt(it.email)) })
            }
}
