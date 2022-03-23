package mixit.import

import com.fasterxml.jackson.databind.ObjectMapper
import mixit.event.handler.AdminEventHandler
import mixit.security.model.Cryptographer
import mixit.talk.model.TalkService
import mixit.user.repository.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.ClassPathResource
import java.io.File
import java.io.FileOutputStream
import java.net.URL


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SpeakerImageDownloader(
    @Autowired val objectMapper: ObjectMapper,
    @Autowired val userRepository: UserRepository,
    @Autowired val talkService: TalkService,
    @Autowired val cryptographer: Cryptographer
) {

    // @Test
    fun `load speakers`() {

        initializeFolder()

        val papercallExports =
            objectMapper.readValue<List<PapercallExport>>(ClassPathResource("import/2022-papercall-export.json").inputStream)

        talkService.initializeCache()
        val talks = talkService.findAll().block()!!

        talks
            .filter { it.event == AdminEventHandler.CURRENT_EVENT }
            .forEach { talk ->
                val sub = papercallExports.first { it.talk!!.title == talk.title }

                // Speakers
                val speakerGravatar = (listOf(sub.profile) + sub.co_presenter_profiles)
                speakerGravatar.forEach { profile ->
                    val speaker = talk.speakers.firstOrNull { it.email == cryptographer.encrypt(profile.email) }
                    if (speaker == null) {
                        println("Pas trouvé ${profile.name}")
                    } else {
                        println("Trouvé ${profile.name}")
                        downloadImage(profile.avatar!!, speaker.login + ".jpg")
                    }
                }
            }
    }

    fun initializeFolder() {
        // Check if /tmp/images/ exists ?
        val directory = File("/tmp/mixit/2022")
        if (!directory.exists()) {
            directory.mkdir()
        }
    }

    fun downloadImage(url: String, filename: String) {
        val emplacement = File("/tmp/mixit/2022/$filename")
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
}


