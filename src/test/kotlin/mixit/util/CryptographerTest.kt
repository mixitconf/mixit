package mixit.util

import io.mockk.junit5.MockKExtension
import mixit.MixitProperties
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class CryptographerTest {

    /**
     * For this test we can use the real object
     */
    val service: Cryptographer = Cryptographer(MixitProperties().apply {
        aes.key = "myKeyIsTheBest01"
        aes.initvector = "myvectorIsBetter"
    })

    @Test
    fun `should encrypt and decrypt a String`() {
        val stringToEncode = "MiXiT is the best conf in the world"
        val encodedString = service.encrypt(stringToEncode)

        Assertions.assertThat(encodedString).isNotEmpty()
        Assertions.assertThat(service.decrypt(encodedString)).isEqualTo(stringToEncode)
    }

    @Test
    fun `encrypt and decrypt functions are null safe`() {
        Assertions.assertThat(service.encrypt(null)).isNull()
        Assertions.assertThat(service.decrypt(null)).isNull()
    }
}

