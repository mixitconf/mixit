package mixit.util

import com.fasterxml.jackson.databind.ObjectMapper
import mixit.event.model.EventImage
import mixit.event.model.EventImagesSection


fun main() {
    // Parse folders /home/devmind/Download/Mixit/files on the system file to display all sub folders in log
    val folderPath = "/home/devmind/Workspace/pictures/mixiconf-images-two/2025"
    val objectMapper = ObjectMapper()
    val folder = java.io.File(folderPath)
    val sections: List<EventImagesSection> = if (folder.exists() && folder.isDirectory) {
        folder.listFiles()?.filter { it.isDirectory }?.map { subFolder ->
            println("images.mixit24.${subFolder.name}=")
            EventImagesSection(
                sectionId = subFolder.name,
                i18n = subFolder.name.replace("_", " ").replace("-", " "),
                images = subFolder.listFiles()?.filter { it.isFile && it.extension in listOf("jpg", "png") }
                    ?.map { file ->
                        EventImage(
                            name = file.name,
                            talkId = null, // Assuming no talkId is provided
                            mustacheTemplate = null // Assuming no mustache template is provided
                        )
                    } ?: emptyList()
            )
        } ?: emptyList()
    } else {
        emptyList()
    }

    val json = """
        [
            {
                 "event": "2025",
                 "sections": ${objectMapper.writeValueAsString(sections)},
                 "rootUrl": "https://raw.githubusercontent.com/mixitconf/mixitconf-images-two/main/"
            }
        ]
    """.trimIndent()

    //println(json)
//    val file = java.io.File(folderPath)
//    if (file.exists() && file.isDirectory) {
//        file.list().forEach {
//            println(it)
//        }
//        file.walk().forEach { child ->
//            if (child.isFile) {
//                println(child.absolutePath)
//            }
//        }
//    } else {
//        println("The specified path is not a directory or does not exist.")
//    }


}

