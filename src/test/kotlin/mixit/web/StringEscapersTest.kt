package mixit.web

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test


/**
 * Test {@link SimpleEscapers}
 */
class StringEscapersTest {

    @Nested
    inner class `HTML escaper`{

        val escaper = StringEscapers().HTML

        @Test
        fun `should sanitize HTML with images`() {
            val html = "<img src=\"/images/png/planning_2017_J1_AM.png\" class=\"\" alt=\"planning2017\" />"
            assertThat(escaper.escape(html)).isEqualTo(html)

            val htmlWithJs = "<img src=\"/images/png/planning_2017_J1_AM.png\" onclick=\"callFunction()\" class=\"\" alt=\"planning2017\" />"
            assertThat(escaper.escape(htmlWithJs)).isEqualTo(html)
        }

        @Test
        fun `should sanitize HTML with pictures`() {
            val html = "<picture>\n" +
                    "<source srcset=\"img/logoWebpUrl.webp\" type=\"image/webp\" alt=\"company\" class=\"mxt-img--sponsors-home\" />\n" +
                    "<source srcset=\"img/logoWebpUrl.jpg\" type=\"svg\" alt=\"company\" class=\"mxt-img--sponsors-home\" />\n" +
                    "<img src=\"img/logoWebpUrl.jpg\" alt=\"company\" class=\"mxt-img--sponsors-home\" />\n" +
                    "</picture>"
            assertThat(escaper.escape(html)).isEqualTo(html)
        }

        @Test
        fun `should not integrate script markup`() {
            val html = "<script>\n" +
                    "function hello(){} </script>"
            assertThat(escaper.escape(html)).isEqualTo("")
        }
    }

    @Nested
    inner class `Markdown escaper`{

        val escaper = StringEscapers().MARKDOWN

        @Test
        fun `should change nothing in correct markdown`() {
            val markdown = "Développeur agile passionné par la technique et l&#39;agilité, fondateur de [Dev-Mind](https://dev-mind.fr/)." +
                    "#Vous pouvez voir mon logo" +
                    "![Dev-Mind](https://www.dev-mind.fr/img/logo/logo_1500.png)"

            assertThat(escaper.escape(markdown)).isEqualTo(markdown)
        }

        @Test
        fun `should accept formatting html tags`() {
            val markdown = "Développeur <b>agile</b> <i>passionné</i> par "

            assertThat(escaper.escape(markdown)).isEqualTo(markdown)
        }

        @Test
        fun `should delete dangerous html tags`() {
            val markdown = "Développeur <script>js</script> <i>passionné</i> par "

            assertThat(escaper.escape(markdown)).isEqualTo("Développeur  <i>passionné</i> par ")
        }
    }
}