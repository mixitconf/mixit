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
class SpeakerImageLoaderTest() {

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var talkRepository: TalkRepository

    @Test
    fun loadSpeakerImages() {
        val speakers = talkRepository
                .findByEvent("2018")
                .collectList()
                .flatMap { userRepository.findMany(it.flatMap(Talk::speakerIds)).collectList() }
                .block()

        speakers?.forEach {
            val imageUrl = getImageUrl(it)
            if(!imageUrl.isNullOrBlank()) {
                downloadImage(imageUrl!!, it.lastname.replace(" ", "_").toLowerCase())
            }
        }

    }

    fun getImageUrl(user: User): String? {
        if (user.photoUrl != null)
            return if (user.photoUrl!!.startsWith("/images") || user.photoUrl!!.contains("/mixitconf"))  null else user.photoUrl!!
        else
            return "http://gravatar.com/avatar/${user.emailHash}?s=400"
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

        FileOutputStream(emplacement).use{
            val out = it

            try{
                URL(url).openConnection().getInputStream().use{
                    val b = ByteArray(1024)
                    var c: Int = it.read(b)
                    while (c != -1) {
                        out.write(b, 0, c)
                        c = it.read(b)
                    }
                }
            }
            catch (e:Exception){
                System.out.println("Impossible to load image for ${filename}")
                e.printStackTrace()
            }
        }
    }
}