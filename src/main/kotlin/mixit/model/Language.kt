package mixit.model

enum class Language {
    FRENCH,
    ENGLISH;

    fun toLanguageTag() {
        name.toLowerCase().subSequence(0, 2)
    }

    companion object {
        fun findByTag(name: String): Language {
            val language = Language.values().filter { value -> value.name.toLowerCase().substring(0, 2) == name }
            if (language.isEmpty()) {
                throw IllegalStateException()
            }
            return language.first()
        }
    }
}