package mixit

import mixit.model.Talk
import mixit.model.User
import mixit.repository.TalkRepository
import mixit.repository.UserRepository
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.io.File
import java.io.FileOutputStream
import java.net.URL

/**
 * This test class is used to download the speaker image files. We have several usages
 * - check the image sizes when speakers give us a photo URL
 * - download all the images file for Android App
 * Files are downloaded in the directory <i>/tmp/images</i>
 */
@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Disabled
class SpeakerImageLoaderTest(@Autowired val userRepository: UserRepository,
                             @Autowired val talkRepository: TalkRepository) {

    val SPECIAL_SLUG_CHARACTERS = mapOf(Pair('é', 'e'), Pair('è', 'e'), Pair('ï', 'i'), Pair(' ', '_'), Pair('ê', 'e'), Pair('à', 'a'), Pair('-', '_'))


    @Test
    fun loadSpeakerImages() {
        val speakers = talkRepository
                .findByEvent("2018")
                .collectList()
                .flatMap { userRepository.findMany(it.flatMap(Talk::speakerIds)).collectList() }
                .block()
                ?.sortedBy { it.lastname }

        speakers?.forEach {
            val imageUrl = getImageUrl(it)
            val filename = if (it.lastname.isNullOrBlank()) santitize(it.firstname) else santitize(it.lastname)
            if (!imageUrl.isNullOrBlank()) {
                downloadImage(imageUrl!!, filename)
            }
        }

    }

    fun santitize(value: String): String = value.toLowerCase().toCharArray().map { if (SPECIAL_SLUG_CHARACTERS.get(it) == null) it else SPECIAL_SLUG_CHARACTERS.get(it) }.joinToString("")

    fun getImageUrl(user: User): String? {
        System.out.println("user ${user.lastname} -> ${user.photoUrl}")
        if (!user.photoUrl.isNullOrEmpty()) {
            if (user.photoUrl!!.contains("/mixitconf")) {
                return user.photoUrl
            }
            if (user.photoUrl!!.startsWith("/images")) {
                return "https://mixitconf.cleverapps.io${user.photoUrl}"
            }
            return user.photoUrl
        } else
            return "https://www.gravatar.com/avatar/${user.emailHash}?s=400"
    }

    fun downloadImage(url: String, filename: String) {
        // Check if /tmp/images/ exists ?
        val directory = File("/tmp/images/")
        if (!directory.exists()) {
            directory.mkdir()
        }
        val emplacement = File("/tmp/images/${filename}")
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
                System.out.println("Impossible to load image for ${filename}")
                e.printStackTrace()
            }
        }
    }
}