package mixit.util

import org.junit.Test

import org.junit.Assert.*

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

}