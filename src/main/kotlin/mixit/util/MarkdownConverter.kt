package mixit.util

import org.commonmark.ext.autolink.AutolinkExtension
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.springframework.stereotype.Component


@Component
class MarkdownConverter {

    private val parser by lazy { Parser.builder().extensions(listOf(AutolinkExtension.create())).build() }
    private val renderer by lazy { HtmlRenderer.builder().build() }

    fun toHTML(input: String?): String = if (input == null || input.isEmpty()) "" else renderer.render(parser.parse(input))

}
