// package mixit.import
//
// import com.fasterxml.jackson.databind.ObjectMapper
// import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
// import kotlinx.coroutines.runBlocking
// import mixit.security.model.Cryptographer
// import mixit.talk.model.Language
// import mixit.talk.model.Room
// import mixit.talk.model.Talk
// import mixit.talk.model.TalkFormat
// import mixit.user.model.Link
// import mixit.user.model.Role
// import mixit.user.model.User
// import mixit.user.repository.UserRepository
// import mixit.util.encodeToMd5
// import org.junit.jupiter.api.Test
// import org.springframework.beans.factory.annotation.Autowired
// import org.springframework.boot.test.context.SpringBootTest
// import org.springframework.core.io.ResourceLoader
// import java.io.File
// import java.io.FileOutputStream
// import java.io.InputStream
// import java.net.URL
// import java.time.LocalDateTime
// import java.time.format.DateTimeFormatter
// import java.util.UUID
//
// inline fun <reified T> ObjectMapper.readValue(src: InputStream): T = readValue(src, jacksonTypeRef<T>())
//

// TODO https://sessionize.com/api/v2/em5tudqq/view/All

// data class TalkSessionizeDto(
//    val session_id: String,
//    val title: String?,
//    val description: String?,
//    val owner: String?,
//    val ownerEmail: String?,
//    val format: String?,
//    val track: String?,
//    val level: String?,
//    val language: String?,
//    val informed: String?,
//    val confirmed: String?,
//    val room: String?,
//    val scheduleAt: String?,
//    val duration: String,
//    val liveLink: String?,
//    val recordedLink: String?,
//    val speakerId: String?,
//    val firstName: String?,
//    val lastName: String?,
//    val email: String?,
//    val tagline: String?,
//    val bio: String?,
//    val coc: String?,
//    val twitter: String?,
//    val linkedin: String?,
//    val blog: String?,
//    val companyWS: String?,
//    val picture: String?,
// )
//
// @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
// class SessionizeImportTests(
//    @Autowired val objectMapper: ObjectMapper,
//    @Autowired val userRepository: UserRepository,
//    @Autowired val cryptographer: Cryptographer,
//    @Autowival pattern = DateTimeFormatter.ofPattern("dd MMM yyyy hh:mm a")red val resourceLoader: ResourceLoader
// ) {
//
//    fun testDate(date: String?): LocalDateTime? {
//        if (date == null) return null
//
//        return LocalDateTime.parse(date, pattern)
//    }
//
//    @Test
//    fun `load speakers`() {
//
//        initializeFolder()
//        val fileReader = resourceLoader.getResource("classpath:import/mixit2023.csv")
//        val data: List<TalkSessionizeDto> = objectMapper.readValue<List<TalkSessionizeDto>>(fileReader.inputStream)
//
//        val sessions: Map<String, List<TalkSessionizeDto>> = data.groupBy { it.session_id }
//
//        // 13 Apr 2023 10:40 AM
//
//        runBlocking {
//            val speakersToPersist = importSpeakers(data)
//            objectMapper.writeValue(File("/tmp/mixit/speakers_2023.json"), speakersToPersist)
//
//            val sessionsToCreate = sessions.map { createSessions(it.value, speakersToPersist) }
//            objectMapper.writeValue(File("/tmp/mixit/talks_2023.json"), sessionsToCreate)
//        }
//    }
//
//    private suspend fun importSpeakers(papercallExports: List<TalkSessionizeDto>): List<User> =
//        papercallExports
//            .filter { it.email != null }
//            .map { talk: TalkSessionizeDto ->
//                val email = talk.email ?: throw IllegalArgumentException("talk ${talk.session_id} has null email")
//                val user = userRepository.findByNonEncryptedEmail(email)
//                if (user != null) {
//                    updateUser(user, talk)
//                } else {
//                    createUser(talk)
//                }
//            }
//
//    private fun getImageName(speakerName: String): String {
//        val speakerWithPngImage =
//            listOf("julien_dubedout", "marilyn_kol", "benoit", "cedric_spalvieri", "nikola_lohinski")
//
//        val imageName = sanitize(speakerName)
//        return imageName + if (speakerWithPngImage.any { it == imageName }) ".png" else ".jpg"
//    }
//
//    private suspend fun computeLinks(talk: TalkSessionizeDto): List<Link> =
//        listOfNotNull(
//            talk.twitter?.let { Link("Twitter", it) },
//            talk.companyWS?.let { Link("Web site", it) },
//            talk.linkedin?.let { Link("Linkedin", it) },
//            talk.blog?.let { Link("Blog", it) },
//        )
//
//    private suspend fun createUser(talk: TalkSessionizeDto): User {
//        var login = talk.email!!.substring(0, talk.email.indexOf("@"))
//        val userCheck = userRepository.findOneOrNull(login)
//        if (userCheck != null) {
//            println("Login ${userCheck.login} already exist: try to create one with suffix")
//            login += UUID.randomUUID().toString()
//            val anotherUserCheck = userRepository.findOneOrNull(login)
//            if (anotherUserCheck != null) {
//                println("Login with suffix ${anotherUserCheck.login} already exist aborting import")
//                throw IllegalArgumentException()
//            }
//        }
//        println("User not found => ${talk.lastName} => login : $login")
//
//        val imageName = getImageName("${talk.firstName}.${talk.lastName}")
//        val hasImageDownloaded = talk.picture?.let { downloadImage(talk.picture, imageName) } ?: false
//
//        return User(
//            login,
//            firstNameOf(talk.firstName!!),
//            lastName(talk.lastName!!),
//            cryptographer.encrypt(talk.email),
//            null,
//            bioToMap(talk.bio),
//            talk.email.encodeToMd5(),
//            if (hasImageDownloaded) "https://mixitconf.org/images/speakers/$imageName" else null,
//            null,
//            Role.USER,
//            computeLinks(talk),
//            // We store id in token to retrieve speaker later when we will save talk
//            token = talk.speakerId!!
//        )
//    }
//
//    private suspend fun updateUser(user: User, talk: TalkSessionizeDto): User {
//        println("User found => ${user.login} : ${user.lastname} ${user.firstname}")
//
//        val imageName = getImageName("${talk.firstName}.${talk.lastName}")
//        val hasImageDownloaded = talk.picture?.let { downloadImage(talk.picture, imageName) } ?: false
//
//        return User(
//            user.login,
//            firstNameOf(talk.firstName!!),
//            lastName(talk.lastName!!),
//            user.email,
//            null,
//            if (talk.bio == null) user.description else bioToMap(talk.bio),
//            talk.email!!.encodeToMd5(),
//            if (!hasImageDownloaded) user.photoUrl else "https://mixitconf.org/images/speakers/$imageName",
//            null,
//            user.role,
//            computeLinks(talk),
//            // We store id in token to retrieve speaker later when we will save talk
//            token = talk.speakerId!!
//        )
//    }
//
//    private fun lastName(name: String) = name // .split(" ").drop(1).joinToString(" ")
//
//    private fun firstNameOf(name: String) = name.split(" ").first()
//
//    private fun createSessions(papercallExports: List<TalkSessionizeDto>, speakersToPersist: List<User>): Talk {
//
//        val sessionSpeakers: List<String> = papercallExports
//            .map { it.speakerId }
//            .map { id -> speakersToPersist.first { it.token == id } }
//            .map { it.login }
//
//        val talk = papercallExports[0]
//
//        val title = talk.title.orEmpty()
//        val summary = talk.description ?: "Coming soon..."
//        val language = getLanguage(talk.language ?: "French")
//        val topic = toMixitTopic(talk.track!!)
//        val talkFormat = getTalkFormat(talk)
//
//        println("Lng=$language topic=$topic  format=$talkFormat")
//
//        return Talk(
//            talkFormat,
//            "2020",
//            title,
//            summary,
//            sessionSpeakers,
//            language,
//            topic = topic,
//            room = toRoom(talk.room),
//            start = testDate(talk.scheduleAt),
//            end = testDate(talk.scheduleAt)?.plusMinutes(talkFormat.duration.toLong())
//        )
//    }
//
//    private fun getTalkFormat(export: TalkSessionizeDto): TalkFormat =
//        when (export.format) {
//            "Lightning talk" -> TalkFormat.RANDOM
//            "Workshop" -> TalkFormat.WORKSHOP
//            else -> TalkFormat.TALK
//        }
//
//    private fun getLanguage(lang: String): Language {
//        return if (lang == "English") Language.ENGLISH else Language.FRENCH
//    }
// }
//
// fun getImageUrl(user: User, pictureUrl: String?): String? {
//    System.out.println("user ${user.lastname} -> ${user.photoUrl}")
//    if (!user.photoUrl.isNullOrEmpty()) {
//        if (user.photoUrl!!.contains("/mixitconf")) {
//            return user.photoUrl
//        }
//        if (user.photoUrl!!.startsWith("/images")) {
//            return "https://mixitconf.cleverapps.io${user.photoUrl}"
//        }
//        return user.photoUrl
//    } else if (pictureUrl.isNullOrEmpty()) {
//        return "https://www.gravatar.com/avatar/${user.emailHash}?s=400"
//    }
//    return pictureUrl
// }
//
// fun bioToMap(bio: String?) = if (bio == null) mapOf(
//    Pair(Language.FRENCH, "UNKNOWN"),
//    Pair(Language.ENGLISH, "UNKNOWN")
// ) else mapOf(Pair(Language.FRENCH, bio), Pair(Language.ENGLISH, bio))
//
// val SPECIAL_SLUG_CHARACTERS = mapOf(
//    Pair('é', 'e'),
//    Pair('è', 'e'),
//    Pair('ï', 'i'),
//    Pair(' ', '_'),
//    Pair('ê', 'e'),
//    Pair('à', 'a'),
//    Pair('-', '_'),
//    Pair('_', '0'),
//    Pair('∴', '0')
// )
//
// fun sanitize(value: String): String = value.lowercase().toCharArray()
//    .map { if (SPECIAL_SLUG_CHARACTERS.get(it) == null) it else SPECIAL_SLUG_CHARACTERS.get(it) }.joinToString("")
//
// fun initializeFolder() {
//    // Check if /tmp/images/ exists ?
//    val directory = File("/tmp/mixit/")
//    if (!directory.exists()) {
//        directory.mkdir()
//    }
// }
//
// fun downloadImage(url: String, filename: String): Boolean {
//    try {
//        val emplacement = File("/tmp/mixit/$filename")
//        if (emplacement.exists()) {
//            emplacement.delete()
//        }
//        emplacement.createNewFile()
//
//        FileOutputStream(emplacement).use {
//            val out = it
//
//            URL(url).openConnection().getInputStream().use {
//                val b = ByteArray(1024)
//                var c: Int = it.read(b)
//                while (c != -1) {
//                    out.write(b, 0, c)
//                    c = it.read(b)
//                }
//            }
//        }
//        return true
//    } catch (e: Exception) {
//        println("Impossible to load image for $filename")
//        e.printStackTrace()
//        return false
//    }
// }
//
// fun toMixitTopic(rawTopic: String): String =
//    when (rawTopic) {
//        "Maker" -> "makers"
//        "Design" -> "design"
//        "Aliens" -> "aliens"
//        "aliens" -> "aliens"
//        "Tech" -> "tech"
//        "Ethic" -> "ethics"
//        "Life style" -> "lifestyle"
//        "Team" -> "team"
//        else -> "other"
//    }
//
// fun toRoom(room: String?): Room =
//    when (room) {
//        "F08 (100 places)" -> Room.ROOM1
//        "F02/F03 (100 places)" -> Room.ROOM2
//        "Amphi 1 (500 places)" -> Room.AMPHI1
//        "SALLE F06 (36 places)" -> Room.ROOM3
//        "SALLE F07 (48 places)" -> Room.ROOM4
//        "SALLE F05  (48 places)" -> Room.ROOM5
//        "Amphi 2 (170 places)" -> Room.AMPHI2
//        else -> Room.UNKNOWN
//    }
