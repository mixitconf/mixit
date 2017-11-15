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
    fun md5() {
        assertEquals("aa4d47d1016e45c23b6af05ec11c0a9c", "gui.ehret@gmail.com".md5Hex())
        val nullable:String? = null
        assertNull(nullable?.md5Hex())
        val empty = ""
        assertNull(empty.md5Hex())
    }
}