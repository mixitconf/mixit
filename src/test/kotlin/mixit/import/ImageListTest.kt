package mixit.import

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.name
import kotlin.io.path.pathString

fun createSection(section: Path): String {
    val name = section.pathString.split("/").last()
    return """
        {
            "sectionId": "$name",
            "i18n": "images.$name",
            "images": [${createSectionImages(section).joinToString(",")}]
        }
    """.trimIndent()
}

fun createSectionImages(section: Path): List<String> =
    Files.walk(section)
        .filter { Files.isRegularFile(it) }
        .map { it.pathString.split("/").last() }
        .map { """
            {
                "name": "$it",
                "talkId": null,
                "mustacheTemplate": null
            }
        """.trimIndent()
        }
        .toList()

fun main() {
    val rootPath = Paths.get("/home/devmind/Workspace/pictures/mixiconf-images-two/2024/")
    // list all files in the directory
    val sections = Files.walk(rootPath)
        .filter { it.pathString.contains("/2024/")}
        .filter { Files.isDirectory(it) }

    println(sections.map { it.name }.toList().joinToString())

//    println("""
//        {
//            "event": "2024",
//            "sections": [${sections.map { createSection(it) }.toList().joinToString(",")}],
//            "rootUrl": "https://mixitconf.org"
//        }
//    """.trimIndent())
}


