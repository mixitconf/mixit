package mixit.import

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URL
import java.util.UUID
import mixit.model.Language
import mixit.model.Link
import mixit.model.Role
import mixit.model.Talk
import mixit.model.TalkFormat
import mixit.model.User
import mixit.repository.UserRepository
import mixit.util.Cryptographer
import mixit.util.encodeToMd5
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.ClassPathResource

inline fun <reified T> ObjectMapper.readValue(src: InputStream): T = readValue(src, jacksonTypeRef<T>())

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SessionizeImportTests(
    @Autowired val objectMapper: ObjectMapper,
    @Autowired val userRepository: UserRepository,
    @Autowired val cryptographer: Cryptographer
) {

    @Test
    fun `load speakers`() {

        initializeFolder()

        val papercallExports =
            objectMapper.readValue<List<PapercallExport>>(ClassPathResource("import/2020-papercall-export.json").inputStream)

        val speakersToPersist = importSpeakers(papercallExports)
        objectMapper.writeValue(File("/tmp/mixit/speakers_2020.json"), speakersToPersist)

        val sessionsToPersist = createSessions(papercallExports, speakersToPersist)
        objectMapper.writeValue(File("/tmp/mixit/talks_2020.json"), sessionsToPersist)
    }

    private fun importSpeakers(papercallExports: List<PapercallExport>): MutableList<User> {

        val papercallSpeakers = extractSpeakers(papercallExports)

        val speakersToPersist: MutableList<User> = mutableListOf()

        papercallSpeakers.forEach { papercallSpeaker ->
            val user = userRepository.findByNonEncryptedEmail(papercallSpeaker.email).block()
            if (user != null) {
                val updatedUser = updateUser(user, papercallSpeaker)
                speakersToPersist.add(updatedUser)
            } else {
                val createdUser = createUser(papercallSpeaker)
                speakersToPersist.add(createdUser)
            }
        }

        return speakersToPersist
    }

    private fun getImageName(speakerName: String): String {
        val speakerWithPngImage =
            listOf("julien_dubedout", "marilyn_kol", "benoit", "cedric_spalvieri", "nikola_lohinski")

        val imageName = sanitize(speakerName)
        return imageName + if (speakerWithPngImage.any { it == imageName }) ".png" else ".jpg"
    }

    private fun extractSpeakers(papercallExports: List<PapercallExport>): MutableList<PapercallProfile> {
        val speakers = mutableListOf<PapercallProfile>()

        // Add a reference to the talk in the user to be able to link them
        papercallExports.forEach { talk ->
            talk.profile.talkId = talk.id
            talk.co_presenter_profiles.forEach { coPresenter -> coPresenter.talkId = talk.id }
        }

        papercallExports
            .map { papercallExport -> papercallExport.profile }
            .forEach { speakers.add(it) }

        papercallExports
            .flatMap { papercallExport -> papercallExport.co_presenter_profiles }
            .forEach { speakers.add(it) }

        return speakers
    }

    private fun createUser(papercallSpeaker: PapercallProfile): User {
        var login = papercallSpeaker.email.substring(0, papercallSpeaker.email.indexOf("@"))
        val userCheck = userRepository.findOne(login).block()
        if (userCheck != null) {
            println("Login ${userCheck.login} already exist: try to create one with suffix")
            login += UUID.randomUUID().toString()
            val anotherUserCheck = userRepository.findOne(login).block()
            if (anotherUserCheck != null) {
                println("Login with suffix ${anotherUserCheck.login} already exist aborting import")
                throw IllegalArgumentException()
            }
        }
        println("User not found => ${papercallSpeaker.name} => login : $login")

        if (papercallSpeaker.avatar != null) {
            downloadImage(papercallSpeaker.avatar, getImageName(papercallSpeaker.name))
        }
        println("User created")

        return User(
            login,
            firstNameOf(papercallSpeaker.name),
            lastName(papercallSpeaker.name),
            cryptographer.encrypt(papercallSpeaker.email),
            papercallSpeaker.company,
            bioToMap(papercallSpeaker.bio),
            papercallSpeaker.email.encodeToMd5(),
            "https://mixitconf.cleverapps.io/images/speakers/${getImageName(papercallSpeaker.name)}",
            Role.USER,
            if (papercallSpeaker.url.isNullOrEmpty()) listOf() else listOf(Link("site", papercallSpeaker.url)),
            // We store id in token to retrieve speaker later when we will save talk
            token = papercallSpeaker.talkId.orEmpty()
        )
    }

    private fun updateUser(user: User, papercallSpeaker: PapercallProfile): User {
        println("User found => ${user.login} : ${user.lastname} ${user.firstname}")

        val imageUrl = getImageUrl(user, papercallSpeaker.avatar)
        if (imageUrl != null && imageUrl.contains("papercallio")) {
            downloadImage(imageUrl, getImageName(papercallSpeaker.name))
        }
        println("User updated")

        return User(
            user.login,
            firstNameOf(papercallSpeaker.name),
            lastName(papercallSpeaker.name),
            user.email,
            if (papercallSpeaker.company == null) user.company else papercallSpeaker.company,
            if (papercallSpeaker.bio == null) user.description else bioToMap(papercallSpeaker.bio),
            papercallSpeaker.email.encodeToMd5(),
            "https://mixitconf.cleverapps.io/images/speakers/${getImageName(papercallSpeaker.name)}",
            user.role,
            if (papercallSpeaker.url.isNullOrEmpty()) listOf() else listOf(Link("site", papercallSpeaker.url)),
            // We store id in token to retrieve speaker later when we will save talk
            token = papercallSpeaker.talkId.orEmpty()
        )
    }

    private fun lastName(name: String) = name.split(" ").drop(1).joinToString(" ")

    private fun firstNameOf(name: String) = name.split(" ").first()

    private fun createSessions(
        papercallExports: List<PapercallExport>,
        speakersToPersist: MutableList<User>
    ): List<Talk> {
        return papercallExports.map { export ->
            // We have to find the sessions speakers
            val sessionSpeakers: List<User> = speakersToPersist.filter { it.token == export.id }

            val title = export.talk?.title.orEmpty()
            val summary = export.talk?.abstract.orEmpty()
            val language = getLanguage(export)
            val topic = getTopic(export)
            val speakerIds = sessionSpeakers.map { it.login }
            val talkFormat = getTalkFormat(export)

            println("Session $title : speakers ${sessionSpeakers.map { it.firstname + it.lastname }}")
            println("Lng=$language topic=$topic  format=$talkFormat")

            Talk(
                talkFormat,
                "2020",
                title,
                summary,
                speakerIds,
                language,
                topic = topic
            )
        }
    }

    private fun getTalkFormat(export: PapercallExport): TalkFormat =
        when (export.talk?.talk_format) {
            "Long talk (50 minutes)" -> TalkFormat.TALK
            "Short talk (20 minutes)" -> TalkFormat.RANDOM
            "Workshop (110 minutes)" -> TalkFormat.WORKSHOP
            else -> TalkFormat.TALK
        }

    private fun getTopic(export: PapercallExport): String {
        val mixitTopics = listOf("maker", "design", "aliens", "tech", "ethic", "life style", "team")
        val papercallTopics: String = export.tags
            .map { it.lowercase() }
            .filter { mixitTopics.contains(it) }
            .firstOrNull().orEmpty().ifEmpty { "other" }
        return toMixitTopic(papercallTopics)
    }

    private fun getLanguage(export: PapercallExport): Language {
        val isInFrench = export.cfp_additional_question_answers
            .filter { question -> question.question_content == "Do you plan to present your talk in French?" }
            .filter { question -> question.content == "yes" }
            .isNotEmpty()
        return if (isInFrench) Language.FRENCH else Language.ENGLISH
    }
}

