package mixit.web

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test


/**
 * Test {@link SimpleEscapers}
 */
class MustacheEscapersTest {

    val escaper = MustacheEscapers().HTML

    @Test
    fun `should sanitize HTML with images`() {
        val html = "<img src=\"/images/png/planning2017_1.png\" class=\"\" alt=\"planning2017\" />"
        Assertions.assertThat(escaper.escape(html)).isEqualTo(html)

        val htmlWithJs = "<img src=\"/images/png/planning2017_1.png\" onclick=\"callFunction()\" class=\"\" alt=\"planning2017\" />"
        Assertions.assertThat(escaper.escape(htmlWithJs)).isEqualTo(html)
    }

    @Test
    fun `should sanitize HTML with pictures`() {
        val html = "<picture>\n" +
                "<source srcset=\"img/logoWebpUrl.webp\" type=\"image/webp\" alt=\"company\" class=\"mxt-img--sponsors-home\" />\n" +
                "<source srcset=\"img/logoWebpUrl.jpg\" type=\"svg\" alt=\"company\" class=\"mxt-img--sponsors-home\" />\n" +
                "<img src=\"img/logoWebpUrl.jpg\" alt=\"company\" class=\"mxt-img--sponsors-home\" />\n" +
                "</picture>"
        Assertions.assertThat(escaper.escape(html)).isEqualTo(html)
    }

    @Test
    fun `should not integrate script markup`() {
        val html = "<script>\n" +
                "function hello(){} </script>"
        Assertions.assertThat(escaper.escape(html)).isEqualTo("")
    }
}