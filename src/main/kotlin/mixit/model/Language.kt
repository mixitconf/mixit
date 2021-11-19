package mixit.model

enum class Language {
    FRENCH,
    ENGLISH;

    fun toLanguageTag(): String = name.lowercase().subSequence(0, 2).toString()

    companion object {
        fun findByTag(name: String): Language {
            val language = values().filter { value -> value.toLanguageTag() == name }
            if (language.isEmpty()) {
                throw IllegalArgumentException()
            }
            return language.first()
        }
    }
}