fun getImageUrl(user: User, pictureUrl: String?): String? {
    System.out.println("user ${user.lastname} -> ${user.photoUrl}")
    if (!user.photoUrl.isNullOrEmpty()) {
        if (user.photoUrl!!.contains("/mixitconf")) {
            return user.photoUrl
        }
        if (user.photoUrl!!.startsWith("/images")) {
            return "https://mixitconf.cleverapps.io${user.photoUrl}"
        }
        return user.photoUrl
    } else if (pictureUrl.isNullOrEmpty()) {
        return "https://www.gravatar.com/avatar/${user.emailHash}?s=400"
    }
    return pictureUrl
}

fun bioToMap(bio: String?) = if (bio == null) mapOf(
    Pair(Language.FRENCH, "UNKNOWN"),
    Pair(Language.ENGLISH, "UNKNOWN")
) else mapOf(Pair(Language.FRENCH, bio), Pair(Language.ENGLISH, bio))

val SPECIAL_SLUG_CHARACTERS = mapOf(
    Pair('é', 'e'),
    Pair('è', 'e'),
    Pair('ï', 'i'),
    Pair(' ', '_'),
    Pair('ê', 'e'),
    Pair('à', 'a'),
    Pair('-', '_'),
    Pair('_', '0'),
    Pair('∴', '0')
)

fun sanitize(value: String): String = value.lowercase().toCharArray()
    .map { if (SPECIAL_SLUG_CHARACTERS.get(it) == null) it else SPECIAL_SLUG_CHARACTERS.get(it) }.joinToString("")

fun initializeFolder() {
    // Check if /tmp/images/ exists ?
    val directory = File("/tmp/mixit/")
    if (!directory.exists()) {
        directory.mkdir()
    }
}

fun downloadImage(url: String, filename: String) {
    val emplacement = File("/tmp/mixit/$filename")
    if (emplacement.exists()) {
        emplacement.delete()
    }
    emplacement.createNewFile()

    FileOutputStream(emplacement).use {
        val out = it

        try {
            URL(url).openConnection().getInputStream().use {
                val b = ByteArray(1024)
                var c: Int = it.read(b)
                while (c != -1) {
                    out.write(b, 0, c)
                    c = it.read(b)
                }
            }
        } catch (e: Exception) {
            System.out.println("Impossible to load image for $filename")
            e.printStackTrace()
        }
    }
}

class PapercallExport(
    val id: String? = null,
    val state: String? = null,
    val talk: PapercallTalk? = null,
    val tags: List<String> = emptyList(),
    val profile: PapercallProfile,
    val co_presenter_profiles: List<PapercallProfile> = emptyList(),
    val cfp_additional_question_answers: List<PapercallAdditionalQuestion>
)

class PapercallAdditionalQuestion(
    val question_content: String? = null,
    val content: String? = null
)

class PapercallProfile(
    val name: String,
    val bio: String? = null,
    val twitter: String? = null,
    val company: String? = null,
    val url: String? = null,
    val email: String,
    val avatar: String? = null,
    var talkId: String? = null
)

class PapercallTalk(
    val title: String? = null,
    val abstract: String? = null,
    val description: String? = null,
    val talk_format: String? = null
)

fun toMixitTopic(rawTopic: String): String =
    when (rawTopic) {
        "Maker" -> "makers"
        "Design" -> "design"
        "Aliens" -> "aliens"
        "aliens" -> "aliens"
        "Tech" -> "tech"
        "Ethic" -> "ethics"
        "Life style" -> "lifestyle"
        "Team" -> "team"
        else -> "other"
    }
