package mixit.util

import mixit.MixitProperties

/**
 * @author Dev-Mind <guillaume@dev-mind.fr>
 * @since 15/10/17.
 */
class Cryptographer(private val properties: MixitProperties) {

    fun encrypt(value: String?): String? = value?.encrypt(properties.aes.key, properties.aes.initvector)

    fun decrypt(value: String?): String? = value?.decrypt(properties.aes.key, properties.aes.initvector)

}