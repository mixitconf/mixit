package mixit.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * Unit tests for Extensions
 */
class ExtensionsTest {

    @Test
    fun toSlug() {
        assertEquals("", "".toSlug())
        assertEquals("-", "---".toSlug())
        assertEquals("billetterie-mixit-2017-pre-inscription", "Billetterie MiXiT 2017 : pré-inscription".toSlug())
        assertEquals("mixit-2017-ticketing-pre-registration", "MiXiT 2017 ticketing: pre-registration".toSlug())
    }

    @Test
    fun encodeToMd5() {
        assertEquals("aa4d47d1016e45c23b6af05ec11c0a9c", "gui.ehret@gmail.com".encodeToMd5())
        val nullable:String? = null
        assertNull(nullable?.encodeToMd5())
        val empty = ""
        assertNull(empty.encodeToMd5())
    }

    @Test
    fun encodeToBase64() {
        assertEquals("Z3VpLmVocmV0QGdtYWlsLmNvbQ==", "gui.ehret@gmail.com".encodeToBase64())
        val nullable:String? = null
        assertNull(nullable?.encodeToBase64())
        val empty = ""
        assertNull(empty.encodeToBase64())
    }

    @Test
    fun decodeFromBase64() {
        assertEquals("gui.ehret@gmail.com", "Z3VpLmVocmV0QGdtYWlsLmNvbQ==".decodeFromBase64())
        val nullable:String? = null
        assertNull(nullable?.decodeFromBase64())
        val empty = ""
        assertNull(empty.decodeFromBase64())
    }
}