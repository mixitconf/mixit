package mixit.util

import org.commonmark.ext.autolink.AutolinkExtension
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.springframework.stereotype.Component


@Component
class MarkdownConverter {

    private val parser = Parser.builder().extensions(listOf(AutolinkExtension.create())).build()
    private val renderer = HtmlRenderer.builder().build()

    fun toHTML(input: String?): String {
        if (input == null || input.isEmpty()) {
            return ""
        }

        return renderer.render(parser.parse(input))
    }
}
