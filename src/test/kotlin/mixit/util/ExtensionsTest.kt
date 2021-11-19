package mixit.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * Unit tests for Extensions
 */
class ExtensionsTest {

    val encryptionKey = "Bar12345Bar12345"
    val initVector = "RandomInitVector"

    @Test
    fun toSlug() {
        assertEquals("", "".toSlug())
        assertEquals("-", "---".toSlug())
        assertEquals("billetterie-mixit-2017-pre-inscription", "Billetterie MiXiT 2017 : pré-inscription".toSlug())
        assertEquals("mixit-2017-ticketing-pre-registration", "MiXiT 2017 ticketing: pre-registration".toSlug())
    }

    @Test
    fun encodeToMd5() {
        assertEquals("1aedb8d9dc4751e229a335e371db8058", "test@gmail.com".encodeToMd5())
        val nullable: String? = null
        assertNull(nullable?.encodeToMd5())
        val empty = ""
        assertNull(empty.encodeToMd5())
    }

    @Test
    fun encodeToBase64() {
        assertEquals("dGVzdEBnbWFpbC5jb20=", "test@gmail.com".encodeToBase64())
        val nullable: String? = null
        assertNull(nullable?.encodeToBase64())
        val empty = ""
        assertNull(empty.encodeToBase64())
    }

    @Test
    fun decodeFromBase64() {
        assertEquals("test@gmail.com", "dGVzdEBnbWFpbC5jb20=".decodeFromBase64())
        val nullable: String? = null
        assertNull(nullable?.decodeFromBase64())
        val empty = ""
        assertNull(empty.decodeFromBase64())
    }

    @Test
    fun encrypt() {
        assertEquals("lnGW8QagnVzABAjptgMCJg==", "test@gmail.com".encrypt(encryptionKey, initVector))
        val nullable: String? = null
        assertNull(nullable?.encrypt(encryptionKey, initVector))
    }

    @Test
    fun decrypt() {
        assertEquals("test@gmail.com", "lnGW8QagnVzABAjptgMCJg==".decrypt(encryptionKey, initVector))
        val nullable: String? = null
        assertNull(nullable?.decrypt(encryptionKey, initVector))
    }
}
